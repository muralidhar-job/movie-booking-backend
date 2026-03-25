package com.movieplatform.booking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stub for calling offer-service via Feign.
 * In production replace with @FeignClient interface.
 * Kept as a service stub to keep demo self-contained.
 */
@Slf4j
@Service
public class OfferClientService {

    public BigDecimal calculateDiscount(String offerCode, int ticketCount, UUID showId) {
        if (offerCode == null || offerCode.isBlank()) return BigDecimal.ZERO;

        log.info("Calculating discount: offerCode={} tickets={}", offerCode, ticketCount);

        return switch (offerCode.toUpperCase()) {
            case "THIRD50" -> ticketCount >= 3
                ? BigDecimal.valueOf(250).multiply(BigDecimal.valueOf(0.5))
                : BigDecimal.ZERO;
            case "AFTERNOON20" -> BigDecimal.valueOf(250)
                .multiply(BigDecimal.valueOf(ticketCount))
                .multiply(BigDecimal.valueOf(0.2));
            default -> BigDecimal.ZERO;
        };
    }
}
