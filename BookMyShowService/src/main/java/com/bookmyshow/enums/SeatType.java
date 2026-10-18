package com.bookmyshow.enums;

/**
 * Physical seat categories inside an auditorium/screen.
 * Determines the base price multiplier for a seat.
 */
public enum SeatType {
    NORMAL,     // Regular seat – base price (e.g. ₹150)
    PREMIUM,    // Better view / more legroom – 1.5x base price (e.g. ₹225)
    RECLINER    // Luxury recliner – 2x base price (e.g. ₹300)
}

