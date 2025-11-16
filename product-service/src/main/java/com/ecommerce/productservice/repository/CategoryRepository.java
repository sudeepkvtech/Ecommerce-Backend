// Package declaration - this interface belongs to the repository package
package com.ecommerce.productservice.repository;

// Import Spring Data JPA repository interface
import org.springframework.data.jpa.repository.JpaRepository;
// Import stereotype annotation to mark this as a Spring component
import org.springframework.stereotype.Repository;
// Import the entity class this repository manages
import com.ecommerce.productservice.entity.Category;

// Import Optional for handling null values safely
import java.util.Optional;

/**
 * Category Repository Interface
 *
 * This interface provides database operations (CRUD) for the Category entity.
 * We don't need to write implementation code - Spring Data JPA generates it automatically!
 *
 * What is a Repository?
 * ---------------------
 * In software architecture, Repository pattern provides an abstraction layer between
 * the business logic and data access layer. It acts like a collection of objects
 * that you can add, remove, or query.
 *
 * Think of it as a "database manager" specifically for Category objects.
 *
 * How Spring Data JPA Works:
 * --------------------------
 * 1. We define this interface extending JpaRepository
 * 2. At application startup, Spring Data JPA creates a proxy implementation
 * 3. This implementation contains all the SQL code needed for database operations
 * 4. We can inject this repository anywhere and use it without writing SQL
 *
 * Magic? Not really! Spring uses reflection and dynamic proxies to generate
 * the implementation at runtime based on the interface definition.
 */

// @Repository marks this as a Data Access Object (DAO)
// Benefits:
// 1. Spring automatically detects and registers this as a bean
// 2. Enables exception translation (converts SQLException to Spring's DataAccessException)
// 3. Makes it eligible for dependency injection in Service classes
@Repository

// JpaRepository<Category, Long> is a generic interface provided by Spring Data JPA
// - Category: The entity type this repository manages
// - Long: The type of the entity's primary key (id field in Category)
//
// By extending JpaRepository, we automatically get these methods for FREE:
// -------------------------------------------------------------------------
// - save(Category): Insert or update a category
// - findById(Long): Find category by ID
// - findAll(): Get all categories
// - deleteById(Long): Delete category by ID
// - delete(Category): Delete a category
// - count(): Count total categories
// - existsById(Long): Check if category exists
// And many more...
//
// We DON'T need to write SQL for these operations!
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Find a category by its name
     *
     * Method Naming Convention:
     * -------------------------
     * Spring Data JPA uses METHOD NAMES to generate SQL queries automatically!
     * Pattern: findBy + FieldName
     *
     * This method name "findByName" tells Spring Data JPA:
     * - "find" = SELECT query
     * - "By" = WHERE clause
     * - "Name" = field in Category entity
     *
     * Generated SQL:
     * --------------
     * SELECT * FROM categories WHERE name = ?
     *
     * The '?' is replaced by the method parameter (name)
     *
     * @param name The category name to search for (e.g., "Electronics")
     * @return Optional<Category>
     *         - Optional.of(category) if found
     *         - Optional.empty() if not found
     *
     * Why Optional?
     * -------------
     * Optional is a container that may or may not contain a value.
     * Benefits:
     * 1. Prevents NullPointerException
     * 2. Forces caller to explicitly handle "not found" case
     * 3. Cleaner code: category.ifPresent(...) instead of if(category != null)
     *
     * Usage example in Service layer:
     * --------------------------------
     * Optional<Category> result = categoryRepository.findByName("Electronics");
     * if (result.isPresent()) {
     *     Category category = result.get();
     *     // Use category object
     * } else {
     *     // Handle not found case
     *     throw new CategoryNotFoundException();
     * }
     */
    Optional<Category> findByName(String name);

    /**
     * Check if a category exists with the given name
     *
     * Method Naming Convention:
     * -------------------------
     * Pattern: existsBy + FieldName
     *
     * Generated SQL:
     * --------------
     * SELECT COUNT(*) > 0 FROM categories WHERE name = ?
     *
     * This is more efficient than findByName() when you only need to check existence
     * because it doesn't load the entire Category object from database.
     *
     * @param name The category name to check
     * @return true if a category with this name exists, false otherwise
     *
     * Usage example in Service layer:
     * --------------------------------
     * if (categoryRepository.existsByName("Electronics")) {
     *     throw new DuplicateCategoryException("Category already exists");
     * }
     *
     * Performance comparison:
     * -----------------------
     * existsByName(): Only checks if record exists (fast)
     * findByName().isPresent(): Loads entire object then checks (slower)
     *
     * Always prefer existsBy when you don't need the actual data!
     */
    boolean existsByName(String name);

    // =========================================================================
    // ADVANCED: Custom Query Methods (commented examples for future use)
    // =========================================================================

    // If you need more complex queries, you can use @Query annotation:
    //
    // Example 1: Custom JPQL query
    // @Query("SELECT c FROM Category c WHERE c.name LIKE %:keyword%")
    // List<Category> searchByKeyword(@Param("keyword") String keyword);
    //
    // Example 2: Native SQL query
    // @Query(value = "SELECT * FROM categories WHERE created_at > ?1", nativeQuery = true)
    // List<Category> findRecentCategories(LocalDateTime date);
    //
    // Example 3: Update query
    // @Modifying
    // @Query("UPDATE Category c SET c.name = :newName WHERE c.id = :id")
    // int updateCategoryName(@Param("id") Long id, @Param("newName") String newName);
    //
    // Example 4: Complex query with multiple conditions
    // List<Category> findByNameContainingAndActiveTrue(String keyword);
    // This would generate: SELECT * FROM categories WHERE name LIKE %?% AND active = true

    // =========================================================================
    // Spring Data JPA Query Method Keywords
    // =========================================================================
    //
    // You can build complex queries just by naming methods correctly:
    //
    // findBy: SELECT * FROM table WHERE
    // existsBy: SELECT COUNT(*) > 0 FROM table WHERE
    // countBy: SELECT COUNT(*) FROM table WHERE
    // deleteBy: DELETE FROM table WHERE
    //
    // Conditions:
    // - And: WHERE field1 = ? AND field2 = ?
    // - Or: WHERE field1 = ? OR field2 = ?
    // - Between: WHERE field BETWEEN ? AND ?
    // - LessThan: WHERE field < ?
    // - GreaterThan: WHERE field > ?
    // - Like: WHERE field LIKE ?
    // - In: WHERE field IN (?)
    // - OrderBy: ORDER BY field ASC/DESC
    //
    // Examples:
    // - findByNameAndActiveTrue(String name): WHERE name = ? AND active = true
    // - findByCreatedAtBetween(LocalDateTime start, LocalDateTime end): WHERE created_at BETWEEN ? AND ?
    // - findByNameContainingIgnoreCase(String keyword): WHERE LOWER(name) LIKE LOWER(?)
    // - findAllByOrderByNameAsc(): SELECT * FROM categories ORDER BY name ASC
}
