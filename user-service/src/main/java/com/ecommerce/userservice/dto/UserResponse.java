package com.ecommerce.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * User Response DTO
 * Used in API responses for user data
 * Excludes sensitive fields like password
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    // User ID
    private Long id;

    // User's first and last name
    private String firstName;
    private String lastName;

    // Email (username)
    private String email;

    // Phone number
    private String phone;

    // Account status
    private Boolean enabled;

    // User's role names
    private Set<String> roles;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
