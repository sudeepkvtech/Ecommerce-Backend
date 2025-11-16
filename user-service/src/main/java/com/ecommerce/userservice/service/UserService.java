package com.ecommerce.userservice.service;

// Import the User entity class that represents users in the database
import com.ecommerce.userservice.entity.User;

// Import DTOs (Data Transfer Objects) used for API requests and responses
import com.ecommerce.userservice.dto.UserRequest;
import com.ecommerce.userservice.dto.UserResponse;
import com.ecommerce.userservice.dto.ChangePasswordRequest;

// Import the UserRepository to perform database operations
import com.ecommerce.userservice.repository.UserRepository;

// Import custom exceptions for error handling
import com.ecommerce.userservice.exception.ResourceNotFoundException;
import com.ecommerce.userservice.exception.BadRequestException;

// Import Spring Security's password encoder for password hashing
import org.springframework.security.crypto.password.PasswordEncoder;

// Import Spring framework annotations
import org.springframework.stereotype.Service;  // Marks this as a service component
import org.springframework.transaction.annotation.Transactional;  // Manages database transactions

// Import Lombok for automatic constructor generation
import lombok.RequiredArgsConstructor;

// Import Java's Optional for handling potentially null values
import java.util.Optional;

// Import List for returning multiple users
import java.util.List;

// Import stream API for data transformation
import java.util.stream.Collectors;

/**
 * User Service
 *
 * Business logic layer for user management operations.
 * This service handles:
 * - User profile retrieval and updates
 * - Password management
 * - User data transformation (Entity ↔ DTO)
 * - Business validation rules
 *
 * Why separate Service from Controller?
 * - Controllers handle HTTP requests/responses
 * - Services contain reusable business logic
 * - Makes testing easier (can test without HTTP)
 * - Allows transactions to span multiple repository calls
 */
@Service  // Spring will create a singleton bean of this class
@RequiredArgsConstructor  // Lombok generates constructor with all 'final' fields
@Transactional  // All methods run in database transactions (auto-commit/rollback)
public class UserService {

    // UserRepository dependency - injected by Spring via constructor
    // 'final' ensures it's immutable after construction
    private final UserRepository userRepository;

    // PasswordEncoder dependency - used for hashing and verifying passwords
    // Spring Security provides BCryptPasswordEncoder by default
    private final PasswordEncoder passwordEncoder;

