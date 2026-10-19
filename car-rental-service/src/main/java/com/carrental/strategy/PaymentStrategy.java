package com.carrental.strategy;

import com.carrental.enums.PaymentMethod;

import java.math.BigDecimal;

/**
 * PaymentStrategy – the Strategy interface for the STRATEGY PATTERN.
 *
 * =============================================
 * Design Pattern: STRATEGY PATTERN
 * =============================================
 * Defines a family of payment algorithms (Credit Card, UPI, Wallet).
 * Each algorithm is encapsulated in a separate class and can be swapped
 * at runtime without changing the PaymentService or any caller.
 *
 * Benefit: Adding a new payment gateway (e.g., NetBanking) requires ONLY
 * adding one new class that implements this interface. Zero changes to
 * existing code (Open/Closed Principle).
 *
 * Used by: PaymentProcessorFactory (Factory Pattern) resolves the concrete
 * strategy at runtime based on the PaymentMethod enum.
 */
public interface PaymentStrategy {

    /**
     * Process a payment charge.
     *
     * @param idempotencyKey unique key to prevent double-charging on retries
     * @param amount         amount to charge in local currency
     * @return PaymentResult containing gateway transaction ID and outcome
     */
    PaymentResult process(String idempotencyKey, BigDecimal amount);

    /**
     * Refund a previously captured charge.
     *
     * @param transactionId  original gateway transaction reference
     * @param amount         amount to refund (may be partial for penalty deduction)
     * @return PaymentResult containing refund transaction ID and outcome
     */
    PaymentResult refund(String transactionId, BigDecimal amount);

    /**
     * Returns the PaymentMethod this strategy handles.
     * Used by PaymentProcessorFactory to build the lookup map.
     */
    PaymentMethod getMethod();
}

