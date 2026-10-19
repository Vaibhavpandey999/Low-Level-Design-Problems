package com.carrental.observer;

import com.carrental.kafka.events.RentalBookedEvent;
import com.carrental.kafka.events.RentalCancelledEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * AnalyticsObserver – OBSERVER PATTERN concrete observer.
 *
 * Records business analytics events when rentals are booked or cancelled.
 * Used for dashboards: daily bookings, revenue per city, cancellation rate.
 *
 * In production: push to Amplitude / Mixpanel / internal analytics DB.
 */
@Component  // Spring-managed bean – auto-discovered by RentalEventPublisher
@Slf4j
public class AnalyticsObserver implements RentalEventObserver {

    /**
     * Record a "rental_booked" analytics event.
     *
     * @param event contains car category, city, amount – all dimensions needed for reports
     */
    @Override
    @Async  // non-blocking analytics recording
    public void onRentalBooked(RentalBookedEvent event) {
        log.info("[ANALYTICS] Recording rental_booked event. rentalId={}, amount={}",
                event.getRentalId(), event.getTotalAmount());

        // ── MOCK ANALYTICS CALL ────────────────────────────────────────────────────────
        // In production:
        //   analyticsClient.track("rental_booked", Map.of(
        //       "rentalId",  event.getRentalId(),
        //       "carId",     event.getCarId(),
        //       "city",      event.getPickupCity(),
        //       "amount",    event.getTotalAmount(),
        //       "category",  event.getCarCategory()
        //   ));
        log.info("[ANALYTICS] rental_booked event recorded.");
        // ──────────────────────────────────────────────────────────────────────────────
    }

    /**
     * Record a "rental_cancelled" analytics event for cancellation rate tracking.
     *
     * @param event cancellation event with reason and penalty amount
     */
    @Override
    @Async  // non-blocking
    public void onRentalCancelled(RentalCancelledEvent event) {
        log.info("[ANALYTICS] Recording rental_cancelled event. rentalId={}", event.getRentalId());

        // ── MOCK ANALYTICS CALL ────────────────────────────────────────────────────────
        // In production: analyticsClient.track("rental_cancelled", Map.of(...))
        log.info("[ANALYTICS] rental_cancelled event recorded.");
        // ──────────────────────────────────────────────────────────────────────────────
    }
}

