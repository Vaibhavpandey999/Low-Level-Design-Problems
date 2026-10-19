package com.carrental.security;

import com.carrental.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * UserDetailsServiceImpl – bridges Spring Security with our User JPA entity.
 *
 * Spring Security calls loadUserByUsername() during authentication to load the
 * user's credentials and authorities from the database.
 *
 * Since User implements UserDetails, we return the User entity directly –
 * no separate UserPrincipal wrapper is needed.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;  // REPOSITORY PATTERN – load user by email

    /**
     * Load a user by email for Spring Security authentication.
     *
     * Called by:
     * 1. UsernamePasswordAuthenticationManager during login
     * 2. JwtAuthFilter on every authenticated request
     *
     * @param email the user's email address (used as username)
     * @return UserDetails for the authenticated user
     * @throws UsernameNotFoundException if no user with this email exists
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Look up user by email – throws if not found
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        // User entity implements UserDetails → returned directly as principal
    }
}

