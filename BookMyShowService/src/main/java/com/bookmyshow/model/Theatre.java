package com.bookmyshow.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================
 * Theatre Entity – maps to 'theatres' table in PostgreSQL
 * ============================================================
 *
 * A Theatre is a physical cinema complex (multiplex).
 * It contains one or more Screens (auditoriums), each of
 * which has a set of Seats.
 *
 * DESIGN PATTERN: Builder (Lombok @Builder)
 */
@Entity
@Table(name = "theatres")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Theatre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // Auto-increment PK
    private Long id;

    @Column(nullable = false)
    private String name;          // Theatre name (e.g. "PVR Cinemas")

    @Column(nullable = false)
    private String address;       // Street address

    @Column(nullable = false)
    private String city;          // City – used for geo-filtering (e.g. "Mumbai")

    @Column(nullable = false)
    private String state;         // State/Province

    @Column(nullable = false)
    private String pincode;       // ZIP / Postal code

    private Double latitude;      // GPS latitude for map integration
    private Double longitude;     // GPS longitude for map integration

    /**
     * One Theatre has many Screens.
     * CascadeType.ALL: persist/remove screens with theatre.
     * fetch = LAZY: screens NOT loaded unless explicitly accessed (performance).
     */
    @OneToMany(mappedBy = "theatre", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Screen> screens; // Auditoriums inside this theatre

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;  // Soft-delete: false = closed/demolished

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

