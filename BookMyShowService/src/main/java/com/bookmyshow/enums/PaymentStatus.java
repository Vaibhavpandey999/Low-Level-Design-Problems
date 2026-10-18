package com.bookmyshow.enums;

/**
 * Lifecycle states for a payment transaction.
 *
 * Transitions:
 *   INITIATED ──► SUCCESS  (gateway confirms charge)
 *   INITIATED ──► FAILED   (gateway rejects / timeout)
 *   SUCCESS   ──► REFUNDED (booking cancelled, money returned)
 */
public enum PaymentStatus {
    INITIATED,  // Payment request sent to gateway, awaiting callback
    SUCCESS,    // Gateway confirmed successful charge
    FAILED,     // Gateway rejected or no callback within timeout
    REFUNDED    // Full/partial refund successfully processed by gateway
}

