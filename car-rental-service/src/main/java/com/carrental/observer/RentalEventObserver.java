package com.carrental.observer;

import com.carrental.kafka.events.RentalBookedEvent;
import com.carrental.kafka.events.RentalCancelledEvent;

/**
 * RentalEventObserver – Observer interface for the OBSERVER PATTERN.
 *
 * =============================================
 * Design Pattern: OBSERVER PATTERN
 * =============================================
 * Defines the callback contract that all observers (email, SMS, analytics)
 * must implement. When a rental event occurs, the subject (RentalEventPublisher)
 * notifies all registered observers without knowing what they do.
 *
 * Benefits:
 * - Decouples booking business logic from side effects (email/SMS/analytics)
 * - New notification channels added without modifying BookingService or RentalFacade
 * - Observers run @Async – booking response returned before emails are sent
 */
public interface RentalEventObserver {

    /**
     * Called when a car rental booking is confirmed and payment succeeds.
     *
     * @param event contains rentalId, carId, userId, dates, amount
     */
    void onRentalBooked(RentalBookedEvent event);

    /**
     * Called when a rental is cancelled (by user or system).
     *
     * @param event contains rentalId, refundAmount, cancellationReason
     */
    void onRentalCancelled(RentalCancelledEvent event);
}

