package com.carrental.repository.jpa;

import com.carrental.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * PaymentRepository – Spring Data JPA REPOSITORY PATTERN for Payment entity.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by the rental it belongs to.
     * Used to fetch payment details on the rental detail page.
     *
     * @param rentalId rental primary key
     */
    Optional<Payment> findByRentalId(Long rentalId);

    /**
     * Find a payment by the gateway transaction reference.
     * Used for idempotency checking and webhook reconciliation.
     *
     * @param transactionId gateway-issued transaction ID
     */
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * Check if a payment already exists for the given idempotency key.
     * Prevents double-charging on network retries.
     *
     * @param idempotencyKey rental reference used as idempotency key
     */
    boolean existsByIdempotencyKey(String idempotencyKey);
}

