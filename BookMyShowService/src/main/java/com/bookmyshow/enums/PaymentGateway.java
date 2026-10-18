package com.bookmyshow.enums;

/**
 * Supported external payment gateways.
 * Used by PaymentProcessorFactory (Factory Pattern) to resolve
 * the correct PaymentProcessor strategy at runtime.
 *
 * DESIGN PATTERN: Strategy + Factory – the enum value acts as the
 * key to select and return the appropriate strategy implementation.
 */
public enum PaymentGateway {
    RAZORPAY,   // Indian payment gateway (UPI, cards, netbanking)
    STRIPE,     // International gateway (credit/debit cards)
    PAYTM       // Wallet + UPI gateway (India)
}

