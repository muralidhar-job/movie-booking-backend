package com.movieplatform.theatre.repository;

import com.movieplatform.theatre.entity.TheatrePartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TheatreRepository extends JpaRepository<TheatrePartner, UUID> {
    List<TheatrePartner> findByCityIgnoreCaseAndActiveTrue(String city);
    Optional<TheatrePartner> findByIdAndUserId(UUID id, UUID userId);
}
