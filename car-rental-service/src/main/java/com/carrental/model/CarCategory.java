package com.carrental.model;

import com.carrental.enums.CarCategoryType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * CarCategory – master data entity for car classification tiers.
 *
 * Each Car belongs to exactly one CarCategory which determines its
 * base daily rental rate and which CancellationPolicy applies to it.
 *
 * Design Pattern: Builder (Lombok @Builder)
 */
@Entity
@Table(name = "car_categories")    // table name for car category master data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)                         // store enum name as string
    @Column(nullable = false, unique = true)             // each category type appears once
    private CarCategoryType type;                        // e.g., ECONOMY, SUV, LUXURY

    @Column(nullable = false)
    private String description;                          // human-readable description for UI

    @Column(nullable = false, precision = 10, scale = 2) // monetary amount with 2 decimal places
    private BigDecimal baseDailyRate;                    // default rate; Car can override
}

