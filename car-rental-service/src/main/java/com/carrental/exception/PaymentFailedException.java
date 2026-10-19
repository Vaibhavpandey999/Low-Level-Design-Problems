package com.carrental.exception;

/** PaymentFailedException – thrown when a payment gateway rejects a charge. Maps to HTTP 402. */
public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String message) { super(message); }
}