    /**
     * Get user profile by user ID
     *
     * This method retrieves a single user's profile information.
     * It's typically used when an admin views any user's profile
     * or when a user views their own profile.
     *
     * @param userId The unique identifier of the user to retrieve
     * @return UserResponse DTO containing user data (without password)
     * @throws ResourceNotFoundException if user doesn't exist
     */
    @Transactional(readOnly = true)  // Optimization: read-only transaction (no write lock)
    public UserResponse getUserById(Long userId) {
        // Find user by ID in the database
        // Optional<User> is returned because the user might not exist
        User user = userRepository.findById(userId)
                // If user not found, throw custom exception
                // .orElseThrow() takes a Supplier<Exception> as parameter
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId
                ));

        // Convert User entity to UserResponse DTO
        // This hides sensitive data (like password) from the response
        return mapToUserResponse(user);
    }

    /**
     * Get current authenticated user's profile
     *
     * This method retrieves the profile of the currently logged-in user.
     * The user's email is extracted from the JWT token by Spring Security
     * and passed to this method.
     *
     * @param email The email of the authenticated user (from JWT token)
     * @return UserResponse DTO with user's profile data
     * @throws ResourceNotFoundException if user doesn't exist
     */
    @Transactional(readOnly = true)  // Read-only: just retrieving data
    public UserResponse getCurrentUserProfile(String email) {
        // Find user by email (email is used as username in our system)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email
                ));

        // Return user data without password
        return mapToUserResponse(user);
    }

    /**
     * Update user profile
     *
     * This method updates user profile information.
     * Users can update: firstName, lastName, phone
     * Users CANNOT update: email, password (those have separate methods)
     *
     * @param userId The ID of the user to update
     * @param request UserRequest DTO containing updated data
     * @return Updated UserResponse DTO
     * @throws ResourceNotFoundException if user doesn't exist
     */
    public UserResponse updateUser(Long userId, UserRequest request) {
        // First, find the existing user in the database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId
                ));

        // Update only the fields that are provided in the request
        // Why not create a new User object? Because we need to preserve:
        // - The existing ID
        // - The existing email (email shouldn't change)
        // - The existing password (password has separate change method)
        // - The existing roles
        // - The creation timestamp

        // Update first name if provided
        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName());
        }

        // Update last name if provided
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName());
        }

        // Update phone if provided
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone());
        }

        // Save the updated user to the database
        // JPA automatically detects changes and generates UPDATE SQL
        // This is called "dirty checking"
        User updatedUser = userRepository.save(user);

        // Return the updated user data as DTO
        return mapToUserResponse(updatedUser);
    }

    /**
     * Change user password
     *
     * This method allows users to change their password.
     * It requires the current password for security (prevents unauthorized changes).
     *
     * Security best practices:
     * 1. Verify current password before allowing change
     * 2. Hash new password with BCrypt (never store plain text)
     * 3. Validate new password strength (done in DTO validation)
     *
     * @param userId The ID of the user changing password
     * @param request ChangePasswordRequest with old and new passwords
     * @throws ResourceNotFoundException if user doesn't exist
     * @throws BadRequestException if current password is incorrect
     */
    public void changePassword(Long userId, ChangePasswordRequest request) {
        // Find the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId
                ));

        // Security check: verify the current password is correct
        // passwordEncoder.matches() compares plain text with BCrypt hash
        // Parameters: (plainPassword, hashedPassword)
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            // If current password is wrong, reject the request
            // This prevents unauthorized password changes
            throw new BadRequestException("Current password is incorrect");
        }

        // Validate that new password is different from current
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }

        // Hash the new password with BCrypt
        // BCrypt automatically generates a salt and applies multiple rounds
        // Example: "password123" → "$2a$10$N9qo8uLOickgx2ZMRZoMye..."
        String hashedPassword = passwordEncoder.encode(request.getNewPassword());

        // Update the user's password with the hashed version
        user.setPassword(hashedPassword);

        // Save to database
        userRepository.save(user);

        // No return value needed - this is a void method
        // If successful, no exception is thrown
    }

    /**
     * Get all users (Admin only)
     *
     * This method retrieves all users in the system.
     * It should only be called by admin users (enforced in controller with @PreAuthorize)
     *
     * @return List of UserResponse DTOs
     */
    @Transactional(readOnly = true)  // Read-only optimization
    public List<UserResponse> getAllUsers() {
        // Get all users from database
        List<User> users = userRepository.findAll();

        // Convert each User entity to UserResponse DTO
        // Using Java Stream API for functional-style transformation
        // Stream operations:
        // 1. stream() - convert List to Stream
        // 2. map() - transform each element (User → UserResponse)
        // 3. collect() - collect results back into a List
        return users.stream()
                .map(this::mapToUserResponse)  // Method reference: same as (user -> mapToUserResponse(user))
                .collect(Collectors.toList());  // Collect into List<UserResponse>
    }

    /**
     * Deactivate user account (Admin only)
     *
     * This method disables a user account without deleting it.
     * Deactivated users cannot log in.
     *
     * Why deactivate instead of delete?
     * - Preserves historical data (orders, etc.)
     * - Maintains referential integrity
     * - Can be reactivated later if needed
     * - Complies with data retention policies
     *
     * @param userId The ID of the user to deactivate
     * @throws ResourceNotFoundException if user doesn't exist
     */
    public void deactivateUser(Long userId) {
        // Find the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId
                ));

        // Set enabled flag to false
        // This prevents login in CustomUserDetailsService
        user.setEnabled(false);

        // Save the change
        userRepository.save(user);
    }

    /**
     * Activate user account (Admin only)
     *
     * This method re-enables a previously deactivated user account.
     *
     * @param userId The ID of the user to activate
     * @throws ResourceNotFoundException if user doesn't exist
     */
    public void activateUser(Long userId) {
        // Find the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId
                ));

        // Set enabled flag to true
        user.setEnabled(true);

        // Save the change
        userRepository.save(user);
    }

    /**
     * Helper method: Convert User entity to UserResponse DTO
     *
     * This method transforms database entities to API response objects.
     *
     * Why use DTOs instead of returning entities directly?
     * 1. Security: Hide sensitive fields (password, internal IDs)
     * 2. Flexibility: Response structure can differ from database structure
     * 3. Versioning: Can have multiple DTO versions for same entity
     * 4. Performance: Can include computed fields or join data
     * 5. Clean API: Client sees only what they need
     *
     * @param user The User entity from database
     * @return UserResponse DTO for API response
     */
    private UserResponse mapToUserResponse(User user) {
        // Use builder pattern to construct UserResponse
        // Builder pattern advantages:
        // - Readable code (clear what each value represents)
        // - Flexible (can set fields in any order)
        // - Immutable (creates object in one step)
        return UserResponse.builder()
                // Copy ID
                .id(user.getId())

                // Copy personal information
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())

                // Copy account status
                .enabled(user.getEnabled())

                // Extract role names from Set<Role>
                // Stream operations:
                // 1. user.getRoles().stream() - convert Set<Role> to Stream<Role>
                // 2. .map(Role::getName) - extract name from each Role
                // 3. .collect(Collectors.toSet()) - collect into Set<String>
                .roles(user.getRoles().stream()
                        .map(role -> role.getName())  // Could also use Role::getName
                        .collect(Collectors.toSet()))

                // Copy timestamps
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())

                // Build the final UserResponse object
                .build();
    }

    /**
     * Helper method: Convert UserRequest DTO to User entity
     *
     * This method transforms API request data to database entities.
     * Used when creating or updating users.
     *
     * Note: This doesn't set:
     * - ID (auto-generated)
     * - Password (hashed separately)
     * - Roles (assigned separately)
     * - Timestamps (auto-generated by @PrePersist/@PreUpdate)
     *
     * @param request UserRequest DTO from API
     * @return User entity ready to save
     */
    private User mapToUser(UserRequest request) {
        // Create new User entity
        User user = new User();

        // Set fields from request
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        // Set default values
        user.setEnabled(true);  // New users are enabled by default

        return user;
    }
}
