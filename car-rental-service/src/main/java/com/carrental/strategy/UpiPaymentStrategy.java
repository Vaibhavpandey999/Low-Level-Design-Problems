package com.carrental.strategy;

import com.carrental.enums.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * UpiPaymentStrategy – STRATEGY PATTERN concrete implementation for UPI payments.
 *
 * =============================================
 * Design Pattern: STRATEGY PATTERN (Concrete Strategy)
 * =============================================
 * UPI (Unified Payments Interface) is India's instant inter-bank transfer system.
 * This strategy handles UPI-specific flow: VPA validation + payment collection request.
 *
 * In production: integrate with Razorpay UPI API or NPCI payment gateway.
 */
@Component  // Spring-managed bean – auto-discovered by PaymentProcessorFactory
@Slf4j
public class UpiPaymentStrategy implements PaymentStrategy {

    /**
     * Process a UPI payment by sending a collection request to the customer's UPI VPA.
     *
     * Production flow:
     * 1. Validate UPI VPA (Virtual Payment Address) format (e.g., user@oksbi)
     * 2. Send collect request via UPI gateway
     * 3. Poll or receive webhook callback for payment confirmation
     *
     * @param idempotencyKey unique booking reference to prevent duplicate charges
     * @param amount         amount to collect via UPI
     */
    @Override
    public PaymentResult process(String idempotencyKey, BigDecimal amount) {
        log.info("[UPI] Processing payment. idempotencyKey={}, amount={}", idempotencyKey, amount);

        // ── MOCK UPI COLLECT REQUEST ───────────────────────────────────────────────────
        // In production: razorpayClient.upi().createCollectRequest(vpa, amount, idempotencyKey)
        String upiTransactionId = "UPI-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        // ──────────────────────────────────────────────────────────────────────────────

        log.info("[UPI] Payment successful. txnId={}", upiTransactionId);
        return PaymentResult.success(upiTransactionId, amount);  // wrap in value object
    }

    /**
     * Initiate a UPI refund back to the customer's bank account.
     *
     * UPI refunds are processed within 5–7 business days per NPCI norms.
     *
     * @param transactionId original UPI transaction reference
     * @param amount        refund amount (may be less than original due to penalty)
     */
    @Override
    public PaymentResult refund(String transactionId, BigDecimal amount) {
        log.info("[UPI] Processing refund. originalTxn={}, amount={}", transactionId, amount);

        // ── MOCK UPI REFUND ────────────────────────────────────────────────────────────
        // In production: razorpayClient.refunds().create(transactionId, amount)
        String refundId = "UPI-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        // ──────────────────────────────────────────────────────────────────────────────

        log.info("[UPI] Refund initiated. refundId={}", refundId);
        return PaymentResult.success(refundId, amount);
    }

    /**
     * Returns the PaymentMethod this strategy handles – used by PaymentProcessorFactory.
     */
    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.UPI;  // factory maps UPI → this bean
    }
}

