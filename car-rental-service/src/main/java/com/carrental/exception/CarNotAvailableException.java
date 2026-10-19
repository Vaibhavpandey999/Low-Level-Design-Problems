package com.carrental.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * CarNotAvailableException – thrown when a car cannot be booked.
 *
 * Triggers a 409 Conflict HTTP response via GlobalExceptionHandler.
 * Scenarios:
 * - Car is already RENTED or in MAINTENANCE
 * - Another user holds the Redis booking lock on this car
 * - Optimistic lock conflict detected on Car entity (@Version mismatch)
 */
@ResponseStatus(HttpStatus.CONFLICT)  // maps to HTTP 409 if not handled by GlobalExceptionHandler
public class CarNotAvailableException extends RuntimeException {

    public CarNotAvailableException(String message) {
        super(message);  // message is returned in the error response body
    }
}

