package com.bookmyshow.observer;

import com.bookmyshow.model.Booking;

/**
 * ============================================================
 * BookingEventObserver – Observer Interface
 * ============================================================
 *
 * DESIGN PATTERN: Observer Pattern (also known as Publish-Subscribe)
 *
 * Defines the contract for all observers that want to react
 * to booking lifecycle events (confirmed, cancelled, refunded).
 *
 * Concrete observers:
 *   - EmailNotificationObserver  → sends confirmation/cancellation email
 *   - SMSNotificationObserver    → sends SMS via Twilio/SNS
 *   - AnalyticsObserver          → records event in analytics pipeline
 *
 * WHY Observer here?
 * BookingService (the Subject) just calls onBookingConfirmed() /
 * onBookingCancelled() – it doesn't know or care HOW many observers
 * are registered or what they do. This fully decouples the booking
 * business logic from notification/analytics side effects.
 */
public interface BookingEventObserver {

    /**
     * Called when a booking is successfully confirmed (payment succeeded).
     *
     * @param booking The confirmed Booking entity (contains user, show, seats, amount)
     */
    void onBookingConfirmed(Booking booking);

    /**
     * Called when a booking is cancelled (by user or due to payment failure).
     *
     * @param booking The cancelled Booking entity
     */
    void onBookingCancelled(Booking booking);
}

