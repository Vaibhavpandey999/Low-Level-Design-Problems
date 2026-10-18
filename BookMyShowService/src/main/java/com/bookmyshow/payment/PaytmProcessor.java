package com.bookmyshow.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ============================================================
 * PaytmProcessor – Concrete Strategy for Paytm Gateway
 * ============================================================
 *
 * DESIGN PATTERN: Strategy Pattern – concrete implementation of
 * PaymentProcessor for Paytm (India – wallet + UPI + cards).
 *
 * In production: use Paytm Java SDK or REST API with CHECKSUMHASH
 * for transaction integrity.
 */
@Slf4j
@Component  // Spring singleton bean
public class PaytmProcessor implements PaymentProcessor {

    /**
     * Simulate Paytm charge.
     * In production: initiate a Paytm transaction via
     * /theia/api/v1/initiateTransaction REST endpoint.
     */
    @Override
    public PaymentResult charge(Double amount, String currency, String bookingRef, String description) {
        log.info("[Paytm] Charging {} {} for booking {}", amount, currency, bookingRef);

        // Mock Paytm transaction ID
        String mockTxnId = "PAYTM" + UUID.randomUUID().toString().replace("-", "").toUpperCase();

        log.info("[Paytm] Charge successful: txnId={}", mockTxnId);

        return PaymentResult.builder()
                .success(true)
                .transactionId(mockTxnId)
                .build();
    }

    /**
     * Simulate Paytm refund.
     * In production: use Paytm's /v2/refund/apply API endpoint.
     */
    @Override
    public PaymentResult refund(String transactionId, Double amount) {
        log.info("[Paytm] Refunding {} for transaction {}", amount, transactionId);

        String mockRefundId = "PAYTM_REFUND_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();

        log.info("[Paytm] Refund successful: refundId={}", mockRefundId);

        return PaymentResult.builder()
                .success(true)
                .refundId(mockRefundId)
                .build();
    }

    /** Must match PaymentGateway.PAYTM.name() */
    @Override
    public String getGatewayName() {
        return "PAYTM";
    }
}

