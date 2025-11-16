// Package declaration - this class belongs to the dto package
package com.ecommerce.productservice.dto;

// Import validation annotations from Jakarta Bean Validation API
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
// Import Lombok annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Category Request DTO (Data Transfer Object)
 *
 * What is a DTO?
 * --------------
 * A DTO is a simple object used to transfer data between different layers of the application.
 * It's like a "carrier" that moves data from the client to the server or between services.
 *
 * Why use DTOs instead of Entity classes directly?
 * -------------------------------------------------
 * 1. SECURITY: Hide internal database structure from clients
 *    - Entity might have sensitive fields (timestamps, internal IDs)
 *    - DTO only exposes what the client needs to know
 *
 * 2. VALIDATION: Validate input data before it reaches the business logic
 *    - Annotations like @NotBlank ensure data integrity
 *    - Prevents invalid data from entering the system
 *
 * 3. FLEXIBILITY: API structure can differ from database structure
 *    - Database might change, but API stays the same
 *    - Reduces coupling between layers
 *
 * 4. DOCUMENTATION: Clear contract of what data is required
 *    - Clients know exactly what fields to send
 *    - Generates clear API documentation (Swagger)
 *
 * Request vs Response DTOs:
 * -------------------------
 * - Request DTO: Data coming FROM client (POST, PUT requests)
 * - Response DTO: Data going TO client (GET responses)
 * - They often have different fields (e.g., Response includes ID, timestamps)
 *
 * This CategoryRequest DTO is used when:
 * - Creating a new category (POST /api/categories)
 * - Updating an existing category (PUT /api/categories/{id})
 */

// @Data is a Lombok annotation that generates:
// - Getters for all fields
// - Setters for all fields
// - toString() method
// - equals() and hashCode() methods
// - A required args constructor
@Data

// @NoArgsConstructor generates a no-argument constructor
// Required for:
// 1. Jackson (JSON library) to deserialize JSON into this object
// 2. Spring to create instances when processing HTTP requests
@NoArgsConstructor

// @AllArgsConstructor generates a constructor with all fields
// Useful for:
// 1. Creating test objects
// 2. Initializing objects in one line
@AllArgsConstructor
public class CategoryRequest {

    /**
     * Category Name
     *
     * Validation Annotations:
     * -----------------------
     *
     * @NotBlank ensures:
     * - Field is not null
     * - Field is not empty string ("")
     * - Field is not just whitespace ("   ")
     *
     * Why @NotBlank instead of @NotNull?
     * - @NotNull: Only checks null, allows empty string ""
     * - @NotEmpty: Checks null and empty, allows whitespace "   "
     * - @NotBlank: Checks null, empty, AND whitespace (most strict for strings)
     *
     * The message parameter customizes the error message shown to the client
     * when validation fails.
     *
     * Validation Example:
     * -------------------
     * Valid: "Electronics", "Clothing"
     * Invalid: null, "", "   "
     *
     * Error Response (if invalid):
     * {
     *   "field": "name",
     *   "message": "Category name is required",
     *   "rejectedValue": null
     * }
     */
    @NotBlank(message = "Category name is required")

    /**
     * @Size restricts the length of the string
     * - min = 2: Name must be at least 2 characters
     * - max = 100: Name cannot exceed 100 characters
     *
     * This matches the database column constraint (@Column(length = 100) in entity)
     *
     * Validation Examples:
     * --------------------
     * Valid: "Electronics" (11 chars), "Books" (5 chars)
     * Invalid: "A" (too short), "A very long category name..." (>100 chars)
     *
     * Why enforce length limits?
     * 1. Database constraint: Prevents database errors
     * 2. UX: Ensures reasonable category names
     * 3. Security: Prevents DOS attacks with huge strings
     */
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    private String name;

    /**
     * Category Description (Optional)
     *
     * Validation Annotations:
     * -----------------------
     *
     * @Size with max only:
     * - min is not specified: Field is optional (can be null or empty)
     * - max = 500: If provided, cannot exceed 500 characters
     *
     * This allows clients to:
     * 1. Omit the field entirely (description: null)
     * 2. Send empty string (description: "")
     * 3. Send a valid description (description: "Category for electronic items")
     *
     * But if they do provide a description, it must be <= 500 characters
     *
     * Validation Examples:
     * --------------------
     * Valid: null, "", "Electronic devices and accessories"
     * Invalid: "A very long description..." (>500 chars)
     */
    @Size(max = 500, message = "Category description cannot exceed 500 characters")
    private String description;

    /**
     * Category Image URL (Optional)
     *
     * Stores URL or path to category image
     *
     * Validation:
     * -----------
     * @Size with max = 255: If provided, must not exceed 255 characters
     *
     * Examples:
     * ---------
     * Valid:
     * - null (no image)
     * - "https://cdn.example.com/categories/electronics.jpg"
     * - "/images/categories/electronics.jpg"
     * - ""
     *
     * Invalid:
     * - "https://very-long-url..." (>255 chars)
     *
     * In a real application, you might want additional validation:
     * - @URL: Ensures it's a valid URL format
     * - @Pattern: Ensures it matches expected pattern (http/https)
     *
     * Example with URL validation (commented):
     * @URL(message = "Image URL must be a valid URL")
     * @Pattern(regexp = "^https?://.*", message = "URL must start with http or https")
     */
    @Size(max = 255, message = "Image URL cannot exceed 255 characters")
    private String imageUrl;

    // =========================================================================
    // How Validation Works
    // =========================================================================
    //
    // 1. Client sends JSON:
    //    POST /api/categories
    //    {
    //      "name": "E",
    //      "description": "Electronics"
    //    }
    //
    // 2. Spring converts JSON to CategoryRequest object (using Jackson)
    //
    // 3. Spring validates the object using @Valid annotation in Controller:
    //    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request)
    //
    // 4. If validation fails:
    //    - Spring throws MethodArgumentNotValidException
    //    - Returns HTTP 400 Bad Request with error details
    //    - Example response:
    //      {
    //        "timestamp": "2024-01-15T10:30:00",
    //        "status": 400,
    //        "errors": [
    //          {
    //            "field": "name",
    //            "message": "Category name must be between 2 and 100 characters",
    //            "rejectedValue": "E"
    //          }
    //        ]
    //      }
    //
    // 5. If validation succeeds:
    //    - Request proceeds to Controller method
    //    - Controller calls Service layer
    //    - Service converts DTO to Entity and saves to database
    //
    // =========================================================================
    // Benefits of This Approach
    // =========================================================================
    //
    // 1. FAIL FAST: Invalid data is rejected immediately at the API boundary
    //    - Don't waste time processing invalid requests
    //    - Don't let bad data reach the database
    //
    // 2. CLEAR FEEDBACK: Client gets specific error messages
    //    - Knows exactly what went wrong
    //    - Can fix and retry the request
    //
    // 3. CONSISTENT VALIDATION: Same rules apply everywhere
    //    - No need to repeat validation logic in multiple places
    //    - Single source of truth for validation rules
    //
    // 4. AUTOMATIC DOCUMENTATION: Swagger/OpenAPI picks up these constraints
    //    - API docs show which fields are required
    //    - Shows min/max length restrictions
    //    - Clients know the contract before making requests
}
