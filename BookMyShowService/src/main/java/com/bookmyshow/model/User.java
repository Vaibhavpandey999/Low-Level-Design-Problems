package com.bookmyshow.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * ============================================================
 * User Entity – maps to 'users' table in PostgreSQL
 * ============================================================
 *
 * Implements UserDetails so Spring Security can load this
 * directly from the DB without a wrapper class.
 *
 * DESIGN PATTERN: Builder (Lombok @Builder) – construct User
 *   objects fluently without telescoping constructors.
 */
@Entity                                  // Marks this class as a JPA-managed entity
@Table(
    name = "users",                      // PostgreSQL table name (avoid reserved word 'user')
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "email")    // DB-level uniqueness on email
    }
)
@Data                                    // Lombok: generates getters, setters, equals, hashCode, toString
@Builder                                 // DESIGN PATTERN: Builder – fluent object construction
@NoArgsConstructor                       // JPA requires a no-arg constructor
@AllArgsConstructor                      // Required by @Builder to set all fields
public class User implements UserDetails {

    @Id                                  // Primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment in PostgreSQL
    private Long id;

    @Column(nullable = false)            // NOT NULL constraint at DB level
    private String name;                 // User's display name

    @Column(nullable = false, unique = true)  // Unique email for login
    private String email;                // User's email address (used as username)

    @Column(nullable = false)
    private String password;             // BCrypt-hashed password (never stored in plain text)

    @Column(name = "phone_number")
    private String phoneNumber;          // Optional mobile number for SMS notifications

    @Column(name = "role", nullable = false)
    @Builder.Default
    private String role = "ROLE_USER";   // Default role; admins have "ROLE_ADMIN"

    @CreationTimestamp                   // Hibernate auto-sets on INSERT
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp                     // Hibernate auto-sets on every UPDATE
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── UserDetails interface methods ──────────────────────────────

    /**
     * Returns the granted authorities (roles) for this user.
     * Spring Security uses this during authorization checks.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Convert role string to a single GrantedAuthority lambda
        return List.of((GrantedAuthority) () -> role);
    }

    /**
     * Spring Security uses getUsername() as the unique identifier.
     * We use email as the unique login credential.
     */
    @Override
    public String getUsername() {
        return email;  // email is the login identifier
    }

    /** Account is never expired in this simple implementation */
    @Override public boolean isAccountNonExpired()    { return true; }

    /** Account is never locked in this simple implementation */
    @Override public boolean isAccountNonLocked()     { return true; }

    /** Credentials never expire in this simple implementation */
    @Override public boolean isCredentialsNonExpired(){ return true; }

    /** All registered users are enabled */
    @Override public boolean isEnabled()              { return true; }
}

