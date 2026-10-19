package com.carrental.strategy;

import com.carrental.enums.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * CreditCardPaymentStrategy – STRATEGY PATTERN concrete implementation for credit card payments.
 *
 * =============================================
 * Design Pattern: STRATEGY PATTERN (Concrete Strategy)
 * =============================================
 * This class encapsulates all credit-card-specific payment logic.
 * In production, replace the mock logic with actual Stripe or Razorpay SDK calls.
 *
 * Registered as a Spring @Component so PaymentProcessorFactory can auto-discover it.
 */
@Component  // Spring bean – auto-discovered by PaymentProcessorFactory via dependency injection
@Slf4j      // Lombok – injects a SLF4J logger field named 'log'
public class CreditCardPaymentStrategy implements PaymentStrategy {

    /**
     * Process a credit card charge via the payment gateway.
     *
     * Production implementation would:
     * 1. Call Stripe/Razorpay SDK with card token + amount
     * 2. Handle 3DS authentication callback
     * 3. Return PaymentResult with gateway transaction reference
     *
     * @param idempotencyKey unique key sent to gateway to prevent duplicate charges
     * @param amount         amount to capture in local currency
     */
    @Override
    public PaymentResult process(String idempotencyKey, BigDecimal amount) {
        log.info("[CREDIT_CARD] Processing payment. idempotencyKey={}, amount={}", idempotencyKey, amount);

        // ── MOCK GATEWAY CALL ──────────────────────────────────────────────────────────
        // In production: stripeClient.charges().create(amount, currency, cardToken, idempotencyKey)
        String gatewayTransactionId = "CC-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        // ──────────────────────────────────────────────────────────────────────────────

        log.info("[CREDIT_CARD] Payment successful. txnId={}", gatewayTransactionId);
        // Build and return success result using PaymentResult value object
        return PaymentResult.success(gatewayTransactionId, amount);
    }

    /**
     * Refund a previously captured credit card charge.
     *
     * Production implementation would:
     * 1. Call Stripe refund API with original charge ID + refund amount
     * 2. Return refund transaction reference
     *
     * @param transactionId original charge transaction ID from process()
     * @param amount        amount to refund (may be partial after penalty deduction)
     */
    @Override
    public PaymentResult refund(String transactionId, BigDecimal amount) {
        log.info("[CREDIT_CARD] Processing refund. originalTxn={}, amount={}", transactionId, amount);

        // ── MOCK REFUND CALL ───────────────────────────────────────────────────────────
        // In production: stripeClient.refunds().create(transactionId, amount)
        String refundId = "CC-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        // ──────────────────────────────────────────────────────────────────────────────

        log.info("[CREDIT_CARD] Refund successful. refundId={}", refundId);
        return PaymentResult.success(refundId, amount);  // reuse success factory with refund txn ID
    }

    /**
     * Identifies this strategy for lookup in PaymentProcessorFactory.
     * Factory maps CREDIT_CARD → this bean.
     */
    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.CREDIT_CARD;  // key for factory registration
    }
}

