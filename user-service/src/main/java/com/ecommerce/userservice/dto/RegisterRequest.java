package com.ecommerce.userservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Register Request DTO
 * Used for new user registration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    // First name - required, 2-100 characters
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    private String firstName;

    // Last name - required, 2-100 characters
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    private String lastName;

    // Email - required, valid format, unique
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    // Password - required, minimum 8 characters
    // Should include uppercase, lowercase, digit, special char (validated in service)
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // Phone - optional, max 20 characters
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phone;
}
