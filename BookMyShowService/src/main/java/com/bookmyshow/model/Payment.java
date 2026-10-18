package com.bookmyshow.model;

import com.bookmyshow.enums.PaymentGateway;
import com.bookmyshow.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ============================================================
 * Payment Entity – maps to 'payments' table in PostgreSQL
 * ============================================================
 *
 * Tracks a single payment transaction associated with a Booking.
 * One Booking → One Payment (1:1 relationship).
 *
 * Stores the gateway's transaction ID for reconciliation
 * and supports refund tracking.
 *
 * DESIGN PATTERN: Builder (Lombok @Builder)
 */
@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * One-to-one link with Booking.
     * LAZY: avoid loading full Booking when querying payment status.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)  // FK + unique ensures 1:1
    private Booking booking;      // Associated booking

    @Column(nullable = false)
    private Double amount;        // Amount charged in INR

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentGateway gateway;  // Which gateway processed the payment (Razorpay/Stripe/Paytm)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.INITIATED;  // Initial state

    @Column(name = "gateway_transaction_id")
    private String gatewayTransactionId;  // External ID from gateway (for reconciliation/refunds)

    @Column(name = "gateway_order_id")
    private String gatewayOrderId;        // Order ID created at gateway (Razorpay order_id etc.)

    @Column(name = "refund_id")
    private String refundId;              // Gateway refund ID (set when status = REFUNDED)

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

