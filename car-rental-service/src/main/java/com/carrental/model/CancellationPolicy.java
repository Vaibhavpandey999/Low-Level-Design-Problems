package com.carrental.model;

import com.carrental.enums.CarCategoryType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * CancellationPolicy – defines the penalty for cancelling a rental.
 *
 * Each policy is scoped to a CarCategoryType and a time window:
 * if the customer cancels more than 'hoursBeforePickup' hours before the
 * start date, the penaltyPercent is applied to the total amount.
 *
 * Example:
 *   Category=LUXURY, hoursBeforePickup=24, penaltyPercent=50
 *   → Cancelling within 24 hours of pickup: 50% penalty on total charge
 *
 * Design Pattern: Builder (Lombok @Builder)
 */
@Entity
@Table(name = "cancellation_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CarCategoryType categoryType;                      // which car category this policy applies to

    @Column(nullable = false)
    private Integer hoursBeforePickup;                         // time threshold in hours before rental start

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal penaltyPercent;                         // % of totalAmount charged as cancellation fee
}

