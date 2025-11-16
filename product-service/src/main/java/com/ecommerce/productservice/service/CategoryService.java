// Package declaration - this class belongs to the service package
package com.ecommerce.productservice.service;

// Import required Spring annotations
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Import Lombok annotation for dependency injection
import lombok.RequiredArgsConstructor;

// Import entities, DTOs, repositories, and exceptions
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.dto.CategoryRequest;
import com.ecommerce.productservice.dto.CategoryResponse;
import com.ecommerce.productservice.repository.CategoryRepository;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import com.ecommerce.productservice.exception.DuplicateResourceException;

// Import Java collections
import java.util.List;
import java.util.stream.Collectors;

/**
 * Category Service
 *
 * Business logic layer for Category operations
 *
 * What is a Service Layer?
 * ------------------------
 * The Service layer sits between the Controller (API) and Repository (Database):
 * Controller → Service → Repository → Database
 *
 * Responsibilities of Service Layer:
 * ----------------------------------
 * 1. BUSINESS LOGIC: Implements business rules and workflows
 *    - Validate data beyond simple field validation
 *    - Check for duplicates before creating resources
 *    - Enforce business constraints
 *
 * 2. TRANSACTION MANAGEMENT: Handles database transactions
 *    - Ensure multiple database operations succeed or fail together
 *    - Rollback on errors
 *
 * 3. DTO ↔ ENTITY CONVERSION: Translate between layers
 *    - Convert DTOs (from Controller) to Entities (for Repository)
 *    - Convert Entities (from Repository) to DTOs (for Controller)
 *
 * 4. EXCEPTION HANDLING: Throw meaningful exceptions
 *    - ResourceNotFoundException when resource not found
 *    - DuplicateResourceException when duplicate detected
 *
 * 5. ORCHESTRATION: Coordinate multiple operations
 *    - Call multiple repositories if needed
 *    - Perform complex workflows
 *
 * Why separate Service from Controller?
 * --------------------------------------
 * - Controller handles HTTP concerns (requests, responses, status codes)
 * - Service handles business logic (what to do with the data)
 * - Separation of concerns = cleaner, more maintainable code
 * - Service can be reused by different controllers or scheduled jobs
 * - Easier to test business logic without HTTP layer
 */

// @Service marks this class as a Spring service component
// Spring will:
// - Automatically detect and register this as a bean
// - Make it available for dependency injection
// - Apply transaction management
@Service

// @RequiredArgsConstructor is a Lombok annotation
// Generates a constructor with required fields (final fields)
// This enables constructor-based dependency injection
//
// Without Lombok:
// private final CategoryRepository categoryRepository;
// public CategoryService(CategoryRepository categoryRepository) {
//     this.categoryRepository = categoryRepository;
// }
//
// With Lombok: Just @RequiredArgsConstructor
@RequiredArgsConstructor

// @Transactional at class level makes all public methods transactional
// What is a transaction?
// ----------------------
// A transaction is a unit of work that either:
// - Completes entirely (all operations succeed) → COMMIT
// - Or fails entirely (rollback all operations) → ROLLBACK
//
// Example: Creating a category with products
// - Create category in database
// - Create products in database
// If products fail, category creation is also rolled back
//
// Benefits:
// - Data consistency (no partial updates)
// - Atomicity (all or nothing)
// - Automatic rollback on exceptions
@Transactional
public class CategoryService {

    // Category Repository for database operations
    // final means it must be initialized and cannot be changed
    // Injected by Spring through constructor (thanks to @RequiredArgsConstructor)
    private final CategoryRepository categoryRepository;

    /**
     * Create a new category
     *
     * Steps:
     * 1. Check if category with same name already exists
     * 2. If exists, throw DuplicateResourceException
     * 3. If not, create new category entity
     * 4. Save to database
     * 5. Convert entity to response DTO
     * 6. Return response
     *
     * @param request CategoryRequest with name, description, imageUrl
     * @return CategoryResponse with all category details including generated ID
     * @throws DuplicateResourceException if category name already exists
     */
    public CategoryResponse createCategory(CategoryRequest request) {
        // Check if category with this name already exists
        // existsByName() returns boolean: true if exists, false otherwise
        if (categoryRepository.existsByName(request.getName())) {
            // Category name already exists - throw exception
            // This will be caught by GlobalExceptionHandler
            // Returns HTTP 409 Conflict to client
            throw new DuplicateResourceException(
                    "Category already exists with name: " + request.getName()
            );
        }

        // Create new Category entity from request DTO
        Category category = new Category();
        // Set fields from request
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        // Note: id, createdAt, updatedAt are set automatically
        // - id: by database (auto-increment)
        // - createdAt, updatedAt: by @PrePersist callback in entity

        // Save to database
        // save() returns the saved entity with generated ID
        Category savedCategory = categoryRepository.save(category);

        // Convert entity to response DTO
        // This hides internal entity structure from API clients
        return convertToResponse(savedCategory);
    }

