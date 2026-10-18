package com.bookmyshow.enums;

/**
 * Represents the lifecycle states of a booking.
 *
 * State transitions:
 *   PENDING ──► CONFIRMED  (payment success)
 *   PENDING ──► CANCELLED  (payment failed or user cancelled)
 *   CONFIRMED ──► CANCELLED (user cancels after payment)
 *   CANCELLED ──► REFUNDED  (refund processed)
 */
public enum BookingStatus {
    PENDING,      // Booking initiated, seats locked, awaiting payment
    CONFIRMED,    // Payment succeeded, seats permanently reserved
    CANCELLED,    // Booking cancelled by user or due to payment failure
    REFUNDED      // Refund issued after cancellation of a CONFIRMED booking
}

