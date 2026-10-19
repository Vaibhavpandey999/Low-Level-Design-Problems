package com.carrental.exception;

/** InvalidRentalStateException – thrown when a state transition is not permitted. Maps to HTTP 400. */
public class InvalidRentalStateException extends RuntimeException {
    public InvalidRentalStateException(String message) { super(message); }
}

