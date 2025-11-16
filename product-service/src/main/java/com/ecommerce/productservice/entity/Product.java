// Package declaration - this class belongs to the entity package
package com.ecommerce.productservice.entity;

// Import JPA annotations for database mapping
import jakarta.persistence.*;
// Import Lombok annotations to reduce boilerplate code
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Import BigDecimal for precise decimal calculations (prices)
import java.math.BigDecimal;
// Import Java time classes for timestamps
import java.time.LocalDateTime;

/**
 * Product Entity Class
 *
 * Represents a product in the e-commerce catalog
 * This class is mapped to a database table called "products"
 *
 * Database Table Structure:
 * -----------------------------------------------------------------------------------
 * | id | name | description | price | stock_quantity | sku | category_id | ... |
 * -----------------------------------------------------------------------------------
 *
 * Relationships:
 * - Many Products belong to One Category (Many-to-One)
 * - This is the "child" side of the relationship
 * - Foreign key "category_id" references the categories table
 */

// @Entity marks this class as a JPA entity (a table in the database)
// Spring Data JPA will automatically create a "products" table
@Entity

// @Table specifies the table name and indexes
// Indexes improve query performance for frequently searched columns
@Table(
    name = "products",
    indexes = {
        // Index on SKU for fast lookup by product code
        // Example query: SELECT * FROM products WHERE sku = 'LAPTOP-001'
        @Index(name = "idx_sku", columnList = "sku"),

        // Index on name for fast product search by name
        // Example query: SELECT * FROM products WHERE name LIKE '%laptop%'
        @Index(name = "idx_name", columnList = "name"),

        // Index on category_id for fast filtering by category
        // Example query: SELECT * FROM products WHERE category_id = 5
        @Index(name = "idx_category_id", columnList = "category_id")
    }
)

// @Data is a Lombok annotation that generates:
// - Getters for all fields
// - Setters for all non-final fields
// - toString() method
// - equals() and hashCode() methods
// This eliminates ~150 lines of boilerplate code
@Data

// @NoArgsConstructor generates a no-argument constructor
// Required by JPA for entity instantiation using reflection
@NoArgsConstructor

// @AllArgsConstructor generates a constructor with all fields
// Useful for creating test objects
@AllArgsConstructor
public class Product {

    /**
     * Primary Key - Unique identifier for each product
     *
     * @Id marks this field as the primary key
     * @GeneratedValue with IDENTITY strategy means:
     * - Database automatically generates sequential IDs (1, 2, 3, ...)
     * - When we insert a product, we don't need to provide an ID
     * - MySQL's AUTO_INCREMENT feature handles this
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Product Name
     *
     * @Column constraints:
     * - nullable = false: Product must have a name (required field)
     * - length = 200: Maximum 200 characters for the product name
     *
     * Database will reject any INSERT/UPDATE that violates these constraints
     * Example: Trying to save a product without a name will throw an exception
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Product Description
     *
     * @Column properties:
     * - columnDefinition = "TEXT": Uses MySQL TEXT type instead of VARCHAR
     * - TEXT can store up to 65,535 characters (~64KB)
     * - Ideal for long product descriptions with detailed information
     *
     * VARCHAR has a limit of 255 or 65,535 bytes depending on configuration
     * TEXT is better for potentially large content
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Product Price
     *
     * Why BigDecimal instead of double or float?
     * - double/float have precision errors: 0.1 + 0.2 = 0.30000000000000004
     * - BigDecimal provides exact decimal arithmetic
     * - Critical for financial calculations to avoid rounding errors
     *
     * @Column properties:
     * - nullable = false: Every product must have a price
     * - precision = 10: Total number of digits (including decimal places)
     * - scale = 2: Number of digits after decimal point
     *
     * Example valid prices: 99.99, 1234.56, 12345678.99
     * Example invalid: 123456789.99 (11 digits, exceeds precision)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Stock Quantity
     *
     * Tracks how many units of this product are available in inventory
     *
     * @Column properties:
     * - nullable = false: Must always have a quantity (even if 0)
     * - Default value of 0 means out of stock
     *
     * Business logic should prevent orders when stockQuantity = 0
     * Inventory Service would typically manage this field
     */
    @Column(nullable = false)
    private Integer stockQuantity = 0;

