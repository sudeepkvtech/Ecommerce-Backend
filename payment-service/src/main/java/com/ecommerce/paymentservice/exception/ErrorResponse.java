// Package declaration - this class belongs to the exception package
package com.ecommerce.paymentservice.exception;

// Import Lombok annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Import Java time class for timestamps
import java.time.LocalDateTime;
// Import List for multiple error messages
import java.util.List;

/**
 * ErrorResponse
 *
 * Standardized error response structure sent to clients when errors occur
 *
 * What is an error response?
 * --------------------------
 * When an error occurs in the API (validation failure, resource not found, etc.),
 * we need to send a structured error message back to the client.
 *
 * Without standardization:
 * ------------------------
 * Different errors might return different JSON structures:
 * - Some errors: { "error": "Not found" }
 * - Other errors: { "message": "Validation failed", "code": 400 }
 * - More errors: { "status": "error", "details": "..." }
 *
 * This is confusing for frontend developers!
 *
 * With standardization (this class):
 * ----------------------------------
 * ALL errors return the same structure:
 * {
 *   "status": 404,
 *   "message": "Product not found",
 *   "timestamp": "2024-01-15T10:30:00",
 *   "errors": ["Product not found with id: 1"]
 * }
 *
 * Benefits:
 * ---------
 * 1. CONSISTENCY: Same structure for all errors
 * 2. PREDICTABILITY: Frontend knows what to expect
 * 3. EASIER HANDLING: Single error handling code in frontend
 * 4. BETTER UX: Can show errors consistently to users
 * 5. DEBUGGING: Timestamps help track when errors occurred
 *
 * This class follows RFC 7807 (Problem Details for HTTP APIs)
 * A standard format for error responses in REST APIs
 */

// @Data generates getters, setters, toString, equals, hashCode
@Data

// @NoArgsConstructor generates a no-argument constructor
// Required for Jackson JSON serialization
@NoArgsConstructor

// @AllArgsConstructor generates a constructor with all fields
// Useful for creating error responses in one line
@AllArgsConstructor
public class ErrorResponse {

    /**
     * HTTP Status Code
     *
     * The HTTP status code associated with this error
     *
     * Common status codes:
     * --------------------
     * 400 Bad Request: Client sent invalid data
     *   - Validation errors (missing required fields)
     *   - Invalid format (text in number field)
     *
     * 401 Unauthorized: Authentication required
     *   - Missing JWT token
     *   - Invalid credentials
     *
     * 403 Forbidden: Authenticated but not authorized
     *   - User lacks permission for this action
     *   - Admin-only endpoint accessed by regular user
     *
     * 404 Not Found: Resource doesn't exist
     *   - Product not found with id: 123
     *   - Category not found
     *
     * 409 Conflict: Resource already exists
     *   - Duplicate SKU
     *   - Duplicate category name
     *
     * 422 Unprocessable Entity: Valid format, but business rules fail
     *   - Insufficient stock to fulfill order
     *   - Cannot delete category with products
     *
     * 500 Internal Server Error: Unexpected error
     *   - Unhandled exceptions
     *   - Database connection failure
     *   - Null pointer exceptions
     *
     * Why include status in JSON body?
     * ---------------------------------
     * The status is already in HTTP response headers:
     * HTTP/1.1 404 Not Found
     *
     * But including it in JSON body:
     * 1. Makes error response self-contained
     * 2. Useful when error is logged or stored
     * 3. Some HTTP clients don't easily access headers
     * 4. Helps with debugging by seeing status in response body
     *
     * Example:
     * --------
     * new ErrorResponse(404, "Product not found", ...)
     */
    private int status;

    /**
     * Error Message
     *
     * Human-readable description of what went wrong
     *
     * Good messages are:
     * ------------------
     * 1. DESCRIPTIVE: Clearly explain the problem
     *    Good: "Product not found with id: 123"
     *    Bad: "Not found"
     *
     * 2. ACTIONABLE: Suggest how to fix it
     *    Good: "Category name already exists. Please choose a different name."
     *    Bad: "Duplicate entry"
     *
     * 3. USER-FRIENDLY: Not too technical
     *    Good: "Price must be greater than 0"
     *    Bad: "javax.validation.constraints.DecimalMin violation"
     *
     * 4. SECURE: Don't expose internal details
     *    Good: "Authentication failed"
     *    Bad: "User 'admin' password incorrect in database users_table"
     *
     * Message examples:
     * -----------------
     * - "Product not found with id: 123"
     * - "Category name is required"
     * - "SKU must be between 2 and 100 characters"
     * - "Product already exists with SKU: LAPTOP-001"
     * - "Insufficient stock. Available: 5, Requested: 10"
     *
     * This is the main error message shown to users
     */
    private String message;

    /**
     * Timestamp
     *
     * When the error occurred
     *
     * Why include timestamp?
     * ----------------------
     * 1. DEBUGGING: Correlate errors with logs
     *    - User reports error at 10:30
     *    - Check server logs at 10:30
     *    - Find root cause
     *
     * 2. MONITORING: Track error patterns over time
     *    - Spike in errors at certain times?
     *    - Errors only on weekends?
     *    - Performance degradation trends?
     *
     * 3. SUPPORT: Help users report issues
     *    - "I got an error at 10:30 AM"
     *    - Support can find exact error
     *
     * 4. AUDITING: Keep track of error history
     *    - When did this user encounter errors?
     *    - How often do errors occur?
     *
     * Format:
     * -------
     * LocalDateTime serializes to ISO-8601 format:
     * "2024-01-15T10:30:00"
     *
     * Can be customized with @JsonFormat:
     * @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
     * Would produce: "2024-01-15 10:30:00"
     *
     * Example:
     * --------
     * LocalDateTime.now() // Current date and time
     */
    private LocalDateTime timestamp;

