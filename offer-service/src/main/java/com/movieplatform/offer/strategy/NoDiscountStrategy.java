package com.movieplatform.offer.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Null Object Pattern — returned when offer code is unknown or invalid.
 * Prevents null checks throughout the codebase.
 */
@Slf4j
@Component
public class NoDiscountStrategy implements DiscountStrategy {

    @Override
    public BigDecimal calculate(int ticketCount, BigDecimal basePrice,
                                LocalDateTime showTime, List<UUID> seatIds) {
        log.info("NoDiscount: no applicable strategy — returning zero discount");
        return BigDecimal.ZERO;
    }

    @Override
    public String getStrategyName() { return "NO_DISCOUNT"; }

    @Override
    public String getOfferCode() { return "NONE"; }

    @Override
    public boolean supports(String offerCode) {
        return offerCode == null || offerCode.isBlank();
    }
}
