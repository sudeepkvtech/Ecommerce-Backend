package com.ecommerce.userservice.dto;

// Import validation annotations for input validation
import jakarta.validation.constraints.NotBlank;  // Ensures field is not null, empty, or whitespace
import jakarta.validation.constraints.Size;      // Validates string length

// Import Lombok annotations to reduce boilerplate code
import lombok.AllArgsConstructor;  // Generates constructor with all fields
import lombok.Data;                // Generates getters, setters, toString, equals, hashCode
import lombok.NoArgsConstructor;   // Generates no-argument constructor

/**
 * Change Password Request DTO
 *
 * This DTO is used when a user wants to change their password.
 * It requires both the current password (for security) and the new password.
 *
 * Security considerations:
 * 1. Current password verification prevents unauthorized changes
 * 2. Password strength enforced by @Size annotation
 * 3. Both passwords must be provided (not blank)
 * 4. Passwords are transmitted over HTTPS only
 * 5. Passwords are never logged or stored in plain text
 *
 * Usage flow:
 * 1. User submits current password + new password
 * 2. Controller validates DTO with @Valid annotation
 * 3. Service verifies current password matches database
 * 4. Service hashes new password with BCrypt
 * 5. Service updates database with hashed password
 */
@Data               // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: generates default constructor (needed for Jackson JSON deserialization)
@AllArgsConstructor // Lombok: generates constructor with all fields (useful for testing)
public class ChangePasswordRequest {

    /**
     * Current password
     *
     * The user must provide their current password to verify their identity.
     * This prevents someone with access to an unlocked session from changing
     * the password without authorization.
     *
     * Validations:
     * - @NotBlank: Cannot be null, empty string "", or whitespace "   "
     * - Must match the BCrypt hash stored in database (checked in service layer)
     */
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    /**
     * New password
     *
     * The new password that will replace the current one.
     *
     * Validations:
     * - @NotBlank: Cannot be null, empty, or whitespace
     * - @Size: Must be between 8-100 characters
     *   - Minimum 8 characters ensures reasonable strength
     *   - Maximum 100 prevents DoS attacks with huge passwords
     * - Must be different from current password (checked in service layer)
     *
     * Password strength recommendations (not enforced here, but good practice):
     * - Mix of uppercase and lowercase letters
     * - At least one number
     * - At least one special character
     * - No common words or patterns
     */
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters")
    private String newPassword;

    // Note: We don't have a "confirm password" field here
    // That validation should happen on the frontend before submission
    // Backend only needs the new password once
}
