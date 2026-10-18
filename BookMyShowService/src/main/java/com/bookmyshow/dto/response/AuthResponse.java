package com.bookmyshow.dto.response;

import lombok.*;

/** AuthResponse – returned after successful login or registration. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;      // JWT token to include in future request headers
    private String email;      // Logged-in user's email
    private String name;       // User's display name
    private String role;       // User role (ROLE_USER / ROLE_ADMIN)
    private Long userId;       // User's DB ID
}

