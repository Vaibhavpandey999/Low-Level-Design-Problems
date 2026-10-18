package com.bookmyshow.security;

import com.bookmyshow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * UserDetailsServiceImpl – Spring Security's user loading bridge.
 *
 * Spring Security calls loadUserByUsername() during authentication.
 * We implement it to load our User entity from PostgreSQL.
 *
 * Since User implements UserDetails, we return it directly.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;  // JPA repo to find user by email

    /**
     * Load user by username (email in our case).
     * Called by Spring Security's authentication manager and our JwtFilter.
     *
     * @param email The user's email address (username)
     * @return UserDetails (our User entity) for Spring Security
     * @throws UsernameNotFoundException if no user with that email exists
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Query PostgreSQL for user; throw standard Spring Security exception if not found
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                    new UsernameNotFoundException("User not found with email: " + email)
                );
        // User implements UserDetails → returned directly without wrapping
    }
}

