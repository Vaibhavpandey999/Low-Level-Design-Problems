package com.bookmyshow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ============================================================
 * JwtFilter – JWT Authentication Filter
 * ============================================================
 *
 * Intercepts every HTTP request and validates the JWT token
 * in the Authorization header. If valid, sets the
 * Authentication in Spring Security's SecurityContext.
 *
 * Extends OncePerRequestFilter: guarantees single execution
 * per request (avoids duplicate processing in filter chains).
 *
 * DESIGN PATTERN: Chain of Responsibility – this filter is
 *   part of Spring Security's filter chain; each filter either
 *   handles the request or passes it to the next filter.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;                               // For token validation/parsing
    private final UserDetailsServiceImpl userDetailsService;     // For loading user from DB

    /**
     * Core filter logic executed once per HTTP request.
     *
     * Flow:
     * 1. Extract JWT from "Authorization: Bearer <token>" header
     * 2. Validate token signature and expiry
     * 3. Load user from DB
     * 4. Set Authentication in SecurityContext
     * 5. Continue filter chain
     *
     * @param request     Incoming HTTP request
     * @param response    HTTP response (not modified here)
     * @param filterChain Next filter in the security chain
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Extract the Authorization header from the HTTP request
        String authHeader = request.getHeader("Authorization");

        // Skip JWT validation if header is missing or doesn't start with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);  // Pass to next filter without authentication
            return;  // Early return: no token to process
        }

        // Extract raw token by removing "Bearer " prefix (7 characters)
        String token = authHeader.substring(7);

        // Extract email from token claims (subject)
        String email = null;
        try {
            email = jwtUtil.extractEmail(token);  // Parse 'sub' claim from JWT
        } catch (Exception e) {
            log.warn("Failed to extract email from JWT: {}", e.getMessage());
            filterChain.doFilter(request, response);  // Continue as unauthenticated
            return;
        }

        // Only set authentication if:
        // 1. Email was extracted from token (non-null)
        // 2. SecurityContext doesn't already have authentication (avoid double-processing)
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load user details from database (verifies user still exists)
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // Validate token (signature, expiry) and that user is still active
            if (jwtUtil.isTokenValid(token)) {

                // Create Spring Security authentication token
                // UsernamePasswordAuthenticationToken is the standard auth object
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,                          // Principal (the User object)
                                null,                                 // Credentials (null for JWT – no password needed post-login)
                                userDetails.getAuthorities()          // Granted authorities/roles
                        );

                // Attach HTTP request details (IP, session ID) for audit logging
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in SecurityContext for this request
                // All subsequent security checks (@PreAuthorize, etc.) will use this
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("JWT authentication set for user: {}", email);
            }
        }

        // Always continue the filter chain (authenticated or not)
        filterChain.doFilter(request, response);
    }
}

