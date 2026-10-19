package com.carrental.model;

import com.carrental.enums.RentalStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Rental – the core booking entity representing a car rental agreement.
 *
 * Lifecycle:
 *   PENDING   → payment initiated
 *   ACTIVE    → car handed to customer after successful payment
 *   COMPLETED → car returned, final charges settled
 *   CANCELLED → booking cancelled by user or admin
 *
 * The rentalReference field acts as an idempotency key:
 * if the same booking request is submitted twice (network retry), the second
 * insert will fail the unique constraint, preventing a double-booking.
 *
 * Design Pattern: Builder (Lombok @Builder)
 */
@Entity
@Table(name = "rentals",
        indexes = {
            @Index(name = "idx_rentals_user",   columnList = "user_id"),  // fast lookup by user
            @Index(name = "idx_rentals_car",    columnList = "car_id"),   // fast lookup by car
            @Index(name = "idx_rentals_status", columnList = "status")    // fast status filter
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)                         // many rentals can belong to one user
    @JoinColumn(name = "user_id", nullable = false)
    private User user;                                         // the customer renting the car

    @ManyToOne(fetch = FetchType.LAZY)                         // many rentals can involve one car
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;                                           // the car being rented

    @ManyToOne(fetch = FetchType.LAZY)                         // where the customer picks up the car
    @JoinColumn(name = "pickup_branch_id", nullable = false)
    private Branch pickupBranch;

    @ManyToOne(fetch = FetchType.LAZY)                         // where the customer will return the car
    @JoinColumn(name = "dropoff_branch_id", nullable = false)
    private Branch dropoffBranch;

    @Column(nullable = false)
    private LocalDate startDate;                               // rental start date (inclusive)

    @Column(nullable = false)
    private LocalDate endDate;                                 // rental end date (inclusive)

    private LocalDate actualReturnDate;                        // set when car is returned (may differ from endDate)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RentalStatus status;                               // current lifecycle state

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;                            // base rental cost (days × dailyRate)

    @Column(precision = 10, scale = 2)
    private BigDecimal damageCharge;                           // additional fee assessed on return

    @Column(precision = 10, scale = 2)
    private BigDecimal overstayCharge;                         // fee for returning car late

    @Column(nullable = false, unique = true)
    private String rentalReference;                            // idempotency key, e.g. "CR-2024-XYZ123"

    @CreationTimestamp
    private LocalDateTime createdAt;                           // timestamp of booking creation
}

