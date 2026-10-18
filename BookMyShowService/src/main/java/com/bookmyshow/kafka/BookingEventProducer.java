package com.bookmyshow.kafka;

import com.bookmyshow.kafka.events.BookingCancelledEvent;
import com.bookmyshow.kafka.events.BookingConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * ============================================================
 * BookingEventProducer – Kafka Producer
 * ============================================================
 *
 * Publishes booking lifecycle events to Kafka topics.
 * Decouples booking confirmation from downstream effects
 * (notifications, analytics, seat release, etc.)
 *
 * DESIGN PATTERN: Event-Driven Architecture – fire-and-forget
 *   with callback logging for reliability monitoring.
 */
@Slf4j
@Component  // Spring-managed bean
@RequiredArgsConstructor  // Lombok: constructor injection for final fields
public class BookingEventProducer {

    // Kafka topic names as constants (avoids typo bugs)
    public static final String BOOKING_CONFIRMED_TOPIC  = "booking-confirmed";   // Confirmed bookings topic
    public static final String BOOKING_CANCELLED_TOPIC  = "booking-cancelled";   // Cancelled bookings topic

    /**
     * KafkaTemplate<String, Object>: generic type allows sending any JSON-serializable object.
     * Spring auto-configures this bean from application.yml kafka.producer settings.
     */

    //You set the number of partitions on the Kafka topic
    //A common Spring approach is to create the topic with NewTopic:

//    @Configuration
//    public class KafkaTopicConfig {
//
//        @Bean
//        public NewTopic bookingConfirmedTopic() {
//            return TopicBuilder.name("booking-confirmed")
//                    .partitions(3)
//                    .replicas(2)
//                    .build();
//        }
//    }
//replicas(1) is about how many copies of each Kafka partition are stored on different Kafka brokers.
//    Topic: booking-confirmed
//
//    Partition 0 → Broker 1 + Broker 2
//    Partition 1 → Broker 2 + Broker 3
//    Partition 2 → Broker 1 + Broker 3
//
//    Each partition has 2 replicas.
//    Why replicas?
//
//    For fault tolerance.
//
//    Suppose Partition 0 has:
//
//    Partition 0
//            |
//            +---- Broker 1  ← leader
//   |
//           +---- Broker 2  ← replica
//
//    If Broker 1 goes down:
//
//    Broker 1 ❌
//            |
//            ↓
//    Broker 2 can become the leader
//
//    Your data can remain available.
//
//    What does .replicas(1) mean?
//            .replicas(1)
//
//    means:
//
//    Keep one copy of each partition.
//
//    For example:
//
//            3 partitions
//            replicas = 1
//
//    P0 → Broker 1
//    P1 → Broker 2
//    P2 → Broker 3
//
//    There is no backup replica for each partition.
//
//    This is commonly fine for local development, especially if you're running a single Kafka broker.
//
//            .partitions(3) vs .replicas(1)
//
//    This distinction is very important:
//
//            .partitions(3)
//.replicas(1)
//
//    means:
//
//    Topic
//                      |
//                              ┌───────────┼───────────┐
//                              ↓           ↓           ↓
//    P0          P1          P2
//          |           |           |
//    Broker 1    Broker 2    Broker 3
//
//    Partitions = parallelism
//
//            Replicas = fault tolerance
//
//Partitions are the part that makes consumer groups really click.
//
//Think of a Kafka topic as a big log split into multiple lanes.
//
//1. One topic can have multiple partitions
//
//For example:
//
//Topic: booking-confirmed
//
//Partition 0:  [M0] [M3] [M6] [M9]
//Partition 1:  [M1] [M4] [M7]
//Partition 2:  [M2] [M5] [M8]
//
//Each partition is an ordered sequence of messages.
//
//So inside Partition 0:
//
//M0 → M3 → M6 → M9
//
//Kafka guarantees their order within that partition.
//
//It does not guarantee:
//
//M0 → M1 → M2 → M3
//
//across different partitions.
//
//2. Why do we have partitions?
//
//Mainly for parallelism and scalability.
//
//Imagine you have 1 million booking events.
//
//With one partition:
//
//Consumer
//   ↓
//Partition 0
//   ↓
//1 million messages
//
//Only one consumer in a consumer group can actively consume that partition at a time.
//
//With 3 partitions:
//
//             Kafka
//               |
//       ┌───────┼───────┐
//       ↓       ↓       ↓
//      P0      P1      P2
//       ↓       ↓       ↓
//    Consumer Consumer Consumer
//       1       2       3
//
//Now three consumers can process messages in parallel.
//
//3. How partitions and groupId work together
//
//This is the important part.
//
//Suppose:
//
//Topic: booking-confirmed
//
//P0
//P1
//P2
//
//And your application has 3 instances:
//
//Instance A
//Instance B
//Instance C
//
//All have:
//
//groupId = "bookmyshow-group"
//
//Kafka can assign:
//
//P0 → Instance A
//P1 → Instance B
//P2 → Instance C
//
//So:
//
//              bookmyshow-group
//                     |
//        ┌────────────┼────────────┐
//        ↓            ↓            ↓
//       P0           P1           P2
//        ↓            ↓            ↓
//     App A        App B        App C
//
//This is how Kafka distributes work.
//
//4. What if you have more consumers than partitions?
//
//Suppose you have only 3 partitions:
//
//P0
//P1
//P2
//
//but 5 application instances:
//
//App A
//App B
//App C
//App D
//App E
//
//All belong to the same group.
//
//Only 3 can actively consume:
//
//P0 → App A
//P1 → App B
//P2 → App C
//
//App D → idle
//App E → idle
//
//Why?
//
//Because one partition can be assigned to only one consumer within a consumer group.
//
//So:
//
//Number of active consumers in a group cannot exceed the number of partitions.
//
//5. What happens if you have fewer consumers?
//
//Suppose:
//
//3 partitions
//2 consumers
//
//Kafka can do:
//
//App A → P0 + P1
//App B → P2
//
//or another distribution.
//
//So one consumer can handle multiple partitions.
//
//6. Now your bookingId becomes important
//
//Earlier you had:
//
//kafkaTemplate.send(
//    BOOKING_CONFIRMED_TOPIC,
//    String.valueOf(event.getBookingId()),
//    event
//);
//
//Remember this second argument?
//
//String.valueOf(event.getBookingId())
//
//That's the key.
//
//Kafka uses the key to determine which partition receives the message (with the default partitioner behavior).
//
//For example:
//
//bookingId = 101 → P0
//bookingId = 102 → P2
//bookingId = 103 → P1
//bookingId = 101 → P0
//
//So:
//
//P0: booking 101
//    booking 101
//    ...
//
//P1: booking 103
//    ...
//
//P2: booking 102
//    ...
//
//The important benefit is that messages with the same key are routed to the same partition, so their order can be maintained there.
//
//7. This is why your comment says "ensures ordering"
//
//You had:
//
//String.valueOf(event.getBookingId()), // Message key (ensures ordering)
//
//More precisely, it should say:
//
//"Message key — ensures ordering for messages with
//the same key, within the same partition."
//
//For example:
//
//bookingId = 123
//
//BookingCreated
//BookingConfirmed
//BookingCancelled
//
//Because all have key 123, they can go to the same partition:
//
//Partition 1:
//
//offset 50 → BookingCreated(123)
//offset 51 → BookingConfirmed(123)
//offset 52 → BookingCancelled(123)
//
//Kafka preserves that order within that partition.
//
//8. Putting everything together
//
//Your architecture looks like this:
//
//                    Kafka Topic
//                 booking-confirmed
//                        |
//             ┌──────────┼──────────┐
//             ↓          ↓          ↓
//           P0          P1         P2
//             |          |          |
//             ↓          ↓          ↓
//          Consumer   Consumer   Consumer
//             A          B          C
//             └──────────┼──────────┘
//                        |
//                 bookmyshow-group
//
//And the producer:
//
//BookingService
//      |
//      | key = bookingId
//      ↓
//Kafka
//      |
//      | Kafka chooses partition based on key
//      ↓
//   P0 / P1 / P2
//The 3 concepts to memorize
//Concept	Meaning
//Topic	Category/log of messages
//Partition	One ordered lane inside a topic
//Consumer Group	Consumers sharing the work
//
//And the relationship:
//
//Topic
// ├── Partition 0 ── Consumer A
// ├── Partition 1 ── Consumer B
// └── Partition 2 ── Consumer C
//                      ↑
//              same consumer group
//
//Partition gives you parallelism. Consumer group gives you workload sharing. Key determines which partition a message goes to (under the normal keyed-partitioning behavior).



