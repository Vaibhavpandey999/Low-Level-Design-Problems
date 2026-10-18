package com.bookmyshow.repository;

import com.bookmyshow.model.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ShowRepository – JPA repository for Show entity.
 * DESIGN PATTERN: Repository Pattern
 */
@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    /**
     * Find all active shows for a specific movie in a specific city.
     * This is the core query powering the "Movie → Shows in City" feature.
     *
     * JPQL joins Show → Screen → Theatre to filter by city.
     * Only returns future (upcoming) shows.
     */
    @Query("""
        SELECT s FROM Show s
        JOIN FETCH s.screen sc
        JOIN FETCH sc.theatre t
        WHERE s.movie.id = :movieId
          AND t.city = :city
          AND s.startTime >= :fromTime
          AND s.isActive = true
        ORDER BY s.startTime ASC
    """)
    List<Show> findUpcomingShowsByMovieAndCity(
        @Param("movieId") Long movieId,
        @Param("city") String city,
        @Param("fromTime") LocalDateTime fromTime
    );

    /**
     * Find all upcoming shows for a screen (used for scheduling conflict detection).
     * Derived: SELECT * FROM shows WHERE screen_id = ? AND start_time >= ? AND is_active = true
     */
    List<Show> findByScreenIdAndStartTimeGreaterThanEqualAndIsActiveTrue(
        Long screenId,
        LocalDateTime fromTime
    );
}

