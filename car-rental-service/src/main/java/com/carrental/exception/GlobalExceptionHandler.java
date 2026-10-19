package com.carrental.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler – centralised error handling for all controllers.
 *
 * @RestControllerAdvice intercepts exceptions thrown from any @RestController
 * and converts them into structured JSON error responses.
 *
 * Benefits:
 * - Controllers don't need try/catch blocks – clean, readable code
 * - Consistent error response format across all endpoints
 * - Prevents stack traces from leaking to clients
 */
@RestControllerAdvice  // applies to all @RestController beans globally
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle car not available – typically a booking conflict.
     * Returns 409 Conflict with details about why the car can't be booked.
     */
    @ExceptionHandler(CarNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleCarNotAvailable(CarNotAvailableException ex) {
        log.warn("[ExceptionHandler] CarNotAvailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)           // HTTP 409
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    /**
     * Handle Hibernate optimistic locking failure – two concurrent bookings race condition.
     * Returns 409 Conflict with a retry suggestion.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.warn("[ExceptionHandler] OptimisticLockConflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)           // HTTP 409
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(),
                        "Concurrent booking conflict. Please retry your request."));
    }

    /**
     * Handle entity not found – e.g., car ID, branch ID, rental ID doesn't exist.
     * Returns 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("[ExceptionHandler] ResourceNotFound: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)          // HTTP 404
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    /**
     * Handle payment failure – gateway declined the charge.
     * Returns 402 Payment Required.
     */
    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentFailed(PaymentFailedException ex) {
        log.error("[ExceptionHandler] PaymentFailed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)   // HTTP 402
                .body(new ErrorResponse(HttpStatus.PAYMENT_REQUIRED.value(), ex.getMessage()));
    }

    /**
     * Handle invalid rental state transitions – e.g., cancelling a completed rental.
     * Returns 400 Bad Request.
     */
    @ExceptionHandler(InvalidRentalStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidRentalStateException ex) {
        log.warn("[ExceptionHandler] InvalidRentalState: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)        // HTTP 400
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    /**
     * Handle Spring Security access denial – user lacks required role.
     * Returns 403 Forbidden.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("[ExceptionHandler] AccessDenied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)          // HTTP 403
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Access denied"));
    }

    /**
     * Handle @Valid bean validation failures – returns all field errors.
     * Returns 400 Bad Request with a map of field → error message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());  // field name → error message
        }

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());               // 400
        body.put("errors", fieldErrors);                                    // map of validation errors
        body.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.badRequest().body(body);                     // HTTP 400
    }

    /**
     * Catch-all handler for unexpected exceptions.
     * Returns 500 Internal Server Error with a generic message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("[ExceptionHandler] Unexpected error", ex);              // full stack trace in logs
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)    // HTTP 500
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "An unexpected error occurred. Please try again later."));
    }

    /**
     * Standardised error response body returned for all error responses.
     */
    public record ErrorResponse(int status, String message) {
        // record auto-generates constructor, getters, equals, hashCode, toString
    }
}

