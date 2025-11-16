// Package declaration - this class belongs to the exception package
package com.ecommerce.userservice.exception;

// Import Spring annotations for exception handling
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Import validation exception
import org.springframework.validation.FieldError;

// Import Java time and collections
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Global Exception Handler
 *
 * Centralized exception handling for the entire application
 *
 * What is @RestControllerAdvice?
 * -------------------------------
 * @RestControllerAdvice is a Spring annotation that combines:
 * 1. @ControllerAdvice: Global exception handling across all controllers
 * 2. @ResponseBody: Automatically serializes return value to JSON
 *
 * Think of it as a "safety net" that catches exceptions from ALL controllers
 * and converts them into proper HTTP error responses.
 *
 * Why use Global Exception Handler?
 * ----------------------------------
 * WITHOUT global handler:
 * -----------------------
 * Every controller method needs try-catch:
 *
 * @GetMapping("/{id}")
 * public ResponseEntity<?> getProduct(@PathVariable Long id) {
 *     try {
 *         Product product = productService.getById(id);
 *         return ResponseEntity.ok(product);
 *     } catch (ResourceNotFoundException e) {
 *         return ResponseEntity.status(404)
 *             .body(new ErrorResponse(404, e.getMessage(), LocalDateTime.now()));
 *     } catch (Exception e) {
 *         return ResponseEntity.status(500)
 *             .body(new ErrorResponse(500, "Internal error", LocalDateTime.now()));
 *     }
 * }
 *
 * This is:
 * - Repetitive (same try-catch in every method)
 * - Error-prone (easy to forget exception handling)
 * - Hard to maintain (changes needed everywhere)
 * - Messy code (business logic mixed with error handling)
 *
 * WITH global handler:
 * --------------------
 * Controllers are clean:
 *
 * @GetMapping("/{id}")
 * public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
 *     ProductResponse product = productService.getById(id);
 *     return ResponseEntity.ok(product);
 * }
 *
 * If ResourceNotFoundException is thrown:
 * - Propagates up from Service → Controller
 * - GlobalExceptionHandler catches it
 * - Returns proper HTTP 404 response
 * - All automatic!
 *
 * Benefits:
 * ---------
 * 1. DRY (Don't Repeat Yourself): Write error handling once
 * 2. CONSISTENCY: All errors handled the same way
 * 3. CLEAN CODE: Controllers focus on business logic
 * 4. MAINTAINABILITY: Change error handling in one place
 * 5. TESTABILITY: Easier to test controllers without error cases
 *
 * How it works:
 * -------------
 * 1. Exception thrown anywhere in application
 * 2. Spring catches the exception
 * 3. Spring looks for @ExceptionHandler methods in @RestControllerAdvice classes
 * 4. Finds matching handler based on exception type
 * 5. Executes handler method
 * 6. Returns ResponseEntity as HTTP response
 * 7. Client receives proper error response
 */

// @RestControllerAdvice makes this class a global exception handler
// It will intercept exceptions from all @RestController classes
// Automatically converts return values to JSON (combines @ControllerAdvice + @ResponseBody)
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle ResourceNotFoundException
     *
     * Catches: ResourceNotFoundException thrown anywhere in the application
     * Returns: HTTP 404 Not Found with error details
     *
     * @ExceptionHandler annotation:
     * ----------------------------
     * Tells Spring: "When ResourceNotFoundException is thrown, call this method"
     *
     * The exception parameter (ex):
     * ----------------------------
     * Spring automatically passes the thrown exception to this parameter
     * We can access:
     * - ex.getMessage(): The error message
     * - ex.getCause(): The underlying cause (if any)
     * - ex.getStackTrace(): For logging
     *
     * @param ex The ResourceNotFoundException that was thrown
     * @return ResponseEntity with ErrorResponse body and HTTP 404 status
     *
     * Example flow:
     * -------------
     * 1. Service: productRepository.findById(123).orElseThrow(
     *        () -> new ResourceNotFoundException("Product not found with id: 123")
     *    )
     * 2. Exception thrown: ResourceNotFoundException
     * 3. This handler catches it
     * 4. Creates ErrorResponse with status=404, message="Product not found..."
     * 5. Returns HTTP 404 with JSON body
     * 6. Client receives:
     *    HTTP/1.1 404 Not Found
     *    {
     *      "status": 404,
     *      "message": "Product not found with id: 123",
     *      "timestamp": "2024-01-15T10:30:00"
     *    }
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex) {

        // Create error response object with:
        // - Status code: 404 (NOT_FOUND)
        // - Message: From the exception
        // - Timestamp: Current time
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),  // 404
                ex.getMessage(),                // "Product not found with id: 123"
                LocalDateTime.now()             // "2024-01-15T10:30:00"
        );

        // Return ResponseEntity with:
        // - HTTP status: 404 Not Found
        // - Body: ErrorResponse object (automatically converted to JSON)
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle DuplicateResourceException
     *
     * Catches: DuplicateResourceException (duplicate SKU, category name, etc.)
     * Returns: HTTP 409 Conflict with error details
     *
     * HTTP 409 Conflict indicates:
     * - The request conflicts with current state of the resource
     * - The resource already exists
     * - Client should modify their request
     *
     * @param ex The DuplicateResourceException that was thrown
     * @return ResponseEntity with ErrorResponse body and HTTP 409 status
     *
     * Example flow:
     * -------------
     * 1. Client: POST /api/products { "sku": "LAPTOP-001", ... }
     * 2. Service checks: productRepository.existsBySku("LAPTOP-001") → true
     * 3. Service throws: new DuplicateResourceException("Product already exists...")
     * 4. This handler catches it
     * 5. Returns HTTP 409 Conflict
     * 6. Client receives:
     *    HTTP/1.1 409 Conflict
     *    {
     *      "status": 409,
     *      "message": "Product already exists with SKU: LAPTOP-001",
     *      "timestamp": "2024-01-15T10:30:00"
     *    }
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex) {

        // Create error response with HTTP 409 status
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),    // 409
                ex.getMessage(),                 // "Product already exists with SKU: LAPTOP-001"
                LocalDateTime.now()
        );

        // Return ResponseEntity with HTTP 409 Conflict status
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handle BadRequestException
     *
     * Catches: BadRequestException (business logic validation failures)
     * Returns: HTTP 400 Bad Request with error details
     *
     * HTTP 400 Bad Request indicates:
     * - The request data fails business logic validation
     * - Contains conflicting or inconsistent data
     * - Violates application-specific rules
     * - Cannot be processed due to client error
     *
     * @param ex The BadRequestException that was thrown
     * @return ResponseEntity with ErrorResponse body and HTTP 400 status
     *
     * Example scenarios:
     * ------------------
     * 1. Password validation:
     *    Service: throw new BadRequestException("Current password is incorrect");
     *    Response: HTTP 400 with message
     *
     * 2. Ownership validation:
     *    Service: throw new BadRequestException("Address does not belong to user");
     *    Response: HTTP 400 with message
     *
     * 3. Business rule:
     *    Service: throw new BadRequestException("New password must be different");
     *    Response: HTTP 400 with message
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException ex) {

        // Create error response with HTTP 400 status
        // The message comes from the exception and explains what was wrong
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),  // 400
                ex.getMessage(),                  // Business logic error message
                LocalDateTime.now()
        );

        // Return ResponseEntity with HTTP 400 Bad Request status
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle MethodArgumentNotValidException
     *
     * Catches: Validation errors from @Valid annotation in controllers
     * Returns: HTTP 400 Bad Request with list of validation errors
     *
     * When does this occur?
     * ---------------------
     * When @Valid fails in controller:
     *
     * @PostMapping
     * public ResponseEntity<?> createProduct(
     *     @Valid @RequestBody ProductRequest request  ← @Valid triggers validation
     * ) { ... }
     *
     * If validation fails:
     * --------------------
     * ProductRequest has:
     * - name: "" ← Fails @NotBlank
     * - price: -10 ← Fails @DecimalMin("0.01")
     * - sku: "" ← Fails @NotBlank
     *
     * Spring throws MethodArgumentNotValidException with all validation errors
     *
     * @param ex The MethodArgumentNotValidException with validation errors
     * @return ResponseEntity with ErrorResponse containing all validation errors
     *
     * Example response:
     * -----------------
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
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {

        // List to collect all validation error messages
        List<String> errors = new ArrayList<>();

        // ex.getBindingResult() contains all validation errors
        // getFieldErrors() returns list of FieldError objects
        // Each FieldError represents one validation failure
        //
        // Example FieldError:
        // - field: "name"
        // - defaultMessage: "Product name is required"
        // - rejectedValue: ""
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            // Build error message: "fieldName: error message"
            // Example: "name: Product name is required"
            String errorMessage = error.getField() + ": " + error.getDefaultMessage();

            // Add to errors list
            errors.add(errorMessage);
        });

        // Alternative: Just use default message without field name
        // ex.getBindingResult().getFieldErrors().forEach(error ->
        //     errors.add(error.getDefaultMessage())
        // );
        // Would produce: ["Product name is required", "Price must be at least 0.01", ...]

        // Create error response with:
        // - Status: 400 Bad Request
        // - Message: Generic "Validation failed"
        // - Errors: List of specific validation messages
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),  // 400
                "Validation failed",              // Generic message
                LocalDateTime.now(),
                errors                            // List of field-specific errors
        );

        // Return ResponseEntity with HTTP 400 Bad Request status
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle all other unhandled exceptions
     *
     * Catches: Any exception not handled by specific handlers above
     * Returns: HTTP 500 Internal Server Error
     *
     * This is a catch-all handler for unexpected errors:
     * - NullPointerException
     * - Database connection errors
     * - Unhandled business logic errors
     * - Any RuntimeException not specifically caught
     *
     * Why have a generic handler?
     * ---------------------------
     * 1. SAFETY NET: Ensures all exceptions return proper HTTP response
     * 2. NO STACK TRACES TO CLIENT: Hides internal errors from users
     * 3. CONSISTENT FORMAT: Even unexpected errors follow ErrorResponse format
     * 4. SECURITY: Don't expose internal system details
     *
     * @param ex The unexpected exception
     * @return ResponseEntity with generic error message and HTTP 500 status
     *
     * Important: Log the actual exception for debugging
     * --------------------------------------------------
     * In production, you want to:
     * 1. Log the full exception with stack trace (for debugging)
     * 2. Return generic message to client (for security)
     *
     * logger.error("Unexpected error occurred", ex);
     *
     * Example response:
     * -----------------
     * HTTP/1.1 500 Internal Server Error
     * {
     *   "status": 500,
     *   "message": "An unexpected error occurred. Please try again later.",
     *   "timestamp": "2024-01-15T10:30:00"
     * }
     *
     * Note: Message is intentionally generic
     * - Don't expose: "NullPointerException at ProductService.java:123"
     * - Security risk: Reveals internal structure
     * - Unprofessional: Confuses users
     * - Just say: "An unexpected error occurred"
     * - Log the details server-side for debugging
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {

        // TODO: Add logging in production
        // logger.error("Unexpected error occurred", ex);
        // This logs the full exception with stack trace to server logs

        // Create error response with generic message
        // Don't expose internal error details to client
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),  // 500
                "An unexpected error occurred. Please try again later.",  // Generic message
                LocalDateTime.now()
        );

        // Return ResponseEntity with HTTP 500 Internal Server Error status
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // =========================================================================
    // How Exception Handlers are Matched
    // =========================================================================
    //
    // Spring uses specificity to determine which handler to use:
    //
    // 1. Most specific handler first
    //    - ResourceNotFoundException → handleResourceNotFoundException
    //    - DuplicateResourceException → handleDuplicateResourceException
    //    - MethodArgumentNotValidException → handleValidationException
    //
    // 2. If no specific handler, use generic handler
    //    - NullPointerException → handleGlobalException (Exception)
    //    - Any RuntimeException → handleGlobalException (Exception)
    //
    // Order doesn't matter in the code - Spring sorts by specificity automatically
    //
    // =========================================================================
    // Additional Exception Handlers (Examples)
    // =========================================================================
    //
    // You can add more specific handlers as needed:
    //
    // @ExceptionHandler(IllegalArgumentException.class)
    // public ResponseEntity<ErrorResponse> handleIllegalArgument(
    //     IllegalArgumentException ex) {
    //     ErrorResponse error = new ErrorResponse(
    //         HttpStatus.BAD_REQUEST.value(),
    //         ex.getMessage(),
    //         LocalDateTime.now()
    //     );
    //     return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    // }
    //
    // @ExceptionHandler(DataIntegrityViolationException.class)
    // public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
    //     DataIntegrityViolationException ex) {
    //     ErrorResponse error = new ErrorResponse(
    //         HttpStatus.CONFLICT.value(),
    //         "Database constraint violation",
    //         LocalDateTime.now()
    //     );
    //     return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    // }
    //
    // @ExceptionHandler(AccessDeniedException.class)
    // public ResponseEntity<ErrorResponse> handleAccessDenied(
    //     AccessDeniedException ex) {
    //     ErrorResponse error = new ErrorResponse(
    //         HttpStatus.FORBIDDEN.value(),
    //         "You don't have permission to access this resource",
    //         LocalDateTime.now()
    //     );
    //     return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    // }
    //
    // =========================================================================
    // Testing Exception Handlers
    // =========================================================================
    //
    // You can test exception handlers using MockMvc:
    //
    // @Test
    // public void testResourceNotFoundHandler() throws Exception {
    //     when(productService.getById(999L))
    //         .thenThrow(new ResourceNotFoundException("Product not found"));
    //
    //     mockMvc.perform(get("/api/products/999"))
    //         .andExpect(status().isNotFound())
    //         .andExpect(jsonPath("$.status").value(404))
    //         .andExpect(jsonPath("$.message").value("Product not found"));
    // }
}
