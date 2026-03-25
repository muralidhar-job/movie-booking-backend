package com.movieplatform.offer.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy Factory — resolves the correct DiscountStrategy for a given offer code.
 *
 * Design Pattern: FACTORY + STRATEGY
 * Spring injects all DiscountStrategy beans into the list automatically.
 * Adding a new strategy requires only creating a new @Component — zero changes here.
 *
 * Interview talking point:
 *  "The factory iterates registered strategies and calls supports(offerCode).
 *   This is the Open/Closed Principle in action — open for extension, closed for modification."
 */
@Slf4j
@Component
public class DiscountStrategyFactory {

    private final List<DiscountStrategy> strategies;
    private final NoDiscountStrategy     noDiscountStrategy;

    public DiscountStrategyFactory(List<DiscountStrategy> strategies,
                                   NoDiscountStrategy noDiscountStrategy) {
        this.strategies         = strategies;
        this.noDiscountStrategy = noDiscountStrategy;
        log.info("DiscountStrategyFactory initialized with {} strategies: {}",
            strategies.size(),
            strategies.stream().map(DiscountStrategy::getOfferCode).toList());
    }

    /**
     * Returns the matching strategy or NoDiscountStrategy if code is unknown.
     * Never returns null.
     */
    public DiscountStrategy resolve(String offerCode) {
        if (offerCode == null || offerCode.isBlank()) {
            return noDiscountStrategy;
        }
        return strategies.stream()
            .filter(s -> s.supports(offerCode))
            .findFirst()
            .orElseGet(() -> {
                log.warn("Unknown offer code '{}' — applying no discount", offerCode);
                return noDiscountStrategy;
            });
    }
}
