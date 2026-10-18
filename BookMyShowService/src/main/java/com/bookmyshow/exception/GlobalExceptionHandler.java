package com.bookmyshow.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 * GlobalExceptionHandler – Centralized Error Handling
 * ============================================================
 *
 * @RestControllerAdvice intercepts exceptions thrown from
 * any @RestController in the application and converts them
 * to structured JSON error responses.
 *
 * WHY centralized error handling?
 * - Consistent error response format across all endpoints
 * - No try-catch in controllers (keeps controllers clean)
 * - Single place to add logging/alerting for errors
 *
 * DESIGN PATTERN: Template Method (via @ExceptionHandler chain) +
 *   Centralized cross-cutting concern (AOP-like).
 */
@Slf4j
@RestControllerAdvice  // Applies to all @RestController classes in the application
public class GlobalExceptionHandler {

    /**
     * Handle ResourceNotFoundException (404 Not Found).
     * Thrown when Movie, Show, Booking, etc. doesn't exist.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());  // Log as warning (not error)
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());  // Return 404 JSON
    }

    /**
     * Handle SeatAlreadyLockedException (409 Conflict).
     * Thrown when concurrent users try to book the same seat.
     */
    @ExceptionHandler(SeatAlreadyLockedException.class)
    public ResponseEntity<Map<String, Object>> handleSeatLocked(SeatAlreadyLockedException ex) {
        log.warn("Seat lock conflict: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ex.getMessage());  // Return 409 JSON
    }

    /**
     * Handle PaymentFailedException (402 Payment Required).
     * Thrown when gateway returns failure.
     */
    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentFailed(PaymentFailedException ex) {
        log.error("Payment failed: {}", ex.getMessage());  // Log as error (needs attention)
        return buildError(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
    }

    /**
     * Handle @Valid / @Validated DTO validation failures (400 Bad Request).
     * Thrown when request body fails validation constraints.
     * Returns a map of field → error message for client-side form validation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        // Collect all field-level validation errors into a map
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());  // e.g. "email" → "must not be blank"
        }

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());     // When the error occurred
        body.put("status", HttpStatus.BAD_REQUEST.value()); // HTTP status code
        body.put("error", "Validation Failed");         // Error type
        body.put("fieldErrors", fieldErrors);           // Per-field error details

        return ResponseEntity.badRequest().body(body);  // Return 400 with field errors
    }

    /**
     * Catch-all handler for any unhandled exception (500 Internal Server Error).
     * Logs the full stack trace but returns a generic message to the client
     * (never expose stack traces to external clients in production).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);  // Log full stack trace
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");  // Safe generic message
    }

    /**
     * Helper: builds a consistent error response body.
     *
     * @param status  HTTP status code
     * @param message Error message
     * @return ResponseEntity with JSON error body
     */
    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());     // ISO timestamp
        body.put("status", status.value());             // Numeric HTTP status (e.g. 404)
        body.put("error", status.getReasonPhrase());    // Text status (e.g. "Not Found")
        body.put("message", message);                   // Application-specific error message
        return ResponseEntity.status(status).body(body);
    }
}

