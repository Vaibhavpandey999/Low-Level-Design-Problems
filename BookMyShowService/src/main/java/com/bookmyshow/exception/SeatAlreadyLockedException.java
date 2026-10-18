package com.bookmyshow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user tries to lock a seat that is already
 * locked by another user or marked as BOOKED.
 *
 * @ResponseStatus: Spring MVC maps this exception → HTTP 409 Conflict
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class SeatAlreadyLockedException extends RuntimeException {
    public SeatAlreadyLockedException(String message) {
        super(message);  // Pass message to RuntimeException constructor
    }
}

