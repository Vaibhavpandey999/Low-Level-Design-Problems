package com.bookmyshow.payment;

import lombok.*;

/**
 * PaymentResult – Value Object returned by PaymentProcessor implementations.
 *
 * Encapsulates the outcome of a charge or refund call to a gateway.
 * Using a Value Object keeps the PaymentProcessor interface clean
 * and avoids coupling to gateway-specific response classes.
 *
 * DESIGN PATTERN: Builder (Lombok @Builder) + Value Object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResult {

    private boolean success;              // true if charge/refund succeeded
    private String transactionId;         // Gateway's unique transaction ID
    private String orderId;               // Gateway's order ID (if applicable)
    private String refundId;              // Refund ID (populated on refund calls)
    private String errorCode;             // Gateway error code (if !success)
    private String errorMessage;          // Human-readable error (if !success)
}

