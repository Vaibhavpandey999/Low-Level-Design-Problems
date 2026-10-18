package com.bookmyshow.repository;

import com.bookmyshow.model.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ScreenRepository – JPA repository for Screen entity.
 * DESIGN PATTERN: Repository Pattern
 */
@Repository
public interface ScreenRepository extends JpaRepository<Screen, Long> {

    /**
     * Find all active screens in a given theatre.
     * Used by admin to list auditoriums in a multiplex.
     * Derived: SELECT * FROM screens WHERE theatre_id = ? AND is_active = true
     */
    List<Screen> findByTheatreIdAndIsActiveTrue(Long theatreId);
}

