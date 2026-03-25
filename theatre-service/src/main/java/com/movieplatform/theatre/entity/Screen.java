package com.movieplatform.theatre.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "screens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Screen {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theatre_id", nullable = false)
    private TheatrePartner theatre;

    @Column(name = "screen_name", nullable = false)
    private String screenName;

    @Column(name = "total_seats")
    private int totalSeats;

    @Enumerated(EnumType.STRING)
    @Column(name = "screen_type")
    @Builder.Default
    private ScreenType screenType = ScreenType.REGULAR;

    @OneToMany(mappedBy = "screen", cascade = CascadeType.ALL)
    private List<SeatLayout> seatLayouts;

    public enum ScreenType { REGULAR, IMAX, FOUR_DX, DOLBY }
}
