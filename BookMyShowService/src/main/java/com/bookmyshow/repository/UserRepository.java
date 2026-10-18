package com.bookmyshow.repository;

import com.bookmyshow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository – Spring Data JPA repository for User entity.
 *
 * Extends JpaRepository which provides out-of-the-box:
 *   save(), findById(), findAll(), delete(), count(), etc.
 *
 * DESIGN PATTERN: Repository Pattern – abstracts data access
 *   behind an interface; service layer never writes SQL directly.
 */
@Repository  // Marks as Spring-managed bean; enables exception translation
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Spring Data derives this query automatically from the method name:
     * SELECT * FROM users WHERE email = ?
     *
     * Used by UserDetailsServiceImpl to load user during authentication.
     *
     * @param email user's email address
     * @return Optional<User> – empty if no user with that email exists
     */
    Optional<User> findByEmail(String email);

    /**
     * Check existence by email – used during registration to prevent
     * duplicate accounts without loading the full entity.
     *
     * Derived query: SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)
     */
    boolean existsByEmail(String email);
}

