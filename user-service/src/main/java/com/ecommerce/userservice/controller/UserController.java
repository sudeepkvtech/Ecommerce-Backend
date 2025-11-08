package com.ecommerce.userservice.controller;

// Import DTOs (Data Transfer Objects) for request and response
import com.ecommerce.userservice.dto.UserRequest;
import com.ecommerce.userservice.dto.UserResponse;
import com.ecommerce.userservice.dto.ChangePasswordRequest;

// Import service layer for business logic
import com.ecommerce.userservice.service.UserService;

// Import Spring Framework annotations
import org.springframework.http.HttpStatus;  // HTTP status codes (200, 201, 404, etc.)
import org.springframework.http.ResponseEntity;  // Wraps response body + status + headers
import org.springframework.security.access.prepost.PreAuthorize;  // Method-level security
import org.springframework.security.core.Authentication;  // Current authenticated user
import org.springframework.web.bind.annotation.*;  // REST controller annotations

// Import validation annotation
import jakarta.validation.Valid;  // Triggers DTO validation

// Import Lombok for constructor injection
import lombok.RequiredArgsConstructor;

// Import List for multiple users
import java.util.List;

/**
 * User Controller
 *
 * REST controller for user profile management operations.
 *
 * This controller handles HTTP requests for:
 * - Getting current user profile
 * - Updating user profile
 * - Changing password
 * - Admin operations (get all users, get user by ID, etc.)
 *
 * Base URL: /api/users
 * All endpoints require authentication (JWT token in Authorization header)
 *
 * What is a REST Controller?
 * --------------------------
 * A REST controller is a class that:
 * 1. Receives HTTP requests (GET, POST, PUT, DELETE)
 * 2. Validates request data
 * 3. Calls service layer for business logic
 * 4. Returns HTTP responses (JSON data + status code)
 *
 * Controller → Service → Repository → Database
 * ↓
 * Handles HTTP    Business     Data         MySQL
 * requests        logic        access
 *
 * Why separate Controller from Service?
 * --------------------------------------
 * - Controllers: Handle HTTP-specific concerns (requests, responses, status codes)
 * - Services: Contain business logic (can be reused outside HTTP context)
 * - Makes testing easier (can test service without HTTP)
 * - Follows Single Responsibility Principle
 */

// @RestController = @Controller + @ResponseBody
// - Marks this class as a REST API controller
// - All method return values are automatically converted to JSON
// - Spring creates a singleton bean of this class
@RestController

// @RequestMapping sets the base URL for all endpoints in this controller
// All URLs will start with /api/users
// Example: GET /api/users/profile, PUT /api/users/password
@RequestMapping("/api/users")

// @RequiredArgsConstructor (Lombok) generates constructor for all 'final' fields
// This enables constructor injection of dependencies (better than @Autowired)
@RequiredArgsConstructor
public class UserController {

    // UserService dependency - injected via constructor
    // 'final' ensures it's immutable after construction (safer)
    // Spring automatically finds and injects the UserService bean
    private final UserService userService;

    /**
     * Get current user profile
     *
     * This endpoint returns the profile of the currently authenticated user.
     * The user's identity is extracted from the JWT token by Spring Security.
     *
     * URL: GET /api/users/profile
     * Authentication: Required (JWT token)
     * Authorization: Any authenticated user
     *
     * Request headers:
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     *
     * Response: 200 OK
     * {
     *   "id": 1,
     *   "firstName": "John",
     *   "lastName": "Doe",
     *   "email": "john@example.com",
     *   "phone": "+1-555-555-5555",
     *   "enabled": true,
     *   "roles": ["ROLE_USER"],
     *   "createdAt": "2024-01-15T10:30:00",
     *   "updatedAt": "2024-01-15T10:30:00"
     * }
     *
     * @param authentication Spring Security's Authentication object
     *                       Contains current user's information
     *                       Automatically injected by Spring
     * @return ResponseEntity with UserResponse and HTTP 200 OK
     */
    @GetMapping("/profile")  // Maps to GET /api/users/profile
    public ResponseEntity<UserResponse> getCurrentUserProfile(
            // Authentication is automatically injected by Spring Security
            // It contains information about the currently logged-in user
            // authentication.getName() returns the username (email in our case)
            Authentication authentication) {

        // Extract the username (email) from the Authentication object
        // When user logs in with JWT:
        // 1. JwtAuthenticationFilter validates the token
        // 2. Extracts email from token
        // 3. Loads user details
        // 4. Creates Authentication object
        // 5. Stores in SecurityContext
        // 6. Spring injects it here
        String email = authentication.getName();

        // Call service to get user profile by email
        // Service will:
        // 1. Find user in database by email
        // 2. Convert User entity to UserResponse DTO
        // 3. Return the DTO
        UserResponse userResponse = userService.getCurrentUserProfile(email);

        // Return HTTP 200 OK with user data in JSON
        // ResponseEntity.ok() is shorthand for:
        // new ResponseEntity<>(userResponse, HttpStatus.OK)
        return ResponseEntity.ok(userResponse);
    }

