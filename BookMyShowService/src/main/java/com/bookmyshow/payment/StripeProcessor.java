package com.bookmyshow.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ============================================================
 * StripeProcessor – Concrete Strategy for Stripe Gateway
 * ============================================================
 *
 * DESIGN PATTERN: Strategy Pattern – concrete implementation of
 * PaymentProcessor for Stripe (global card processing).
 *
 * In production: use Stripe Java SDK (com.stripe:stripe-java)
 * and create PaymentIntents for idempotent charge handling.
 */
@Slf4j
@Component  // Spring singleton bean
public class StripeProcessor implements PaymentProcessor {

    /**
     * Simulate charging via Stripe PaymentIntent API.
     * In production:
     *   PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
     *       .setAmount((long)(amount * 100))  // Stripe uses cents
     *       .setCurrency(currency.toLowerCase())
     *       .setDescription(description)
     *       .setIdempotencyKey(bookingRef)     // Idempotency key prevents duplicates
     *       .build();
     *   PaymentIntent intent = PaymentIntent.create(params);
     */
    @Override
    public PaymentResult charge(Double amount, String currency, String bookingRef, String description) {
        log.info("[Stripe] Charging {} {} for booking {}", amount, currency, bookingRef);

        // Mock Stripe payment intent ID (format: pi_xxx)
        String mockPaymentIntentId = "pi_" + UUID.randomUUID().toString().replace("-", "");

        log.info("[Stripe] Charge successful: paymentIntentId={}", mockPaymentIntentId);

        return PaymentResult.builder()
                .success(true)                      // Payment succeeded (mock)
                .transactionId(mockPaymentIntentId) // Stripe's payment_intent id
                .build();
    }

    /**
     * Simulate refund via Stripe Refund API.
     * In production:
     *   RefundCreateParams params = RefundCreateParams.builder()
     *       .setPaymentIntent(transactionId)
     *       .setAmount((long)(amount * 100))
     *       .build();
     *   Refund refund = Refund.create(params);
     */
    @Override
    public PaymentResult refund(String transactionId, Double amount) {
        log.info("[Stripe] Refunding {} for transaction {}", amount, transactionId);

        String mockRefundId = "re_" + UUID.randomUUID().toString().replace("-", "");

        log.info("[Stripe] Refund successful: refundId={}", mockRefundId);

        return PaymentResult.builder()
                .success(true)
                .refundId(mockRefundId)
                .build();
    }

    /** Must match PaymentGateway.STRIPE.name() */
    @Override
    public String getGatewayName() {
        return "STRIPE";
    }
}

