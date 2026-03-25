package com.movieplatform.movie.controller;

import com.movieplatform.movie.entity.Movie;
import com.movieplatform.movie.entity.Show;
import com.movieplatform.movie.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
@Tag(name = "Movie Service", description = "Catalogue and show listings")
public class MovieController {

    private final MovieService movieService;

    /** B2C: Browse movies filtered by city, language, genre */
    @GetMapping
    @Operation(summary = "Browse movies — filter by city, language, genre")
    public ResponseEntity<Page<Movie>> browseMovies(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String genre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(movieService.browseMovies(city, language, genre, page, size));
    }

    /** B2C READ SCENARIO: Shows for a movie on a date in a city */
    @GetMapping("/{movieId}/shows")
    @Operation(summary = "Browse theatres running a movie on a chosen date in a city")
    public ResponseEntity<List<Show>> getShowsForMovie(
            @PathVariable UUID movieId,
            @RequestParam String city,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(movieService.getShowsForMovie(movieId, city, date));
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<Movie> getMovie(@PathVariable UUID movieId) {
        return ResponseEntity.ok(movieService.getMovie(movieId));
    }

    /** Admin: Add movie to catalogue */
    @PostMapping
    @Operation(summary = "Admin: Add movie to catalogue")
    public ResponseEntity<Movie> addMovie(@Valid @RequestBody Movie movie) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movieService.addMovie(movie));
    }
}
