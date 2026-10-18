package com.bookmyshow.service;

import com.bookmyshow.dto.request.LoginRequest;
import com.bookmyshow.dto.request.RegisterRequest;
import com.bookmyshow.dto.response.AuthResponse;
import com.bookmyshow.exception.ResourceNotFoundException;
import com.bookmyshow.model.User;
import com.bookmyshow.repository.UserRepository;
import com.bookmyshow.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================
 * UserService – User Registration and Authentication
 * ============================================================
 *
 * Handles:
 * 1. User registration (save with BCrypt password)
 * 2. User login (authenticate → generate JWT)
 *
 * DESIGN PATTERN: Facade – hides Spring Security's
 *   AuthenticationManager + JwtUtil complexity behind simple
 *   register()/login() methods for the controller.
 */
@Slf4j
@Service            // Spring-managed singleton service bean
@RequiredArgsConstructor  // Lombok: inject all final fields via constructor
public class UserService {

    private final UserRepository userRepository;         // JPA repo for User entity
    private final PasswordEncoder passwordEncoder;        // BCrypt encoder (from SecurityConfig)
    private final JwtUtil jwtUtil;                       // JWT token generator
    private final AuthenticationManager authenticationManager;  // Spring Security auth manager

    /**
     * Register a new user.
     *
     * Steps:
     * 1. Check if email already registered (prevent duplicates)
     * 2. Hash password with BCrypt
     * 3. Save user to PostgreSQL
     * 4. Generate JWT for the new user
     * 5. Return AuthResponse
     *
     * @param request Registration details (name, email, password, phone)
     * @return AuthResponse with JWT token
     * @throws IllegalArgumentException if email already registered
     */
    @Transactional  // Wrap DB write in a transaction (rollback on exception)
    public AuthResponse register(RegisterRequest request) {
        // STEP 1: Check for duplicate email (DB-level unique constraint would also catch this)
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        // STEP 2: Build User entity using Builder pattern
        User user = User.builder()
                .name(request.getName())                               // Set display name
                .email(request.getEmail())                             // Set login email
                .password(passwordEncoder.encode(request.getPassword()))  // BCrypt hash password
                .phoneNumber(request.getPhoneNumber())                 // Optional phone
                .role("ROLE_USER")                                     // Default role
                .build();

        // STEP 3: Persist to PostgreSQL
        User savedUser = userRepository.save(user);
        log.info("New user registered: id={} email={}", savedUser.getId(), savedUser.getEmail());

        // STEP 4: Generate JWT for immediate login after registration
        String token = jwtUtil.generateToken(savedUser.getEmail());

        // STEP 5: Build and return response DTO
        return AuthResponse.builder()
                .token(token)                  // JWT for subsequent API calls
                .email(savedUser.getEmail())   // Confirm registered email
                .name(savedUser.getName())     // Display name
                .role(savedUser.getRole())     // Granted role
                .userId(savedUser.getId())     // User's DB ID
                .build();
    }

    /**
     * Authenticate user login.
     *
     * Steps:
     * 1. Delegate to Spring Security's AuthenticationManager
     *    (which uses DaoAuthenticationProvider → BCrypt verify)
     * 2. If authentication succeeds, generate JWT
     * 3. Return AuthResponse
     *
     * @param request Login credentials (email, password)
     * @return AuthResponse with JWT token
     * @throws org.springframework.security.core.AuthenticationException if credentials invalid
     */
    public AuthResponse login(LoginRequest request) {
        // STEP 1: Authenticate via Spring Security
        // This internally:
        //   a) Loads user by email via UserDetailsServiceImpl
        //   b) Compares provided password against BCrypt hash
        //   c) Throws BadCredentialsException if mismatch
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),     // Username (our primary key is email)
                        request.getPassword()   // Raw password (compared against BCrypt hash)
                )
        );

        // STEP 2: Get the authenticated User principal
        User user = (User) authentication.getPrincipal();  // Cast to our User entity

        // STEP 3: Generate a fresh JWT for this session
        String token = jwtUtil.generateToken(user.getEmail());
        log.info("User logged in: id={} email={}", user.getId(), user.getEmail());

        // STEP 4: Return AuthResponse
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .userId(user.getId())
                .build();
    }

    /**
     * Load user by ID (used internally).
     *
     * @param userId User's DB ID
     * @return User entity
     * @throws ResourceNotFoundException if not found
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}

