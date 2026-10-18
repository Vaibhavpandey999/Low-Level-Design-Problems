package com.bookmyshow.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * ============================================================
 * BookingItem Entity – maps to 'booking_items' table
 * ============================================================
 *
 * Represents a single seat within a Booking.
 * Normalization: one row per seat, allows multi-seat bookings
 * cleanly and supports per-seat cancellation in the future.
 *
 * DESIGN PATTERN: Builder (Lombok @Builder)
 */
@Entity
@Table(name = "booking_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Many BookingItems belong to one Booking.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)   // FK to bookings table
    private Booking booking;      // Parent booking

    /**
     * Each BookingItem is tied to a specific ShowSeat.
     * This captures which seat in which show was booked.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_seat_id", nullable = false)  // FK to show_seats table
    private ShowSeat showSeat;    // The (show, seat) pair booked

    @Column(nullable = false)
    private Double price;         // Actual price paid for this seat (snapshot at booking time)
}