    /**
     * Update current user profile
     *
     * This endpoint allows users to update their own profile information.
     * Users can update: firstName, lastName, phone
     * Users CANNOT update: email, password, roles, enabled status
     *
     * URL: PUT /api/users/profile
     * Authentication: Required (JWT token)
     * Authorization: Any authenticated user
     *
     * Request body:
     * {
     *   "firstName": "John",
     *   "lastName": "Doe",
     *   "email": "john@example.com",  // Ignored (cannot change email)
     *   "phone": "+1-555-555-5555"
     * }
     *
     * Response: 200 OK
     * {
     *   "id": 1,
     *   "firstName": "John",
     *   "lastName": "Doe",
     *   "email": "john@example.com",
     *   "phone": "+1-555-555-5555",
     *   "enabled": true,
     *   "roles": ["ROLE_USER"],
     *   "createdAt": "2024-01-15T10:30:00",
     *   "updatedAt": "2024-01-15T11:00:00"  // Updated timestamp
     * }
     *
     * @param request UserRequest DTO with updated profile data
     * @param authentication Current authenticated user
     * @return ResponseEntity with updated UserResponse and HTTP 200 OK
     */
    @PutMapping("/profile")  // Maps to PUT /api/users/profile
    public ResponseEntity<UserResponse> updateCurrentUserProfile(
            // @Valid triggers validation on UserRequest
            // Checks @NotBlank, @Size, @Email, @Pattern annotations
            // If validation fails, throws MethodArgumentNotValidException
            // GlobalExceptionHandler catches it and returns HTTP 400
            @Valid @RequestBody UserRequest request,

            // Current authenticated user (from JWT token)
            Authentication authentication) {

        // Extract email from authentication
        String email = authentication.getName();

        // Get current user to retrieve their ID
        // We need the user ID to update the correct user
        UserResponse currentUser = userService.getCurrentUserProfile(email);

        // Update user profile
        // Service will:
        // 1. Find user by ID
        // 2. Update modifiable fields (firstName, lastName, phone)
        // 3. Ignore fields that shouldn't change (email, password, roles)
        // 4. Save to database
        // 5. Return updated user as DTO
        UserResponse updatedUser = userService.updateUser(currentUser.getId(), request);

        // Return HTTP 200 OK with updated user data
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Change password
     *
     * This endpoint allows users to change their own password.
     * Requires current password for security (prevents unauthorized changes).
     *
     * URL: PUT /api/users/password
     * Authentication: Required (JWT token)
     * Authorization: Any authenticated user
     *
     * Request body:
     * {
     *   "currentPassword": "old_password_123",
     *   "newPassword": "new_secure_password_456"
     * }
     *
     * Success response: 200 OK
     * {
     *   "message": "Password changed successfully"
     * }
     *
     * Error responses:
     * - 400 Bad Request: Current password is incorrect
     * - 400 Bad Request: New password same as current
     * - 400 Bad Request: New password doesn't meet requirements
     *
     * Security notes:
     * ---------------
     * - Current password is verified before allowing change
     * - New password is hashed with BCrypt (never stored plain)
     * - Password validation in ChangePasswordRequest (@Size)
     * - No password hint/reset in response
     *
     * @param request ChangePasswordRequest with current and new passwords
     * @param authentication Current authenticated user
     * @return ResponseEntity with success message and HTTP 200 OK
     */
    @PutMapping("/password")  // Maps to PUT /api/users/password
    public ResponseEntity<String> changePassword(
            // @Valid triggers validation on ChangePasswordRequest
            // Checks that:
            // - currentPassword is not blank
            // - newPassword is not blank
            // - newPassword is between 8-100 characters
            @Valid @RequestBody ChangePasswordRequest request,

            // Current authenticated user
            Authentication authentication) {

        // Extract email from authentication
        String email = authentication.getName();

        // Get current user to retrieve their ID
        UserResponse currentUser = userService.getCurrentUserProfile(email);

        // Change password
        // Service will:
        // 1. Find user by ID
        // 2. Verify current password matches (BCrypt)
        // 3. Check new password is different
        // 4. Hash new password with BCrypt
        // 5. Update user's password
        // 6. Save to database
        //
        // If current password is wrong, service throws BadRequestException
        // GlobalExceptionHandler catches it and returns HTTP 400
        userService.changePassword(currentUser.getId(), request);

        // Return HTTP 200 OK with success message
        // No sensitive data in response (password not included)
        return ResponseEntity.ok("Password changed successfully");
    }

    /**
     * Get user by ID (Admin only)
     *
     * This endpoint allows administrators to view any user's profile.
     * Regular users cannot access this endpoint.
     *
     * URL: GET /api/users/{id}
     * Authentication: Required (JWT token)
     * Authorization: ROLE_ADMIN only
     *
     * Request: GET /api/users/123
     *
     * Response: 200 OK
     * {
     *   "id": 123,
     *   "firstName": "Jane",
     *   "lastName": "Smith",
     *   "email": "jane@example.com",
     *   "phone": "+1-555-555-1234",
     *   "enabled": true,
     *   "roles": ["ROLE_USER"],
     *   "createdAt": "2024-01-10T09:00:00",
     *   "updatedAt": "2024-01-10T09:00:00"
     * }
     *
     * Error responses:
     * - 403 Forbidden: User is not an admin
     * - 404 Not Found: User with given ID doesn't exist
     *
     * @PreAuthorize annotation:
     * -------------------------
     * Checks user's roles BEFORE executing method
     * "hasRole('ADMIN')" means only users with ROLE_ADMIN can call this
     * If user lacks permission, Spring Security throws AccessDeniedException
     * Returns HTTP 403 Forbidden automatically
     *
     * @param id The ID of the user to retrieve
     * @return ResponseEntity with UserResponse and HTTP 200 OK
     */
    @GetMapping("/{id}")  // Maps to GET /api/users/{id}
    @PreAuthorize("hasRole('ADMIN')")  // Only admins can call this method
    public ResponseEntity<UserResponse> getUserById(
            // @PathVariable extracts {id} from URL path
            // Example: GET /api/users/123 → id = 123
            @PathVariable Long id) {

        // Get user by ID from service
        // Service will:
        // 1. Find user in database by ID
        // 2. If not found, throw ResourceNotFoundException → HTTP 404
        // 3. Convert User entity to UserResponse DTO
        // 4. Return the DTO
        UserResponse userResponse = userService.getUserById(id);

        // Return HTTP 200 OK with user data
        return ResponseEntity.ok(userResponse);
    }

    /**
     * Get all users (Admin only)
     *
     * This endpoint returns a list of all users in the system.
     * Only administrators can access this endpoint.
     *
     * URL: GET /api/users
     * Authentication: Required (JWT token)
     * Authorization: ROLE_ADMIN only
     *
     * Response: 200 OK
     * [
     *   {
     *     "id": 1,
     *     "firstName": "John",
     *     "lastName": "Doe",
     *     "email": "john@example.com",
     *     ...
     *   },
     *   {
     *     "id": 2,
     *     "firstName": "Jane",
     *     "lastName": "Smith",
     *     "email": "jane@example.com",
     *     ...
     *   }
     * ]
     *
     * @return ResponseEntity with List of UserResponse and HTTP 200 OK
     */
    @GetMapping  // Maps to GET /api/users
    @PreAuthorize("hasRole('ADMIN')")  // Only admins can call this
    public ResponseEntity<List<UserResponse>> getAllUsers() {

        // Get all users from service
        // Service will:
        // 1. Query all users from database
        // 2. Convert each User entity to UserResponse DTO
        // 3. Return List<UserResponse>
        List<UserResponse> users = userService.getAllUsers();

        // Return HTTP 200 OK with list of users
        // List is automatically converted to JSON array
        return ResponseEntity.ok(users);
    }

    /**
     * Deactivate user (Admin only)
     *
     * This endpoint disables a user account without deleting it.
     * Deactivated users cannot log in until reactivated.
     *
     * URL: PUT /api/users/{id}/deactivate
     * Authentication: Required (JWT token)
     * Authorization: ROLE_ADMIN only
     *
     * Request: PUT /api/users/123/deactivate
     *
     * Response: 200 OK
     * {
     *   "message": "User deactivated successfully"
     * }
     *
     * Why deactivate instead of delete?
     * ----------------------------------
     * - Preserves user data for auditing
     * - Can be reversed (reactivated)
     * - Maintains referential integrity (foreign keys)
     * - Complies with data retention policies
     *
     * @param id The ID of the user to deactivate
     * @return ResponseEntity with success message and HTTP 200 OK
     */
    @PutMapping("/{id}/deactivate")  // Maps to PUT /api/users/{id}/deactivate
    @PreAuthorize("hasRole('ADMIN')")  // Only admins can call this
    public ResponseEntity<String> deactivateUser(@PathVariable Long id) {

        // Deactivate user via service
        // Service will:
        // 1. Find user by ID
        // 2. Set enabled = false
        // 3. Save to database
        // 4. User cannot log in anymore (CustomUserDetailsService checks enabled flag)
        userService.deactivateUser(id);

        // Return HTTP 200 OK with success message
        return ResponseEntity.ok("User deactivated successfully");
    }

    /**
     * Activate user (Admin only)
     *
     * This endpoint re-enables a previously deactivated user account.
     *
     * URL: PUT /api/users/{id}/activate
     * Authentication: Required (JWT token)
     * Authorization: ROLE_ADMIN only
     *
     * Request: PUT /api/users/123/activate
     *
     * Response: 200 OK
     * {
     *   "message": "User activated successfully"
     * }
     *
     * @param id The ID of the user to activate
     * @return ResponseEntity with success message and HTTP 200 OK
     */
    @PutMapping("/{id}/activate")  // Maps to PUT /api/users/{id}/activate
    @PreAuthorize("hasRole('ADMIN')")  // Only admins can call this
    public ResponseEntity<String> activateUser(@PathVariable Long id) {

        // Activate user via service
        // Service will:
        // 1. Find user by ID
        // 2. Set enabled = true
        // 3. Save to database
        // 4. User can log in again
        userService.activateUser(id);

        // Return HTTP 200 OK with success message
        return ResponseEntity.ok("User activated successfully");
    }

    // =========================================================================
    // HTTP Method Summary
    // =========================================================================
    //
    // GET: Retrieve data (read-only)
    //   - GET /api/users/profile → Get current user
    //   - GET /api/users/{id} → Get user by ID
    //   - GET /api/users → Get all users
    //
    // POST: Create new resource
    //   - (Handled in AuthController for user registration)
    //
    // PUT: Update existing resource
    //   - PUT /api/users/profile → Update current user
    //   - PUT /api/users/password → Change password
    //   - PUT /api/users/{id}/deactivate → Deactivate user
    //   - PUT /api/users/{id}/activate → Activate user
    //
    // DELETE: Remove resource
    //   - (Not implemented - we deactivate instead)
    //
    // =========================================================================
    // Response Entity Patterns
    // =========================================================================
    //
    // 1. Success with data:
    //    return ResponseEntity.ok(data);  // HTTP 200 with JSON body
    //
    // 2. Success with message:
    //    return ResponseEntity.ok("Success message");  // HTTP 200 with string
    //
    // 3. Created (for POST):
    //    return ResponseEntity.status(HttpStatus.CREATED).body(data);  // HTTP 201
    //
    // 4. No content (for DELETE):
    //    return ResponseEntity.noContent().build();  // HTTP 204
    //
    // 5. Custom status:
    //    return new ResponseEntity<>(data, HttpStatus.ACCEPTED);  // HTTP 202
    //
    // =========================================================================
    // Security Flow
    // =========================================================================
    //
    // 1. Client sends request with JWT token
    //    GET /api/users/profile
    //    Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
    //
    // 2. JwtAuthenticationFilter intercepts request
    //    - Extracts JWT from Authorization header
    //    - Validates JWT signature and expiration
    //    - Extracts username (email) from token
    //    - Loads user details from database
    //    - Creates Authentication object
    //    - Sets in SecurityContext
    //
    // 3. Spring Security checks @PreAuthorize (if present)
    //    - Checks user's roles
    //    - If lacks permission, returns HTTP 403 Forbidden
    //    - If has permission, continues to controller method
    //
    // 4. Controller method executes
    //    - Receives Authentication object
    //    - Calls service methods
    //    - Returns response
    //
    // 5. Response sent to client
    //    - Data serialized to JSON
    //    - HTTP status code set
    //    - Headers added
    //    - Client receives response
}
