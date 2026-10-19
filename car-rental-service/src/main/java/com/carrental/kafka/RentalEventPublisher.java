package com.carrental.kafka;

import com.carrental.kafka.events.RentalBookedEvent;
import com.carrental.kafka.events.RentalCancelledEvent;
import com.carrental.observer.RentalEventObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RentalEventPublisher – the Subject/Publisher in the OBSERVER PATTERN.
 *
 * =============================================
 * Design Pattern: OBSERVER PATTERN (Subject)
 * =============================================
 * This class is the "subject" that maintains a list of observers and
 * notifies them when rental events occur.
 *
 * Two notification paths:
 * 1. In-process: directly calls registered RentalEventObserver beans (@Async)
 *    for low-latency side effects (email/SMS within same JVM)
 * 2. Kafka: publishes events to topics for cross-service consumption
 *    (AnalyticsService, InvoiceService running as separate microservices)
 *
 * This dual approach provides both immediate in-process notification
 * and durable cross-service event propagation.
 */
@Component
@RequiredArgsConstructor  // Lombok – generates constructor for all final fields
@Slf4j
public class RentalEventPublisher {

    // Spring injects ALL RentalEventObserver beans (Email, SMS, Analytics)
    // OBSERVER PATTERN: this is the observer registry
    private final List<RentalEventObserver> observers;

    // Kafka template for publishing events to topics (cross-service communication)
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** Kafka topic constants */
    private static final String TOPIC_RENTAL_BOOKED    = "rental.booked";    // consumed by external services
    private static final String TOPIC_RENTAL_CANCELLED = "rental.cancelled"; // consumed by RefundService etc.

    /**
     * Publish a rental booked event to all observers and Kafka.
     *
     * Called by RentalFacade after successful payment and rental activation.
     *
     * @param event fully populated booking event
     */
    public void publishRentalBooked(RentalBookedEvent event) {
        log.info("[Publisher] Publishing rental_booked event. rentalId={}", event.getRentalId());

        // ── OBSERVER PATTERN: notify all in-process observers ─────────────────────────
        for (RentalEventObserver observer : observers) {
            try {
                observer.onRentalBooked(event);  // each observer runs @Async in its own thread
            } catch (Exception ex) {
                // Log but don't propagate – one observer failure must not break others
                log.error("[Publisher] Observer {} failed on rental_booked: {}",
                        observer.getClass().getSimpleName(), ex.getMessage());
            }
        }
        // ──────────────────────────────────────────────────────────────────────────────

        // ── KAFKA: publish durable event for external consumers ────────────────────────
        // rentalId is the partition key → all events for same rental go to same partition
        kafkaTemplate.send(TOPIC_RENTAL_BOOKED, event.getRentalId().toString(), event);
        log.info("[Publisher] Event sent to Kafka topic: {}", TOPIC_RENTAL_BOOKED);
        // ──────────────────────────────────────────────────────────────────────────────
    }

    /**
     * Publish a rental cancelled event to all observers and Kafka.
     *
     * Called by CancellationFacade after cancellation is processed.
     *
     * @param event cancellation event with refund details
     */
    public void publishRentalCancelled(RentalCancelledEvent event) {
        log.info("[Publisher] Publishing rental_cancelled event. rentalId={}", event.getRentalId());

        // ── OBSERVER PATTERN: notify all in-process observers ─────────────────────────
        for (RentalEventObserver observer : observers) {
            try {
                observer.onRentalCancelled(event);  // @Async – non-blocking
            } catch (Exception ex) {
                log.error("[Publisher] Observer {} failed on rental_cancelled: {}",
                        observer.getClass().getSimpleName(), ex.getMessage());
            }
        }
        // ──────────────────────────────────────────────────────────────────────────────

        // ── KAFKA: durable event for RefundService, AnalyticsService ──────────────────
        kafkaTemplate.send(TOPIC_RENTAL_CANCELLED, event.getRentalId().toString(), event);
        log.info("[Publisher] Event sent to Kafka topic: {}", TOPIC_RENTAL_CANCELLED);
        // ──────────────────────────────────────────────────────────────────────────────
    }
}