    /**
     * Get category by ID
     *
     * @param id Category ID to retrieve
     * @return CategoryResponse with category details
     * @throws ResourceNotFoundException if category not found
     */
    public CategoryResponse getCategoryById(Long id) {
        // findById returns Optional<Category>
        // Optional is a container that may or may not contain a value
        //
        // orElseThrow():
        // - If category found: returns the category
        // - If not found: throws ResourceNotFoundException
        //
        // This pattern prevents NullPointerException and provides clear error
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + id
                ));

        // Convert entity to response DTO
        return convertToResponse(category);
    }

    /**
     * Get all categories
     *
     * @return List of CategoryResponse containing all categories
     */
    public List<CategoryResponse> getAllCategories() {
        // findAll() retrieves all categories from database
        // Returns List<Category>
        List<Category> categories = categoryRepository.findAll();

        // Convert list of entities to list of DTOs using Stream API
        //
        // Stream API pipeline:
        // 1. categories.stream() - create a stream from the list
        // 2. map(this::convertToResponse) - transform each Category to CategoryResponse
        // 3. collect(Collectors.toList()) - collect results into a List
        //
        // Equivalent to:
        // List<CategoryResponse> responses = new ArrayList<>();
        // for (Category category : categories) {
        //     responses.add(convertToResponse(category));
        // }
        // return responses;
        //
        // Stream version is more concise and functional
        return categories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update an existing category
     *
     * @param id Category ID to update
     * @param request CategoryRequest with updated data
     * @return CategoryResponse with updated category details
     * @throws ResourceNotFoundException if category not found
     * @throws DuplicateResourceException if new name already exists
     */
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        // Find existing category or throw exception
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + id
                ));

        // Check if trying to change name to one that already exists
        // Only check if name is being changed
        if (!category.getName().equals(request.getName())) {
            // Name is being changed - check if new name already exists
            if (categoryRepository.existsByName(request.getName())) {
                // New name already exists - throw exception
                throw new DuplicateResourceException(
                        "Category already exists with name: " + request.getName()
                );
            }
        }

        // Update category fields with new values from request
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        // Note: updatedAt is automatically updated by @PreUpdate callback

        // Save updated category to database
        // save() handles both insert and update
        // Since category has an ID, it performs UPDATE
        Category updatedCategory = categoryRepository.save(category);

        // Convert and return response
        return convertToResponse(updatedCategory);
    }

    /**
     * Delete a category by ID
     *
     * Note: This will also delete all products in this category
     * due to CascadeType.ALL in Category entity
     *
     * In production, you might want to:
     * - Prevent deletion if category has products
     * - Move products to another category
     * - Use soft delete (set active = false)
     *
     * @param id Category ID to delete
     * @throws ResourceNotFoundException if category not found
     */
    public void deleteCategory(Long id) {
        // Check if category exists
        // If not, throw ResourceNotFoundException
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Category not found with id: " + id
            );
        }

        // Delete category from database
        // This will also delete all products in this category (cascade delete)
        categoryRepository.deleteById(id);
    }

    /**
     * Search categories by name (case-insensitive)
     *
     * @param name Category name to search for
     * @return CategoryResponse if found
     * @throws ResourceNotFoundException if not found
     */
    public CategoryResponse getCategoryByName(String name) {
        // findByName returns Optional<Category>
        Category category = categoryRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with name: " + name
                ));

        return convertToResponse(category);
    }

    /**
     * Convert Category entity to CategoryResponse DTO
     *
     * Why this conversion?
     * --------------------
     * - Entity contains JPA annotations, relationships, lifecycle callbacks
     * - DTO is a clean data structure for API responses
     * - Hides internal implementation details
     * - Can add computed fields (like productCount)
     *
     * @param category Category entity from database
     * @return CategoryResponse DTO for API response
     */
    private CategoryResponse convertToResponse(Category category) {
        // Use builder pattern to create response
        // Builder is provided by @Builder annotation on CategoryResponse
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                // Count products in this category
                // category.getProducts() returns List<Product>
                // If products are lazy-loaded, this triggers database query
                .productCount(category.getProducts() != null ? category.getProducts().size() : 0)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    // =========================================================================
    // How Service Layer Fits in the Architecture
    // =========================================================================
    //
    // Request Flow:
    // -------------
    // 1. Client sends HTTP request
    //    POST /api/categories
    //    { "name": "Electronics", "description": "..." }
    //
    // 2. Controller receives request
    //    @PostMapping
    //    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
    //        CategoryResponse response = categoryService.createCategory(request);
    //        return ResponseEntity.status(201).body(response);
    //    }
    //
    // 3. Service processes business logic
    //    - Validate category name is unique
    //    - Create Category entity
    //    - Save to database via repository
    //    - Convert to DTO
    //    - Return response
    //
    // 4. Repository interacts with database
    //    - Execute SQL: INSERT INTO categories (name, description, ...) VALUES (?, ?, ...)
    //    - Return saved entity with generated ID
    //
    // 5. Service returns DTO to Controller
    //
    // 6. Controller returns HTTP response
    //    HTTP 201 Created
    //    { "id": 1, "name": "Electronics", "description": "...", "createdAt": "..." }
    //
    // =========================================================================
    // Benefits of Service Layer Pattern
    // =========================================================================
    //
    // 1. SEPARATION OF CONCERNS
    //    - Controller: HTTP, routing, validation
    //    - Service: Business logic, transactions
    //    - Repository: Database operations
    //
    // 2. REUSABILITY
    //    - Service methods can be called from:
    //      * REST controllers
    //      * GraphQL resolvers
    //      * Scheduled jobs
    //      * Message queue handlers
    //
    // 3. TESTABILITY
    //    - Test business logic without HTTP layer
    //    - Mock repositories in unit tests
    //    - No need for MockMvc or HTTP testing
    //
    // 4. TRANSACTION MANAGEMENT
    //    - @Transactional ensures atomicity
    //    - Automatic rollback on exceptions
    //    - Consistent data state
    //
    // 5. MAINTAINABILITY
    //    - Business logic in one place
    //    - Easy to modify workflows
    //    - Clear responsibilities
}
