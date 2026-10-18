package com.bookmyshow.observer;

import com.bookmyshow.model.Booking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ============================================================
 * BookingEventPublisher – Subject / Publisher
 * ============================================================
 *
 * DESIGN PATTERN: Observer Pattern – this is the "Subject"
 * (also called "Publisher" or "Observable").
 *
 * Maintains a list of BookingEventObservers and notifies all
 * of them when a booking event occurs.
 *
 * Spring auto-injects ALL @Component beans implementing
 * BookingEventObserver into the observers list. Adding a new
 * observer = add one @Component class – zero changes here.
 *
 * This is also an example of the Open/Closed Principle (OCP):
 * the publisher is closed for modification but open for extension
 * (new observers can be added without changing this class).
 */
@Slf4j
@Component  // Spring singleton – one publisher shared by the whole app
public class BookingEventPublisher {

    /**
     * All registered observers (injected by Spring from classpath scan).
     * Spring collects all @Component classes implementing BookingEventObserver.
     */
    private final List<BookingEventObserver> observers;

    /**
     * Constructor injection – Spring provides the complete list of
     * BookingEventObserver beans automatically.
     *
     * @param observers List of all registered observers
     */
    public BookingEventPublisher(List<BookingEventObserver> observers) {
        this.observers = observers;  // Store observer list for later notification
        log.info("BookingEventPublisher initialized with {} observers: {}",
                observers.size(),
                observers.stream().map(o -> o.getClass().getSimpleName()).toList());
    }

    /**
     * Notify ALL registered observers that a booking was confirmed.
     * Each observer handles it in its own way (email, SMS, analytics).
     *
     * @param booking The confirmed booking
     */
    public void publishBookingConfirmed(Booking booking) {
        log.info("Publishing BookingConfirmed event for bookingId={}", booking.getId());

        // Iterate all observers and call their onBookingConfirmed method
        for (BookingEventObserver observer : observers) {
            try {
                observer.onBookingConfirmed(booking);  // Notify each observer
            } catch (Exception e) {
                // IMPORTANT: catch per-observer exceptions so one failing observer
                // (e.g. SMS fails) does NOT prevent other observers (email) from running
                log.error("Observer {} failed on BookingConfirmed: {}",
                        observer.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Notify ALL registered observers that a booking was cancelled.
     *
     * @param booking The cancelled booking
     */
    public void publishBookingCancelled(Booking booking) {
        log.info("Publishing BookingCancelled event for bookingId={}", booking.getId());

        // Iterate all observers and call their onBookingCancelled method
        for (BookingEventObserver observer : observers) {
            try {
                observer.onBookingCancelled(booking);  // Notify each observer
            } catch (Exception e) {
                // Same fault-isolation pattern as above
                log.error("Observer {} failed on BookingCancelled: {}",
                        observer.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }
}

