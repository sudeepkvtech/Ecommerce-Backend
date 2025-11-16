// Package declaration - this class belongs to the entity package
package com.ecommerce.productservice.entity;

// Import JPA annotations for database mapping
import jakarta.persistence.*;
// Import Lombok annotations to reduce boilerplate code
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Import Java time classes for timestamps
import java.time.LocalDateTime;
// Import List and Set collections for relationships
import java.util.ArrayList;
import java.util.List;

/**
 * Category Entity Class
 *
 * Represents a product category in the database (e.g., Electronics, Clothing, Books)
 * This class is mapped to a database table called "categories"
 *
 * Database Table Structure:
 * --------------------------
 * | id | name | description | created_at | updated_at |
 * --------------------------
 *
 * Relationships:
 * - One Category can have many Products (One-to-Many)
 * - This is the "parent" side of the relationship
 */

// @Entity marks this class as a JPA entity (a table in the database)
// Spring Data JPA will automatically create a table for this class
@Entity

// @Table specifies the table name in the database
// If not specified, table name would default to the class name (Category)
@Table(name = "categories")

// @Data is a Lombok annotation that generates:
// - Getters for all fields
// - Setters for all non-final fields
// - toString() method
// - equals() and hashCode() methods
// - A required args constructor
// This saves us from writing ~100 lines of boilerplate code
@Data

// @NoArgsConstructor generates a no-argument constructor
// Required by JPA to create instances using reflection
@NoArgsConstructor

// @AllArgsConstructor generates a constructor with all fields as parameters
// Useful for creating test objects or initializing all fields at once
@AllArgsConstructor
public class Category {

    /**
     * Primary Key - Unique identifier for each category
     *
     * @Id marks this field as the primary key
     * @GeneratedValue tells JPA to automatically generate values for this field
     * GenerationType.IDENTITY means the database will auto-increment this value
     * Each new category will get an automatically assigned ID (1, 2, 3, ...)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Category Name
     *
     * @Column specifies column properties:
     * - nullable = false: This field cannot be NULL (required field)
     * - unique = true: No two categories can have the same name
     * - length = 100: Maximum 100 characters allowed
     *
     * Database constraint: If you try to insert a category without a name
     * or with a duplicate name, the database will reject it
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * Category Description
     *
     * @Column properties:
     * - length = 500: Allows longer text (up to 500 characters)
     * - nullable = true (default): Description is optional
     *
     * This field can be NULL in the database if not provided
     */
    @Column(length = 500)
    private String description;

    /**
     * Category Image URL
     *
     * Stores the URL or path to the category image
     * Example: "https://cdn.example.com/categories/electronics.jpg"
     * Or: "/images/categories/electronics.jpg"
     *
     * This is optional and can be NULL
     */
    @Column(length = 255)
    private String imageUrl;

    /**
     * List of Products in this Category
     *
     * @OneToMany defines a one-to-many relationship:
     * - One Category can have many Products
     * - mappedBy = "category": The Product entity has a field called "category"
     *   that owns this relationship (the foreign key is in the products table)
     *
     * cascade = CascadeType.ALL means:
     * - If we save a Category, all its products are also saved
     * - If we delete a Category, all its products are also deleted
     * - If we update a Category, all its products are also updated
     *
     * orphanRemoval = true means:
     * - If a Product is removed from this list, it will be deleted from database
     *
     * Note: In a real production system, you might want to prevent cascade deletion
     * to avoid accidentally deleting products when removing a category
     */
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products = new ArrayList<>();

    /**
     * Created Timestamp
     *
     * @Column properties:
     * - nullable = false: Must have a value
     * - updatable = false: Once set, this value cannot be changed
     *   This prevents accidentally modifying the creation timestamp
     *
     * This field stores when the category was first created in the database
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Updated Timestamp
     *
     * This field stores when the category was last modified
     * Unlike createdAt, this CAN be updated (updatable = true by default)
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Lifecycle callback method - called before persisting (saving) a new entity
     *
     * @PrePersist is a JPA lifecycle annotation
     * This method automatically executes before a new Category is inserted into database
     *
     * Use case: Automatically set createdAt and updatedAt timestamps
     * when creating a new category, so developers don't have to remember
     * to set these fields manually
     */
    @PrePersist
    protected void onCreate() {
        // Get current date and time
        LocalDateTime now = LocalDateTime.now();

        // Set both createdAt and updatedAt to current time
        // When first creating a category, both timestamps should be the same
        createdAt = now;
        updatedAt = now;
    }

    /**
     * Lifecycle callback method - called before updating an existing entity
     *
     * @PreUpdate is a JPA lifecycle annotation
     * This method automatically executes before an existing Category is updated in database
     *
     * Use case: Automatically update the updatedAt timestamp whenever
     * a category is modified, tracking when the last change occurred
     */
    @PreUpdate
    protected void onUpdate() {
        // Update the updatedAt timestamp to current time
        // createdAt remains unchanged (because it's marked as updatable = false)
        updatedAt = LocalDateTime.now();
    }
}
