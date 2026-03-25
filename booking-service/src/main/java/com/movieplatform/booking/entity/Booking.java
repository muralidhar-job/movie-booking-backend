package com.movieplatform.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Booking entity — central aggregate for the booking saga.
 * Status lifecycle: PENDING_PAYMENT → CONFIRMED | FAILED | CANCELLED
 */
@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_booking_user",   columnList = "user_id"),
    @Index(name = "idx_booking_show",   columnList = "show_id"),
    @Index(name = "idx_booking_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "show_id", nullable = false)
    private UUID showId;

    @Column(name = "theatre_id")
    private UUID theatreId;

    @Column(name = "movie_title")
    private String movieTitle;

    @Column(name = "theatre_name")
    private String theatreName;

    @Column(name = "show_time")
    private LocalDateTime showTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING_PAYMENT;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_applied", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountApplied = BigDecimal.ZERO;

    @Column(name = "offer_code")
    private String offerCode;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;  // prevents duplicate bookings

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // payment window — 10 min

    @CreationTimestamp
    @Column(name = "booked_at", updatable = false)
    private LocalDateTime bookedAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<BookingSeat> seats;

    public enum BookingStatus {
        PENDING_PAYMENT, CONFIRMED, FAILED, CANCELLED, REFUND_INITIATED, REFUNDED
    }
}
