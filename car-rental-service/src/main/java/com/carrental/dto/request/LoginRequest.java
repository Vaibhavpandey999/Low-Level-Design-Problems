package com.carrental.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** LoginRequest – DTO for the login endpoint. */
@Getter @Setter
public class LoginRequest {

    @Email @NotBlank(message = "Email is required")
    private String email;     // user's registered email address

    @NotBlank(message = "Password is required")
    private String password;  // plaintext password – verified against BCrypt hash
}

