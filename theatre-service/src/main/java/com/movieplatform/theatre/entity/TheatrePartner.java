package com.movieplatform.theatre.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "theatre_partners")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TheatrePartner {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "theatre_name", nullable = false)
    private String theatreName;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    private String state;
    private String country;

    @Column(name = "gst_number")
    private String gstNumber;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OnboardingStatus status = OnboardingStatus.PENDING_VERIFICATION;

    @Column(name = "is_active") @Builder.Default
    private boolean active = false;

    @CreationTimestamp
    @Column(name = "onboarded_at", updatable = false)
    private LocalDateTime onboardedAt;

    @OneToMany(mappedBy = "theatre", cascade = CascadeType.ALL)
    private List<Screen> screens;

    public enum OnboardingStatus { PENDING_VERIFICATION, ACTIVE, SUSPENDED }
}
