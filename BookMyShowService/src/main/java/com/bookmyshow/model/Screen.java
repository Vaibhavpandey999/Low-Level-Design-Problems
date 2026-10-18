package com.bookmyshow.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * ============================================================
 * Screen Entity – maps to 'screens' table in PostgreSQL
 * ============================================================
 *
 * A Screen is one auditorium inside a Theatre.
 * It has a defined set of Seats and can host many Shows.
 *
 * DESIGN PATTERN: Builder (Lombok @Builder)
 */
@Entity
@Table(name = "screens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Screen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;            // Screen name (e.g. "Audi 1", "Screen 3")

    @Column(nullable = false)
    private Integer totalSeats;     // Total seat count – denormalized for fast queries

    /**
     * Many Screens belong to one Theatre.
     * LAZY fetch: theatre not loaded until needed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theatre_id", nullable = false)  // FK column in 'screens' table
    private Theatre theatre;        // Parent theatre

    /**
     * One Screen has many Seats (fixed layout).
     * CascadeType.ALL: seats are created/deleted with screen.
     */
    @OneToMany(mappedBy = "screen", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Seat> seats= new Arraylist<>();       // Physical seats in this auditorium

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;  // false = screen under maintenance
}

