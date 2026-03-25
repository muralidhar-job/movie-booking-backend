package com.movieplatform.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "booking_seats")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "seat_layout_id", nullable = false)
    private UUID seatLayoutId;

    @Column(name = "seat_label")  // e.g. "A1", "B5"
    private String seatLabel;

    @Column(name = "seat_category")
    private String seatCategory;

    @Column(name = "seat_price", precision = 10, scale = 2)
    private BigDecimal seatPrice;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SeatStatus status = SeatStatus.LOCKED;

    public enum SeatStatus { LOCKED, CONFIRMED, RELEASED }
}
