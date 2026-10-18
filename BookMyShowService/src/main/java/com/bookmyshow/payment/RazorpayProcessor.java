package com.bookmyshow.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ============================================================
 * RazorpayProcessor – Concrete Strategy for Razorpay Gateway
 * ============================================================
 *
 * DESIGN PATTERN: Strategy Pattern – concrete implementation of
 * PaymentProcessor interface for Razorpay (India's most popular
 * payment gateway supporting UPI, cards, netbanking, wallets).
 *
 * In production this would use the Razorpay Java SDK:
 *   com.razorpay:razorpay-java
 * For this LLD, we simulate the API calls with mock responses.
 */
@Slf4j               // Lombok: generates a static 'log' field using SLF4J
@Component           // Spring-managed bean – singleton by default
public class RazorpayProcessor implements PaymentProcessor {

    /**
     * Simulate charging via Razorpay.
     * In production: call RazorpayClient.orders.create() then capture payment.
     *
     * @param amount      Amount in INR
     * @param currency    e.g. "INR"
     * @param bookingRef  Idempotency key (our booking reference)
     * @param description Order description shown to user
     * @return PaymentResult with simulated success and transaction ID
     */
    @Override
    public PaymentResult charge(Double amount, String currency, String bookingRef, String description) {
        // LOG: record every charge attempt for audit trail
        log.info("[Razorpay] Charging {} {} for booking {}", amount, currency, bookingRef);

        // SIMULATE: In production, call Razorpay REST API here
        // JSONObject orderRequest = new JSONObject();
        // orderRequest.put("amount", amount * 100);  // Razorpay uses paise (₹1 = 100 paise)
        // orderRequest.put("currency", currency);
        // orderRequest.put("receipt", bookingRef);
        // Order order = razorpayClient.orders.create(orderRequest);

        // Mock successful response
        String mockTransactionId = "rzp_" + UUID.randomUUID().toString().replace("-", "");  // Simulate Razorpay txn ID format
        String mockOrderId       = "order_" + UUID.randomUUID().toString().replace("-", ""); // Simulate Razorpay order ID format

        log.info("[Razorpay] Charge successful: transactionId={}", mockTransactionId);

        // Build and return result Value Object
        return PaymentResult.builder()
                .success(true)                         // Payment succeeded (mock)
                .transactionId(mockTransactionId)      // Gateway's payment ID
                .orderId(mockOrderId)                  // Gateway's order ID
                .build();
    }

    /**
     * Simulate refund via Razorpay.
     * In production: call RazorpayClient.payments.refund(transactionId, refundRequest)
     *
     * @param transactionId Razorpay payment ID (rzp_xxx) to refund
     * @param amount        Amount to refund in INR
     * @return PaymentResult with refund ID
     */
    @Override
    public PaymentResult refund(String transactionId, Double amount) {
        // LOG: record refund attempt
        log.info("[Razorpay] Refunding {} for transaction {}", amount, transactionId);

        // Mock refund ID
        String mockRefundId = "rfnd_" + UUID.randomUUID().toString().replace("-", "");

        log.info("[Razorpay] Refund successful: refundId={}", mockRefundId);

        return PaymentResult.builder()
                .success(true)           // Refund succeeded (mock)
                .refundId(mockRefundId)  // Gateway's refund ID for tracking
                .build();
    }

    /**
     * Returns the gateway name for factory lookup.
     * Must match PaymentGateway.RAZORPAY.name()
     */
    @Override
    public String getGatewayName() {
        return "RAZORPAY";  // Matches PaymentGateway enum name
    }
}

