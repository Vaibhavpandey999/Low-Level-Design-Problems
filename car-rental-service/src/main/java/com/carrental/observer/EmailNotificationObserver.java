package com.carrental.observer;

import com.carrental.kafka.events.RentalBookedEvent;
import com.carrental.kafka.events.RentalCancelledEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * EmailNotificationObserver – OBSERVER PATTERN concrete observer.
 *
 * Sends booking confirmation and cancellation emails to customers.
 * Runs @Async so the booking HTTP response is returned BEFORE the email is sent.
 *
 * In production: integrate JavaMailSender / AWS SES / SendGrid API here.
 */
@Component  // Spring-managed bean – auto-discovered by RentalEventPublisher
@Slf4j
public class EmailNotificationObserver implements RentalEventObserver {

    /**
     * Send booking confirmation email when a rental is successfully booked.
     *
     * @param event rental booking event with customer and car details
     */
    @Override
    @Async  // execute in Spring's async thread pool – non-blocking for the HTTP request thread
    public void onRentalBooked(RentalBookedEvent event) {
        log.info("[EMAIL] Sending booking confirmation email. rentalId={}, userId={}",
                event.getRentalId(), event.getUserId());

        // ── MOCK EMAIL SEND ────────────────────────────────────────────────────────────
        // In production:
        //   SimpleMailMessage msg = new SimpleMailMessage();
        //   msg.setTo(user.getEmail());
        //   msg.setSubject("Car Rental Confirmed – " + event.getRentalReference());
        //   msg.setText(buildBookingConfirmationTemplate(event));
        //   mailSender.send(msg);
        log.info("[EMAIL] Booking confirmation email sent. rentalRef={}", event.getRentalReference());
        // ──────────────────────────────────────────────────────────────────────────────
    }

    /**
     * Send cancellation and refund notification email.
     *
     * @param event rental cancellation event with refund details
     */
    @Override
    @Async  // non-blocking – email goes out in background thread
    public void onRentalCancelled(RentalCancelledEvent event) {
        log.info("[EMAIL] Sending cancellation email. rentalId={}, refund={}",
                event.getRentalId(), event.getRefundAmount());

        // ── MOCK EMAIL SEND ────────────────────────────────────────────────────────────
        // In production: build cancellation email template with refund amount and timeline
        log.info("[EMAIL] Cancellation email sent. rentalId={}", event.getRentalId());
        // ──────────────────────────────────────────────────────────────────────────────
    }
}

