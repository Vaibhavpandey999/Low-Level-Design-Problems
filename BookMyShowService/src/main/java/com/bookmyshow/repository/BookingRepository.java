package com.bookmyshow.repository;

import com.bookmyshow.enums.BookingStatus;
import com.bookmyshow.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * BookingRepository – JPA repository for Booking entity.
 * DESIGN PATTERN: Repository Pattern
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Find all bookings by a user (booking history page).
     * Derived: SELECT * FROM bookings WHERE user_id = ?
     */
    List<Booking> findByUserId(Long userId);

    /**
     * Find bookings by user filtered by status.
     * e.g. "My confirmed bookings" or "My cancelled bookings"
     * Derived: SELECT * FROM bookings WHERE user_id = ? AND status = ?
     */
    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);

    /**
     * Find booking by booking reference (shown on ticket).
     * Used for QR code scanning at theatre gate.
     * Derived: SELECT * FROM bookings WHERE booking_reference = ?
     */
    Optional<Booking> findByBookingReference(String bookingReference);
}

