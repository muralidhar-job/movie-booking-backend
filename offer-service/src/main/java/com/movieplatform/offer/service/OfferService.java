package com.movieplatform.offer.service;

import com.movieplatform.offer.strategy.DiscountStrategy;
import com.movieplatform.offer.strategy.DiscountStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Offer Service — applies discount strategies to bookings.
 *
 * READ SCENARIO from problem statement:
 *  "Booking platform offers in selected cities and theatres:
 *    - 50% discount on the third ticket
 *    - Tickets booked for the afternoon show get a 20% discount"
 *
 * Design Pattern: Strategy — strategies are resolved at runtime via factory.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfferService {

    private final DiscountStrategyFactory strategyFactory;

    /**
     * Apply an offer to a booking and return discount details.
     *
     * @param offerCode    e.g. "THIRD50", "AFTERNOON20"
     * @param ticketCount  number of tickets
     * @param basePrice    price per ticket
     * @param showTime     show date/time (for afternoon check)
     * @param seatIds      seat IDs being booked
     */
    public Map<String, Object> applyOffer(String offerCode, int ticketCount,
                                          BigDecimal basePrice, LocalDateTime showTime,
                                          List<UUID> seatIds) {
        log.info("Applying offer: code={} tickets={} basePrice={} showTime={}",
            offerCode, ticketCount, basePrice, showTime);

        // Factory resolves correct strategy — Strategy Pattern
        DiscountStrategy strategy = strategyFactory.resolve(offerCode);

        BigDecimal discount    = strategy.calculate(ticketCount, basePrice, showTime, seatIds);
        BigDecimal totalBase   = basePrice.multiply(BigDecimal.valueOf(ticketCount));
        BigDecimal finalAmount = totalBase.subtract(discount);

        log.info("Offer result: strategy={} discount=₹{} total=₹{} final=₹{}",
            strategy.getStrategyName(), discount, totalBase, finalAmount);

        return Map.of(
            "offerCode",      offerCode != null ? offerCode : "NONE",
            "valid",          discount.compareTo(BigDecimal.ZERO) > 0,
            "strategyApplied", strategy.getStrategyName(),
            "baseTotal",      totalBase,
            "discountAmount", discount,
            "finalAmount",    finalAmount
        );
    }

    /** List all active offers (in production: read from DB) */
    public List<Map<String, String>> listActiveOffers() {
        return List.of(
            Map.of(
                "code",        "THIRD50",
                "description", "50% off on the 3rd ticket",
                "type",        "THIRD_TICKET_DISCOUNT",
                "validUntil",  "2025-12-31"
            ),
            Map.of(
                "code",        "AFTERNOON20",
                "description", "20% off for afternoon shows (12:00 - 17:00)",
                "type",        "AFTERNOON_SHOW_DISCOUNT",
                "validUntil",  "2025-12-31"
            )
        );
    }
}
