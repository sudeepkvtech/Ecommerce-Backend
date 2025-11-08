// Package declaration - this class belongs to the exception package
package com.ecommerce.orderservice.exception;

/**
 * DuplicateResourceException
 *
 * Custom exception thrown when attempting to create a resource that already exists
 *
 * What is a duplicate resource?
 * -----------------------------
 * A duplicate occurs when trying to create a resource with a unique field
 * that already exists in the database.
 *
 * Examples:
 * ---------
 * 1. Creating a category with name "Electronics" when it already exists
 * 2. Creating a product with SKU "LAPTOP-001" when another product has this SKU
 * 3. Creating a user with email "john@example.com" when it's already registered
 *
 * Why is this a separate exception from ResourceNotFoundException?
 * -----------------------------------------------------------------
 * Different error scenarios require different HTTP status codes:
 * - ResourceNotFoundException → HTTP 404 Not Found
 * - DuplicateResourceException → HTTP 409 Conflict
 *
 * HTTP 409 Conflict indicates:
 * - The request conflicts with the current state of the server
 * - The resource already exists
 * - Client needs to modify their request
 *
 * When is this exception thrown?
 * -------------------------------
 * POST /api/categories
 * {
 *   "name": "Electronics"  ← This name already exists
 * }
 * → Throws DuplicateResourceException
 * → Returns HTTP 409 Conflict
 *
 * POST /api/products
 * {
 *   "sku": "LAPTOP-001"  ← This SKU already exists
 * }
 * → Throws DuplicateResourceException
 * → Returns HTTP 409 Conflict
 *
 * Example usage in Service layer:
 * --------------------------------
 * public CategoryResponse createCategory(CategoryRequest request) {
 *     // Check if category with this name already exists
 *     if (categoryRepository.existsByName(request.getName())) {
 *         throw new DuplicateResourceException(
 *             "Category already exists with name: " + request.getName()
 *         );
 *     }
 *
 *     // Proceed with creation if no duplicate
 *     Category category = new Category();
 *     category.setName(request.getName());
 *     categoryRepository.save(category);
 *     return toResponse(category);
 * }
 *
 * Another example for Product SKU:
 * ---------------------------------
 * public ProductResponse createProduct(ProductRequest request) {
 *     // Check if product with this SKU already exists
 *     if (productRepository.existsBySku(request.getSku())) {
 *         throw new DuplicateResourceException(
 *             "Product already exists with SKU: " + request.getSku()
 *         );
 *     }
 *
 *     // Create product if SKU is unique
 *     Product product = new Product();
 *     product.setSku(request.getSku());
 *     // ... set other fields
 *     productRepository.save(product);
 *     return toResponse(product);
 * }
 *
 * Alternative: Let database handle duplicates
 * --------------------------------------------
 * We could rely on database UNIQUE constraints:
 * @Column(unique = true)
 * private String sku;
 *
 * If duplicate inserted, database throws:
 * SQLException: Duplicate entry 'LAPTOP-001' for key 'sku'
 *
 * But checking beforehand is better because:
 * -------------------------------------------
 * 1. CLEANER ERROR MESSAGES: Custom message vs cryptic SQL error
 * 2. EARLY DETECTION: Fail before complex processing
 * 3. CONSISTENT RESPONSES: Same error format for all validation
 * 4. BUSINESS LOGIC: Can add complex duplicate checks beyond DB constraints
 *
 * However, there's a race condition:
 * ----------------------------------
 * Thread 1: Check SKU → not exists → prepare to insert
 * Thread 2: Check SKU → not exists → prepare to insert
 * Thread 1: Insert → Success
 * Thread 2: Insert → Database error (duplicate)
 *
 * Solution:
 * ---------
 * 1. Use database UNIQUE constraint as safety net
 * 2. Catch DataIntegrityViolationException and convert to DuplicateResourceException
 * 3. Or use pessimistic locking / transactions
 */

// Extends RuntimeException to make this an unchecked exception
// Same reasoning as ResourceNotFoundException:
// - No need for "throws" declarations
// - Spring handles automatically
// - Cleaner code
public class DuplicateResourceException extends RuntimeException {

    /**
     * Constructor with error message only
     *
     * @param message Descriptive error message explaining what duplicate was detected
     *
     * Example messages:
     * -----------------
     * - "Category already exists with name: Electronics"
     * - "Product already exists with SKU: LAPTOP-001"
     * - "User already registered with email: john@example.com"
     *
     * The message should:
     * -------------------
     * 1. Clearly state what is duplicate
     * 2. Include the duplicate value
     * 3. Help user understand how to fix it
     *
     * Good message: "Category already exists with name: Electronics"
     * → Clear: What's duplicate (category)
     * → Specific: Which value (Electronics)
     * → Actionable: User knows to choose different name
     *
     * Bad message: "Duplicate"
     * → Unclear: What's duplicate?
     * → Not specific: Which field?
     * → Not actionable: How to fix?
     *
     * super(message) calls RuntimeException constructor with the message
     * This stores the message for later retrieval with getMessage()
     */
    public DuplicateResourceException(String message) {
        // Call parent class (RuntimeException) constructor
        // Stores the error message in the exception object
        super(message);
    }

