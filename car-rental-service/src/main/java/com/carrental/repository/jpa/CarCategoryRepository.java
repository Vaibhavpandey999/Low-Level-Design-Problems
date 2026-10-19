package com.carrental.repository.jpa;

import com.carrental.enums.CarCategoryType;
import com.carrental.model.CarCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * CarCategoryRepository – Spring Data JPA REPOSITORY PATTERN for CarCategory entity.
 */
@Repository
public interface CarCategoryRepository extends JpaRepository<CarCategory, Long> {

    /**
     * Find a category by its type enum value.
     * Used when adding a car to look up the category entity by type name.
     *
     * @param type e.g., CarCategoryType.SUV
     * @return Optional<CarCategory> – empty if this category hasn't been seeded
     */
    Optional<CarCategory> findByType(CarCategoryType type);
}

