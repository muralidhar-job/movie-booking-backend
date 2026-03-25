package com.movieplatform.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

public class PaymentDtos {

    @Data
    public static class InitiatePaymentRequest {
        @NotNull  private UUID bookingId;
        @NotNull  private BigDecimal amount;
        @NotBlank private String currency;
        private String gateway = "STRIPE";
    }

    @Data
    public static class PaymentResponse {
        private UUID paymentId;
        private String clientSecret;   // for Stripe.js frontend
        private String transactionId;
        private String status;
        private String message;
    }

    @Data
    public static class WebhookPayload {
        private String type;           // payment_intent.succeeded | payment_intent.payment_failed
        private WebhookData data;

        @Data
        public static class WebhookData {
            private WebhookObject object;
        }

        @Data
        public static class WebhookObject {
            private String id;         // PaymentIntent ID
            private String status;
            private Long   amount;
        }
    }
}