    /**
     * Detailed Errors (Optional)
     *
     * List of specific error messages, especially useful for validation errors
     *
     * This field may be null for simple errors (404, 409)
     * But very useful for validation errors (400)
     *
     * Why a List?
     * -----------
     * Validation can fail on multiple fields:
     * POST /api/products
     * {
     *   "name": "",           ← Error: Name is required
     *   "price": -10,         ← Error: Price must be positive
     *   "sku": ""             ← Error: SKU is required
     * }
     *
     * Single error message:
     * "Validation failed"  ← Not helpful, which fields?
     *
     * Multiple error messages (List):
     * [
     *   "Product name is required",
     *   "Price must be at least 0.01",
     *   "Product SKU is required"
     * ]
     * ← User knows exactly what to fix!
     *
     * When to use:
     * ------------
     * 1. Validation errors (400):
     *    Multiple fields can be invalid
     *    errors = ["Name required", "Price invalid", "SKU too long"]
     *
     * 2. Business rule violations (422):
     *    Multiple rules can fail
     *    errors = ["Insufficient stock", "Product inactive"]
     *
     * 3. Simple errors (404, 409):
     *    Usually just one error
     *    errors = ["Product not found with id: 1"]
     *    Or errors = null (just use message field)
     *
     * Example response with multiple errors:
     * ---------------------------------------
     * HTTP/1.1 400 Bad Request
     * {
     *   "status": 400,
     *   "message": "Validation failed",
     *   "timestamp": "2024-01-15T10:30:00",
     *   "errors": [
     *     "Product name is required",
     *     "Price must be at least 0.01",
     *     "Product SKU is required"
     *   ]
     * }
     *
     * Example response with single error:
     * ------------------------------------
     * HTTP/1.1 404 Not Found
     * {
     *   "status": 404,
     *   "message": "Product not found with id: 123",
     *   "timestamp": "2024-01-15T10:30:00",
     *   "errors": null
     * }
     * Or simply omit the errors field
     */
    private List<String> errors;

    /**
     * Convenience constructor for simple errors (without error list)
     *
     * Used when there's only one error message (404, 409, 500)
     *
     * @param status HTTP status code
     * @param message Error message
     * @param timestamp When error occurred
     *
     * Example usage:
     * --------------
     * throw new ResourceNotFoundException("Product not found");
     *
     * // In GlobalExceptionHandler:
     * ErrorResponse error = new ErrorResponse(
     *     404,
     *     ex.getMessage(),  // "Product not found"
     *     LocalDateTime.now()
     * );
     *
     * return ResponseEntity.status(404).body(error);
     *
     * Resulting JSON:
     * ---------------
     * {
     *   "status": 404,
     *   "message": "Product not found",
     *   "timestamp": "2024-01-15T10:30:00"
     * }
     *
     * Note: errors field will be null (or omitted by Jackson)
     */
    public ErrorResponse(int status, String message, LocalDateTime timestamp) {
        // Set the three basic fields
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        // errors field remains null
    }

    // =========================================================================
    // Example Error Responses
    // =========================================================================
    //
    // 1. Resource Not Found (404)
    // ---------------------------
    // {
    //   "status": 404,
    //   "message": "Product not found with id: 123",
    //   "timestamp": "2024-01-15T10:30:00"
    // }
    //
    // 2. Duplicate Resource (409)
    // ---------------------------
    // {
    //   "status": 409,
    //   "message": "Product already exists with SKU: LAPTOP-001",
    //   "timestamp": "2024-01-15T10:30:00"
    // }
    //
    // 3. Validation Error (400)
    // -------------------------
    // {
    //   "status": 400,
    //   "message": "Validation failed",
    //   "timestamp": "2024-01-15T10:30:00",
    //   "errors": [
    //     "Product name is required",
    //     "Price must be at least 0.01",
    //     "Product SKU must be between 2 and 100 characters"
    //   ]
    // }
    //
    // 4. Internal Server Error (500)
    // ------------------------------
    // {
    //   "status": 500,
    //   "message": "An unexpected error occurred. Please try again later.",
    //   "timestamp": "2024-01-15T10:30:00"
    // }
    //
    // =========================================================================
    // Frontend Error Handling
    // =========================================================================
    //
    // try {
    //   const response = await fetch('/api/products/123');
    //
    //   if (!response.ok) {
    //     const errorData = await response.json();
    //
    //     // Handle based on status code
    //     if (errorData.status === 404) {
    //       showError('Product not found');
    //     } else if (errorData.status === 400) {
    //       // Show all validation errors
    //       errorData.errors.forEach(error => showError(error));
    //     } else {
    //       // Generic error
    //       showError(errorData.message);
    //     }
    //   }
    // } catch (error) {
    //   showError('Network error. Please check your connection.');
    // }
}
