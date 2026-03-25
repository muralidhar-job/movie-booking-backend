package com.movieplatform.offer.controller;

import com.movieplatform.offer.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/offers")
@RequiredArgsConstructor
@Tag(name = "Offer Service", description = "Discount offers and pricing rules")
public class OfferController {

    private final OfferService offerService;

    /** List all active platform offers */
    @GetMapping
    @Operation(summary = "List active offers")
    public ResponseEntity<List<Map<String, String>>> listOffers(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) UUID theatreId) {
        return ResponseEntity.ok(offerService.listActiveOffers());
    }

    /** Validate and apply an offer to a booking */
    @PostMapping("/apply")
    @Operation(summary = "Validate and apply offer — returns discount amount")
    public ResponseEntity<Map<String, Object>> applyOffer(
            @RequestParam              String offerCode,
            @RequestParam              int    ticketCount,
            @RequestParam              BigDecimal basePrice,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime showTime,
            @RequestParam(required = false) List<UUID> seatIds) {
        return ResponseEntity.ok(
            offerService.applyOffer(offerCode, ticketCount, basePrice, showTime, seatIds));
    }
}
