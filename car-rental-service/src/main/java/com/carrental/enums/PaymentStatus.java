package com.carrental.enums;

/**
 * PaymentStatus – tracks the outcome of a payment transaction.
 *
 * PENDING  : payment initiated but gateway response not yet received
 * SUCCESS  : gateway confirmed charge; rental moves to ACTIVE
 * FAILED   : gateway rejected charge; rental stays PENDING then cancelled
 * REFUNDED : charge reversed after cancellation or dispute resolution
 */
public enum PaymentStatus {
    PENDING,   // waiting for gateway response
    SUCCESS,   // charge captured successfully
    FAILED,    // charge declined or gateway error
    REFUNDED   // funds returned to customer
}

