package com.movieplatform.gateway.fallback;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Circuit Breaker fallback responses.
 * When a downstream service is down, gateway returns these instead of 503.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/booking")
    public ResponseEntity<Map<String, String>> bookingFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "status", "SERVICE_UNAVAILABLE",
                "message", "Booking service is temporarily unavailable. Please try again in a moment.",
                "code", "BOOKING_SERVICE_DOWN"
            ));
    }

    @GetMapping("/payment")
    public ResponseEntity<Map<String, String>> paymentFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "status", "SERVICE_UNAVAILABLE",
                "message", "Payment service is temporarily unavailable. Your booking is held for 10 minutes.",
                "code", "PAYMENT_SERVICE_DOWN"
            ));
    }

    @GetMapping("/movie")
    public ResponseEntity<Map<String, String>> movieFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "status", "SERVICE_UNAVAILABLE",
                "message", "Movie catalogue is temporarily unavailable. Please try again shortly.",
                "code", "MOVIE_SERVICE_DOWN"
            ));
    }
}
