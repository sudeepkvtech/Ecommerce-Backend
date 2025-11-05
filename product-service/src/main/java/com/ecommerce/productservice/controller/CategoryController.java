// Package declaration - this class belongs to the controller package
package com.ecommerce.productservice.controller;

// Import Spring Web annotations
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Import Jakarta validation
import jakarta.validation.Valid;

// Import Lombok annotation
import lombok.RequiredArgsConstructor;

// Import DTOs and Service
import com.ecommerce.productservice.dto.CategoryRequest;
import com.ecommerce.productservice.dto.CategoryResponse;
import com.ecommerce.productservice.service.CategoryService;

// Import Java collections
import java.util.List;

/**
 * Category Controller
 *
 * REST API endpoints for Category management
 *
 * What is a REST Controller?
 * --------------------------
 * A controller handles HTTP requests and returns HTTP responses
 * It's the entry point for clients to interact with the application
 *
 * REST (Representational State Transfer) principles:
 * - Use HTTP methods for CRUD operations:
 *   * GET: Retrieve resources
 *   * POST: Create new resources
 *   * PUT: Update existing resources
 *   * DELETE: Remove resources
 * - Use meaningful URLs (e.g., /api/categories, /api/categories/{id})
 * - Return appropriate HTTP status codes (200, 201, 404, etc.)
 * - Stateless (each request contains all needed information)
 *
 * Request Flow:
 * Client → Controller → Service → Repository → Database
 * Database → Repository → Service → Controller → Client
 */

// @RestController combines @Controller and @ResponseBody
// - @Controller: Marks this as a Spring MVC controller
// - @ResponseBody: Automatically serializes return values to JSON
@RestController

// @RequestMapping defines the base URL path for all endpoints in this controller
// All URLs will start with /api/categories
// Example: http://localhost:8081/product-service/api/categories
@RequestMapping("/api/categories")

// @RequiredArgsConstructor generates constructor for dependency injection
@RequiredArgsConstructor
public class CategoryController {

    // Inject CategoryService to handle business logic
    private final CategoryService categoryService;

