// Package declaration - this class belongs to the entity package
package com.ecommerce.userservice.entity;

// Import JPA annotations for database mapping
import jakarta.persistence.*;
// Import Lombok annotations to reduce boilerplate code
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Role Entity Class
 *
 * Represents user roles in the system for authorization
 * This class is mapped to a database table called "roles"
 *
 * What is a Role?
 * ---------------
 * A role defines what permissions a user has in the system
 * Examples:
 * - ROLE_USER: Regular customer (can browse products, place orders)
 * - ROLE_ADMIN: Administrator (can manage products, users, orders)
 * - ROLE_MODERATOR: Content moderator (can review products, comments)
 *
 * Why use Roles?
 * --------------
 * - Authorization: Control who can access what
 * - Security: Protect sensitive operations (only admins can delete users)
 * - Flexibility: Easy to add new roles without code changes
 * - Scalability: One user can have multiple roles
 *
 * Database Table Structure:
 * --------------------------
 * | id | name |
 * --------------------------
 * | 1  | ROLE_USER  |
 * | 2  | ROLE_ADMIN |
 *
 * Relationships:
 * - Many-to-Many with User (many users can have many roles)
 */

// @Entity marks this class as a JPA entity (a table in the database)
// Spring Data JPA will automatically create a "roles" table
@Entity

// @Table specifies the table name in the database
@Table(name = "roles")

// @Data is a Lombok annotation that generates:
// - Getters for all fields
// - Setters for all fields
// - toString() method
// - equals() and hashCode() methods
@Data

// @NoArgsConstructor generates a no-argument constructor
// Required by JPA for entity instantiation
@NoArgsConstructor

// @AllArgsConstructor generates a constructor with all fields
// Useful for creating role objects in code
@AllArgsConstructor
public class Role {

    /**
     * Primary Key - Unique identifier for each role
     *
     * @Id marks this field as the primary key
     * @GeneratedValue tells JPA to automatically generate values
     * GenerationType.IDENTITY means database auto-increments this value
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Role Name
     *
     * @Column constraints:
     * - nullable = false: Role must have a name
     * - unique = true: No two roles can have the same name
     * - length = 50: Maximum 50 characters
     *
     * Naming Convention:
     * ------------------
     * Spring Security expects role names to start with "ROLE_"
     * Examples:
     * - ROLE_USER
     * - ROLE_ADMIN
     * - ROLE_MODERATOR
     *
     * Why ROLE_ prefix?
     * -----------------
     * Spring Security uses this prefix to distinguish roles from authorities
     * When checking permissions: hasRole('ADMIN') looks for 'ROLE_ADMIN'
     * When checking authorities: hasAuthority('ADMIN') looks for 'ADMIN'
     *
     * Database values:
     * ----------------
     * Good: "ROLE_USER", "ROLE_ADMIN"
     * Bad: "user", "admin" (missing ROLE_ prefix)
     */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    // =========================================================================
    // Common Roles in E-commerce Application
    // =========================================================================
    //
    // ROLE_USER (Customer):
    // - Browse products
    // - Add to cart
    // - Place orders
    // - View order history
    // - Update profile
    // - Write product reviews
    //
    // ROLE_ADMIN (Administrator):
    // - All USER permissions PLUS:
    // - Manage products (create, update, delete)
    // - Manage categories
    // - Manage users
    // - View all orders
    // - Update order status
    // - View analytics and reports
    //
    // ROLE_SELLER (Vendor):
    // - Manage own products
    // - View own sales
    // - Update product inventory
    //
    // ROLE_MODERATOR:
    // - Review product listings
    // - Moderate user reviews
    // - Handle customer complaints
    //
    // =========================================================================
    // How Roles are Used in Controllers
    // =========================================================================
    //
    // Example 1: Only admins can delete users
    // @PreAuthorize("hasRole('ADMIN')")
    // @DeleteMapping("/users/{id}")
    // public void deleteUser(@PathVariable Long id) {
    //     userService.deleteUser(id);
    // }
    //
    // Example 2: Both users and admins can view profile
    // @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    // @GetMapping("/profile")
    // public UserResponse getProfile() {
    //     return userService.getCurrentUserProfile();
    // }
    //
    // Example 3: Only the user themselves or admin can update profile
    // @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    // @PutMapping("/users/{id}")
    // public UserResponse updateUser(@PathVariable Long id, @RequestBody UserRequest request) {
    //     return userService.updateUser(id, request);
    // }
    //
    // =========================================================================
    // Role Assignment
    // =========================================================================
    //
    // New User Registration:
    // - Automatically assigned ROLE_USER
    // - Admin manually assigns additional roles
    //
    // Example code:
    // Role userRole = roleRepository.findByName("ROLE_USER")
    //     .orElseThrow(() -> new RuntimeException("Role not found"));
    // user.setRoles(Set.of(userRole));
    //
    // =========================================================================
    // Database Initialization
    // =========================================================================
    //
    // Roles should be pre-populated in database
    // Can be done via:
    // 1. SQL script (data.sql)
    // 2. CommandLineRunner in Spring Boot
    // 3. Database migration tool (Flyway, Liquibase)
    //
    // Example SQL:
    // INSERT INTO roles (name) VALUES ('ROLE_USER');
    // INSERT INTO roles (name) VALUES ('ROLE_ADMIN');
    //
    // Example CommandLineRunner:
    // @Component
    // public class DataInitializer implements CommandLineRunner {
    //     @Autowired private RoleRepository roleRepository;
    //
    //     @Override
    //     public void run(String... args) {
    //         if (roleRepository.count() == 0) {
    //             roleRepository.save(new Role(null, "ROLE_USER"));
    //             roleRepository.save(new Role(null, "ROLE_ADMIN"));
    //         }
    //     }
    // }
}
