package com.carrental.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * KafkaConfig – configures Kafka producer and creates topic beans.
 *
 * Why Kafka for car rental events?
 * - Decouples booking confirmation from notification delivery
 * - If notification service is down, events are replayed from Kafka offset on restart
 * - Multiple consumer groups (notifications, analytics, invoicing) independently consume events
 * - Ordered delivery per partition (rentalId used as partition key)
 *
 * Topics:
 *   rental.booked     – consumed by notification + analytics services
 *   rental.cancelled  – consumed by notification + refund services
 *   rental.returned   – consumed by billing + notification services
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;  // Kafka broker address from application.yml

    /**
     * Kafka producer factory with JSON serialisation for event payloads.
     * Producer sends String keys (rentalId) and JSON-serialised event objects.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers); // broker address
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);  // key = String
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);  // value = JSON
        props.put(ProducerConfig.ACKS_CONFIG, "all");          // wait for all replicas to acknowledge
        props.put(ProducerConfig.RETRIES_CONFIG, 3);            // retry up to 3 times on failure
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // exactly-once producer semantics
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * KafkaTemplate – the high-level API for sending messages to Kafka topics.
     * Injected into RentalEventPublisher.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());  // wrap factory in template
    }

    /** Kafka topic for rental booking confirmations */
    @Bean
    public NewTopic rentalBookedTopic() {
        return TopicBuilder.name("rental.booked")
                .partitions(3)      // 3 partitions for parallel consumption
                .replicas(1)        // 1 replica (use 3 in production multi-broker setup)
                .build();
    }

    /** Kafka topic for rental cancellations */
    @Bean
    public NewTopic rentalCancelledTopic() {
        return TopicBuilder.name("rental.cancelled")
                .partitions(3)
                .replicas(1)
                .build();
    }

    /** Kafka topic for car return events */
    @Bean
    public NewTopic rentalReturnedTopic() {
        return TopicBuilder.name("rental.returned")
                .partitions(3)
                .replicas(1)
                .build();
    }
}

