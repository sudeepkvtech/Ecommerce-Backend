package com.ecommerce.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JWT Response DTO
 * Returned after successful login
 * Contains JWT token and user info
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtResponse {

    // JWT access token - client sends this in Authorization header
    private String token;

    // Token type - always "Bearer" for JWT
    private String type = "Bearer";

    // User ID
    private Long id;

    // User email
    private String email;

    // User's full name
    private String firstName;
    private String lastName;

    // User's roles (e.g., ["ROLE_USER", "ROLE_ADMIN"])
    private java.util.Set<String> roles;
}
