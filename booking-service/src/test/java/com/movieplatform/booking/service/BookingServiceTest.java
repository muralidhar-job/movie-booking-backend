package com.movieplatform.booking.service;

import com.movieplatform.booking.dto.BookingDtos.BookingRequest;
import com.movieplatform.booking.entity.Booking;
import com.movieplatform.booking.entity.BookingSeat;
import com.movieplatform.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * BookingService unit tests — JUnit 5 + Mockito.
 *
 * Covers:
 *  1. Happy path — booking created, seats locked, Kafka event published
 *  2. Seat conflict — lock fails → SeatAlreadyLockedException
 *  3. Idempotency — duplicate booking rejected
 *  4. Payment success saga — booking confirmed, notification published
 *  5. Payment failure saga — seats released, booking marked FAILED
 *  6. Cancellation — refund initiated, seats released
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService unit tests")
class BookingServiceTest {

    @Mock private BookingRepository  bookingRepo;
    @Mock private SeatLockService    seatLockService;
    @Mock private OfferClientService offerClient;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private BookingService bookingService;

    private UUID userId;
    private UUID showId;
    private List<UUID> seatIds;
    private BookingRequest request;
    private static final String CORRELATION_ID = "test-corr-123";

    @BeforeEach
    void setUp() {
        userId  = UUID.randomUUID();
        showId  = UUID.randomUUID();
        seatIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        request = new BookingRequest();
        request.setShowId(showId);
        request.setSeatLayoutIds(seatIds);
        request.setOfferCode("THIRD50");
        request.setIdempotencyKey(null); // auto-generated
    }

    // ── 1. Happy path ────────────────────────────────────────────────────

    @Nested
    @DisplayName("createBooking — happy path")
    class CreateBookingHappyPath {

        @Test
        @DisplayName("should lock seats, persist booking, and publish BOOKING_INITIATED event")
        void shouldCreateBookingSuccessfully() {
            // Arrange
            given(bookingRepo.findByIdempotencyKey(anyString())).willReturn(Optional.empty());
            given(seatLockService.lockSeats(eq(seatIds), eq(showId), any(UUID.class))).willReturn(true);
            given(offerClient.calculateDiscount("THIRD50", 3, showId)).willReturn(new BigDecimal("125.00"));

            Booking savedBooking = buildPendingBooking();
            given(bookingRepo.save(any(Booking.class))).willReturn(savedBooking);

            // Act
            Booking result = bookingService.createBooking(userId, request, CORRELATION_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.PENDING_PAYMENT);
            assertThat(result.getUserId()).isEqualTo(userId);

            // Verify seat lock was called with correct args
            then(seatLockService).should(times(1))
                .lockSeats(eq(seatIds), eq(showId), any(UUID.class));

            // Verify booking was persisted
            ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
            then(bookingRepo).should(times(1)).save(bookingCaptor.capture());
            Booking captured = bookingCaptor.getValue();
            assertThat(captured.getOfferCode()).isEqualTo("THIRD50");
            assertThat(captured.getExpiresAt()).isAfter(LocalDateTime.now().minusSeconds(1));

            // Verify Kafka event published
            then(kafkaTemplate).should(times(1))
                .send(eq("booking-events"), anyString(), any());
        }

        @Test
        @DisplayName("should apply THIRD50 discount — 50% off 3rd ticket")
        void shouldApplyThirdTicketDiscount() {
            given(bookingRepo.findByIdempotencyKey(anyString())).willReturn(Optional.empty());
            given(seatLockService.lockSeats(any(), any(), any())).willReturn(true);
            given(offerClient.calculateDiscount("THIRD50", 3, showId))
                .willReturn(new BigDecimal("125.00")); // 50% of ₹250

            Booking savedBooking = buildPendingBooking();
            savedBooking.setDiscountApplied(new BigDecimal("125.00"));
            savedBooking.setTotalAmount(new BigDecimal("625.00")); // 750 - 125
            given(bookingRepo.save(any())).willReturn(savedBooking);

            Booking result = bookingService.createBooking(userId, request, CORRELATION_ID);

            assertThat(result.getDiscountApplied()).isEqualByComparingTo("125.00");
            assertThat(result.getTotalAmount()).isEqualByComparingTo("625.00");
        }
    }

    // ── 2. Seat conflict ─────────────────────────────────────────────────

    @Nested
    @DisplayName("createBooking — seat conflicts")
    class SeatConflicts {

        @Test
        @DisplayName("should throw SeatAlreadyLockedException when seats are taken")
        void shouldThrowWhenSeatsLocked() {
            given(bookingRepo.findByIdempotencyKey(anyString())).willReturn(Optional.empty());
            given(seatLockService.lockSeats(eq(seatIds), eq(showId), any())).willReturn(false);

            assertThatThrownBy(() ->
                bookingService.createBooking(userId, request, CORRELATION_ID))
                .isInstanceOf(BookingService.SeatAlreadyLockedException.class)
                .hasMessageContaining("already booked");

            // Booking must NOT be persisted if seat lock fails
            then(bookingRepo).should(never()).save(any());
            then(kafkaTemplate).should(never()).send(any(), any(), any());
        }
    }

    // ── 3. Idempotency ───────────────────────────────────────────────────

