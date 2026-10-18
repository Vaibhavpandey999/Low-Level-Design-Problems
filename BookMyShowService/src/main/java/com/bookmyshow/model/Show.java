package com.bookmyshow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ============================================================
 * Show Entity – maps to 'shows' table in PostgreSQL
 * ============================================================
 *
 * A Show is a scheduled screening: a specific Movie playing in
 * a specific Screen at a specific time. ShowSeats are created
 * automatically when a new Show is scheduled.
 *
 * DESIGN PATTERN: Builder (Lombok @Builder)
 */
@Entity
@Table(
    name = "shows",
    uniqueConstraints = {
        // Prevent double-scheduling same screen at same start time
        @UniqueConstraint(columnNames = {"screen_id", "start_time"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Many Shows can be for the same Movie.
     * LAZY: don't load Movie every time we fetch a Show list.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)   // FK to movies table
    private Movie movie;          // Which movie is being screened

    /**
     * Many Shows can run in the same Screen (at different times).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)  // FK to screens table
    private Screen screen;        // Which auditorium the show is in

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;  // Scheduled start time of the show

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;    // Calculated: startTime + movie.durationMinutes

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;  // false = show cancelled or removed
}

