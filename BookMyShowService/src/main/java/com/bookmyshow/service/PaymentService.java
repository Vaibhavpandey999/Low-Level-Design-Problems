package com.bookmyshow.service;

import com.bookmyshow.enums.PaymentStatus;
import com.bookmyshow.exception.PaymentFailedException;
import com.bookmyshow.exception.ResourceNotFoundException;
import com.bookmyshow.model.Booking;
import com.bookmyshow.model.Payment;
import com.bookmyshow.payment.PaymentProcessor;
import com.bookmyshow.payment.PaymentProcessorFactory;
import com.bookmyshow.payment.PaymentResult;
import com.bookmyshow.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================
 * PaymentService – Payment Processing
 * ============================================================
 *
 * Orchestrates charging and refunding via the Strategy pattern.
 * Delegates to the correct gateway via PaymentProcessorFactory.
 *
 * DESIGN PATTERN: Strategy – uses PaymentProcessorFactory to
 *   select the appropriate PaymentProcessor at runtime.
 * DESIGN PATTERN: Facade – hides factory + processor complexity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;       // JPA repo for Payment entity
    private final PaymentProcessorFactory processorFactory;  // Factory to get the right strategy

    /**
     * Process payment for a booking.
     *
     * Steps:
     * 1. Get the correct payment processor via Factory
     * 2. Call charge() on the Strategy
     * 3. Persist Payment record with result
     *
     * @param booking  The booking being paid for
     * @param payment  Payment entity (already created with INITIATED status)
     * @return Updated Payment with SUCCESS/FAILED status and gateway transaction ID
     */
    @Transactional  // DB write must be atomic
    public Payment processPayment(Booking booking, Payment payment) {
        log.info("[Payment] Processing payment for bookingId={} | gateway={} | amount={}",
                booking.getId(), payment.getGateway(), payment.getAmount());

        // STEP 1: Factory resolves the correct Strategy (Razorpay/Stripe/Paytm)
        // DESIGN PATTERN: Factory selects Strategy
        PaymentProcessor processor = processorFactory.getProcessor(payment.getGateway());

        // STEP 2: Call charge via Strategy interface (polymorphism)
        PaymentResult result = processor.charge(
                payment.getAmount(),                    // Amount to charge
                "INR",                                  // Currency (all amounts in INR)
                booking.getBookingReference(),          // Idempotency key
                "Booking: " + booking.getBookingReference() + " | " +
                        booking.getShow().getMovie().getTitle()  // Description for bank statement
        );

        // STEP 3: Update Payment based on gateway result
        if (result.isSuccess()) {
            // Payment succeeded: update status and store gateway transaction ID
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setGatewayTransactionId(result.getTransactionId());  // For reconciliation
            payment.setGatewayOrderId(result.getOrderId());              // For refund reference
            log.info("[Payment] SUCCESS: bookingId={} | txnId={}", booking.getId(), result.getTransactionId());
        } else {
            // Payment failed: store error information
            payment.setStatus(PaymentStatus.FAILED);
            log.error("[Payment] FAILED: bookingId={} | error={}", booking.getId(), result.getErrorMessage());
        }

        // STEP 4: Persist payment status to PostgreSQL
        return paymentRepository.save(payment);  // Save updated payment record
    }

    /**
     * Process a refund for a cancelled confirmed booking.
     *
     * @param bookingId The booking ID to refund
     * @return Updated Payment with REFUNDED status
     */
    @Transactional
    public Payment processRefund(Long bookingId) {
        // Find existing payment for this booking
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment for booking", bookingId));

        // Can only refund a successfully charged payment
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new PaymentFailedException(
                "Cannot refund payment with status: " + payment.getStatus()
            );
        }

        log.info("[Payment] Processing refund for bookingId={} | txnId={} | amount={}",
                bookingId, payment.getGatewayTransactionId(), payment.getAmount());

        // Get the correct processor and call refund via Strategy
        PaymentProcessor processor = processorFactory.getProcessor(payment.getGateway());
        PaymentResult refundResult = processor.refund(
                payment.getGatewayTransactionId(),  // Original transaction to refund
                payment.getAmount()                 // Full refund amount
        );

        if (refundResult.isSuccess()) {
            payment.setStatus(PaymentStatus.REFUNDED);         // Update to REFUNDED
            payment.setRefundId(refundResult.getRefundId());   // Store refund ID for tracking
            log.info("[Payment] Refund SUCCESS: bookingId={} | refundId={}", bookingId, refundResult.getRefundId());
        } else {
            throw new PaymentFailedException("Refund failed for bookingId: " + bookingId);
        }

        return paymentRepository.save(payment);  // Persist refunded status
    }
}

