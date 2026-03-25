package com.movieplatform.theatre.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Show entity owned by theatre-service.
 * Write scenario: "Theatres can create, update, and delete shows for the day."
 */
@Entity
@Table(name = "theatre_shows", indexes = {
    @Index(name = "idx_tshow_theatre",  columnList = "theatre_id"),
    @Index(name = "idx_tshow_datetime", columnList = "show_time")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TheatreShow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "theatre_id", nullable = false)
    private UUID theatreId;

    @Column(name = "screen_id", nullable = false)
    private UUID screenId;

    @Column(name = "movie_id", nullable = false)
    private UUID movieId;

    @Column(name = "movie_title")
    private String movieTitle;

    @Column(name = "show_time", nullable = false)
    private LocalDateTime showTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "show_type")
    @Builder.Default
    private ShowType showType = ShowType.REGULAR;

    @Column(name = "price_multiplier", precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal priceMultiplier = BigDecimal.ONE;

    @Column(name = "total_seats")
    private int totalSeats;

    @Column(name = "available_seats")
    private int availableSeats;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ShowType { REGULAR, IMAX, FOUR_DX, DOLBY }

    @Version
    private Long version; // optimistic locking for seat availability updates
}
