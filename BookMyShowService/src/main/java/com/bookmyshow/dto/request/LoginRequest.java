package com.bookmyshow.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** LoginRequest – DTO for user login. */
@Data
public class LoginRequest {
    @Email @NotBlank private String email;     // Login email
    @NotBlank private String password;         // Plain-text password (verified against BCrypt hash)
}

