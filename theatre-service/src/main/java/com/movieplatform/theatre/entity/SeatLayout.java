package com.movieplatform.theatre.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "seat_layout", indexes = {
    @Index(name = "idx_seat_screen", columnList = "screen_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SeatLayout {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @Column(name = "row_label", nullable = false)
    private String rowLabel;       // A, B, C ...

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;        // 1, 2, 3 ...

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_category", nullable = false)
    @Builder.Default
    private SeatCategory seatCategory = SeatCategory.REGULAR;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    public enum SeatCategory { REGULAR, PREMIUM, RECLINER, COUPLE }
}
