package com.carrental.repository.jpa;

import com.carrental.enums.CarStatus;
import com.carrental.model.Car;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CarRepository – Spring Data JPA REPOSITORY PATTERN for Car entity.
 *
 * Design Pattern: REPOSITORY PATTERN
 * All Car-related DB operations go through this interface.
 *
 * Note: findByIdWithOptimisticLock uses OPTIMISTIC write-lock which
 * validates the @Version during the transaction commit.
 */
@Repository
public interface CarRepository extends JpaRepository<Car, Long> {

    /**
     * Find all cars with a given availability status.
     * Used for the browse-cars listing endpoint.
     *
     * @param status CarStatus.AVAILABLE typically
     */
    List<Car> findByStatus(CarStatus status);

    /**
     * Find cars by status and category type for filtered listing.
     * JPQL join – avoids N+1 by specifying the path through the category relation.
     *
     * @param status       only AVAILABLE cars
     * @param categoryType e.g., SUV, LUXURY
     */
    @Query("SELECT c FROM Car c JOIN c.category cat WHERE c.status = :status AND cat.type = :categoryType")
    List<Car> findByStatusAndCategoryType(@Param("status") CarStatus status,
                                          @Param("categoryType") com.carrental.enums.CarCategoryType categoryType);

    /**
     * Load a Car with an OPTIMISTIC write lock for the booking transaction.
     *
     * OPTIMISTIC_FORCE_INCREMENT: Hibernate increments @Version even if the entity
     * is not modified within the transaction – ensures concurrent booking attempts
     * on the same car see a version conflict.
     *
     * @param id Car primary key
     * @return Optional<Car> with a version lock applied
     */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)  // ← OPTIMISTIC LOCKING PATTERN
    @Query("SELECT c FROM Car c WHERE c.id = :id")
    Optional<Car> findByIdWithOptimisticLock(@Param("id") Long id);

    /**
     * Check if a licence plate is already registered in the fleet.
     * Used during car creation to enforce uniqueness.
     */
    boolean existsByLicensePlate(String licensePlate);
}

