package com.carrental.model;

import com.carrental.enums.CarStatus;
import com.carrental.enums.FuelType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Car – central entity representing a vehicle in the rental fleet.
 *
 * =========================================================
 * Design Pattern: OPTIMISTIC LOCKING (Concurrency Pattern)
 * =========================================================
 * The @Version field 'version' is automatically incremented by Hibernate on
 * every UPDATE. If two threads simultaneously try to book the same car:
 *   - Thread A reads Car (version=0), sets status=RENTED, saves → version becomes 1
 *   - Thread B reads Car (version=0), sets status=RENTED, saves → FAILS with
 *     ObjectOptimisticLockingFailureException because version=0 no longer matches DB
 * This prevents double-booking without pessimistic row-level locking.
 *
 * Design Pattern: Builder (Lombok @Builder) for fluent object construction.
 */
@Entity
@Table(name = "cars",
        indexes = {
            @Index(name = "idx_cars_status", columnList = "status"),   // fast availability filter
            @Index(name = "idx_cars_category", columnList = "category_id") // fast category filter
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String make;                                  // manufacturer brand, e.g., "Toyota"

    @Column(nullable = false)
    private String model;                                 // model name, e.g., "Camry"

    @Column(nullable = false)
    private Integer year;                                 // manufacturing year

    @Column(nullable = false, unique = true)
    private String licensePlate;                          // unique vehicle registration number

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CarStatus status;                             // AVAILABLE / RENTED / MAINTENANCE

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyRate;                         // per-day rental price in local currency

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FuelType fuelType;                            // PETROL / DIESEL / ELECTRIC / HYBRID

    @Column
    private Integer mileage;                              // current odometer reading in km

    @Column(length = 500)
    private String features;                              // comma-separated features e.g. "GPS,AC,Bluetooth"

    // ============================
    // OPTIMISTIC LOCKING KEY
    // @Version is the core of the Optimistic Locking concurrency pattern
    // Hibernate reads this value before UPDATE and verifies it hasn't changed
    // ============================
    @Version                                              // ← OPTIMISTIC LOCKING PATTERN
    private Long version;                                 // auto-managed by Hibernate; never set manually

    @ManyToOne(fetch = FetchType.LAZY)                    // lazy-load category – not always needed
    @JoinColumn(name = "category_id", nullable = false)   // FK column in cars table
    private CarCategory category;                         // category determines base pricing tier

    @CreationTimestamp
    private LocalDateTime createdAt;                      // immutable – set once on INSERT

    @UpdateTimestamp
    private LocalDateTime updatedAt;                      // refreshed on every UPDATE
}

