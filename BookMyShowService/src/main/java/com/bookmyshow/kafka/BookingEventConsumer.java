package com.bookmyshow.kafka;

import com.bookmyshow.kafka.events.BookingCancelledEvent;
import com.bookmyshow.kafka.events.BookingConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * ============================================================
 * BookingEventConsumer – Kafka Consumer
 * ============================================================
 *
 * Listens to Kafka topics for booking events and triggers
 * appropriate downstream actions (notifications, analytics, etc.)
 *
 * WHY separate Consumer from the booking transaction?
 * - Consumer runs independently of the HTTP request/response cycle.
 * - If notification delivery fails, Kafka offset is not committed
 *   and the message is retried automatically.
 * - BookingService can return 200 OK to user before notification finishes.
 *
 * DESIGN PATTERN: Event-Driven Architecture + Consumer pattern.
 * Each @KafkaListener is a separate consumer thread.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventConsumer {

    // Service dependencies (to be created in service layer)
    // In production these would call notification / analytics services
    // For now we log the received events as proof-of-concept



    //  from appliction properties
    //    consumer:
    //      group-id: bookmyshow-group                               # Consumer group id
    //      auto-offset-reset: earliest                              # Start from beginning on new group
    //      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    //      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
    //      properties:
    //
    //
    //        spring.json.trusted.packages: "com.bookmyshow.kafka.events"  # Trust these packages for deserialization
   //Think of a consumer group as a team of workers sharing the work from a Kafka topic.
    //
    //The most important rule is:
    //
    //Within the same consumer group, each Kafka message is processed by only one consumer.
    //
    //Simple example
    //
    //Suppose your topic has 3 messages:
    //
    //booking-confirmed
    //
    //Message A
    //Message B
    //Message C
    //
    //You have 2 application instances:
    //
    //App 1
    //App 2
    //
    //Both use:
    //
    //groupId = "bookmyshow-group"
    //
    //Kafka might distribute the messages:
    //
    //bookmyshow-group
    //
    //App 1 → Message A
    //         Message C
    //
    //App 2 → Message B
    //
    //So the group ID tells Kafka which consumers are working together.
    //
    //Why do we need it?
    //
    //Imagine you have:
    //
    //Kafka
    //  |
    //  | booking-confirmed
    //  |
    //  +----> Notification Service
    //  |
    //  +----> Analytics Service
    //
    //You probably want both services to receive every booking event.
    //
    //So give them different group IDs:
    //
    //Notification Service
    //groupId = "notification-service"
    //
    //Analytics Service
    //groupId = "analytics-service"
    //
    //Kafka then does:
    //
    //                 booking-confirmed
    //                       |
    //              ┌────────┴────────┐
    //              ↓                 ↓
    //     notification-service   analytics-service
    //              ↓                 ↓
    //        gets message A       gets message A
    //        gets message B       gets message B
    //        gets message C       gets message C
    //
    //Both groups get their own copy.
    //
    //Same group vs different group
    //
    //This is the key thing to remember:
    //
    //Same groupId
    //Consumer A ─┐
    //Consumer B ─┼── group: "bookmyshow"
    //Consumer C ─┘
    //
    //They share the work.
    //
    //A message goes to one of them.
    //
    //Different groupId
    //Consumer A ── group: "notification"
    //Consumer B ── group: "analytics"
    //
    //They are independent.
    //
    //Both can receive the same message.


    /**
     * Listens to "booking-confirmed" topic.
     * groupId must match spring.kafka.consumer.group-id in application.yml.
     *
     * @Payload:       The deserialized BookingConfirmedEvent object
     * @Header:        Kafka partition and offset for logging/debugging
     */
    @KafkaListener(
        topics = BookingEventProducer.BOOKING_CONFIRMED_TOPIC,   // Subscribe to this topic
        groupId = "bookmyshow-group"                             // Consumer group (enables partitioned consumption)
    )
    public void consumeBookingConfirmed(
            @Payload BookingConfirmedEvent event,                                        // Deserialized message body
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,                     // Which Kafka partition
            @Header(KafkaHeaders.OFFSET) long offset                                    // Message offset in partition
    ) {
        log.info("[Kafka Consumer] Received BookingConfirmedEvent: bookingId={} | partition={} | offset={}",
                event.getBookingId(), partition, offset);

        // TRIGGER DOWNSTREAM ACTIONS:
        // In production, call notification services here:
        // notificationService.sendBookingConfirmation(event);
        // analyticsService.recordBookingConfirmed(event);
        // invoiceService.generateInvoice(event);

        log.info("[Kafka Consumer] Processing complete for bookingId={}: notifications queued, analytics recorded",
                event.getBookingId());
        // If no exception is thrown, Kafka commits the offset → message is acknowledged

//        Configure Spring Kafka
//        In application.yml:
//        spring:
//        kafka:
//        consumer:
//        group-id: bookmyshow-group
//        enable-auto-commit: false
//
//        listener:
//        ack-mode: record
//
//
//                With enable-auto-commit: false
//                and ack-mode: record,
//                Spring Kafka commits the record's offset after the listener r
//                eturns successfully
       // Remember: With record, you do not write commitSync() yourself. Spring Kafka manages the offset commit for you, provided enable-auto-commit is disabled and the listener container is configured accordingly.

    }

    /**
     * Listens to "booking-cancelled" topic.
     */
    @KafkaListener(
        topics = BookingEventProducer.BOOKING_CANCELLED_TOPIC,
        groupId = "bookmyshow-group"
    )
    public void consumeBookingCancelled(
            @Payload BookingCancelledEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("[Kafka Consumer] Received BookingCancelledEvent: bookingId={} | partition={} | offset={}",
                event.getBookingId(), partition, offset);

        // TRIGGER DOWNSTREAM ACTIONS on cancellation:
        // notificationService.sendCancellationNotification(event);
        // refundService.processRefund(event);
        // seatReleaseService.releaseSeat(event);

        log.info("[Kafka Consumer] Cancellation processing complete for bookingId={}", event.getBookingId());
    }
}

