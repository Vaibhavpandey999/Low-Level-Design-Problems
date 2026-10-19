package com.carrental.enums;

/**
 * RentalStatus – state machine for the lifecycle of a car rental.
 *
 * State transitions:
 *   PENDING   → ACTIVE      : payment succeeded, car handed over
 *   PENDING   → CANCELLED   : payment failed or user cancelled before pickup
 *   ACTIVE    → COMPLETED   : car returned, final charges settled
 *   ACTIVE    → CANCELLED   : admin override (exceptional case)
 *
 * Only PENDING and ACTIVE rentals can be cancelled by the customer.
 * COMPLETED and CANCELLED are terminal states.
 */
public enum RentalStatus {
    PENDING,    // booking created, awaiting payment confirmation
    ACTIVE,     // payment confirmed, car is with the customer
    COMPLETED,  // car returned, billing finalised
    CANCELLED   // booking was cancelled (before or during rental)
}