    //  from appliction properties
    //    producer:
    //      key-serializer: org.apache.kafka.common.serialization.StringSerializer        # Key serializer
    //      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer  # Value as JSON
    //      acks: all                                                # Wait for all replicas to ack (durability)
    //      retries: 3                                               # Retry on transient failures
    //
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish a BookingConfirmedEvent to Kafka.
     * The bookingId is used as the message key to ensure all events for
     * the same booking go to the same Kafka partition (ordering guarantee).
     *
     * @param event Populated event with booking details
     */
    public void publishBookingConfirmed(BookingConfirmedEvent event) {
        log.info("[Kafka] Publishing BookingConfirmedEvent: bookingId={}", event.getBookingId());

        // Send message to Kafka; returns a CompletableFuture for async completion tracking
        //SendResult is a Spring Kafka class that represents the result of successfully sending a message to Kafka.
        //SendResult
        // ├── ProducerRecord
        // │    ├── topic
        // │    ├── key
        // │    └── value
        // │
        // └── RecordMetadata
        //      ├── topic
        //      ├── partition
        //      ├── offset
        //      └── timestamp
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(
                        BOOKING_CONFIRMED_TOPIC,                      // Topic name
                        String.valueOf(event.getBookingId()),          // Message key (ensures ordering)
                        event                                          // Message value (serialized to JSON)
                );

        // Register callbacks for success/failure logging (non-blocking)
        //ex-->exception
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                // SUCCESS: log partition and offset for traceability
                //For example, Kafka might tell you:
                //
                //Topic: booking-confirmed
                //Partition: 2
                //Offset: 157
                //
                //This means your message was written to:
                //
                //booking-confirmed
                //       ↓
                //   Partition 2
                //       ↓
                //   Offset 157
                log.info("[Kafka] BookingConfirmedEvent sent successfully: bookingId={} | partition={} | offset={}",
                        event.getBookingId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                );
            } else {
                // FAILURE: log error; in production, store in outbox or retry queue
                log.error("[Kafka] Failed to send BookingConfirmedEvent: bookingId={} | error={}",
                        event.getBookingId(), ex.getMessage(), ex);
            }
        });
    }

    /**
     * Publish a BookingCancelledEvent to Kafka.
     *
     * @param event Populated cancellation event
     */
    public void publishBookingCancelled(BookingCancelledEvent event) {
        log.info("[Kafka] Publishing BookingCancelledEvent: bookingId={}", event.getBookingId());

        // Send to cancellations topic with bookingId as key
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(BOOKING_CANCELLED_TOPIC, String.valueOf(event.getBookingId()), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("[Kafka] BookingCancelledEvent sent: bookingId={}", event.getBookingId());
            } else {
                log.error("[Kafka] Failed to send BookingCancelledEvent: bookingId={} | error={}",
                        event.getBookingId(), ex.getMessage(), ex);
            }
        });
    }
}

