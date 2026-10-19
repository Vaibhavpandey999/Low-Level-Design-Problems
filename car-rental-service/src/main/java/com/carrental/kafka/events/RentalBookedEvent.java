package com.carrental.kafka.events;

import com.carrental.enums.CarCategoryType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * RentalBookedEvent – Kafka event published when a rental is successfully booked.
 *
 * This is the payload serialised to JSON and sent to the "rental.booked" Kafka topic.
 * Consumers (NotificationConsumer, AnalyticsConsumer) receive this event and
 * process it independently without coupling to the booking service.
 *
 * Kafka guarantees message ordering per partition (rentalId used as partition key),
 * and events are replayed on consumer restart (durability).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalBookedEvent {

    private Long rentalId;            // primary identifier for the rental
    private String rentalReference;   // human-readable reference e.g. "CR-2024-XYZ123"
    private Long userId;              // customer who made the booking
    private Long carId;               // car that was booked
    private CarCategoryType carCategory; // category for analytics segmentation
    private Long pickupBranchId;      // pickup location
    private String pickupCity;        // city for regional analytics
    private LocalDate startDate;      // rental start
    private LocalDate endDate;        // planned return date
    private BigDecimal totalAmount;   // amount charged
    private LocalDateTime eventTime;  // when the event was published (for ordering checks)
}

