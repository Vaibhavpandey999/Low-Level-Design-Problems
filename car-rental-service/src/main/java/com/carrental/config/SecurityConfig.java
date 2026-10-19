package com.carrental.config;

import com.carrental.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig – Spring Security configuration for JWT-based stateless auth.
 *
 * Key decisions:
 * - Sessions: STATELESS (no HttpSession – JWT carries all auth state)
 * - CSRF: disabled (not needed for stateless REST APIs)
 * - Password encoding: BCrypt with strength 12 (2^12 iterations)
 * - Method security: @PreAuthorize annotations on controller methods
 */
@Configuration
@EnableWebSecurity              // activates Spring Security's web integration
@EnableMethodSecurity           // enables @PreAuthorize / @Secured annotations
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;            // our custom JWT filter
    private final UserDetailsService userDetailsService;  // loads user by email

    /**
     * Defines the HTTP security filter chain:
     * - Permits public endpoints (auth, car browsing, search)
     * - Requires authentication for booking, cancellation, payment endpoints
     * - Inserts JwtAuthFilter before the username/password filter
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF – stateless JWT APIs do not use session cookies
            .csrf(AbstractHttpConfigurer::disable)

            // URL-level authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints – no JWT required
                .requestMatchers("/api/auth/**").permitAll()              // login, register
                .requestMatchers("/api/cars/**").permitAll()              // browse cars
                .requestMatchers("/api/branches/**").permitAll()          // view branches
                .requestMatchers("/actuator/health").permitAll()          // health check
                // Admin-only endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")        // fleet reports
                // All other endpoints require a valid JWT
                .anyRequest().authenticated()
            )

            // Stateless session – Spring Security never creates HttpSession
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Wire our DaoAuthenticationProvider (BCrypt + UserDetailsService)
            .authenticationProvider(authenticationProvider())

            // Insert JWT filter BEFORE Spring's built-in username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCryptPasswordEncoder bean – used to hash passwords on registration
     * and verify them on login.
     *
     * Strength 12 = 2^12 = 4096 iterations (good balance of security vs. speed)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // 12 rounds of BCrypt hashing
    }

    /**
     * DaoAuthenticationProvider – bridges Spring Security auth with our UserDetailsService.
     * Automatically uses our BCryptPasswordEncoder for password verification.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);   // our UserDetailsServiceImpl
        provider.setPasswordEncoder(passwordEncoder());        // BCrypt for password matching
        return provider;
    }

    /**
     * AuthenticationManager bean – exposed so AuthService can programmatically authenticate.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();  // delegates to the configured provider
    }
}