    /**
     * SKU (Stock Keeping Unit)
     *
     * A unique identifier/code for the product used for inventory management
     * Examples: "LAPTOP-DELL-XPS13", "PHONE-IPHONE-15-PRO", "BOOK-ISBN-123456"
     *
     * @Column constraints:
     * - unique = true: No two products can have the same SKU
     * - nullable = false: Every product must have a SKU
     * - length = 100: Maximum 100 characters for SKU code
     *
     * SKU is different from ID:
     * - ID is internal database identifier (auto-generated)
     * - SKU is business identifier (often set manually or by business rules)
     */
    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    /**
     * Product Image URL
     *
     * Stores the URL or file path to the product image
     * Examples:
     * - "https://cdn.example.com/products/laptop-xps13.jpg"
     * - "/images/products/laptop-xps13.jpg"
     * - Can be comma-separated for multiple images: "img1.jpg,img2.jpg,img3.jpg"
     *
     * In production, you might:
     * - Store images in cloud storage (AWS S3, Google Cloud Storage)
     * - Use a CDN (Content Delivery Network) for fast image delivery
     * - Store multiple images in a separate ProductImage table (one-to-many)
     */
    @Column(length = 500)
    private String imageUrl;

    /**
     * Product Active Status
     *
     * Indicates whether this product is currently available for sale
     * - true: Product is active and visible to customers
     * - false: Product is hidden/deactivated (soft delete)
     *
     * Why use a flag instead of deleting?
     * - Preserves historical data (past orders still reference this product)
     * - Can be reactivated later without losing data
     * - Maintains referential integrity in the database
     *
     * Default value: true (new products are active by default)
     */
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Category Relationship
     *
     * @ManyToOne defines a many-to-one relationship:
     * - Many Products belong to One Category
     * - This creates a foreign key column "category_id" in products table
     *
     * @JoinColumn specifies the foreign key column:
     * - name = "category_id": Name of the foreign key column
     * - nullable = false: Every product must belong to a category
     * - referencedColumnName = "id": Points to the "id" column in categories table
     *
     * fetch = FetchType.LAZY means:
     * - When loading a Product, don't automatically load its Category
     * - Category is only loaded when explicitly accessed: product.getCategory().getName()
     * - Improves performance by avoiding unnecessary database queries
     * - Alternative: FetchType.EAGER would always load the category (slower)
     *
     * Example in database:
     * products table:
     * | id | name           | category_id |
     * | 1  | Laptop XPS 13  | 5           | → references categories.id = 5
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false, referencedColumnName = "id")
    private Category category;

    /**
     * Created Timestamp
     *
     * Stores when this product was first added to the catalog
     *
     * @Column properties:
     * - nullable = false: Must have a creation timestamp
     * - updatable = false: Once set, cannot be changed
     *   Prevents accidentally modifying the historical creation date
     *
     * Automatically set by @PrePersist lifecycle callback
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Updated Timestamp
     *
     * Stores when this product was last modified
     * Updated automatically whenever product details change
     *
     * Useful for:
     * - Tracking when prices were last updated
     * - Showing "Recently Updated" products
     * - Auditing product catalog changes
     *
     * Automatically updated by @PreUpdate lifecycle callback
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Lifecycle Callback - Before Insert
     *
     * @PrePersist is called automatically before saving a NEW product to database
     * This happens before the INSERT SQL statement is executed
     *
     * Use case: Set timestamps automatically so developers don't forget
     * Without this, we'd have to manually do:
     *   product.setCreatedAt(LocalDateTime.now());
     *   product.setUpdatedAt(LocalDateTime.now());
     * every time we create a product
     */
    @PrePersist
    protected void onCreate() {
        // Get current date and time
        LocalDateTime now = LocalDateTime.now();

        // Set both timestamps to current time
        // For a new product, created and updated times are the same
        createdAt = now;
        updatedAt = now;
    }

    /**
     * Lifecycle Callback - Before Update
     *
     * @PreUpdate is called automatically before updating an EXISTING product
     * This happens before the UPDATE SQL statement is executed
     *
     * Use case: Automatically track when product was last modified
     * Without this, we'd have to remember to call:
     *   product.setUpdatedAt(LocalDateTime.now());
     * every time we update any product field
     *
     * Note: createdAt is NOT updated because it's marked as updatable = false
     */
    @PreUpdate
    protected void onUpdate() {
        // Update only the updatedAt timestamp
        // createdAt remains unchanged (preserves original creation time)
        updatedAt = LocalDateTime.now();
    }
}
