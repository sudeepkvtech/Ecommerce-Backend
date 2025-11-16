// Package declaration - this interface belongs to the repository package
package com.ecommerce.productservice.repository;

// Import Spring Data JPA repository interface
import org.springframework.data.jpa.repository.JpaRepository;
// Import Query annotation for custom queries
import org.springframework.data.jpa.repository.Query;
// Import parameter annotation for named parameters
import org.springframework.data.repository.query.Param;
// Import stereotype annotation
import org.springframework.stereotype.Repository;
// Import the entity classes
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.entity.Category;

// Import Optional and List for return types
import java.util.Optional;
import java.util.List;
// Import BigDecimal for price comparisons
import java.math.BigDecimal;

/**
 * Product Repository Interface
 *
 * This interface provides database operations for the Product entity.
 * Spring Data JPA automatically implements this interface at runtime.
 *
 * What's included:
 * ----------------
 * 1. Basic CRUD operations (inherited from JpaRepository)
 * 2. Custom query methods (defined by method names)
 * 3. Complex queries using @Query annotation
 *
 * All methods are automatically implemented - no SQL code needed!
 */

// @Repository marks this as a Data Access component
// Spring will:
// 1. Automatically detect and register this interface as a bean
// 2. Create a proxy implementation with all database operations
// 3. Enable exception translation (SQLException → DataAccessException)
@Repository

