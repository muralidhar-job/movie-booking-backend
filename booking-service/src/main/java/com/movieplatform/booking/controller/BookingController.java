package com.movieplatform.booking.controller;

import com.movieplatform.booking.dto.BookingDtos.*;
import com.movieplatform.booking.entity.Booking;
import com.movieplatform.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking Service", description = "Seat booking, cancellation, bulk booking")
public class BookingController {

    private final BookingService bookingService;

    /** WRITE SCENARIO: Book tickets — initiates Kafka saga */
    @PostMapping
    @Operation(summary = "Book movie tickets — initiates booking saga")
    public ResponseEntity<Booking> createBooking(
            @RequestHeader("X-User-Id")       String userId,
            @RequestHeader("X-Correlation-Id") String correlationId,
            @Valid @RequestBody BookingRequest request) {
        Booking booking = bookingService.createBooking(
            UUID.fromString(userId), request, correlationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    /** WRITE SCENARIO: Bulk booking */
    @PostMapping("/bulk")
    @Operation(summary = "Bulk booking for groups")
    public ResponseEntity<Map<String, Object>> bulkBooking(
            @RequestHeader("X-User-Id")       String userId,
            @RequestHeader("X-Correlation-Id") String correlationId,
            @Valid @RequestBody BulkBookingRequest request) {
        List<Booking> bookings = bookingService.bulkBooking(
            UUID.fromString(userId), request, correlationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "totalBookings", bookings.size(),
            "bookings", bookings.stream().map(b -> Map.of(
                "bookingId", b.getId(),
                "status", b.getStatus()
            )).toList()
        ));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking details")
    public ResponseEntity<Booking> getBooking(
            @PathVariable UUID bookingId,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(bookingService.getBookingById(bookingId));
    }

    @GetMapping("/my")
    @Operation(summary = "My booking history")
    public ResponseEntity<Page<Booking>> myBookings(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
            bookingService.getMyBookings(UUID.fromString(userId), status, page, size));
    }

    /** WRITE SCENARIO: Cancel booking */
    @DeleteMapping("/{bookingId}")
    @Operation(summary = "Cancel booking — triggers refund saga")
    public ResponseEntity<Map<String, Object>> cancelBooking(
            @PathVariable UUID bookingId,
            @RequestHeader("X-User-Id") String userId) {
        Booking cancelled = bookingService.cancelBooking(bookingId, UUID.fromString(userId));
        return ResponseEntity.ok(Map.of(
            "bookingId", cancelled.getId(),
            "status",    cancelled.getStatus(),
            "message",   "Booking cancellation initiated",
            "refundAmount", cancelled.getTotalAmount()
        ));
    }
}
