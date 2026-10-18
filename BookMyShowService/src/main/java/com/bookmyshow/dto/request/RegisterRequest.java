package com.bookmyshow.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * RegisterRequest – DTO for user registration.
 * Validated with @Valid in AuthController.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")      // Must not be null or empty
    private String name;                          // User's full name

    @Email(message = "Email must be valid")       // Must be a valid email format
    @NotBlank(message = "Email is required")
    private String email;                         // Login email

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")  // Minimum length
    private String password;                      // Plain-text password (hashed in service)

    private String phoneNumber;                   // Optional: for SMS notifications
}

