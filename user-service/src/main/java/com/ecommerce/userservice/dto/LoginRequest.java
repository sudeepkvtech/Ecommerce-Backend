// Package declaration
package com.ecommerce.userservice.dto;

// Import validation annotations
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
// Import Lombok
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login Request DTO
 *
 * Used when user attempts to login
 * Contains credentials for authentication
 *
 * Request Flow:
 * POST /api/auth/login
 * { "email": "user@example.com", "password": "password123" }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * Email (username) for login
     * @Email validates format
     * @NotBlank ensures not empty
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /**
     * Password for login
     * Plain text (encrypted in transit via HTTPS)
     * Never stored - only used for verification
     */
    @NotBlank(message = "Password is required")
    private String password;
}