    /**
     * Constructor with message and cause
     *
     * @param message Descriptive error message
     * @param cause The underlying exception that caused this (e.g., SQLException)
     *
     * Use case: Database constraint violation
     * ----------------------------------------
     * try {
     *     productRepository.save(product);
     * } catch (DataIntegrityViolationException e) {
     *     // Database threw error due to UNIQUE constraint
     *     throw new DuplicateResourceException(
     *         "Product already exists with SKU: " + product.getSku(),
     *         e  // Original database exception
     *     );
     * }
     *
     * Why chain exceptions?
     * ---------------------
     * 1. Preserve original database error for debugging
     * 2. Show full stack trace in logs
     * 3. Help diagnose if it's a code bug vs expected duplicate
     * 4. Allow advanced error handling based on root cause
     *
     * Example stack trace:
     * --------------------
     * DuplicateResourceException: Product already exists with SKU: LAPTOP-001
     *     at ProductService.createProduct(ProductService.java:50)
     * Caused by: org.springframework.dao.DataIntegrityViolationException: ...
     * Caused by: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry
     *     at com.mysql.cj.jdbc.PreparedStatement.executeUpdate(...)
     *
     * This shows the full chain:
     * 1. Our custom exception (application level)
     * 2. Spring's data exception (framework level)
     * 3. MySQL's SQL exception (database level)
     *
     * super(message, cause) stores both message and original exception
     */
    public DuplicateResourceException(String message, Throwable cause) {
        // Call parent class constructor with message and cause
        // Chains the exceptions together for complete error context
        super(message, cause);
    }

    // =========================================================================
    // How This Exception Flows Through the Application
    // =========================================================================
    //
    // Scenario: Creating duplicate category
    // --------------------------------------
    //
    // 1. Client sends POST request
    //    -----------------------------------------
    //    POST /api/categories
    //    {
    //      "name": "Electronics"
    //    }
    //
    // 2. Controller receives and validates request
    //    -----------------------------------------
    //    @PostMapping
    //    public ResponseEntity<CategoryResponse> createCategory(
    //        @Valid @RequestBody CategoryRequest request) {
    //        CategoryResponse response = categoryService.createCategory(request);
    //        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    //    }
    //
    // 3. Service checks for duplicate
    //    -----------------------------------------
    //    public CategoryResponse createCategory(CategoryRequest request) {
    //        if (categoryRepository.existsByName(request.getName())) {
    //            throw new DuplicateResourceException(
    //                "Category already exists with name: " + request.getName()
    //            );
    //        }
    //        // ... create category
    //    }
    //
    // 4. Exception thrown and caught by GlobalExceptionHandler
    //    -----------------------------------------
    //    @ExceptionHandler(DuplicateResourceException.class)
    //    public ResponseEntity<ErrorResponse> handleDuplicate(
    //        DuplicateResourceException ex) {
    //        ErrorResponse error = new ErrorResponse(
    //            HttpStatus.CONFLICT.value(),
    //            ex.getMessage(),
    //            LocalDateTime.now()
    //        );
    //        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    //    }
    //
    // 5. Client receives HTTP 409 Conflict
    //    -----------------------------------------
    //    HTTP/1.1 409 Conflict
    //    Content-Type: application/json
    //
    //    {
    //      "status": 409,
    //      "message": "Category already exists with name: Electronics",
    //      "timestamp": "2024-01-15T10:30:00"
    //    }
    //
    // 6. Frontend handles the error
    //    -----------------------------------------
    //    try {
    //      const response = await fetch('/api/categories', {
    //        method: 'POST',
    //        body: JSON.stringify({ name: 'Electronics' })
    //      });
    //
    //      if (response.status === 409) {
    //        // Show error to user
    //        showError('Category name already exists. Please choose a different name.');
    //      }
    //    } catch (error) {
    //      console.error(error);
    //    }
    //
    // =========================================================================
    // Best Practices for Duplicate Detection
    // =========================================================================
    //
    // 1. CHECK BEFORE INSERT (Optimistic approach)
    //    -----------------------------------------
    //    if (repository.existsByUniqueField(value)) {
    //        throw new DuplicateResourceException("...");
    //    }
    //    repository.save(entity);
    //
    //    Pros: Clear error message, early detection
    //    Cons: Race condition possible (two requests at same time)
    //
    // 2. CATCH DATABASE EXCEPTION (Pessimistic approach)
    //    -----------------------------------------
    //    try {
    //        repository.save(entity);
    //    } catch (DataIntegrityViolationException e) {
    //        throw new DuplicateResourceException("...", e);
    //    }
    //
    //    Pros: Race condition safe (database handles it)
    //    Cons: Less clear error messages, harder to debug
    //
    // 3. COMBINED APPROACH (Recommended)
    //    -----------------------------------------
    //    // Check first for clear error message
    //    if (repository.existsByUniqueField(value)) {
    //        throw new DuplicateResourceException("...");
    //    }
    //
    //    try {
    //        // Database constraint as safety net
    //        repository.save(entity);
    //    } catch (DataIntegrityViolationException e) {
    //        // Handle race condition edge case
    //        throw new DuplicateResourceException("...", e);
    //    }
    //
    //    Pros: Clear errors + race condition safe
    //    Cons: Slightly more code
    //
    // =========================================================================
    // HTTP Status Codes for Resource Errors
    // =========================================================================
    //
    // 404 Not Found (ResourceNotFoundException)
    //    - Resource doesn't exist
    //    - GET, PUT, DELETE requests
    //
    // 409 Conflict (DuplicateResourceException)
    //    - Resource already exists
    //    - POST requests creating resources
    //
    // 400 Bad Request (Validation errors)
    //    - Invalid input data
    //    - Missing required fields
    //    - Invalid format
    //
    // 422 Unprocessable Entity (Business rule violation)
    //    - Valid format but business rules fail
    //    - Example: Insufficient stock for order
}
