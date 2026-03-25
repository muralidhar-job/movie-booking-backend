package com.movieplatform.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment entity — one-to-one with Booking.
 * Tracks payment gateway transaction lifecycle.
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_booking",     columnList = "booking_id"),
    @Index(name = "idx_payment_transaction", columnList = "transaction_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Gateway gateway = Gateway.STRIPE;

    @Column(name = "transaction_id")
    private String transactionId;   // Stripe PaymentIntent ID

    @Column(name = "client_secret")
    private String clientSecret;    // returned to frontend for Stripe.js

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;  // prevents double charge on retry

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Gateway { STRIPE, RAZORPAY, MOCK }

    public enum PaymentStatus {
        INITIATED, PROCESSING, SUCCESS, FAILED, REFUND_INITIATED, REFUNDED
    }
}
