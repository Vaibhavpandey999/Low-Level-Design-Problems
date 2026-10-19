package com.carrental.strategy;

import com.carrental.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;

/**
 * PaymentResult – immutable value object returned by every PaymentStrategy.
 *
 * Encapsulates the outcome of a payment or refund operation so that
 * PaymentService can handle results uniformly regardless of which
 * concrete PaymentStrategy was invoked.
 *
 * Design Pattern: Value Object (part of Strategy pattern result)
 */
@Getter               // getters for all fields
@Builder              // BUILDER PATTERN for construction
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResult {

    private boolean success;           // true if charge/refund was captured by gateway

    private String transactionId;      // gateway-issued reference ID for this transaction

    private String errorMessage;       // non-null only when success=false; explains failure reason

    private PaymentStatus status;      // maps to domain PaymentStatus enum

    private BigDecimal amountProcessed; // actual amount processed (may differ from requested on partial captures)

    /**
     * Factory method for a successful payment result.
     *
     * @param txnId  gateway transaction reference
     * @param amount amount that was processed
     */
    public static PaymentResult success(String txnId, BigDecimal amount) {
        return PaymentResult.builder()
                .success(true)                            // mark as successful
                .transactionId(txnId)                     // store gateway reference
                .status(PaymentStatus.SUCCESS)            // set domain status
                .amountProcessed(amount)                  // record processed amount
                .build();
    }

    /**
     * Factory method for a failed payment result.
     *
     * @param reason human-readable error from gateway
     */
    public static PaymentResult failure(String reason) {
        return PaymentResult.builder()
                .success(false)                           // mark as failed
                .errorMessage(reason)                     // store error reason for logging
                .status(PaymentStatus.FAILED)             // set domain status
                .build();
    }
}

