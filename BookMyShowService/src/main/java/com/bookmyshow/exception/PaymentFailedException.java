package com.bookmyshow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when payment gateway returns failure or refund fails. */
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String message) { super(message); }
}

