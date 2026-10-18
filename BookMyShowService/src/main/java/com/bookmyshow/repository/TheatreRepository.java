package com.bookmyshow.repository;

import com.bookmyshow.model.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TheatreRepository – JPA repository for Theatre entity.
 * DESIGN PATTERN: Repository Pattern
 */
@Repository
public interface TheatreRepository extends JpaRepository<Theatre, Long> {

    /**
     * Find active theatres by city.
     * Used for the "select city → show theatres" feature.
     * Derived: SELECT * FROM theatres WHERE city = ? AND is_active = true
     */
    List<Theatre> findByCityAndIsActiveTrue(String city);
}

