package com.bookmyshow.config;

import com.bookmyshow.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
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
 * ============================================================
 * SecurityConfig – Spring Security Configuration
 * ============================================================
 *
 * Configures:
 * 1. Stateless JWT-based session management (no HTTP sessions)
 * 2. Public vs. protected endpoints
 * 3. JWT filter placement in the security filter chain
 * 4. BCrypt password encoding
 * 5. Authentication provider for login
 *
 * DESIGN PATTERN: Builder Pattern – HttpSecurity fluent API builds
 *   the security configuration step by step.
 * DESIGN PATTERN: Chain of Responsibility – Spring's FilterChain
 *   processes security concerns in order.
 */
@Configuration                // Marks this as a Spring configuration class
@EnableWebSecurity            // Activates Spring Security's web security support
@EnableMethodSecurity         // Enables @PreAuthorize, @PostAuthorize on methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;                   // Our custom JWT filter
    private final UserDetailsService userDetailsService; // For loading users in auth provider

    /**
     * Main security filter chain definition.
     * Configures which endpoints are public, which require auth, and the session strategy.
     *
     * @param http Spring's HttpSecurity builder
     * @return Configured SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // Disable CSRF: not needed for stateless REST APIs with JWT
            // CSRF protection is for session-based auth where cookies carry state
            .csrf(AbstractHttpConfigurer::disable)

            // Define URL authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints: no JWT required
                .requestMatchers(
                    "/api/auth/**",          // Login and registration
                    "/api/movies/**",        // Browse movies (public)
                    "/api/shows/**",         // View show timings (public)
                    "/api/theatres/**",      // View theatres (public)
                    "/api/search/**",        // Search (public)
                    "/swagger-ui/**",        // Swagger UI (dev convenience)
                    "/api-docs/**",          // OpenAPI JSON (dev convenience)
                    "/actuator/health",      // Health check (for load balancer)
                    "/actuator/info"         // Info endpoint
                ).permitAll()
                // Admin-only endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // All other endpoints require valid JWT
                .anyRequest().authenticated()
            )

            // Use STATELESS sessions: Spring Security won't create HTTP sessions
            // Every request must include a valid JWT in the Authorization header
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Set our DaoAuthenticationProvider (DB-backed user lookup + BCrypt)
            .authenticationProvider(authenticationProvider())

            // Insert JwtFilter BEFORE Spring's default UsernamePasswordAuthenticationFilter
            // So JWT is validated before any form-login processing
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

            .build();  // Build and return the configured chain
    }

    /**
     * BCrypt password encoder.
     * BCrypt with work factor 10 (default): takes ~100ms to hash,
     * making brute-force attacks extremely slow.
     *
     * @return BCryptPasswordEncoder with default strength (10)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // BCrypt: auto-salted, adaptive hash
    }

    /**
     * DaoAuthenticationProvider: loads user from DB via UserDetailsService
     * and validates password using BCryptPasswordEncoder.
     *
     * @return Configured DaoAuthenticationProvider
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);  // Load user from PostgreSQL
        provider.setPasswordEncoder(passwordEncoder());       // Verify BCrypt hash
        return provider;
    }

    /**
     * AuthenticationManager: used by AuthController to authenticate
     * login requests (email + password).
     *
     * @param config Spring's AuthenticationConfiguration (auto-configured)
     * @return AuthenticationManager bean
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();  // Delegate to Spring's auto-configured manager
    }
}

