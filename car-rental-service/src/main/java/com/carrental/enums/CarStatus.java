package com.carrental.enums;

/**
 * CarStatus – tracks the real-time operational state of a car in the fleet.
 *
 * AVAILABLE   : car is at a branch and can be booked
 * RENTED      : car is currently on an active rental (cannot be double-booked)
 * MAINTENANCE : car is off-road for servicing (not bookable)
 *
 * This value is stored on the Car entity and is protected by optimistic locking
 * (@Version) to prevent concurrent updates from two booking requests.
 */
public enum CarStatus {
    AVAILABLE,    // ready to be booked
    RENTED,       // currently out with a customer
    MAINTENANCE   // undergoing service – not available for booking
}

