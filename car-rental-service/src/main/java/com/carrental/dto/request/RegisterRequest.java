package com.carrental.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * RegisterRequest – DTO for user registration endpoint.
 * All fields are validated with Bean Validation annotations.
 * @Valid in the controller triggers these constraints before the method body runs.
 */
@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Name is required")        // must not be null or whitespace
    private String name;                            // customer's full name

    @Email(message = "Invalid email format")       // must match email pattern
    @NotBlank(message = "Email is required")
    private String email;                           // login identifier (unique)

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;                        // plaintext – hashed by AuthService with BCrypt

    @NotBlank(message = "Phone is required")
    private String phone;                           // customer's phone for SMS notifications

    @NotBlank(message = "Licence number is required")
    private String licenceNumber;                   // driving licence – required to rent a car
}

