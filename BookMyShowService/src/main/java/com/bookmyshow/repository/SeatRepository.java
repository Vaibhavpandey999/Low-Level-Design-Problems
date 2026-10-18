package com.bookmyshow.repository;

import com.bookmyshow.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SeatRepository – JPA repository for Seat entity.
 * DESIGN PATTERN: Repository Pattern
 */
@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    /**
     * Find all seats belonging to a screen.
     * Used when creating ShowSeats for a new Show.
     * Derived: SELECT * FROM seats WHERE screen_id = ?
     */
    List<Seat> findByScreenId(Long screenId);
}

