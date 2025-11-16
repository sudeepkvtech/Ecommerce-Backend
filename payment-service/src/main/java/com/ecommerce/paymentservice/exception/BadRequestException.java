// Package declaration - this class belongs to the exception package
package com.ecommerce.paymentservice.exception;

/**
 * Bad Request Exception
 *
 * Custom exception thrown when client sends invalid or malformed request data
 *
 * HTTP Status Code: 400 BAD REQUEST
 *
 * What is a bad request?
 * ----------------------
 * A bad request occurs when the client sends data that:
 * 1. Fails business logic validation (beyond basic format validation)
 * 2. Contains inconsistent or conflicting data
 * 3. Violates application-specific rules
 * 4. Is semantically incorrect (correct format but wrong meaning)
 *
 * When to use BadRequestException vs other exceptions:
 * -----------------------------------------------------
 * - BadRequestException (400): Invalid request data or business logic violation
 *   Example: "Current password is incorrect"
 *   Example: "Cannot set address as default - it belongs to another user"
 *
 * - ResourceNotFoundException (404): Resource doesn't exist
 *   Example: "User not found with ID: 123"
 *
 * - DuplicateResourceException (409): Resource already exists
 *   Example: "User already registered with email: user@example.com"
 *
 * - MethodArgumentNotValidException (400): Format validation failures (@Valid)
 *   Example: "Email must be valid format"
 *   Example: "Password must be at least 8 characters"
 *
 * Common scenarios for BadRequestException:
 * ------------------------------------------
 * 1. Password validation:
 *    - User provides wrong current password when changing password
 *    - throw new BadRequestException("Current password is incorrect")
 *
 * 2. Business rule violations:
 *    - User tries to update address that belongs to another user
 *    - throw new BadRequestException("Address does not belong to user")
 *
 * 3. Inconsistent data:
 *    - New password same as current password
 *    - throw new BadRequestException("New password must be different from current")
 *
 * 4. Invalid state transitions:
 *    - Trying to activate an already active user
 *    - throw new BadRequestException("User is already active")
 *
 * Why create this exception?
 * --------------------------
 * 1. CLEAR INTENT: Name explicitly describes the error category
 * 2. CONSISTENT RESPONSES: All bad requests return HTTP 400 consistently
 * 3. SEPARATION: Distinguishes from validation errors, not found, conflicts
 * 4. FLEXIBILITY: Can use for any business logic validation
 */

// Extends RuntimeException to make this an unchecked exception
// Unchecked exceptions:
// - Don't need to be declared in method signatures (no "throws" required)
// - Don't force try-catch blocks everywhere
// - Spring automatically handles them via GlobalExceptionHandler
// - Modern best practice for application-level exceptions
public class BadRequestException extends RuntimeException {

    /**
     * Constructor with error message only
     *
     * This is the most common constructor used in the application.
     * It creates a BadRequestException with a descriptive error message
     * that will be sent to the client.
     *
     * @param message Descriptive error message explaining what was wrong with the request
     *
     * Usage examples:
     * ---------------
     * 1. Password validation:
     *    throw new BadRequestException("Current password is incorrect");
     *
     * 2. Ownership validation:
     *    throw new BadRequestException(
     *        "Address " + addressId + " does not belong to user " + userId
     *    );
     *
     * 3. Business rule:
     *    throw new BadRequestException(
     *        "New password must be different from current password"
     *    );
     *
     * 4. State validation:
     *    throw new BadRequestException("Cannot delete default address");
     *
     * Message guidelines:
     * -------------------
     * - Be specific: Explain exactly what was wrong
     * - Be helpful: Suggest how to fix it (when possible)
     * - Be secure: Don't expose sensitive system details
     * - Be professional: User-friendly language
     *
     * Good message: "Current password is incorrect. Please try again."
     * Bad message: "Error"
     *
     * super(message) calls the RuntimeException constructor
     * This stores the message so it can be retrieved later with getMessage()
     * and displayed to the client in the error response
     */
    public BadRequestException(String message) {
        // Call parent class (RuntimeException) constructor with the message
        // This initializes the exception with the error message
        super(message);
    }

    /**
     * Constructor with message and cause
     *
     * This constructor is used for exception chaining - when one exception
     * causes another exception to be thrown.
     *
     * @param message Descriptive error message for the client
     * @param cause The underlying exception that caused this error
     *
     * Usage example:
     * --------------
     * try {
     *     // Some operation that might fail
     *     passwordEncoder.matches(rawPassword, encodedPassword);
     * } catch (IllegalArgumentException e) {
     *     // Wrap the technical exception in a user-friendly one
     *     throw new BadRequestException(
     *         "Password verification failed",
     *         e  // Original exception preserved for debugging
     *     );
     * }
     *
     * Why chain exceptions?
     * ---------------------
     * 1. PRESERVE CONTEXT: Keep the original error for debugging
     * 2. USER-FRIENDLY: Show clear message to user, technical details in logs
     * 3. DEBUGGING: Full stack trace shows both exceptions
     * 4. ROOT CAUSE: Can trace back to the actual problem
     *
     * Stack trace with chaining:
     * --------------------------
     * BadRequestException: Password verification failed
     *     at AuthService.changePassword(AuthService.java:123)
     * Caused by: IllegalArgumentException: Invalid encoded password
     *     at BCryptPasswordEncoder.matches(BCryptPasswordEncoder.java:45)
     *
     * The stack trace shows:
     * - What the user sees: "Password verification failed"
     * - What actually happened: "Invalid encoded password"
     * - Where it happened: Full stack trace of both exceptions
     *
     * Accessing the cause:
     * --------------------
     * BadRequestException ex = ...;
     * Throwable cause = ex.getCause();  // Get underlying exception
     * String rootMessage = cause.getMessage();  // Get original error message
     *
     * super(message, cause) calls RuntimeException constructor with both parameters
     * This stores both the user-friendly message and the original technical exception
     */
    public BadRequestException(String message, Throwable cause) {
        // Call parent class constructor with both message and cause
        // This creates an exception chain for full debugging context
        super(message, cause);
    }

