package com.carrental.enums;

/**
 * PaymentMethod – the payment channel chosen by the customer at checkout.
 *
 * CREDIT_CARD : card-based payment via a payment gateway (e.g., Stripe / Razorpay)
 * UPI         : Unified Payments Interface (India-specific instant bank transfer)
 * WALLET      : in-app wallet balance (pre-funded by the user)
 *
 * This enum is the key used by PaymentProcessorFactory to resolve the
 * correct PaymentStrategy implementation at runtime (Strategy + Factory pattern).
 */
public enum PaymentMethod {
    CREDIT_CARD,  // card payment routed through a payment gateway
    UPI,          // instant bank transfer via UPI handle
    WALLET        // debit from in-app pre-loaded wallet balance
}

