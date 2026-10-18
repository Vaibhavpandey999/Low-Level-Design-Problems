package com.bookmyshow.kafka.events;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * BookingConfirmedEvent – Kafka message payload
 *
 * Published to Kafka topic "booking-confirmed" when a booking
 * is successfully confirmed (payment succeeded).
 *
 * Implements Serializable for JSON serialization via Jackson.
 *
 * WHY Kafka instead of synchronous calls?
 * - Decouples booking from downstream consumers (notifications, reports)
 * - Consumers can fail and replay from last committed offset
 * - Booking response is returned to user immediately
 * - Multiple consumer groups can process the same event independently
 *
 * DESIGN PATTERN: Event-Driven Architecture (EDA)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfirmedEvent implements Serializable {

    private Long bookingId;            // DB ID of the confirmed booking
    private String bookingReference;   // Human-readable reference (BMS-xxx)
    private Long userId;               // User who made the booking
    private String userEmail;          // Denormalized email (avoid DB lookup in consumers)
    private String userPhone;          // Denormalized phone (for SMS consumer)
    private Long showId;               // Show that was booked
    private String movieTitle;         // Denormalized movie title (for email body)
    private Double totalAmount;        // Amount paid in INR
    private LocalDateTime confirmedAt; // Timestamp of confirmation
}

