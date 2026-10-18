package com.bookmyshow.repository;

import com.bookmyshow.enums.SeatStatus;
import com.bookmyshow.model.ShowSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ShowSeatRepository – JPA repository for ShowSeat entity.
 *
 * This is the most critical repository in the system because
 * it handles concurrent seat selection.
 *
 * DESIGN PATTERN: Repository Pattern
 * DESIGN PATTERN: Optimistic Locking – via @Version on ShowSeat entity.
 */
@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    /**
     * Find all show seats for a given show.
     * Used to render the seat map on the booking page.
     * Derived: SELECT * FROM show_seats WHERE show_id = ?
     */
    List<ShowSeat> findByShowId(Long showId);

    /**
     * Count available seats for a show – used in the show listing to display
     * "X seats available" without loading all ShowSeat objects.
     * Derived: SELECT COUNT(*) FROM show_seats WHERE show_id = ? AND status = 'AVAILABLE'
     */
    long countByShowIdAndStatus(Long showId, SeatStatus status);

    /**
     * Find a specific ShowSeat by show and seat IDs.
     * Used during seat locking to fetch the exact row.
     * Derived: SELECT * FROM show_seats WHERE show_id = ? AND seat_id = ?
     */
    Optional<ShowSeat> findByShowIdAndSeatId(Long showId, Long seatId);

    /**
     * Find all ShowSeats by a list of IDs.
     * Used to fetch multiple seats at once during booking.
     * Derived: SELECT * FROM show_seats WHERE id IN (?)
     */
    List<ShowSeat> findByIdIn(List<Long> ids);

    /**
     * Bulk update ShowSeat status for multiple IDs.
     * Used during booking confirmation: LOCKED → BOOKED.
     *
     * @Modifying: tells Spring Data this is a DML query (not SELECT)
     * @Query: JPQL UPDATE statement
     */
    @Modifying
    @Query("UPDATE ShowSeat ss SET ss.status = :status WHERE ss.id IN :ids")
    void updateStatusByIds(@Param("ids") List<Long> ids, @Param("status") SeatStatus status);
}