    /**
     * Create a new category
     *
     * Endpoint: POST /api/categories
     * Request Body: CategoryRequest JSON
     * Response: CategoryResponse with HTTP 201 Created
     *
     * @PostMapping maps HTTP POST requests to this method
     * POST is used for creating new resources
     *
     * @Valid triggers validation on CategoryRequest
     * - Checks @NotBlank, @Size annotations
     * - If validation fails: returns HTTP 400 with error details
     * - If validation passes: proceeds with method execution
     *
     * @RequestBody tells Spring to:
     * - Read HTTP request body
     * - Deserialize JSON to CategoryRequest object
     * - Example JSON:
     *   {
     *     "name": "Electronics",
     *     "description": "Electronic devices",
     *     "imageUrl": "https://example.com/electronics.jpg"
     *   }
     *
     * ResponseEntity<CategoryResponse>:
     * - Allows setting HTTP status code and headers
     * - Body contains CategoryResponse (automatically converted to JSON)
     *
     * HTTP Status 201 Created:
     * - Indicates new resource was successfully created
     * - RESTful best practice for POST operations
     *
     * @param request CategoryRequest with name, description, imageUrl
     * @return ResponseEntity with created category and HTTP 201
     */
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request) {

        // Call service to create category
        // Service handles business logic and database operations
        CategoryResponse response = categoryService.createCategory(request);

        // Return response with HTTP 201 Created status
        // ResponseEntity.status(HttpStatus.CREATED) sets status to 201
        // .body(response) sets the response body (converted to JSON)
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get category by ID
     *
     * Endpoint: GET /api/categories/{id}
     * Example: GET /api/categories/1
     * Response: CategoryResponse with HTTP 200 OK
     *
     * @GetMapping("{id}") maps GET requests with path variable
     * - {id} is a placeholder for the category ID
     * - Example: /api/categories/1 → id = 1
     *
     * @PathVariable("id") extracts the ID from the URL
     * - Binds URL parameter to method parameter
     * - Type conversion happens automatically (String → Long)
     *
     * If category not found:
     * - Service throws ResourceNotFoundException
     * - GlobalExceptionHandler catches it
     * - Returns HTTP 404 Not Found
     *
     * @param id Category ID from URL path
     * @return ResponseEntity with category and HTTP 200
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable("id") Long id) {

        // Call service to retrieve category
        CategoryResponse response = categoryService.getCategoryById(id);

        // Return response with HTTP 200 OK (default status)
        // ResponseEntity.ok() is shorthand for status(HttpStatus.OK)
        return ResponseEntity.ok(response);
    }

    /**
     * Get all categories
     *
     * Endpoint: GET /api/categories
     * Response: List of CategoryResponse with HTTP 200
     *
     * @GetMapping (no path) maps to base URL: /api/categories
     *
     * Returns List<CategoryResponse>:
     * - Automatically serialized to JSON array
     * - Example response:
     *   [
     *     { "id": 1, "name": "Electronics", ... },
     *     { "id": 2, "name": "Clothing", ... }
     *   ]
     *
     * @return ResponseEntity with list of categories
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {

        // Call service to retrieve all categories
        List<CategoryResponse> responses = categoryService.getAllCategories();

        // Return list with HTTP 200 OK
        return ResponseEntity.ok(responses);
    }

    /**
     * Get category by name
     *
     * Endpoint: GET /api/categories/name/{name}
     * Example: GET /api/categories/name/Electronics
     * Response: CategoryResponse with HTTP 200
     *
     * Why separate endpoint from GET /{id}?
     * - Avoids ambiguity between ID and name
     * - Clear intent: searching by name
     * - ID is numeric, name is string
     *
     * @param name Category name from URL path
     * @return ResponseEntity with category
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<CategoryResponse> getCategoryByName(@PathVariable("name") String name) {

        // Call service to find category by name
        CategoryResponse response = categoryService.getCategoryByName(name);

        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing category
     *
     * Endpoint: PUT /api/categories/{id}
     * Example: PUT /api/categories/1
     * Request Body: CategoryRequest JSON
     * Response: CategoryResponse with HTTP 200
     *
     * @PutMapping maps HTTP PUT requests
     * PUT is used for updating existing resources
     *
     * Requires both:
     * - @PathVariable: Which category to update (ID from URL)
     * - @RequestBody: New data for the category
     *
     * Example request:
     * PUT /api/categories/1
     * {
     *   "name": "Consumer Electronics",
     *   "description": "Updated description",
     *   "imageUrl": "new-image.jpg"
     * }
     *
     * If category not found:
     * - Service throws ResourceNotFoundException
     * - Returns HTTP 404
     *
     * If name already exists:
     * - Service throws DuplicateResourceException
     * - Returns HTTP 409 Conflict
     *
     * @param id Category ID to update
     * @param request CategoryRequest with updated data
     * @return ResponseEntity with updated category
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable("id") Long id,
            @Valid @RequestBody CategoryRequest request) {

        // Call service to update category
        CategoryResponse response = categoryService.updateCategory(id, request);

        // Return updated category with HTTP 200 OK
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a category
     *
     * Endpoint: DELETE /api/categories/{id}
     * Example: DELETE /api/categories/1
     * Response: HTTP 204 No Content
     *
     * @DeleteMapping maps HTTP DELETE requests
     * DELETE is used for removing resources
     *
     * HTTP 204 No Content:
     * - Indicates successful deletion
     * - No response body needed (resource no longer exists)
     * - RESTful best practice for DELETE operations
     *
     * ResponseEntity<Void>:
     * - Void means no response body
     * - Only HTTP status is returned
     *
     * Warning: This will also delete all products in the category!
     * - Due to CascadeType.ALL in Category entity
     * - In production, consider:
     *   * Preventing deletion if products exist
     *   * Moving products to another category
     *   * Using soft delete instead
     *
     * @param id Category ID to delete
     * @return ResponseEntity with no body and HTTP 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable("id") Long id) {

        // Call service to delete category
        // No return value (void method)
        categoryService.deleteCategory(id);

        // Return HTTP 204 No Content
        // noContent() is shorthand for status(HttpStatus.NO_CONTENT)
        // build() creates ResponseEntity with no body
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Example HTTP Requests and Responses
    // =========================================================================
    //
    // 1. CREATE CATEGORY
    // -------------------
    // Request:
    // POST http://localhost:8081/product-service/api/categories
    // Content-Type: application/json
    //
    // {
    //   "name": "Electronics",
    //   "description": "Electronic devices and accessories",
    //   "imageUrl": "https://example.com/electronics.jpg"
    // }
    //
    // Response:
    // HTTP/1.1 201 Created
    // Content-Type: application/json
    //
    // {
    //   "id": 1,
    //   "name": "Electronics",
    //   "description": "Electronic devices and accessories",
    //   "imageUrl": "https://example.com/electronics.jpg",
    //   "productCount": 0,
    //   "createdAt": "2024-01-15T10:30:00",
    //   "updatedAt": "2024-01-15T10:30:00"
    // }
    //
    // 2. GET CATEGORY BY ID
    // ---------------------
    // Request:
    // GET http://localhost:8081/product-service/api/categories/1
    //
    // Response:
    // HTTP/1.1 200 OK
    // Content-Type: application/json
    //
    // {
    //   "id": 1,
    //   "name": "Electronics",
    //   "description": "Electronic devices and accessories",
    //   "imageUrl": "https://example.com/electronics.jpg",
    //   "productCount": 15,
    //   "createdAt": "2024-01-15T10:30:00",
    //   "updatedAt": "2024-01-15T10:30:00"
    // }
    //
    // 3. GET ALL CATEGORIES
    // ----------------------
    // Request:
    // GET http://localhost:8081/product-service/api/categories
    //
    // Response:
    // HTTP/1.1 200 OK
    // Content-Type: application/json
    //
    // [
    //   {
    //     "id": 1,
    //     "name": "Electronics",
    //     "productCount": 15,
    //     ...
    //   },
    //   {
    //     "id": 2,
    //     "name": "Clothing",
    //     "productCount": 23,
    //     ...
    //   }
    // ]
    //
    // 4. UPDATE CATEGORY
    // ------------------
    // Request:
    // PUT http://localhost:8081/product-service/api/categories/1
    // Content-Type: application/json
    //
    // {
    //   "name": "Consumer Electronics",
    //   "description": "Updated description",
    //   "imageUrl": "new-image.jpg"
    // }
    //
    // Response:
    // HTTP/1.1 200 OK
    // {
    //   "id": 1,
    //   "name": "Consumer Electronics",
    //   "description": "Updated description",
    //   ...
    //   "updatedAt": "2024-01-16T14:25:00"
    // }
    //
    // 5. DELETE CATEGORY
    // ------------------
    // Request:
    // DELETE http://localhost:8081/product-service/api/categories/1
    //
    // Response:
    // HTTP/1.1 204 No Content
    //
    // 6. ERROR RESPONSE (Category Not Found)
    // ---------------------------------------
    // Request:
    // GET http://localhost:8081/product-service/api/categories/999
    //
    // Response:
    // HTTP/1.1 404 Not Found
    // Content-Type: application/json
    //
    // {
    //   "status": 404,
    //   "message": "Category not found with id: 999",
    //   "timestamp": "2024-01-15T10:30:00"
    // }
    //
    // 7. ERROR RESPONSE (Duplicate Category)
    // ---------------------------------------
    // Request:
    // POST http://localhost:8081/product-service/api/categories
    // {
    //   "name": "Electronics"  ← Already exists
    // }
    //
    // Response:
    // HTTP/1.1 409 Conflict
    // {
    //   "status": 409,
    //   "message": "Category already exists with name: Electronics",
    //   "timestamp": "2024-01-15T10:30:00"
    // }
}