// JpaRepository<Product, Long>
// - Product: The entity type this repository manages
// - Long: Type of Product's primary key (id field)
//
// Inherited methods (available automatically):
// - save(Product): Insert/update a product
// - saveAll(List<Product>): Batch insert/update
// - findById(Long): Find by ID
// - findAll(): Get all products
// - findAllById(List<Long>): Get products by list of IDs
// - deleteById(Long): Delete by ID
// - delete(Product): Delete a product
// - deleteAll(): Delete all products (USE WITH CAUTION!)
// - count(): Count total products
// - existsById(Long): Check if product exists
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find a product by its SKU (Stock Keeping Unit)
     *
     * Method Naming Convention:
     * -------------------------
     * "findBySku" tells Spring Data JPA to:
     * - Generate: SELECT * FROM products WHERE sku = ?
     *
     * @param sku The unique product identifier (e.g., "LAPTOP-DELL-XPS13")
     * @return Optional<Product> - Contains product if found, empty otherwise
     *
     * Usage example:
     * --------------
     * Optional<Product> product = productRepository.findBySku("LAPTOP-001");
     * product.ifPresent(p -> System.out.println(p.getName()));
     */
    Optional<Product> findBySku(String sku);

    /**
     * Check if a product exists with the given SKU
     *
     * Method Naming Convention:
     * -------------------------
     * "existsBySku" generates: SELECT COUNT(*) > 0 FROM products WHERE sku = ?
     *
     * More efficient than findBySku() when you only need to check existence
     * because it doesn't load the entire Product object.
     *
     * @param sku The product SKU to check
     * @return true if product exists, false otherwise
     *
     * Usage example:
     * --------------
     * if (productRepository.existsBySku("LAPTOP-001")) {
     *     throw new DuplicateSkuException();
     * }
     */
    boolean existsBySku(String sku);

    /**
     * Find all products belonging to a specific category
     *
     * Method Naming Convention:
     * -------------------------
     * "findByCategory" generates: SELECT * FROM products WHERE category_id = ?
     *
     * Note: Takes Category object as parameter, not category ID
     * Spring Data JPA automatically extracts the ID from the Category object
     *
     * @param category The category object
     * @return List<Product> - All products in this category (empty list if none)
     *
     * Usage example:
     * --------------
     * Category electronics = categoryRepository.findById(1L).get();
     * List<Product> products = productRepository.findByCategory(electronics);
     */
    List<Product> findByCategory(Category category);

    /**
     * Find all products by category ID
     *
     * Method Naming Convention:
     * -------------------------
     * "findByCategoryId" generates: SELECT * FROM products WHERE category_id = ?
     *
     * Alternative to findByCategory() when you only have the category ID
     *
     * @param categoryId The category ID
     * @return List<Product> - All products in this category
     *
     * Usage example:
     * --------------
     * List<Product> products = productRepository.findByCategoryId(1L);
     */
    List<Product> findByCategoryId(Long categoryId);

    /**
     * Find all active products (products available for sale)
     *
     * Method Naming Convention:
     * -------------------------
     * "findByActiveTrue" generates: SELECT * FROM products WHERE active = true
     *
     * The "True" suffix tells Spring Data JPA to check for boolean = true
     * Alternative: "findByActiveFalse" for inactive products
     *
     * @return List<Product> - All active products
     *
     * Usage example:
     * --------------
     * List<Product> activeProducts = productRepository.findByActiveTrue();
     * // Display these products to customers
     */
    List<Product> findByActiveTrue();

    /**
     * Find all inactive products
     *
     * Method Naming Convention:
     * -------------------------
     * "findByActiveFalse" generates: SELECT * FROM products WHERE active = false
     *
     * @return List<Product> - All inactive/hidden products
     */
    List<Product> findByActiveFalse();

    /**
     * Find active products by category
     *
     * Method Naming Convention:
     * -------------------------
     * "findByCategoryIdAndActiveTrue" generates:
     * SELECT * FROM products WHERE category_id = ? AND active = true
     *
     * The "And" keyword combines multiple conditions with AND operator
     *
     * @param categoryId The category ID
     * @return List<Product> - All active products in the category
     *
     * Usage example:
     * --------------
     * List<Product> activeElectronics = productRepository.findByCategoryIdAndActiveTrue(1L);
     * // Show only active products to customers when browsing Electronics
     */
    List<Product> findByCategoryIdAndActiveTrue(Long categoryId);

    /**
     * Search products by name (case-insensitive partial match)
     *
     * Method Naming Convention:
     * -------------------------
     * "findByNameContainingIgnoreCase" generates:
     * SELECT * FROM products WHERE LOWER(name) LIKE LOWER(?)
     *
     * - "Containing": SQL LIKE with wildcards (%keyword%)
     * - "IgnoreCase": Converts both sides to lowercase for case-insensitive search
     *
     * @param name The search keyword
     * @return List<Product> - All products with names containing the keyword
     *
     * Usage example:
     * --------------
     * List<Product> results = productRepository.findByNameContainingIgnoreCase("laptop");
     * // Finds: "Dell Laptop", "Gaming LAPTOP", "Laptop Bag", etc.
     */
    List<Product> findByNameContainingIgnoreCase(String name);

    /**
     * Find products within a price range
     *
     * Method Naming Convention:
     * -------------------------
     * "findByPriceBetween" generates:
     * SELECT * FROM products WHERE price BETWEEN ? AND ?
     *
     * @param minPrice Minimum price (inclusive)
     * @param maxPrice Maximum price (inclusive)
     * @return List<Product> - All products in the price range
     *
     * Usage example:
     * --------------
     * List<Product> affordableProducts = productRepository.findByPriceBetween(
     *     new BigDecimal("100.00"),
     *     new BigDecimal("500.00")
     * );
     * // Finds all products priced between $100 and $500
     */
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Find products with price less than or equal to specified amount
     *
     * Method Naming Convention:
     * -------------------------
     * "findByPriceLessThanEqual" generates:
     * SELECT * FROM products WHERE price <= ?
     *
     * @param price Maximum price
     * @return List<Product> - All products with price <= specified amount
     *
     * Usage example:
     * --------------
     * List<Product> budgetProducts = productRepository.findByPriceLessThanEqual(
     *     new BigDecimal("100.00")
     * );
     */
    List<Product> findByPriceLessThanEqual(BigDecimal price);

    /**
     * Find products with price greater than or equal to specified amount
     *
     * Method Naming Convention:
     * -------------------------
     * "findByPriceGreaterThanEqual" generates:
     * SELECT * FROM products WHERE price >= ?
     *
     * @param price Minimum price
     * @return List<Product> - All products with price >= specified amount
     */
    List<Product> findByPriceGreaterThanEqual(BigDecimal price);

    /**
     * Find products with stock quantity greater than zero
     *
     * Method Naming Convention:
     * -------------------------
     * "findByStockQuantityGreaterThan" generates:
     * SELECT * FROM products WHERE stock_quantity > ?
     *
     * @return List<Product> - All products in stock
     *
     * Usage example:
     * --------------
     * List<Product> inStock = productRepository.findByStockQuantityGreaterThan(0);
     * // Show only available products that can be ordered
     */
    List<Product> findByStockQuantityGreaterThan(Integer quantity);

    /**
     * Find products that are out of stock
     *
     * Method Naming Convention:
     * -------------------------
     * "findByStockQuantity" generates:
     * SELECT * FROM products WHERE stock_quantity = ?
     *
     * @return List<Product> - All out of stock products
     *
     * Usage example:
     * --------------
     * List<Product> outOfStock = productRepository.findByStockQuantity(0);
     * // Notify admin to reorder these products
     */
    List<Product> findByStockQuantity(Integer quantity);

    /**
     * Find all products ordered by price (ascending)
     *
     * Method Naming Convention:
     * -------------------------
     * "findAllByOrderByPriceAsc" generates:
     * SELECT * FROM products ORDER BY price ASC
     *
     * - "AllBy": All records (no WHERE clause)
     * - "OrderBy": ORDER BY clause
     * - "PriceAsc": Sort by price ascending (low to high)
     *
     * @return List<Product> - All products sorted by price (cheapest first)
     *
     * Alternative: findAllByOrderByPriceDesc() for descending (high to low)
     */
    List<Product> findAllByOrderByPriceAsc();

    /**
     * Find all products ordered by price (descending)
     *
     * @return List<Product> - All products sorted by price (most expensive first)
     */
    List<Product> findAllByOrderByPriceDesc();

    /**
     * Advanced Search - Custom JPQL Query
     *
     * JPQL (Java Persistence Query Language) is object-oriented query language
     * Similar to SQL but operates on entity objects, not database tables
     *
     * @Query annotation allows writing custom queries when method names aren't enough
     *
     * This query searches products by:
     * - Name contains keyword (case-insensitive) OR
     * - Description contains keyword (case-insensitive)
     * AND product is active
     *
     * JPQL Syntax:
     * -----------
     * - Product p: 'p' is an alias for Product entity (like table alias in SQL)
     * - LOWER(): Convert to lowercase for case-insensitive comparison
     * - LIKE: Pattern matching (% = wildcard)
     * - CONCAT: Concatenate strings (adds % on both sides of keyword)
     * - :keyword: Named parameter (value comes from method parameter @Param)
     *
     * @param keyword The search term
     * @return List<Product> - Products matching the search criteria
     *
     * Usage example:
     * --------------
     * List<Product> results = productRepository.searchProducts("wireless");
     * // Finds products with "wireless" in name or description
     * // Examples: "Wireless Mouse", "Product with wireless connectivity"
     */
    @Query("SELECT p FROM Product p WHERE " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND p.active = true")
    List<Product> searchProducts(@Param("keyword") String keyword);

    /**
     * Advanced Filter - Custom JPQL Query
     *
     * Filter products by multiple criteria:
     * - Category (optional - if null, ignored)
     * - Price range (optional - if null, ignored)
     * - Active status
     *
     * JPQL Features:
     * --------------
     * - (:categoryId IS NULL OR p.category.id = :categoryId)
     *   This pattern makes the parameter optional:
     *   - If categoryId is NULL: condition is ignored (always true)
     *   - If categoryId has value: filters by that category
     *
     * @param categoryId Category ID to filter by (nullable)
     * @param minPrice Minimum price (nullable)
     * @param maxPrice Maximum price (nullable)
     * @return List<Product> - Filtered products
     *
     * Usage examples:
     * ---------------
     * // Filter by category only
     * productRepository.filterProducts(1L, null, null);
     *
     * // Filter by price range only
     * productRepository.filterProducts(null, new BigDecimal("100"), new BigDecimal("500"));
     *
     * // Filter by both
     * productRepository.filterProducts(1L, new BigDecimal("100"), new BigDecimal("500"));
     */
    @Query("SELECT p FROM Product p WHERE " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "p.active = true")
    List<Product> filterProducts(
        @Param("categoryId") Long categoryId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice
    );

    // =========================================================================
    // REFERENCE: More Query Method Keywords
    // =========================================================================
    //
    // Comparison:
    // - LessThan: <
    // - LessThanEqual: <=
    // - GreaterThan: >
    // - GreaterThanEqual: >=
    // - Between: BETWEEN x AND y
    //
    // String matching:
    // - Like: LIKE 'x'
    // - NotLike: NOT LIKE 'x'
    // - StartingWith: LIKE 'x%'
    // - EndingWith: LIKE '%x'
    // - Containing: LIKE '%x%'
    //
    // Collection:
    // - In: IN (x, y, z)
    // - NotIn: NOT IN (x, y, z)
    //
    // Null checks:
    // - IsNull: IS NULL
    // - IsNotNull: IS NOT NULL
    //
    // Boolean:
    // - True: = true
    // - False: = false
    //
    // Sorting:
    // - OrderBy...Asc: ORDER BY field ASC
    // - OrderBy...Desc: ORDER BY field DESC
    //
    // Logical:
    // - And: AND
    // - Or: OR
    //
    // Limiting:
    // - First: LIMIT 1
    // - Top: LIMIT n
    //
    // Examples:
    // - findFirstByOrderByCreatedAtDesc(): Get most recent product
    // - findTop10ByOrderByPriceAsc(): Get 10 cheapest products
    // - findByNameStartingWithAndActiveTrue(String prefix): Name starts with prefix and active
}
