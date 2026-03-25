package com.movieplatform.movie.repository;

import com.movieplatform.movie.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShowRepository extends JpaRepository<Show, UUID> {

    /**
     * READ scenario: Browse theatres running a movie on a given date in a city.
     * Problem statement: "Browse theatres currently running the show (movie selected)
     * in the town, including show timing by a chosen date."
     */
    @Query("""
        SELECT s FROM Show s
        WHERE s.movie.id = :movieId
          AND s.active   = true
          AND LOWER(s.city) = LOWER(:city)
          AND s.showTime >= :dayStart
          AND s.showTime <  :dayEnd
        ORDER BY s.showTime ASC
        """)
    List<Show> findShowsForMovieInCityOnDate(
        @Param("movieId")  UUID movieId,
        @Param("city")     String city,
        @Param("dayStart") LocalDateTime dayStart,
        @Param("dayEnd")   LocalDateTime dayEnd
    );

    List<Show> findByTheatreIdAndActiveTrue(UUID theatreId);
}
