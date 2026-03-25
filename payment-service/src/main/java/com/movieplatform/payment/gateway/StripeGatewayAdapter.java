package com.movieplatform.payment.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stripe Gateway — mock implementation for interview demo.
 *
 * In production: inject stripe-java SDK and call:
 *   PaymentIntent.create(PaymentIntentCreateParams.builder()...build())
 *
 * Design Pattern: Adapter — wraps external Stripe API behind
 * our internal PaymentGateway interface for easy swap.
 */
@Slf4j
@Component
public class StripeGatewayAdapter implements PaymentGateway {

    @Value("${stripe.secret-key:sk_test_placeholder}")
    private String stripeSecretKey;

    @Override
    public GatewayResult initiatePayment(UUID bookingId, BigDecimal amount, String currency) {
        log.info("Stripe: initiating payment bookingId={} amount={} {}", bookingId, amount, currency);

        // Mock — in production: Stripe.apiKey = stripeSecretKey; PaymentIntent.create(...)
        String paymentIntentId = "pi_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String clientSecret    = paymentIntentId + "_secret_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        log.info("Stripe PaymentIntent created: id={}", paymentIntentId);
        return GatewayResult.builder()
            .transactionId(paymentIntentId)
            .clientSecret(clientSecret)
            .status("INITIATED")
            .build();
    }

    @Override
    public GatewayResult processRefund(String transactionId, BigDecimal amount) {
        log.info("Stripe: processing refund for transactionId={} amount={}", transactionId, amount);
        // Mock — in production: Refund.create(RefundCreateParams.builder().setPaymentIntent(transactionId).build())
        String refundId = "re_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        log.info("Stripe Refund created: refundId={}", refundId);
        return GatewayResult.builder()
            .transactionId(refundId)
            .status("REFUNDED")
            .build();
    }
}
