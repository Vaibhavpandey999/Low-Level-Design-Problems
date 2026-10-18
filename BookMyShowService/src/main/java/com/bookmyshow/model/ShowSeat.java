package com.bookmyshow.model;

import com.bookmyshow.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * ============================================================
 * ShowSeat Entity – maps to 'show_seats' table in PostgreSQL
 * ============================================================
 *
 * JOIN TABLE between Show and Seat, carrying per-show status.
 * One ShowSeat row is created for each (show, seat) pair when
 * a Show is scheduled.
 *
 * KEY DESIGN DECISIONS:
 * 1. @Version (Optimistic Locking) – prevents double-booking
 *    under concurrent requests without table-level locks.
 *    If two threads try to update the same row, one will get
 *    ObjectOptimisticLockingFailureException and must retry.
 * 2. SeatStatus stored here (not on Seat) because the same
 *    physical seat can be AVAILABLE for one show and BOOKED
 *    for another show at a different time.
 *
 * DESIGN PATTERN: Builder (Lombok @Builder)
 */
@Entity
@Table(
    name = "show_seats",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"show_id", "seat_id"})  // One row per show-seat pair
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Many ShowSeats belong to one Show.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)   // FK to shows table
    private Show show;            // The scheduled screening

    /**
     * Many ShowSeats reference one Seat (physical seat).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)   // FK to seats table
    private Seat seat;            // The physical seat in the auditorium

    @Enumerated(EnumType.STRING)  // Store enum name, not ordinal
    @Column(nullable = false)
    @Builder.Default
    private SeatStatus status = SeatStatus.AVAILABLE;  // Default: seat is free

    /**
     * OPTIMISTIC LOCKING – DESIGN PATTERN: Optimistic Concurrency Control
     *
     * Hibernate auto-increments this field on every UPDATE.
     * If two concurrent transactions read the same row (same version)
     * and both attempt to UPDATE, the second one will fail with
     * OptimisticLockingFailureException – preventing double-booking.
     *
     * This is better than pessimistic locking (SELECT FOR UPDATE) for
     * read-heavy workloads where conflicts are rare.
     */
    @Version
    private Long version;         // Optimistic lock version counter
}

