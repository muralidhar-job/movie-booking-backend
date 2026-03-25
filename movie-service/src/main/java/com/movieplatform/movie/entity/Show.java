package com.movieplatform.movie.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shows", indexes = {
    @Index(name = "idx_shows_movie",    columnList = "movie_id"),
    @Index(name = "idx_shows_screen",   columnList = "screen_id"),
    @Index(name = "idx_shows_datetime", columnList = "show_time")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(name = "screen_id", nullable = false)
    private UUID screenId;

    @Column(name = "theatre_id", nullable = false)
    private UUID theatreId;

    @Column(name = "theatre_name")
    private String theatreName;

    private String city;

    @Column(name = "show_time", nullable = false)
    private LocalDateTime showTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "show_type")
    @Builder.Default
    private ShowType showType = ShowType.REGULAR;

    @Column(name = "price_multiplier", precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal priceMultiplier = BigDecimal.ONE;

    @Column(name = "available_seats")
    private int availableSeats;

    @Column(name = "total_seats")
    private int totalSeats;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    public enum ShowType { REGULAR, IMAX, FOUR_DX, DOLBY }

    /** Convenience: is this an afternoon show? (12:00–17:00) */
    public boolean isAfternoonShow() {
        int hour = showTime.getHour();
        return hour >= 12 && hour < 17;
    }
}
