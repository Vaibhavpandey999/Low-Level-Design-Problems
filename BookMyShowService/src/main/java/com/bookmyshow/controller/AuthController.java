package com.bookmyshow.controller;

import com.bookmyshow.dto.request.LoginRequest;
import com.bookmyshow.dto.request.RegisterRequest;
import com.bookmyshow.dto.response.AuthResponse;
import com.bookmyshow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================
 * AuthController – User Registration and Login Endpoints
 * ============================================================
 *
 * Public endpoints (no JWT required).
 * Returns JWT token on success.
 */
@Slf4j
@RestController                     // Marks as REST controller; all methods return JSON by default
@RequestMapping("/api/auth")        // Base URL for all endpoints in this controller
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration and login APIs")  // Swagger group
public class AuthController {

    private final UserService userService;  // Injected user service

    /**
     * POST /api/auth/register
     * Register a new user account.
     *
     * @Valid: triggers DTO validation (email format, password length, etc.)
     * Returns 201 CREATED with JWT token in body.
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account and returns a JWT token")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for email={}", request.getEmail());
        AuthResponse response = userService.register(request);  // Delegate to service
        return ResponseEntity.status(HttpStatus.CREATED).body(response);  // 201 Created
    }

    /**
     * POST /api/auth/login
     * Authenticate an existing user.
     *
     * @Valid: validates email format and non-blank password.
     * Returns 200 OK with JWT token.
     */
    @PostMapping("/login")
    @Operation(summary = "Login with email and password", description = "Returns a JWT token on successful authentication")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email={}", request.getEmail());
        AuthResponse response = userService.login(request);  // Delegate to service
        return ResponseEntity.ok(response);  // 200 OK
    }
}