    // =========================================================================
    // How This Exception Flows Through the Application
    // =========================================================================
    //
    // Scenario: User provides wrong current password when changing password
    // ----------------------------------------------------------------------
    //
    // 1. Client sends request
    //    -----------------------------------------
    //    PUT /api/users/password
    //    Authorization: Bearer eyJhbGc...
    //    {
    //      "currentPassword": "wrong_password",
    //      "newPassword": "new_secure_password"
    //    }
    //
    // 2. Controller receives request
    //    -----------------------------------------
    //    @PutMapping("/password")
    //    public ResponseEntity<?> changePassword(
    //        @Valid @RequestBody ChangePasswordRequest request,
    //        Authentication authentication) {
    //
    //        String email = authentication.getName();
    //        User user = userRepository.findByEmail(email)...;
    //        userService.changePassword(user.getId(), request);
    //        return ResponseEntity.ok("Password changed successfully");
    //    }
    //
    // 3. Service validates current password
    //    -----------------------------------------
    //    public void changePassword(Long userId, ChangePasswordRequest request) {
    //        User user = userRepository.findById(userId)...;
    //
    //        // Verify current password
    //        if (!passwordEncoder.matches(
    //                request.getCurrentPassword(),
    //                user.getPassword())) {
    //            // Current password doesn't match - throw exception
    //            throw new BadRequestException("Current password is incorrect");
    //        }
    //
    //        // ... rest of password change logic
    //    }
    //
    // 4. Exception propagates to GlobalExceptionHandler
    //    -----------------------------------------
    //    @ExceptionHandler(BadRequestException.class)
    //    public ResponseEntity<ErrorResponse> handleBadRequest(
    //        BadRequestException ex) {
    //        ErrorResponse error = new ErrorResponse(
    //            HttpStatus.BAD_REQUEST.value(),  // 400
    //            ex.getMessage(),                 // "Current password is incorrect"
    //            LocalDateTime.now()
    //        );
    //        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    //    }
    //
    // 5. Client receives HTTP 400 response
    //    -----------------------------------------
    //    HTTP/1.1 400 Bad Request
    //    Content-Type: application/json
    //
    //    {
    //      "status": 400,
    //      "message": "Current password is incorrect",
    //      "timestamp": "2024-01-15T10:30:00"
    //    }
    //
    // 6. Frontend displays error to user
    //    -----------------------------------------
    //    if (response.status === 400) {
    //        showError(data.message);  // "Current password is incorrect"
    //        // Highlight the password field
    //        // Let user try again
    //    }
    //
    // =========================================================================
    // Difference Between Validation Types
    // =========================================================================
    //
    // 1. FORMAT VALIDATION (@Valid annotations)
    //    -----------------------------------------
    //    Handled by: MethodArgumentNotValidException → HTTP 400
    //    Examples:
    //    - @NotBlank: Field cannot be empty
    //    - @Email: Must be valid email format
    //    - @Size(min=8): Must be at least 8 characters
    //    - @Pattern: Must match regex pattern
    //
    //    This validates the FORMAT of the data
    //    Checked BEFORE controller method executes
    //
    // 2. BUSINESS VALIDATION (BadRequestException)
    //    -----------------------------------------
    //    Handled by: BadRequestException → HTTP 400
    //    Examples:
    //    - Current password is incorrect (password doesn't match database)
    //    - Address doesn't belong to user (ownership validation)
    //    - New password same as current (business rule)
    //    - Cannot delete default address (state validation)
    //
    //    This validates the MEANING/BUSINESS LOGIC of the data
    //    Checked DURING service method execution
    //
    // Both return HTTP 400, but:
    // - Format validation: Automatic, happens in framework layer
    // - Business validation: Manual, happens in service layer
    //
    // =========================================================================
    // When to Use BadRequestException
    // =========================================================================
    //
    // Use BadRequestException when:
    // ------------------------------
    // ✅ Request data fails business logic validation
    // ✅ Request contains conflicting or inconsistent data
    // ✅ Request violates application-specific rules
    // ✅ User doesn't have permission for this specific operation
    // ✅ Operation cannot be performed due to current state
    //
    // Don't use BadRequestException when:
    // ------------------------------------
    // ❌ Resource doesn't exist → Use ResourceNotFoundException (404)
    // ❌ Resource already exists → Use DuplicateResourceException (409)
    // ❌ Format validation fails → Let @Valid handle it (400)
    // ❌ Server error/bug → Let it become 500 Internal Server Error
    //
    // =========================================================================
    // Examples in User Service
    // =========================================================================
    //
    // 1. Password change validation:
    //    if (!passwordEncoder.matches(currentPassword, storedPassword)) {
    //        throw new BadRequestException("Current password is incorrect");
    //    }
    //
    // 2. Ownership validation:
    //    if (!address.getUser().getId().equals(userId)) {
    //        throw new BadRequestException(
    //            "Address " + addressId + " does not belong to user " + userId
    //        );
    //    }
    //
    // 3. Business rule validation:
    //    if (currentPassword.equals(newPassword)) {
    //        throw new BadRequestException(
    //            "New password must be different from current password"
    //        );
    //    }
    //
    // 4. State validation:
    //    if (user.isEnabled()) {
    //        throw new BadRequestException("User is already active");
    //    }
}
