package com.movieplatform.theatre.controller;

import com.movieplatform.theatre.dto.TheatreDtos.*;
import com.movieplatform.theatre.entity.*;
import com.movieplatform.theatre.service.TheatreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/theatres")
@RequiredArgsConstructor
@Tag(name = "Theatre Service", description = "B2B theatre partner operations")
public class TheatreController {

    private final TheatreService theatreService;

    /** B2B: Onboard a new theatre */
    @PostMapping
    @Operation(summary = "Theatre partner onboards a new theatre")
    public ResponseEntity<TheatrePartner> onboard(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody OnboardRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(theatreService.onboardTheatre(UUID.fromString(userId), req));
    }

    @GetMapping("/{theatreId}")
    public ResponseEntity<TheatrePartner> getTheatre(@PathVariable UUID theatreId) {
        return ResponseEntity.ok(theatreService.getTheatreById(theatreId));
    }

    @GetMapping
    public ResponseEntity<List<TheatrePartner>> getByCity(@RequestParam String city) {
        return ResponseEntity.ok(theatreService.getTheatresByCity(city));
    }

    /** B2B: Add a screen to the theatre */
    @PostMapping("/{theatreId}/screens")
    @Operation(summary = "Add a screen to theatre")
    public ResponseEntity<Screen> addScreen(
            @PathVariable UUID theatreId,
            @RequestParam String screenName,
            @RequestParam int totalSeats,
            @RequestParam(defaultValue = "REGULAR") String screenType) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(theatreService.addScreen(theatreId, screenName, totalSeats, screenType));
    }

    /** B2B WRITE SCENARIO: Allocate seat inventory for a screen */
    @PostMapping("/{theatreId}/screens/{screenId}/seats")
    @Operation(summary = "Allocate seat inventory for a screen")
    public ResponseEntity<Map<String, Object>> allocateSeats(
            @PathVariable UUID theatreId,
            @PathVariable UUID screenId,
            @Valid @RequestBody AllocateSeatsRequest req) {
        List<SeatLayout> seats = theatreService.allocateSeats(screenId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("allocated", seats.size(), "message", "Seat layout saved"));
    }

    @GetMapping("/{theatreId}/screens/{screenId}/seats")
    public ResponseEntity<List<SeatLayout>> getSeats(
            @PathVariable UUID theatreId,
            @PathVariable UUID screenId) {
        return ResponseEntity.ok(theatreService.getSeatsForScreen(screenId));
    }

    /** B2B WRITE SCENARIO: Create a show */
    @PostMapping("/{theatreId}/shows")
    @Operation(summary = "Theatre creates a new show")
    public ResponseEntity<TheatreShow> createShow(
            @PathVariable UUID theatreId,
            @Valid @RequestBody CreateShowRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(theatreService.createShow(theatreId, req));
    }

    /** B2B WRITE SCENARIO: Update a show */
    @PutMapping("/{theatreId}/shows/{showId}")
    @Operation(summary = "Update or cancel a show")
    public ResponseEntity<TheatreShow> updateShow(
            @PathVariable UUID theatreId,
            @PathVariable UUID showId,
            @RequestBody UpdateShowRequest req) {
        return ResponseEntity.ok(theatreService.updateShow(theatreId, showId, req));
    }

    /** B2B WRITE SCENARIO: Delete (soft) a show */
    @DeleteMapping("/{theatreId}/shows/{showId}")
    @Operation(summary = "Delete a show")
    public ResponseEntity<Map<String, String>> deleteShow(
            @PathVariable UUID theatreId,
            @PathVariable UUID showId) {
        theatreService.deleteShow(theatreId, showId);
        return ResponseEntity.ok(Map.of("message", "Show cancelled successfully"));
    }

    @GetMapping("/{theatreId}/shows")
    public ResponseEntity<List<TheatreShow>> getShows(@PathVariable UUID theatreId) {
        return ResponseEntity.ok(theatreService.getShowsForTheatre(theatreId));
    }
}
