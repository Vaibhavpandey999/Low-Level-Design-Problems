package com.carrental.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Branch – represents a physical rental location (office / depot).
 *
 * Customers pick up and drop off cars at branches. A car can be
 * reassigned between branches (tracked via CarBranchAssignment).
 *
 * The latitude/longitude fields enable geo-proximity queries in
 * Elasticsearch (branch-nearby search) and native PostgreSQL queries.
 *
 * Design Pattern: Builder (Lombok @Builder)
 */
@Entity
@Table(name = "branches",
        indexes = {
            @Index(name = "idx_branches_city", columnList = "city") // fast city filter
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;                 // human-readable branch name, e.g., "Mumbai Airport"

    @Column(nullable = false)
    private String address;              // full postal address

    @Column(nullable = false)
    private String city;                 // city used for search/filter (e.g., "Mumbai")

    @Column(nullable = false)
    private String state;                // state/province for regional grouping

    @Column(nullable = false)
    private Double latitude;             // geo-coordinate for proximity search

    @Column(nullable = false)
    private Double longitude;            // geo-coordinate for proximity search

    private String phone;                // branch contact number (nullable – may not always be available)

    @Column(nullable = false)
    private Boolean active;              // soft-delete flag; inactive branches hidden from UI
}

