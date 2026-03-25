package com.movieplatform.movie.repository;

import com.movieplatform.movie.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID> {

    /**
     * Browse movies — READ scenario from problem statement.
     * Filters by city (via shows), language, genre.
     */
    @Query("""
        SELECT DISTINCT m FROM Movie m
        JOIN m.shows s
        WHERE m.active = true
          AND (:city     IS NULL OR LOWER(s.city)      = LOWER(:city))
          AND (:language IS NULL OR LOWER(m.language)  = LOWER(:language))
          AND (:genre    IS NULL OR LOWER(m.genre)     = LOWER(:genre))
        ORDER BY m.releaseDate DESC
        """)
    Page<Movie> browseMovies(
        @Param("city")     String city,
        @Param("language") String language,
        @Param("genre")    String genre,
        Pageable pageable
    );
}
