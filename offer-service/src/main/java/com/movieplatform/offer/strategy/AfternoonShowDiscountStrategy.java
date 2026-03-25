package com.movieplatform.offer.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Strategy: 20% discount on all tickets for afternoon shows.
 *
 * Problem statement READ scenario:
 * "Tickets booked for the afternoon show get a 20% discount"
 *
 * Afternoon window: 12:00 (noon) to 16:59 (before 5 PM)
 *
 * Example: 2 tickets @ ₹250 each = ₹500
 *   Discount = 20% of ₹500 = ₹100
 *   Final    = ₹500 - ₹100 = ₹400
 */
@Slf4j
@Component
public class AfternoonShowDiscountStrategy implements DiscountStrategy {

    private static final BigDecimal DISCOUNT_RATE    = new BigDecimal("0.20");
    private static final int        AFTERNOON_START  = 12;  // 12:00 noon
    private static final int        AFTERNOON_END    = 17;  // before 17:00

    @Override
    public BigDecimal calculate(int ticketCount, BigDecimal basePrice,
                                LocalDateTime showTime, List<UUID> seatIds) {
        if (showTime == null) {
            log.warn("AFTERNOON20: showTime is null — no discount applied");
            return BigDecimal.ZERO;
        }

        int hour = showTime.getHour();
        boolean isAfternoon = hour >= AFTERNOON_START && hour < AFTERNOON_END;

        if (!isAfternoon) {
            log.info("AFTERNOON20: not eligible — show at {}:00 is outside afternoon window ({}-{}h)",
                hour, AFTERNOON_START, AFTERNOON_END);
            return BigDecimal.ZERO;
        }

        // 20% off total (all tickets)
        BigDecimal totalBase = basePrice.multiply(BigDecimal.valueOf(ticketCount));
        BigDecimal discount  = totalBase.multiply(DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
        log.info("AFTERNOON20: applying ₹{} discount on {} tickets at {}:00", discount, ticketCount, hour);
        return discount;
    }

    @Override
    public String getStrategyName() { return "AFTERNOON_SHOW_DISCOUNT"; }

    @Override
    public String getOfferCode() { return "AFTERNOON20"; }
}
