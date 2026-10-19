package com.carrental.kafka.events;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RentalCancelledEvent – Kafka event published when a rental is cancelled.
 *
 * Consumed by:
 * - NotificationService: sends email/SMS about the cancellation and refund
 * - RefundService:        processes refund via the original payment gateway
 * - AnalyticsService:     records cancellation rate metrics
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalCancelledEvent {

    private Long rentalId;              // rental that was cancelled
    private String rentalReference;     // human-readable reference
    private Long userId;                // customer who cancelled
    private Long carId;                 // car that was released back to fleet
    private BigDecimal refundAmount;    // amount to be refunded (original − penalty)
    private BigDecimal penaltyAmount;   // cancellation penalty charged
    private String cancellationReason;  // reason provided by user (optional)
    private LocalDateTime eventTime;    // event publication timestamp
}

