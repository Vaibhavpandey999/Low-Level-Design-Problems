package com.bookmyshow.model;

import com.bookmyshow.enums.SeatType;
import jakarta.persistence.*;
import lombok.*;

/**
 * ============================================================
 * Seat Entity – maps to 'seats' table in PostgreSQL
 * ============================================================
 *
 * Represents a physical seat in a Screen.
 * The same seat object is referenced by many ShowSeat entries
 * (one per show that runs in the same screen).
 *
 * DESIGN PATTERN: Builder (Lombok @Builder)
 */
@Entity
@Table(
    name = "seats",
    uniqueConstraints = {
        // A seat row+number must be unique within a screen
        @UniqueConstraint(columnNames = {"screen_id", "row_number", "seat_number"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "row_number", nullable = false)
    private String rowNumber;     // Row label (e.g. "A", "B", "C")

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;   // Seat number within the row (e.g. 1, 2, 3)

    @Enumerated(EnumType.STRING)  // Store type name, not ordinal index
    @Column(nullable = false)
    private SeatType seatType;    // NORMAL, PREMIUM, or RECLINER

    @Column(nullable = false)
    private Double basePrice;     // Base price in INR for this seat type

    /**
     * Many Seats belong to one Screen.
     * LAZY: don't load Screen when fetching Seat list.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)  // FK to screens table
    private Screen screen;        // The auditorium this seat belongs to
}

