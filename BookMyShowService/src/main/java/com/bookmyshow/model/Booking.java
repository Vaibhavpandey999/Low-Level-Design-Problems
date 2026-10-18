package com.bookmyshow.model;

import com.bookmyshow.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================
 * Booking Entity – maps to 'bookings' table in PostgreSQL
 * ============================================================
 *
 * A Booking represents a user's reservation for one or more
 * seats in a specific Show. It is created in PENDING state
 * and transitions to CONFIRMED after successful payment.
 *
 * DESIGN PATTERN: Builder (Lombok @Builder)
 * DESIGN PATTERN: Template Method – BookingService.book() follows
 *   the fixed steps: lock → create Booking(PENDING) → pay →
 *   confirm → notify.
 */
@Entity
@Table(name = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who made this booking.
     * LAZY: don't load User on every booking query.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)   // FK to users table
    private User user;            // Who made the booking

    /**
     * The show being booked.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)   // FK to shows table
    private Show show;            // Which show is being booked

    /**
     * One Booking has many BookingItems (one per seat).
     * CascadeType.ALL: items are created/deleted with booking.
     */
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<BookingItem> bookingItems;  // Seats included in this booking

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;  // Initial state

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;   // Sum of all seat prices in INR

    @Column(name = "booking_reference", unique = true)
    private String bookingReference;  // Human-readable reference (e.g. "BMS-2024-A1B2C3")

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

