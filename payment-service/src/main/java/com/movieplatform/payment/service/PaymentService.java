package com.movieplatform.payment.service;

import com.movieplatform.payment.dto.PaymentDtos.*;
import com.movieplatform.payment.entity.Payment;
import com.movieplatform.payment.gateway.PaymentGateway;
import com.movieplatform.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Payment Service — Saga participant.
 *
 * Listens to booking-events for BOOKING_INITIATED.
 * On payment success → publishes PAYMENT_SUCCESS → booking-service confirms.
 * On payment failure → publishes PAYMENT_FAILED → booking-service compensates.
 *
 * Design Patterns: Adapter (gateway), Idempotency, Outbox (in production).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final PaymentGateway    paymentGateway;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_PAYMENT = "payment-events";

    // ── Initiate payment (called by customer via API) ────────────────────

    @Transactional
    public PaymentResponse initiatePayment(UUID userId, InitiatePaymentRequest request) {
        log.info("Initiating payment: bookingId={} amount={}", request.getBookingId(), request.getAmount());

        // Idempotency — prevent double charge
        String idempKey = userId + ":" + request.getBookingId();
        paymentRepo.findByIdempotencyKey(idempKey).ifPresent(existing -> {
            throw new RuntimeException("Payment already initiated for this booking. paymentId=" + existing.getId());
        });

        // Call payment gateway (Stripe adapter)
        PaymentGateway.GatewayResult result = paymentGateway.initiatePayment(
            request.getBookingId(), request.getAmount(), request.getCurrency());

        // Persist payment record
        Payment payment = Payment.builder()
            .bookingId(request.getBookingId())
            .userId(userId)
            .gateway(Payment.Gateway.valueOf(request.getGateway().toUpperCase()))
            .transactionId(result.getTransactionId())
            .clientSecret(result.getClientSecret())
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .status(Payment.PaymentStatus.INITIATED)
            .idempotencyKey(idempKey)
            .build();

        Payment saved = paymentRepo.save(payment);
        log.info("Payment record created: paymentId={} transactionId={}", saved.getId(), result.getTransactionId());

        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(saved.getId());
        response.setTransactionId(result.getTransactionId());
        response.setClientSecret(result.getClientSecret());
        response.setStatus("INITIATED");
        response.setMessage("Payment initiated. Use clientSecret with Stripe.js to complete.");
        return response;
    }

    // ── Webhook from Stripe — confirms payment outcome ───────────────────

    @Transactional
    public void handleStripeWebhook(WebhookPayload payload, String stripeSignature) {
        log.info("Stripe webhook received: type={}", payload.getType());

        // In production: verify Stripe-Signature header with WebhookEvent.constructEvent()
        String transactionId = payload.getData().getObject().getId();

        Payment payment = paymentRepo.findByTransactionId(transactionId)
            .orElseThrow(() -> new RuntimeException("Payment not found for transactionId: " + transactionId));

        switch (payload.getType()) {
            case "payment_intent.succeeded" -> handlePaymentSuccess(payment);
            case "payment_intent.payment_failed" -> handlePaymentFailure(payment, "Payment declined by gateway");
            default -> log.warn("Unhandled Stripe event type: {}", payload.getType());
        }
    }

    private void handlePaymentSuccess(Payment payment) {
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepo.save(payment);

        // Publish PAYMENT_SUCCESS → booking-service
        Map<String, Object> event = Map.of(
            "type",          "PaymentSuccessEvent",
            "bookingId",     payment.getBookingId().toString(),
            "paymentId",     payment.getId().toString(),
            "transactionId", payment.getTransactionId(),
            "amount",        payment.getAmount()
        );
        kafkaTemplate.send(TOPIC_PAYMENT, payment.getBookingId().toString(), event);
        log.info("PAYMENT_SUCCESS published: bookingId={}", payment.getBookingId());
    }

    private void handlePaymentFailure(Payment payment, String reason) {
        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        paymentRepo.save(payment);

        // Publish PAYMENT_FAILED → booking-service triggers compensation
        Map<String, Object> event = Map.of(
            "type",      "PaymentFailedEvent",
            "bookingId", payment.getBookingId().toString(),
            "reason",    reason
        );
        kafkaTemplate.send(TOPIC_PAYMENT, payment.getBookingId().toString(), event);
        log.warn("PAYMENT_FAILED published: bookingId={} reason={}", payment.getBookingId(), reason);
    }

    // ── Kafka: listen for booking cancellation → process refund ──────────

    @KafkaListener(topics = "booking-events", groupId = "payment-service-group",
                   containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleBookingEvent(Map<String, Object> event) {
        String eventType = (String) event.get("type");
        if (!"BookingCancelledEvent".equals(eventType)) return;

        String bookingIdStr = (String) event.get("bookingId");
        UUID bookingId = UUID.fromString(bookingIdStr);
        log.info("Received BookingCancelledEvent for bookingId={}", bookingId);

        paymentRepo.findByBookingId(bookingId).ifPresent(payment -> {
            if (payment.getStatus() == Payment.PaymentStatus.SUCCESS) {
                PaymentGateway.GatewayResult refund = paymentGateway.processRefund(
                    payment.getTransactionId(), payment.getAmount());
                payment.setStatus(Payment.PaymentStatus.REFUNDED);
                paymentRepo.save(payment);
                log.info("Refund processed: bookingId={} refundTxn={}", bookingId, refund.getTransactionId());
            }
        });
    }

    // ── Demo endpoint: simulate payment success (for testing) ─────────────

    @Transactional
    public void simulatePaymentSuccess(UUID bookingId) {
        log.info("DEMO: simulating payment success for bookingId={}", bookingId);
        Payment payment = paymentRepo.findByBookingId(bookingId)
            .orElseThrow(() -> new RuntimeException("No payment found for bookingId: " + bookingId));
        handlePaymentSuccess(payment);
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByBookingId(UUID bookingId) {
        return paymentRepo.findByBookingId(bookingId)
            .orElseThrow(() -> new RuntimeException("Payment not found for bookingId: " + bookingId));
    }
}
