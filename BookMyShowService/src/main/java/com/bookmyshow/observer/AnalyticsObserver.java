package com.bookmyshow.observer;

import com.bookmyshow.model.Booking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * ============================================================
 * AnalyticsObserver – Concrete Observer
 * ============================================================
 *
 * DESIGN PATTERN: Observer Pattern – records booking events in an
 * analytics pipeline for business intelligence.
 *
 * In production: push events to Apache Kafka analytics topic,
 * AWS Kinesis, Segment, Mixpanel, or a data warehouse (BigQuery/Redshift).
 */
@Slf4j
@Component  // Auto-registered with BookingEventPublisher
public class AnalyticsObserver implements BookingEventObserver {

    /**
     * Record a booking_confirmed event in the analytics system.
     * Used to track revenue, popular movies, peak booking times, etc.
     * @Async: analytics recording must not slow down booking response.
     */
    @Async
    @Override
    public void onBookingConfirmed(Booking booking) {
        // Build analytics event data (in production this would be a structured event object)
        log.info("[Analytics] 📊 EVENT: booking_confirmed | bookingId={} | userId={} | showId={} | movieId={} | amount={}",
                booking.getId(),
                booking.getUser().getId(),
                booking.getShow().getId(),
                booking.getShow().getMovie().getId(),
                booking.getTotalAmount()
        );

        // PRODUCTION: publish to Kafka analytics topic or Segment:
        // analyticsKafkaTemplate.send("analytics-events", BookingConfirmedEvent.from(booking));
        // Analytics.track("booking_confirmed", properties);
    }

    /**
     * Record a booking_cancelled event.
     * Used to track cancellation rates, refund volumes, etc.
     */
    @Async
    @Override
    public void onBookingCancelled(Booking booking) {
        log.info("[Analytics] 📊 EVENT: booking_cancelled | bookingId={} | userId={} | amount={}",
                booking.getId(),
                booking.getUser().getId(),
                booking.getTotalAmount()
        );
    }
}

