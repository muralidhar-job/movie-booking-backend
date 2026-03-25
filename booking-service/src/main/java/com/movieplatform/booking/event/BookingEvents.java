package com.movieplatform.booking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Kafka event DTOs for the booking saga choreography.
 * Design Pattern: Observer (Kafka pub/sub).
 *
 * Topics:
 *   booking-events      → BOOKING_INITIATED, BOOKING_CONFIRMED, BOOKING_CANCELLED
 *   payment-events      → PAYMENT_SUCCESS, PAYMENT_FAILED
 *   notification-events → consumed by notification-service
 */
public class BookingEvents {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BookingInitiatedEvent {
        private UUID bookingId;
        private UUID userId;
        private UUID showId;
        private List<String> seatIds;
        private BigDecimal totalAmount;
        private String offerCode;
        private LocalDateTime expiresAt;
        private String correlationId;
        private LocalDateTime eventTime = LocalDateTime.now();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BookingConfirmedEvent {
        private UUID bookingId;
        private UUID userId;
        private String userEmail;
        private String movieTitle;
        private String theatreName;
        private LocalDateTime showTime;
        private List<String> seatLabels;
        private BigDecimal totalAmount;
        private String correlationId;
        private LocalDateTime eventTime = LocalDateTime.now();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BookingCancelledEvent {
        private UUID bookingId;
        private UUID userId;
        private UUID showId;
        private List<String> seatIds;
        private String reason;
        private BigDecimal refundAmount;
        private String correlationId;
        private LocalDateTime eventTime = LocalDateTime.now();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentSuccessEvent {
        private UUID bookingId;
        private UUID paymentId;
        private String transactionId;
        private BigDecimal amount;
        private String correlationId;
        private LocalDateTime eventTime = LocalDateTime.now();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentFailedEvent {
        private UUID bookingId;
        private String reason;
        private String correlationId;
        private LocalDateTime eventTime = LocalDateTime.now();
    }
}
