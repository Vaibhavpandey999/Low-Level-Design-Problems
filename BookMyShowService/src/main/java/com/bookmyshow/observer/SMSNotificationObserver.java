package com.bookmyshow.observer;

import com.bookmyshow.model.Booking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * ============================================================
 * SMSNotificationObserver – Concrete Observer
 * ============================================================
 *
 * DESIGN PATTERN: Observer Pattern – sends SMS on booking events.
 *
 * In production: integrate with Twilio SDK or AWS SNS.
 * Twilio example:
 *   Message.creator(new PhoneNumber(toPhone), new PhoneNumber(fromPhone), body).create();
 */
@Slf4j
@Component  // Auto-registered with BookingEventPublisher
public class SMSNotificationObserver implements BookingEventObserver {

    /**
     * Send SMS confirmation after successful booking.
     * @Async: SMS delivery is non-blocking.
     */
    @Async
    @Override
    public void onBookingConfirmed(Booking booking) {
        // Get phone number; skip SMS if user has no phone number registered
        String phone = booking.getUser().getPhoneNumber();
        if (phone == null || phone.isBlank()) {
            log.debug("[SMS] No phone number for userId={}, skipping SMS", booking.getUser().getId());
            return;  // Early return: can't send SMS without phone
        }

        // Build compact SMS message (SMS < 160 chars to avoid multi-part billing)
        String message = String.format(
            "BookMyShow: Booking confirmed! Ref: %s | %s | ₹%.0f. Enjoy the show!",
            booking.getBookingReference(),
            booking.getShow().getMovie().getTitle(),
            booking.getTotalAmount()
        );

        log.info("[SMS] Sending confirmation SMS to {}", phone);
        // PRODUCTION: twilioClient.messages().create(phone, FROM_NUMBER, message);
        log.info("[SMS] 📱 SMS SENT to {} | Message: {}", phone, message);
    }

    /**
     * Send SMS notification on booking cancellation.
     * @Async: non-blocking.
     */
    @Async
    @Override
    public void onBookingCancelled(Booking booking) {
        String phone = booking.getUser().getPhoneNumber();
        if (phone == null || phone.isBlank()) {
            return;  // Skip if no phone number
        }

        String message = String.format(
            "BookMyShow: Booking %s cancelled. Refund will be processed in 5-7 business days.",
            booking.getBookingReference()
        );

        log.info("[SMS] 📱 Cancellation SMS SENT to {}", phone);
    }
}

