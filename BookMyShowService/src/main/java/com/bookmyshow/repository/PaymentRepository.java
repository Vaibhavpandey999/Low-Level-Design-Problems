package com.bookmyshow.repository;

import com.bookmyshow.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * PaymentRepository – JPA repository for Payment entity.
 * DESIGN PATTERN: Repository Pattern
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment record for a given booking.
     * Used to check existing payment status and for refund processing.
     * Derived: SELECT * FROM payments WHERE booking_id = ?
     */
    Optional<Payment> findByBookingId(Long bookingId);

    /**
     * Find payment by gateway's transaction ID.
     * Used to process payment gateway webhooks/callbacks.
     * e.g. Razorpay sends payment.captured webhook with transaction ID.
     * Derived: SELECT * FROM payments WHERE gateway_transaction_id = ?
     */
    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);
}

