package com.carrental.repository.jpa;

import com.carrental.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository – Spring Data JPA REPOSITORY PATTERN for User entity.
 *
 * Design Pattern: REPOSITORY PATTERN
 * Abstracts all database access for User behind this interface.
 * Service layer never writes SQL – it calls these methods.
 * Spring Data generates the implementation at runtime.
 */
@Repository  // marks this as a Spring-managed repository bean
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by email address.
     * Used by AuthService during login and by UserDetailsServiceImpl during JWT validation.
     *
     * @param email the user's email (unique in DB)
     * @return Optional<User> – empty if no user with this email exists
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if an email is already registered.
     * Used during registration to return a 409 before attempting to insert.
     *
     * @param email email to check
     * @return true if a user with this email exists
     */
    boolean existsByEmail(String email);
}

