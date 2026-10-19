package com.carrental.service;

import com.carrental.dto.request.LoginRequest;
import com.carrental.dto.request.RegisterRequest;
import com.carrental.dto.response.AuthResponse;
import com.carrental.enums.UserRole;
import com.carrental.exception.ResourceNotFoundException;
import com.carrental.model.User;
import com.carrental.repository.jpa.UserRepository;
import com.carrental.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService – handles user registration and login.
 *
 * Registration: hashes password with BCrypt, saves User entity, returns JWT.
 * Login: delegates credential verification to Spring Security's AuthenticationManager,
 *        then generates a new JWT for the authenticated user.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;           // REPOSITORY PATTERN – find/save users
    private final PasswordEncoder passwordEncoder;         // BCrypt for password hashing
    private final JwtService jwtService;                   // generates JWT tokens
    private final AuthenticationManager authenticationManager; // Spring Security authentication

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;  // token validity from config

    /**
     * Register a new user.
     *
     * @param request validated registration data
     * @return AuthResponse with JWT token for immediate login after registration
     * @throws IllegalArgumentException if email is already registered
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // ── Check for existing email (avoid duplicate registration) ───────────────────
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }
        // ──────────────────────────────────────────────────────────────────────────────

        // ── Build User entity using BUILDER PATTERN ────────────────────────────────────
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword())) // BCrypt hash – NEVER store plaintext
                .phone(request.getPhone())
                .licenceNumber(request.getLicenceNumber())
                .role(UserRole.CUSTOMER)  // all self-registered users start as CUSTOMER
                .build();

        user = userRepository.save(user);  // persist to PostgreSQL
        log.info("[AuthService] New user registered: {}", user.getEmail());
        // ──────────────────────────────────────────────────────────────────────────────

        // ── Generate JWT for immediate use after registration ──────────────────────────
        String token = jwtService.generateToken(user.getEmail());  // sign with email as subject
        return AuthResponse.builder()
                .token(token)
                .expiresInMs(jwtExpirationMs)
                .email(user.getEmail())
                .role(user.getRole())
                .build();
        // ──────────────────────────────────────────────────────────────────────────────
    }

    /**
     * Authenticate an existing user and return a new JWT.
     *
     * @param request login credentials
     * @return AuthResponse with fresh JWT token
     * @throws org.springframework.security.core.AuthenticationException if credentials are wrong
     */
    public AuthResponse login(LoginRequest request) {
        // Delegate to Spring Security's AuthenticationManager.
        // This calls UserDetailsServiceImpl.loadUserByUsername() + BCrypt password check.
        // Throws BadCredentialsException if email/password don't match.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // If authenticate() didn't throw, credentials are valid – load user for token
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found after authentication"));

        // Generate fresh JWT token for the authenticated user
        String token = jwtService.generateToken(user.getEmail());
        log.info("[AuthService] User logged in: {}", user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .expiresInMs(jwtExpirationMs)
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}

