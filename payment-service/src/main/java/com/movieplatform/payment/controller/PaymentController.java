package com.movieplatform.payment.controller;

import com.movieplatform.payment.dto.PaymentDtos.*;
import com.movieplatform.payment.entity.Payment;
import com.movieplatform.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Service", description = "Payment initiation, webhook, refunds")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    @Operation(summary = "Initiate payment via Stripe")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody InitiatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(paymentService.initiatePayment(UUID.fromString(userId), request));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Stripe webhook — receives payment outcome")
    public ResponseEntity<Map<String, Boolean>> stripeWebhook(
            @RequestBody WebhookPayload payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        paymentService.handleStripeWebhook(payload, signature);
        return ResponseEntity.ok(Map.of("received", true));
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get payment status for a booking")
    public ResponseEntity<Payment> getPaymentByBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentByBookingId(bookingId));
    }

    /** Demo only — simulate Stripe webhook for testing without real Stripe */
    @PostMapping("/demo/simulate-success/{bookingId}")
    @Operation(summary = "DEMO: Simulate payment success (testing only)")
    public ResponseEntity<Map<String, String>> simulateSuccess(@PathVariable UUID bookingId) {
        paymentService.simulatePaymentSuccess(bookingId);
        return ResponseEntity.ok(Map.of(
            "message", "Payment success simulated for bookingId: " + bookingId,
            "note",    "This endpoint is for demo/testing only"
        ));
    }
}
