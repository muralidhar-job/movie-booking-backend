package com.movieplatform.booking.service;

import com.movieplatform.booking.dto.BookingDtos.*;
import com.movieplatform.booking.entity.Booking;
import com.movieplatform.booking.entity.BookingSeat;
import com.movieplatform.booking.event.BookingEvents.*;
import com.movieplatform.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Booking Service — implements the Saga choreography pattern.
 *
 * WRITE SCENARIO: "Book movie tickets by selecting a theatre, timing,
 * and preferred seats for the day."
 *
 * SAGA FLOW:
 *   1. Lock seats in Redis (SETNX, 10-min TTL)
 *   2. Persist Booking (PENDING_PAYMENT)
 *   3. Publish BOOKING_INITIATED → Kafka
 *   4. [payment-service processes payment]
 *   5a. On PAYMENT_SUCCESS → mark CONFIRMED, publish BOOKING_CONFIRMED
 *   5b. On PAYMENT_FAILED  → compensate: release seats, mark FAILED
 *
 * Design Patterns: Saga, Observer (Kafka), Builder, Idempotency.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository  bookingRepo;
    private final SeatLockService    seatLockService;
    private final OfferClientService offerClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_BOOKING = "booking-events";
    private static final String TOPIC_NOTIFY  = "notification-events";

    // ── WRITE SCENARIO: Create booking ──────────────────────────────────

    @Transactional
    public Booking createBooking(UUID userId, BookingRequest request, String correlationId) {
        log.info("Create booking: userId={} showId={} seats={} correlationId={}",
            userId, request.getShowId(), request.getSeatLayoutIds().size(), correlationId);

        // 1. Idempotency check — prevent duplicate booking
        String idempKey = request.getIdempotencyKey() != null
            ? request.getIdempotencyKey()
            : userId + ":" + request.getShowId() + ":" + request.getSeatLayoutIds().hashCode();

        bookingRepo.findByIdempotencyKey(idempKey).ifPresent(existing -> {
            throw new RuntimeException("Duplicate booking detected. Existing bookingId: " + existing.getId());
        });

        // 2. Lock seats in Redis (SETNX)
        UUID tempBookingId = UUID.randomUUID();
        boolean locked = seatLockService.lockSeats(
            request.getSeatLayoutIds(), request.getShowId(), tempBookingId);

        if (!locked) {
            throw new SeatAlreadyLockedException(
                "One or more selected seats are already booked. Please choose different seats.");
        }

        // 3. Calculate pricing via offer service
        BigDecimal baseTotal    = calculateBaseTotal(request.getSeatLayoutIds());
        BigDecimal discount     = offerClient.calculateDiscount(
            request.getOfferCode(), request.getSeatLayoutIds().size(), request.getShowId());
        BigDecimal finalAmount  = baseTotal.subtract(discount);

        // 4. Build seat records
        List<BookingSeat> bookingSeats = request.getSeatLayoutIds().stream()
            .map(seatId -> BookingSeat.builder()
                .seatLayoutId(seatId)
                .seatLabel(seatId.toString().substring(0, 4).toUpperCase()) // placeholder
                .status(BookingSeat.SeatStatus.LOCKED)
                .seatPrice(baseTotal.divide(BigDecimal.valueOf(request.getSeatLayoutIds().size()), 2, java.math.RoundingMode.HALF_UP))
                .build())
            .collect(Collectors.toList());

        // 5. Persist booking
        Booking booking = Booking.builder()
            .id(tempBookingId)
            .userId(userId)
            .showId(request.getShowId())
            .status(Booking.BookingStatus.PENDING_PAYMENT)
            .totalAmount(finalAmount)
            .discountApplied(discount)
            .offerCode(request.getOfferCode())
            .idempotencyKey(idempKey)
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .seats(bookingSeats)
            .build();

        bookingSeats.forEach(s -> s.setBooking(booking));
        Booking saved = bookingRepo.save(booking);

        // 6. Publish BOOKING_INITIATED event → Kafka (triggers payment-service)
        BookingInitiatedEvent event = BookingInitiatedEvent.builder()
            .bookingId(saved.getId())
            .userId(userId)
            .showId(request.getShowId())
            .seatIds(request.getSeatLayoutIds().stream().map(UUID::toString).toList())
            .totalAmount(finalAmount)
            .offerCode(request.getOfferCode())
            .expiresAt(saved.getExpiresAt())
            .correlationId(correlationId)
            .build();

        kafkaTemplate.send(TOPIC_BOOKING, saved.getId().toString(), event);
        log.info("Published BOOKING_INITIATED: bookingId={} amount={}", saved.getId(), finalAmount);

        return saved;
    }

    // ── SAGA COMPENSATION: Listen for payment results ────────────────────

    @KafkaListener(topics = "payment-events", groupId = "booking-service-group")
    @Transactional
    public void handlePaymentEvent(Object rawEvent) {
        // Kafka sends either PaymentSuccessEvent or PaymentFailedEvent
        if (rawEvent instanceof PaymentSuccessEvent event) {
            onPaymentSuccess(event);
        } else if (rawEvent instanceof PaymentFailedEvent event) {
            onPaymentFailed(event);
        }
    }

    private void onPaymentSuccess(PaymentSuccessEvent event) {
        log.info("Payment success received: bookingId={} txnId={}",
            event.getBookingId(), event.getTransactionId());

        Booking booking = getBookingById(event.getBookingId());
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.getSeats().forEach(s -> s.setStatus(BookingSeat.SeatStatus.CONFIRMED));
        bookingRepo.save(booking);

        // Publish confirmation event → notification-service sends email/SMS
        BookingConfirmedEvent confirmEvent = BookingConfirmedEvent.builder()
            .bookingId(booking.getId())
            .userId(booking.getUserId())
            .movieTitle(booking.getMovieTitle())
            .theatreName(booking.getTheatreName())
            .showTime(booking.getShowTime())
            .seatLabels(booking.getSeats().stream().map(BookingSeat::getSeatLabel).toList())
            .totalAmount(booking.getTotalAmount())
            .correlationId(event.getCorrelationId())
            .build();

        kafkaTemplate.send(TOPIC_NOTIFY, booking.getId().toString(), confirmEvent);
        log.info("Booking CONFIRMED: bookingId={}", booking.getId());
    }

    private void onPaymentFailed(PaymentFailedEvent event) {
        log.warn("Payment failed: bookingId={} reason={}", event.getBookingId(), event.getReason());

        Booking booking = getBookingById(event.getBookingId());

        // COMPENSATING TRANSACTION: release Redis seat locks
        List<UUID> seatIds = booking.getSeats().stream()
            .map(BookingSeat::getSeatLayoutId).toList();
        seatLockService.releaseSeats(seatIds, booking.getShowId());

        // Update booking status
        booking.setStatus(Booking.BookingStatus.FAILED);
        booking.getSeats().forEach(s -> s.setStatus(BookingSeat.SeatStatus.RELEASED));
        bookingRepo.save(booking);

        // Publish cancellation event → notification-service sends failure email
        BookingCancelledEvent cancelEvent = BookingCancelledEvent.builder()
            .bookingId(booking.getId())
            .userId(booking.getUserId())
            .showId(booking.getShowId())
            .seatIds(seatIds.stream().map(UUID::toString).toList())
            .reason(event.getReason())
            .refundAmount(BigDecimal.ZERO)
            .correlationId(event.getCorrelationId())
            .build();

        kafkaTemplate.send(TOPIC_NOTIFY, booking.getId().toString(), cancelEvent);
        log.info("Booking FAILED and seats released: bookingId={}", booking.getId());
    }

    // ── WRITE SCENARIO: Cancel booking ──────────────────────────────────

    @Transactional
    public Booking cancelBooking(UUID bookingId, UUID userId) {
        Booking booking = getBookingById(bookingId);

        if (!booking.getUserId().equals(userId)) {
            throw new RuntimeException("Booking does not belong to user");
        }
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking already cancelled");
        }

        // Release Redis locks
        List<UUID> seatIds = booking.getSeats().stream()
            .map(BookingSeat::getSeatLayoutId).toList();
        seatLockService.releaseSeats(seatIds, booking.getShowId());

        booking.setStatus(Booking.BookingStatus.REFUND_INITIATED);
        Booking saved = bookingRepo.save(booking);

        // Publish cancellation → payment-service triggers refund
        BookingCancelledEvent event = BookingCancelledEvent.builder()
            .bookingId(bookingId)
            .userId(userId)
            .showId(booking.getShowId())
            .seatIds(seatIds.stream().map(UUID::toString).toList())
            .reason("CUSTOMER_CANCELLED")
            .refundAmount(booking.getTotalAmount())
            .build();

        kafkaTemplate.send(TOPIC_BOOKING, bookingId.toString(), event);
        log.info("Booking cancellation initiated: bookingId={}", bookingId);
        return saved;
    }

    // ── WRITE SCENARIO: Bulk booking ────────────────────────────────────

    @Transactional
    public List<Booking> bulkBooking(UUID requestingUserId, BulkBookingRequest request, String correlationId) {
        log.info("Bulk booking: showId={} groups={}", request.getShowId(), request.getBookings().size());
        List<Booking> results = new ArrayList<>();
        for (BulkBookingRequest.SingleBooking single : request.getBookings()) {
            BookingRequest br = new BookingRequest();
            br.setShowId(request.getShowId());
            br.setSeatLayoutIds(single.getSeatLayoutIds());
            results.add(createBooking(single.getUserId(), br, correlationId));
        }
        return results;
    }

    // ── Queries ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Booking getBookingById(UUID bookingId) {
        return bookingRepo.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
    }

    @Transactional(readOnly = true)
    public Page<Booking> getMyBookings(UUID userId, String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        if (status != null) {
            return bookingRepo.findByUserIdAndStatus(
                userId, Booking.BookingStatus.valueOf(status.toUpperCase()), pageable);
        }
        return bookingRepo.findByUserId(userId, pageable);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private BigDecimal calculateBaseTotal(List<UUID> seatLayoutIds) {
        // In production: call theatre-service for actual seat prices
        // For demo: use fixed prices per seat
        return BigDecimal.valueOf(250).multiply(BigDecimal.valueOf(seatLayoutIds.size()));
    }

    public static class SeatAlreadyLockedException extends RuntimeException {
        public SeatAlreadyLockedException(String msg) { super(msg); }
    }
}
