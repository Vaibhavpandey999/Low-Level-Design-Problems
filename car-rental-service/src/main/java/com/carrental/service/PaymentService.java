package com.carrental.service;

import com.carrental.exception.PaymentFailedException;
import com.carrental.factory.PaymentProcessorFactory;
import com.carrental.model.Payment;
import com.carrental.model.Rental;
import com.carrental.repository.jpa.PaymentRepository;
import com.carrental.strategy.PaymentResult;
import com.carrental.strategy.PaymentStrategy;
import com.carrental.enums.PaymentMethod;
import com.carrental.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PaymentService – orchestrates the full payment lifecycle using the STRATEGY PATTERN.
 *
 * =============================================
 * Design Pattern: STRATEGY PATTERN (via Factory)
 * =============================================
 * PaymentService does NOT know which payment gateway is used.
 * It delegates to PaymentProcessorFactory which resolves the correct
 * PaymentStrategy (CreditCard, UPI, Wallet) at runtime.
 *
 * =============================================
 * Design Pattern: TEMPLATE METHOD (partial)
 * =============================================
 * processPayment() defines a fixed algorithm skeleton:
 *   1. Idempotency check
 *   2. Create PENDING payment record
 *   3. Delegate to strategy
 *   4. Update payment status
 *   5. Handle failure
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentProcessorFactory processorFactory; // FACTORY PATTERN – resolves strategy
    private final PaymentRepository paymentRepository;      // REPOSITORY PATTERN – persists payment

    /**
     * Process a payment for a rental.
     *
     * @param rental  the rental to charge for
     * @param method  payment channel chosen by customer
     * @param amount  amount to charge
     * @return saved Payment entity with SUCCESS status
     * @throws PaymentFailedException if gateway declines the charge
     */
    @Transactional  // entire method runs in one DB transaction
    public Payment processPayment(Rental rental, PaymentMethod method, BigDecimal amount) {
        String idempotencyKey = rental.getRentalReference();  // reuse rental ref as idempotency key

        // ── Step 1: Idempotency check ──────────────────────────────────────────────────
        // If a payment already exists for this idempotency key, return it without charging again
        if (paymentRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.warn("[PaymentService] Duplicate payment attempt. idempotencyKey={}", idempotencyKey);
            return paymentRepository.findByRentalId(rental.getId())
                    .orElseThrow(() -> new PaymentFailedException("Idempotency conflict"));
        }
        // ──────────────────────────────────────────────────────────────────────────────

        // ── Step 2: Create PENDING payment record ──────────────────────────────────────
        // Persist payment before calling gateway – so we have a record even if gateway times out
        Payment payment = Payment.builder()
                .rental(rental)               // link to the rental
                .amount(amount)               // amount to charge
                .method(method)               // payment channel
                .status(PaymentStatus.PENDING) // starts as PENDING until gateway responds
                .idempotencyKey(idempotencyKey) // deduplication key
                .build();
        payment = paymentRepository.save(payment);  // persist PENDING record
        log.info("[PaymentService] Payment record created (PENDING). id={}, method={}", payment.getId(), method);
        // ──────────────────────────────────────────────────────────────────────────────

        // ── Step 3: Resolve strategy via Factory and delegate charge ───────────────────
        // STRATEGY PATTERN: factory picks the right gateway implementation
        PaymentStrategy strategy = processorFactory.getProcessor(method);
        PaymentResult result = strategy.process(idempotencyKey, amount);  // call gateway
        // ──────────────────────────────────────────────────────────────────────────────

        // ── Step 4: Update payment record based on gateway response ────────────────────
        if (result.isSuccess()) {
            payment.setStatus(PaymentStatus.SUCCESS);          // mark successful
            payment.setTransactionId(result.getTransactionId()); // store gateway reference
            payment.setCompletedAt(LocalDateTime.now());        // record completion timestamp
            paymentRepository.save(payment);                    // update DB record
            log.info("[PaymentService] Payment SUCCESS. txnId={}", result.getTransactionId());
        } else {
            // ── Step 5: Handle failure ─────────────────────────────────────────────────
            payment.setStatus(PaymentStatus.FAILED);            // mark as failed
            paymentRepository.save(payment);                    // persist failure status
            log.error("[PaymentService] Payment FAILED. reason={}", result.getErrorMessage());
            throw new PaymentFailedException("Payment failed: " + result.getErrorMessage());
        }
        // ──────────────────────────────────────────────────────────────────────────────

        return payment;  // return SUCCESS payment to RentalFacade
    }

    /**
     * Refund a previously processed payment.
     *
     * @param payment        original payment to refund
     * @param refundAmount   amount to refund (may be less than original due to penalty)
     */
    @Transactional
    public void refundPayment(Payment payment, BigDecimal refundAmount) {
        log.info("[PaymentService] Processing refund. paymentId={}, amount={}", payment.getId(), refundAmount);

        // Resolve the same strategy used for the original payment
        PaymentStrategy strategy = processorFactory.getProcessor(payment.getMethod());

        // Call the refund method on the strategy (each gateway has its own refund API)
        PaymentResult refundResult = strategy.refund(payment.getTransactionId(), refundAmount);

        if (refundResult.isSuccess()) {
            payment.setStatus(PaymentStatus.REFUNDED);                       // mark as refunded
            payment.setRefundTransactionId(refundResult.getTransactionId()); // store refund reference
            paymentRepository.save(payment);                                  // persist
            log.info("[PaymentService] Refund SUCCESS. refundTxnId={}", refundResult.getTransactionId());
        } else {
            // Refund failure is logged but NOT thrown – manual reconciliation needed
            log.error("[PaymentService] Refund FAILED. paymentId={}, reason={}", payment.getId(), refundResult.getErrorMessage());
        }
    }
}

