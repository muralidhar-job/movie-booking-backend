package com.movieplatform.movie.service;

import com.movieplatform.movie.entity.Movie;
import com.movieplatform.movie.entity.Show;
import com.movieplatform.movie.repository.MovieRepository;
import com.movieplatform.movie.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Movie service — catalogue browse and show listings.
 *
 * Implements READ scenario from problem statement:
 *  "Browse theatres currently running the show (movie selected)
 *   in the town, including show timing by a chosen date."
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final ShowRepository  showRepository;

    /**
     * B2C READ: Browse movies with city / language / genre filters.
     */
    @Transactional(readOnly = true)
    public Page<Movie> browseMovies(String city, String language, String genre, int page, int size) {
        log.info("Browse movies — city={} language={} genre={} page={}", city, language, genre, page);
        Pageable pageable = PageRequest.of(page, size);
        return movieRepository.browseMovies(city, language, genre, pageable);
    }

    /**
     * B2C READ: Get all shows for a specific movie on a chosen date in a city.
     * Key interview scenario implementation.
     */
    @Transactional(readOnly = true)
    public List<Show> getShowsForMovie(UUID movieId, String city, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd   = date.plusDays(1).atStartOfDay();
        log.info("Fetching shows — movieId={} city={} date={}", movieId, city, date);
        return showRepository.findShowsForMovieInCityOnDate(movieId, city, dayStart, dayEnd);
    }

    @Transactional
    public Movie addMovie(Movie movie) {
        log.info("Adding movie: {}", movie.getTitle());
        return movieRepository.save(movie);
    }

    @Transactional(readOnly = true)
    public Movie getMovie(UUID movieId) {
        return movieRepository.findById(movieId)
            .orElseThrow(() -> new RuntimeException("Movie not found: " + movieId));
    }
}
