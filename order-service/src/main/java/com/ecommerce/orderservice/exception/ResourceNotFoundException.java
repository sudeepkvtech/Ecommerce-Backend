// Package declaration - this class belongs to the exception package
package com.ecommerce.orderservice.exception;

/**
 * ResourceNotFoundException
 *
 * Custom exception thrown when a requested resource (Product, Category) is not found
 *
 * What is a Custom Exception?
 * ---------------------------
 * A custom exception is a user-defined exception class that extends
 * Java's built-in exception classes to represent specific error conditions.
 *
 * Why create custom exceptions?
 * ------------------------------
 * 1. CLARITY: Clearly communicates what went wrong
 *    - ResourceNotFoundException is more descriptive than generic Exception
 *    - Code readers immediately understand the error
 *
 * 2. SPECIFIC HANDLING: Can catch and handle specific exceptions
 *    try {
 *      product = productService.getById(id);
 *    } catch (ResourceNotFoundException e) {
 *      // Handle not found case specifically
 *      return ResponseEntity.status(404).body("Product not found");
 *    }
 *
 * 3. CONSISTENT ERROR RESPONSES: GlobalExceptionHandler can catch this
 *    exception type and return consistent HTTP 404 responses
 *
 * 4. BETTER DEBUGGING: Stack trace shows custom exception name
 *    - Makes logs more readable
 *    - Easier to track down issues
 *
 * When is this exception thrown?
 * -------------------------------
 * - GET /api/products/{id} → Product not found
 * - GET /api/categories/{id} → Category not found
 * - PUT /api/products/{id} → Trying to update non-existent product
 * - DELETE /api/products/{id} → Trying to delete non-existent product
 *
 * Example usage in Service layer:
 * --------------------------------
 * public ProductResponse getProductById(Long id) {
 *     Product product = productRepository.findById(id)
 *         .orElseThrow(() -> new ResourceNotFoundException(
 *             "Product not found with id: " + id
 *         ));
 *     return toResponse(product);
 * }
 *
 * What happens when this exception is thrown?
 * --------------------------------------------
 * 1. Exception is thrown in Service layer
 * 2. Propagates up to Controller
 * 3. GlobalExceptionHandler catches it
 * 4. Returns HTTP 404 with error message
 * 5. Client receives JSON error response
 *
 * Exception Hierarchy:
 * --------------------
 * RuntimeException extends from Exception
 * ResourceNotFoundException extends from RuntimeException
 *
 * RuntimeException vs Exception:
 * ------------------------------
 * RuntimeException:
 * - Unchecked exception (no need to declare in method signature)
 * - No need for try-catch (optional)
 * - Examples: NullPointerException, IllegalArgumentException
 *
 * Exception (checked):
 * - Must be declared in method signature: throws Exception
 * - Must be caught with try-catch or declared
 * - Forces caller to handle the exception
 *
 * We extend RuntimeException because:
 * -----------------------------------
 * 1. More convenient (no forced try-catch everywhere)
 * 2. Spring handles runtime exceptions automatically
 * 3. Cleaner code (no throws declarations)
 * 4. Modern best practice for application exceptions
 */

