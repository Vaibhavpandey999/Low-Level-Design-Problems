package com.bookmyshow.kafka.events;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * BookingCancelledEvent – Kafka message payload for cancellations.
 *
 * Published to "booking-cancelled" topic.
 * Consumers: notification service (email+SMS), refund processor,
 * analytics pipeline, seat availability updater.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCancelledEvent implements Serializable {

    private Long bookingId;            // DB ID of the cancelled booking
    private String bookingReference;   // Reference number
    private Long userId;               // User whose booking was cancelled
    private String userEmail;          // For email notification
    private String userPhone;          // For SMS notification
    private Double refundAmount;       // Amount to be refunded
    private LocalDateTime cancelledAt; // Timestamp of cancellation
}

