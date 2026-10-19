package com.carrental.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * CarBranchAssignment – join entity tracking which branch a car currently belongs to.
 *
 * A car can be relocated between branches (e.g., after one-way rentals),
 * so this is a separate entity rather than a direct FK on Car.
 *
 * Design Pattern: Builder (Lombok @Builder)
 */
@Entity
@Table(name = "car_branch_assignments",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = "car_id") // each car has exactly one current branch
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarBranchAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)             // one-to-one: each car has one current assignment
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;                              // the car being assigned

    @ManyToOne(fetch = FetchType.LAZY)            // many cars can be at the same branch
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;                        // current home branch of this car
}

