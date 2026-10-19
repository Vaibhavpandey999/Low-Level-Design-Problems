package com.carrental.repository.jpa;

import com.carrental.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * BranchRepository – Spring Data JPA REPOSITORY PATTERN for Branch entity.
 */
@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    /**
     * Find all active branches in a specific city.
     * Used for the "find branches in your city" search.
     *
     * @param city  city name (case-sensitive – normalise before calling)
     * @param active true to return only operational branches
     */
    List<Branch> findByCityAndActive(String city, Boolean active);

    /**
     * Find all active branches across all cities.
     * Used for admin fleet overview.
     */
    List<Branch> findByActive(Boolean active);
}

