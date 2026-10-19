package com.carrental.dto.response;

import com.carrental.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

/**
 * AuthResponse – returned after successful registration or login.
 * Contains the JWT token the client should store and send in future requests.
 */
@Getter
@Builder  // BUILDER PATTERN for construction in AuthService
public class AuthResponse {

    private String token;         // JWT Bearer token
    private long expiresInMs;     // token validity duration in milliseconds
    private String email;         // the authenticated user's email
    private UserRole role;        // CUSTOMER or ADMIN (for frontend role-based UI)
}

