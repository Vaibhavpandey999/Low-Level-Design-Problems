package com.bookmyshow.observer;

import com.bookmyshow.model.Booking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * ============================================================
 * EmailNotificationObserver – Concrete Observer
 * ============================================================
 *
 * DESIGN PATTERN: Observer Pattern – concrete observer implementation.
 * Reacts to booking events by sending transactional emails.
 *
 * @Async: Email sending is non-blocking (runs in separate thread pool).
 * This prevents slow email delivery from blocking the booking response.
 *
 * In production: integrate with AWS SES, SendGrid, or Spring Mail
 * (spring-boot-starter-mail) using JavaMailSender.
 */
@Slf4j
@Component  // Registered as Spring bean → auto-added to observer list
public class EmailNotificationObserver implements BookingEventObserver {

    /**
     * Sends a booking confirmation email to the user.
     *
     * @Async: runs in a separate thread from @EnableAsync thread pool.
     * The booking response is returned to the user immediately, while
     * the email is dispatched asynchronously.
     *
     * @param booking Confirmed booking with user, show, and seat details
     */
    @Async           // Non-blocking: email dispatched asynchronously
    @Override
    public void onBookingConfirmed(Booking booking) {
        // Extract user email for delivery target
        String email = booking.getUser().getEmail();
        String bookingRef = booking.getBookingReference();
        String movieTitle = booking.getShow().getMovie().getTitle();

        // LOG: always log notification attempts for debugging & audit
        log.info("[Email] Sending booking confirmation to {} for booking {} | Movie: {}",
                email, bookingRef, movieTitle);

        // PRODUCTION IMPLEMENTATION would use JavaMailSender:
        // SimpleMailMessage message = new SimpleMailMessage();
        // message.setTo(email);
        // message.setSubject("Booking Confirmed – " + movieTitle);
        // message.setText(buildConfirmationBody(booking));
        // mailSender.send(message);

        // MOCK: log the email content instead
        log.info("[Email] ✉️  Confirmation email SENT to {} | Booking: {} | Amount: ₹{}",
                email, bookingRef, booking.getTotalAmount());
    }

    /**
     * Sends a booking cancellation email to the user.
     *
     * @Async: non-blocking cancellation notification.
     * @param booking Cancelled booking
     */
    @Async
    @Override
    public void onBookingCancelled(Booking booking) {
        String email = booking.getUser().getEmail();
        String bookingRef = booking.getBookingReference();

        log.info("[Email] Sending cancellation email to {} for booking {}", email, bookingRef);

        // MOCK: in production, compose HTML email with refund details
        log.info("[Email] ✉️  Cancellation email SENT to {} | Booking: {}", email, bookingRef);
    }
}