// Extends RuntimeException to make this an unchecked exception
// This means:
// - No need to declare "throws ResourceNotFoundException" in method signatures
// - No forced try-catch blocks
// - Spring's exception handlers can catch it automatically
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructor with error message only
     *
     * @param message Descriptive error message explaining what resource was not found
     *
     * When you create this exception:
     * new ResourceNotFoundException("Product not found with id: 1")
     *
     * The message is stored and can be:
     * - Retrieved with getMessage()
     * - Logged to console/files
     * - Sent to client in HTTP response
     * - Displayed in stack traces
     *
     * Example messages:
     * -----------------
     * - "Product not found with id: 123"
     * - "Product not found with SKU: LAPTOP-001"
     * - "Category not found with id: 5"
     * - "Category not found with name: Electronics"
     *
     * super(message) calls the RuntimeException constructor with the message
     * This stores the message in the exception object
     */
    public ResourceNotFoundException(String message) {
        // Call parent class (RuntimeException) constructor with the message
        // This initializes the exception with the error message
        super(message);
    }

    /**
     * Constructor with message and cause
     *
     * @param message Descriptive error message
     * @param cause The underlying exception that caused this exception
     *
     * What is exception chaining?
     * ---------------------------
     * Sometimes one exception causes another exception
     * We want to preserve the original exception for debugging
     *
     * Example scenario:
     * -----------------
     * try {
     *     Product product = productRepository.findById(id)
     *         .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
     * } catch (DatabaseException e) {
     *     // Database error occurred while searching
     *     throw new ResourceNotFoundException(
     *         "Could not retrieve product due to database error",
     *         e  // Original database exception
     *     );
     * }
     *
     * Why chain exceptions?
     * ---------------------
     * 1. PRESERVE CONTEXT: Keep original error information
     * 2. ROOT CAUSE ANALYSIS: Trace back to the real problem
     * 3. DEBUGGING: Stack trace shows full exception chain
     * 4. LOGGING: Logs can show all related exceptions
     *
     * Stack trace with chaining:
     * --------------------------
     * ResourceNotFoundException: Could not retrieve product due to database error
     *     at ProductService.getProductById(ProductService.java:45)
     * Caused by: DatabaseException: Connection timeout
     *     at Database.query(Database.java:123)
     *
     * You can see both exceptions and their stack traces
     *
     * Accessing the cause:
     * --------------------
     * ResourceNotFoundException e = ...;
     * Throwable cause = e.getCause(); // Gets the underlying exception
     * String rootMessage = cause.getMessage(); // Gets original error message
     *
     * super(message, cause) calls RuntimeException constructor with both parameters
     * This stores both the message and the original exception
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        // Call parent class constructor with message and cause
        // This chains the exceptions together
        super(message, cause);
    }

    // =========================================================================
    // How This Exception Flows Through the Application
    // =========================================================================
    //
    // 1. Repository returns Optional.empty()
    //    -----------------------------------------
    //    Optional<Product> result = productRepository.findById(1L);
    //    // result is empty - product doesn't exist
    //
    // 2. Service throws ResourceNotFoundException
    //    -----------------------------------------
    //    Product product = result.orElseThrow(() ->
    //        new ResourceNotFoundException("Product not found with id: 1")
    //    );
    //
    // 3. Exception propagates to Controller
    //    -----------------------------------------
    //    // Controller method doesn't need try-catch
    //    // Exception automatically handled by Spring
    //
    // 4. GlobalExceptionHandler catches it
    //    -----------------------------------------
    //    @ExceptionHandler(ResourceNotFoundException.class)
    //    public ResponseEntity<ErrorResponse> handleResourceNotFound(
    //        ResourceNotFoundException ex) {
    //        ErrorResponse error = new ErrorResponse(
    //            HttpStatus.NOT_FOUND.value(),
    //            ex.getMessage(),
    //            LocalDateTime.now()
    //        );
    //        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    //    }
    //
    // 5. Client receives HTTP 404 response
    //    -----------------------------------------
    //    HTTP/1.1 404 Not Found
    //    Content-Type: application/json
    //
    //    {
    //      "status": 404,
    //      "message": "Product not found with id: 1",
    //      "timestamp": "2024-01-15T10:30:00"
    //    }
    //
    // =========================================================================
    // Benefits of This Approach
    // =========================================================================
    //
    // 1. SEPARATION OF CONCERNS
    //    - Service layer focuses on business logic
    //    - Controller doesn't need error handling code
    //    - GlobalExceptionHandler centralizes error responses
    //
    // 2. CONSISTENT ERROR RESPONSES
    //    - All 404 errors have same JSON structure
    //    - Clients know what to expect
    //    - Easy to handle on frontend
    //
    // 3. CLEAN CODE
    //    - No try-catch blocks everywhere
    //    - No if-else for error handling
    //    - Readable and maintainable
    //
    // 4. FLEXIBLE HANDLING
    //    - Can catch and handle differently in different contexts
    //    - Can add more information to error response
    //    - Can log exceptions centrally
    //
    // 5. STANDARD HTTP STATUS
    //    - 404 Not Found is universally understood
    //    - RESTful API best practice
    //    - Clients can handle by status code
}
