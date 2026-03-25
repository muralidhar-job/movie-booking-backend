package com.movieplatform.offer.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Strategy: 50% discount on the third ticket.
 *
 * Problem statement READ scenario:
 * "Booking platform offers: 50% discount on the third ticket"
 *
 * Example: 3 tickets @ ₹250 each = ₹750
 *   Discount = 50% of ₹250 = ₹125
 *   Final    = ₹750 - ₹125 = ₹625
 */
@Slf4j
@Component
public class ThirdTicketDiscountStrategy implements DiscountStrategy {

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.50");
    private static final int        MIN_TICKETS   = 3;

    @Override
    public BigDecimal calculate(int ticketCount, BigDecimal basePrice,
                                LocalDateTime showTime, List<UUID> seatIds) {
        if (ticketCount < MIN_TICKETS) {
            log.info("THIRD50: not eligible — only {} tickets (minimum {})", ticketCount, MIN_TICKETS);
            return BigDecimal.ZERO;
        }

        // 50% off the price of exactly the 3rd ticket
        BigDecimal discount = basePrice.multiply(DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
        log.info("THIRD50: applying ₹{} discount on 3rd ticket (basePrice=₹{})", discount, basePrice);
        return discount;
    }

    @Override
    public String getStrategyName() { return "THIRD_TICKET_DISCOUNT"; }

    @Override
    public String getOfferCode() { return "THIRD50"; }
}