    @Nested
    @DisplayName("createBooking — idempotency")
    class Idempotency {

        @Test
        @DisplayName("should reject duplicate booking with same idempotency key")
        void shouldRejectDuplicateBooking() {
            Booking existingBooking = buildPendingBooking();
            given(bookingRepo.findByIdempotencyKey(anyString()))
                .willReturn(Optional.of(existingBooking));

            assertThatThrownBy(() ->
                bookingService.createBooking(userId, request, CORRELATION_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Duplicate booking");

            then(seatLockService).should(never()).lockSeats(any(), any(), any());
        }
    }

    // ── 4. Payment success saga ──────────────────────────────────────────

    @Nested
    @DisplayName("saga — payment success")
    class PaymentSuccessSaga {

        @Test
        @DisplayName("should confirm booking and publish BOOKING_CONFIRMED notification")
        void shouldConfirmBookingOnPaymentSuccess() {
            Booking pending = buildPendingBooking();
            given(bookingRepo.findById(pending.getId())).willReturn(Optional.of(pending));
            given(bookingRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            com.movieplatform.booking.event.BookingEvents.PaymentSuccessEvent successEvent =
                com.movieplatform.booking.event.BookingEvents.PaymentSuccessEvent.builder()
                    .bookingId(pending.getId())
                    .paymentId(UUID.randomUUID())
                    .transactionId("pi_test_123")
                    .amount(new BigDecimal("750.00"))
                    .correlationId(CORRELATION_ID)
                    .build();

            bookingService.handlePaymentEvent(successEvent);

            // Booking status must be CONFIRMED
            ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
            then(bookingRepo).should(times(1)).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(Booking.BookingStatus.CONFIRMED);

            // Notification event published
            then(kafkaTemplate).should(times(1))
                .send(eq("notification-events"), anyString(), any());
        }
    }

    // ── 5. Payment failure saga ──────────────────────────────────────────

    @Nested
    @DisplayName("saga — payment failure (compensating transaction)")
    class PaymentFailureSaga {

        @Test
        @DisplayName("should release seat locks and mark booking FAILED on payment failure")
        void shouldCompensateOnPaymentFailure() {
            Booking pending = buildPendingBooking();
            given(bookingRepo.findById(pending.getId())).willReturn(Optional.of(pending));
            given(bookingRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            com.movieplatform.booking.event.BookingEvents.PaymentFailedEvent failedEvent =
                com.movieplatform.booking.event.BookingEvents.PaymentFailedEvent.builder()
                    .bookingId(pending.getId())
                    .reason("Insufficient funds")
                    .correlationId(CORRELATION_ID)
                    .build();

            bookingService.handlePaymentEvent(failedEvent);

            // Seat locks MUST be released — compensating transaction
            then(seatLockService).should(times(1))
                .releaseSeats(anyList(), eq(showId));

            // Booking marked FAILED
            ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
            then(bookingRepo).should(times(1)).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(Booking.BookingStatus.FAILED);

            // Failure notification published
            then(kafkaTemplate).should(times(1))
                .send(eq("notification-events"), anyString(), any());
        }
    }

    // ── 6. Cancellation ──────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelBooking")
    class CancelBooking {

        @Test
        @DisplayName("should initiate refund and release seat locks on cancellation")
        void shouldCancelAndReleaseSeats() {
            Booking confirmed = buildConfirmedBooking();
            given(bookingRepo.findById(confirmed.getId())).willReturn(Optional.of(confirmed));
            given(bookingRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            Booking result = bookingService.cancelBooking(confirmed.getId(), userId);

            assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.REFUND_INITIATED);
            then(seatLockService).should(times(1)).releaseSeats(anyList(), eq(showId));
            then(kafkaTemplate).should(times(1))
                .send(eq("booking-events"), anyString(), any());
        }

        @Test
        @DisplayName("should reject cancellation if booking belongs to different user")
        void shouldRejectCancellationForWrongUser() {
            Booking confirmed = buildConfirmedBooking();
            UUID otherUser = UUID.randomUUID();
            given(bookingRepo.findById(confirmed.getId())).willReturn(Optional.of(confirmed));

            assertThatThrownBy(() ->
                bookingService.cancelBooking(confirmed.getId(), otherUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does not belong to user");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Booking buildPendingBooking() {
        List<BookingSeat> seats = seatIds.stream().map(id ->
            BookingSeat.builder()
                .id(UUID.randomUUID())
                .seatLayoutId(id)
                .seatLabel("A1")
                .seatPrice(new BigDecimal("250.00"))
                .status(BookingSeat.SeatStatus.LOCKED)
                .build()
        ).toList();

        Booking booking = Booking.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .showId(showId)
            .status(Booking.BookingStatus.PENDING_PAYMENT)
            .totalAmount(new BigDecimal("750.00"))
            .discountApplied(BigDecimal.ZERO)
            .offerCode("THIRD50")
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .seats(seats)
            .build();

        seats.forEach(s -> s.setBooking(booking));
        return booking;
    }

    private Booking buildConfirmedBooking() {
        Booking b = buildPendingBooking();
        b.setStatus(Booking.BookingStatus.CONFIRMED);
        return b;
    }
}
