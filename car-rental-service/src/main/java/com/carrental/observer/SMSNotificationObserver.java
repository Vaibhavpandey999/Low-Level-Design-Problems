package com.carrental.observer;

import com.carrental.kafka.events.RentalBookedEvent;
import com.carrental.kafka.events.RentalCancelledEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * SMSNotificationObserver – OBSERVER PATTERN concrete observer.
 *
 * Sends booking confirmation and cancellation SMS to the customer's phone.
 * Runs @Async – SMS is dispatched in background without blocking HTTP response.
 *
 * In production: integrate Twilio / AWS SNS / MSG91 API here.
 */
@Component  // Spring-managed bean – auto-discovered by RentalEventPublisher
@Slf4j
public class SMSNotificationObserver implements RentalEventObserver {

    /**
     * Send SMS confirmation when a rental booking is confirmed.
     *
     * @param event rental booked event with customer phone and rental details
     */
    @Override
    @Async  // non-blocking – SMS sent in background thread pool
    public void onRentalBooked(RentalBookedEvent event) {
        log.info("[SMS] Sending booking confirmation SMS. rentalId={}", event.getRentalId());

        // ── MOCK SMS SEND ──────────────────────────────────────────────────────────────
        // In production:
        //   twilioClient.messages().create(
        //       PhoneNumber.of(user.getPhone()),
        //       twilioPhoneNumber,
        //       "Car booked! Ref: " + event.getRentalReference() + ". Pickup: " + event.getStartDate()
        //   );
        log.info("[SMS] Booking SMS sent for rentalRef={}", event.getRentalReference());
        // ──────────────────────────────────────────────────────────────────────────────
    }

    /**
     * Send cancellation SMS with refund amount and timeline.
     *
     * @param event rental cancelled event
     */
    @Override
    @Async  // non-blocking
    public void onRentalCancelled(RentalCancelledEvent event) {
        log.info("[SMS] Sending cancellation SMS. rentalId={}", event.getRentalId());

        // ── MOCK SMS SEND ──────────────────────────────────────────────────────────────
        // In production: send "Your rental CR-XXXX has been cancelled. Refund of ₹X in 5-7 days."
        log.info("[SMS] Cancellation SMS sent. rentalId={}", event.getRentalId());
        // ──────────────────────────────────────────────────────────────────────────────
    }
}

