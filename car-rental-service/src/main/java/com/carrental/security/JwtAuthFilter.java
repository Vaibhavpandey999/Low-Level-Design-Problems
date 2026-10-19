package com.carrental.security;

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
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthFilter – per-request JWT authentication filter.
 *
 * Runs ONCE per HTTP request (OncePerRequestFilter) to extract and validate the JWT.
 * If valid, populates Spring Security's SecurityContext with the authenticated user.
 *
 * Filter chain position: before UsernamePasswordAuthenticationFilter.
 *
 * Request flow:
 *   HTTP Request
 *     → JwtAuthFilter (extract + validate token)
 *       → [valid] → set authentication in SecurityContext
 *       → [invalid/missing] → leave SecurityContext empty (unauthenticated)
 *     → SecurityConfig (block or allow based on authentication + URL rules)
 *     → Controller
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;                    // token parsing + validation
    private final UserDetailsServiceImpl userDetailsService; // loads user from DB by email

    /**
     * Core filter method – called once per HTTP request.
     *
     * @param request     incoming HTTP request
     * @param response    outgoing HTTP response
     * @param filterChain the remaining filter chain
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // ── Extract JWT from Authorization header ──────────────────────────────────────
        String token = extractTokenFromRequest(request);  // returns null if header absent/malformed
        // ──────────────────────────────────────────────────────────────────────────────

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Only process if token exists AND user is not already authenticated this request

            // ── Parse username from JWT ────────────────────────────────────────────────
            String username = jwtService.extractUsername(token);  // throws if token is tampered
            // ──────────────────────────────────────────────────────────────────────────

            if (username != null) {
                // ── Load user from DB to get authorities ──────────────────────────────
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                // ──────────────────────────────────────────────────────────────────────

                // ── Validate token (signature + expiry + username match) ───────────────
                if (jwtService.isTokenValid(token, userDetails.getUsername())) {
                    // Create Spring Security authentication token with user details + authorities
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,               // principal – the User entity
                                    null,                      // credentials – null for JWT auth (no password needed)
                                    userDetails.getAuthorities() // roles from User.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); // attach request info

                    // Set authentication in SecurityContext – marks this request as authenticated
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("[JwtFilter] User authenticated: {}", username);
                }
                // ──────────────────────────────────────────────────────────────────────
            }
        }

        // Pass request down to next filter / servlet regardless of authentication outcome
        filterChain.doFilter(request, response);
    }

    /**
     * Extract the Bearer token from the Authorization header.
     *
     * Expected format: "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
     *
     * @param request HTTP request
     * @return JWT token string, or null if header is absent or not a Bearer token
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");  // read Authorization header

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            // Strip "Bearer " prefix (7 characters) to get the raw token
            return authHeader.substring(7);
        }

        return null;  // no valid Bearer token found
    }
}

