package com.bookmyshow.payment;

/**
 * ============================================================
 * PaymentProcessor – Strategy Interface
 * ============================================================
 *
 * DESIGN PATTERN: Strategy Pattern
 *
 * Defines the contract that all payment gateway implementations
 * must fulfil. The Strategy pattern allows us to swap between
 * Razorpay, Stripe, and Paytm at runtime without changing the
 * client code (PaymentService).
 *
 * Client (PaymentService) depends on this interface, NOT on
 * any concrete implementation → Open/Closed Principle (OCP).
 *
 * How it works:
 *   1. PaymentProcessorFactory selects the right concrete strategy.
 *   2. PaymentService calls charge() / refund() on the interface.
 *   3. The concrete class (Razorpay/Stripe/Paytm) executes the logic.
 */
public interface PaymentProcessor {

    /**
     * Initiates a charge at the payment gateway.
     *
     * @param amount         Amount to charge in INR (Indian Rupees)
     * @param currency       Currency code (e.g. "INR", "USD")
     * @param bookingRef     Unique booking reference for idempotency
     * @param description    Human-readable description (shown on statement)
     * @return PaymentResult containing gateway transaction ID and status
     */
    PaymentResult charge(Double amount, String currency, String bookingRef, String description);

    /**
     * Initiates a refund for an existing transaction.
     *
     * @param transactionId  Original gateway transaction ID
     * @param amount         Amount to refund (can be partial)
     * @return PaymentResult with refund ID and status
     */
    PaymentResult refund(String transactionId, Double amount);

    /**
     * Returns the name of the payment gateway this processor handles.
     * Used by PaymentProcessorFactory to identify strategies.
     *
     * @return gateway name matching PaymentGateway enum
     */
    String getGatewayName();
}

