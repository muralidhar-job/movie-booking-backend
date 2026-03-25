package com.movieplatform.payment.gateway;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payment gateway interface — Adapter pattern.
 * Allows swapping Stripe → Razorpay → PayU without changing PaymentService.
 */
public interface PaymentGateway {
    GatewayResult initiatePayment(UUID bookingId, BigDecimal amount, String currency);
    GatewayResult processRefund(String transactionId, BigDecimal amount);

    @Data @Builder
    class GatewayResult {
        private String transactionId;
        private String clientSecret;
        private String status;
        private String errorMessage;
    }
}
