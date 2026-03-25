package com.movieplatform.theatre.repository;

import com.movieplatform.theatre.entity.TheatreShow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TheatreShowRepository extends JpaRepository<TheatreShow, UUID> {

    List<TheatreShow> findByTheatreIdAndActiveTrue(UUID theatreId);

    @Modifying
    @Query("UPDATE TheatreShow s SET s.availableSeats = s.availableSeats - :count WHERE s.id = :showId AND s.availableSeats >= :count")
    int decrementAvailableSeats(@Param("showId") UUID showId, @Param("count") int count);

    @Modifying
    @Query("UPDATE TheatreShow s SET s.availableSeats = s.availableSeats + :count WHERE s.id = :showId")
    void incrementAvailableSeats(@Param("showId") UUID showId, @Param("count") int count);
}
