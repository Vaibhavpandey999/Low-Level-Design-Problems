package com.carrental.exception;

/** ResourceNotFoundException – thrown when a requested entity is not found in DB. Maps to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}

