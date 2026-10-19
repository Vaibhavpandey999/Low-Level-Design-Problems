package com.carrental.model;

import com.carrental.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * User – JPA entity representing a registered platform user.
 *
 * Implements Spring Security's UserDetails so the same object can be used
 * directly as the authenticated principal in the security context –
 * avoids a separate "UserPrincipal" wrapper class.
 *
 * Design Pattern: Builder (Lombok @Builder) – constructing User objects
 * with a fluent API instead of a telescoping constructor.
 */
@Entity                           // marks this class as a JPA-managed table
@Table(name = "users",            // explicit table name avoids SQL reserved-word clash
        uniqueConstraints = {
            @UniqueConstraint(columnNames = "email") // DB-level uniqueness enforcement
        })
@Getter                           // Lombok – generates all getters
@Setter                           // Lombok – generates all setters
@NoArgsConstructor                // Lombok – JPA requires a no-arg constructor
@AllArgsConstructor               // Lombok – used by @Builder internally
@Builder                          // Lombok – BUILDER PATTERN: User.builder()...build()
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // auto-increment primary key
    private Long id;

    @Column(nullable = false)                             // name is required
    private String name;

    @Column(nullable = false, unique = true)              // email is the login identifier
    private String email;

    @Column(nullable = false)                             // BCrypt-hashed password
    private String passwordHash;

    @Column(nullable = false)                             // for booking confirmation SMS
    private String phone;

    @Column(nullable = false)                             // driving licence is required to rent
    private String licenceNumber;

    @Enumerated(EnumType.STRING)                          // store as "CUSTOMER" / "ADMIN" string
    @Column(nullable = false)
    private UserRole role;

    @CreationTimestamp                                    // automatically set on INSERT
    private LocalDateTime createdAt;

    @UpdateTimestamp                                      // automatically updated on every UPDATE
    private LocalDateTime updatedAt;

    // ==================== Spring Security UserDetails implementation ====================

    /**
     * Returns the granted authorities for Spring Security.
     * Prefix "ROLE_" is required by Spring Security's hasRole() checks.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // wrap the role enum as a Spring authority (e.g., "ROLE_CUSTOMER")
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /** Spring Security uses email as the username identifier */
    @Override
    public String getUsername() {
        return email;  // email is our unique login identifier
    }

    /** Returns the stored BCrypt password hash for Spring Security to verify */
    @Override
    public String getPassword() {
        return passwordHash;  // Spring Security compares this with the incoming plaintext
    }

    /** All accounts are non-expired by default; extend with expiry logic if needed */
    @Override public boolean isAccountNonExpired()     { return true; }
    /** Locking would be implemented by adding a 'locked' column if required */
    @Override public boolean isAccountNonLocked()      { return true; }
    /** Credential expiry not implemented; return true (valid) */
    @Override public boolean isCredentialsNonExpired() { return true; }
    /** Enabled by default; add 'active' column for email-verification flow */
    @Override public boolean isEnabled()               { return true; }
}

