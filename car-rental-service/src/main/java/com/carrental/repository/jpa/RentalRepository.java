package com.carrental.repository.jpa;

import com.carrental.enums.RentalStatus;
import com.carrental.model.Rental;
import com.carrental.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RentalRepository – Spring Data JPA REPOSITORY PATTERN for Rental entity.
 */
@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {

    /**
     * Fetch all rentals for a given user, ordered newest first.
     * Used for "My Bookings" history page.
     */
    List<Rental> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find rentals by user and status.
     * Used to check if a user has any active rentals before allowing a new booking.
     */
    List<Rental> findByUserAndStatus(User user, RentalStatus status);

    /**
     * Find the currently active rental for a specific car.
     * Used during return processing to locate the rental record.
     *
     * @param carId  the car's primary key
     * @param status RentalStatus.ACTIVE expected
     */
    @Query("SELECT r FROM Rental r WHERE r.car.id = :carId AND r.status = :status")
    Optional<Rental> findActiveRentalByCar(@Param("carId") Long carId,
                                           @Param("status") RentalStatus status);

    /**
     * Find a rental by its idempotency reference string.
     * Used to detect duplicate booking submissions.
     */
    Optional<Rental> findByRentalReference(String rentalReference);

    /**
     * Find a rental by ID and owning user (security check).
     * Ensures a user cannot access another user's rental details.
     */
    Optional<Rental> findByIdAndUser(Long id, User user);
}

