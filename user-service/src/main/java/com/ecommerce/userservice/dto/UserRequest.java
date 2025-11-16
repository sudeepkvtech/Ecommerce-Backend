package com.ecommerce.userservice.dto;

// Import validation annotations
import jakarta.validation.constraints.Email;      // Validates email format
import jakarta.validation.constraints.NotBlank;   // Ensures field is not null/empty/whitespace
import jakarta.validation.constraints.Pattern;    // Validates against regex pattern
import jakarta.validation.constraints.Size;       // Validates string length

// Import Lombok annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Request DTO
 *
 * This DTO is used when updating user profile information.
 * It contains fields that users are allowed to update themselves.
 *
 * Fields users CANNOT update via this DTO:
 * - Email (fixed after registration)
 * - Password (has separate change password endpoint)
 * - Roles (only admins can change roles)
 * - Enabled status (only admins can activate/deactivate)
 *
 * Fields users CAN update:
 * - First name
 * - Last name
 * - Phone number
 */
@Data               // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: default constructor for Jackson
@AllArgsConstructor // Lombok: constructor with all fields for testing
public class UserRequest {

    /**
     * First name
     *
     * Validations:
     * - @NotBlank: Cannot be null, empty, or whitespace
     * - @Size: Must be 1-100 characters
     *   - Maximum 100 prevents database overflow
     *   - Minimum 1 enforced by @NotBlank
     */
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    /**
     * Last name
     *
     * Validations:
     * - @NotBlank: Cannot be null, empty, or whitespace
     * - @Size: Must be 1-100 characters
     */
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    /**
     * Email address
     *
     * Note: This field is included for completeness but is typically
     * NOT updatable after registration. It's here for reference.
     *
     * Validations:
     * - @Email: Must be valid email format (e.g., user@example.com)
     *   - Checks for @ symbol and basic structure
     *   - Does NOT verify if email exists or is deliverable
     * - @Size: Maximum 255 characters (standard email length limit)
     */
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;

    /**
     * Phone number
     *
     * Optional field for contact information.
     *
     * Validations:
     * - @Pattern: Validates international phone format
     *   - Starts with optional + sign
     *   - Followed by digits, spaces, hyphens, or parentheses
     *   - Examples: +1-555-555-5555, (555) 555-5555, +44 20 1234 5678
     * - @Size: Maximum 20 characters
     *   - Accommodates international numbers with country codes
     *
     * Note: Not using @NotBlank here because phone is optional
     */
    @Pattern(
            regexp = "^[+]?[(]?[0-9]{1,4}[)]?[-\\s\\.]?[(]?[0-9]{1,4}[)]?[-\\s\\.]?[0-9]{1,9}$",
            message = "Phone number must be valid"
    )
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phone;

    // Note: No password field here
    // Password updates use separate ChangePasswordRequest DTO
    // This separation improves security (password changes require current password)
}
