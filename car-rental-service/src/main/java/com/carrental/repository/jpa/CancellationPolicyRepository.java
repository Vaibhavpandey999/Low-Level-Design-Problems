package com.carrental.repository.jpa;

import com.carrental.enums.CarCategoryType;
import com.carrental.model.CancellationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CancellationPolicyRepository – Spring Data JPA REPOSITORY PATTERN.
 */
@Repository
public interface CancellationPolicyRepository extends JpaRepository<CancellationPolicy, Long> {

    /**
     * Fetch all cancellation policies for a given car category type.
     * Multiple policies can exist for the same category (different time thresholds).
     * The most restrictive matching policy is applied by CancellationService.
     *
     * @param categoryType the car's category (e.g., LUXURY)
     */
    List<CancellationPolicy> findByCategoryType(CarCategoryType categoryType);
}

