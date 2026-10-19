package com.carrental.model;

import com.carrental.enums.PaymentMethod;
import com.carrental.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment – records a payment transaction associated with a Rental.
 *
 * Each rental has at most one Payment record. The transactionId returned
 * by the payment gateway is stored here and used as the refund reference.
 *
 * The rentalReference is stored as a denormalised idempotency key so that
 * duplicate payment submissions (network retries) can be detected and
 * rejected without querying the Rental table.
 *
 * Design Pattern: Builder (Lombok @Builder)
 */
@Entity
@Table(name = "payments",
        indexes = {
            @Index(name = "idx_payments_rental",    columnList = "rental_id"),     // lookup by rental
            @Index(name = "idx_payments_txn",       columnList = "transaction_id") // lookup by gateway reference
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)                          // one payment per rental
    @JoinColumn(name = "rental_id", nullable = false, unique = true)
    private Rental rental;                                     // the rental this payment belongs to

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;                                 // total amount charged

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;                              // CREDIT_CARD / UPI / WALLET

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;                              // PENDING / SUCCESS / FAILED / REFUNDED

    private String transactionId;                              // external gateway reference (nullable until charged)

    private String refundTransactionId;                        // gateway refund reference (set on cancellation)

    @Column(nullable = false, unique = true)
    private String idempotencyKey;                             // rentalReference reused – prevents double charge

    @CreationTimestamp
    private LocalDateTime createdAt;                           // timestamp when payment was initiated

    private LocalDateTime completedAt;                         // timestamp when gateway confirmed SUCCESS/FAILED
}

