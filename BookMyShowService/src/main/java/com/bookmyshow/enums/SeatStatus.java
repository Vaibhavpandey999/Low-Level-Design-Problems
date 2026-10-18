package com.bookmyshow.enums;

/**
 * Represents the real-time status of a seat for a given show.
 * Stored in ShowSeat entity + mirrored in Redis for fast reads.
 *
 * State transitions:
 *   AVAILABLE ──► LOCKED    (user selects seat, Redis TTL lock applied)
 *   LOCKED    ──► BOOKED    (payment confirmed)
 *   LOCKED    ──► AVAILABLE (lock expires after TTL or user abandons)
 *   BOOKED    ──► AVAILABLE (booking cancelled / refunded)
 */
public enum SeatStatus {
    AVAILABLE,  // Seat is free to be selected by any user
    LOCKED,     // Seat is temporarily held (max 10 min) for a specific user
    BOOKED      // Seat is permanently reserved with a confirmed payment
}

