package com.carrental.controller;

import com.carrental.dto.request.LoginRequest;
import com.carrental.dto.request.RegisterRequest;
import com.carrental.dto.response.AuthResponse;
import com.carrental.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController – REST endpoints for user registration and login.
 *
 * These endpoints are public (no JWT required) – configured in SecurityConfig.
 * All endpoints return JSON responses.
 *
 * Base path: /api/auth
 */
@RestController                  // @Controller + @ResponseBody – all methods return JSON
@RequestMapping("/api/auth")     // base URL for all auth endpoints
@RequiredArgsConstructor         // Lombok – constructor injection for AuthService
public class AuthController {

    private final AuthService authService;  // handles registration and login logic

    /**
     * Register a new user account.
     *
     * POST /api/auth/register
     * Body: { "name": "...", "email": "...", "password": "...", "phone": "...", "licenceNumber": "..." }
     *
     * @param request validated registration data (Spring validates before method runs)
     * @return 201 Created with JWT token for immediate use
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // @Valid triggers Bean Validation on RegisterRequest fields
        // If validation fails, GlobalExceptionHandler.handleValidation() catches it
        AuthResponse response = authService.register(request);  // hash password, save user, generate JWT
        return ResponseEntity.status(HttpStatus.CREATED).body(response);  // HTTP 201
    }

    /**
     * Login with email and password to receive a JWT token.
     *
     * POST /api/auth/login
     * Body: { "email": "...", "password": "..." }
     *
     * @param request login credentials
     * @return 200 OK with JWT token, or 401 Unauthorized if credentials are wrong
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // authService.login() calls Spring Security AuthenticationManager internally
        // Bad credentials throw BadCredentialsException → GlobalExceptionHandler → 401
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);  // HTTP 200 with token
    }
}

