package com.bookmyshow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when a Show, Movie, Booking, or Theatre is not found by ID. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " not found with id: " + id);  // e.g. "Show not found with id: 42"
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

