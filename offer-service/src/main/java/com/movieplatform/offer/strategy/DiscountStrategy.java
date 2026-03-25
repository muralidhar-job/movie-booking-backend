package com.movieplatform.offer.strategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Discount Strategy interface.
 *
 * Design Pattern: STRATEGY
 * Each offer rule is a separate strategy class. New discount types
 * can be added without changing existing code (Open/Closed Principle).
 *
 * Current strategies:
 *  - ThirdTicketDiscountStrategy  : 50% off on the 3rd ticket
 *  - AfternoonShowDiscountStrategy: 20% off for afternoon shows (12:00-17:00)
 *  - NoDiscountStrategy           : default null object
 */
public interface DiscountStrategy {

    /**
     * Calculate the discount amount for this booking.
     *
     * @param ticketCount  number of tickets being booked
     * @param basePrice    price per ticket
     * @param showTime     show date/time (for time-based discounts)
     * @param seatIds      seat IDs (for seat-category-based discounts)
     * @return discount amount (never negative)
     */
    BigDecimal calculate(int ticketCount, BigDecimal basePrice,
                         LocalDateTime showTime, List<UUID> seatIds);

    /** Human-readable name for logging and response */
    String getStrategyName();

    /** The offer code this strategy handles */
    String getOfferCode();

    /** Returns true if this strategy can handle the given offer code */
    default boolean supports(String offerCode) {
        return getOfferCode().equalsIgnoreCase(offerCode);
    }
}
