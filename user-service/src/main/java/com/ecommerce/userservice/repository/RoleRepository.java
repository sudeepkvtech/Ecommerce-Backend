// Package declaration - this interface belongs to the repository package
package com.ecommerce.userservice.repository;

// Import Spring Data JPA repository interface
import org.springframework.data.jpa.repository.JpaRepository;
// Import stereotype annotation
import org.springframework.stereotype.Repository;
// Import the entity class this repository manages
import com.ecommerce.userservice.entity.Role;

// Import Optional for handling null values safely
import java.util.Optional;

/**
 * Role Repository Interface
 *
 * This interface provides database operations (CRUD) for the Role entity.
 * Spring Data JPA automatically generates the implementation - no code needed!
 *
 * What is this repository used for?
 * ----------------------------------
 * - Find roles by name ("ROLE_USER", "ROLE_ADMIN")
 * - Check if a role exists
 * - Create new roles (during system initialization)
 * - Assign roles to users
 *
 * Why do we need this?
 * --------------------
 * When creating or updating users, we need to:
 * 1. Find the default role (ROLE_USER) for new registrations
 * 2. Find admin role when promoting users
 * 3. Validate that roles exist before assigning them
 *
 * Example Usage:
 * --------------
 * During user registration:
 * Role userRole = roleRepository.findByName("ROLE_USER")
 *     .orElseThrow(() -> new RuntimeException("Role not found"));
 * newUser.setRoles(Set.of(userRole));
 *
 * Magic of Spring Data JPA:
 * --------------------------
 * We just define method signatures following naming conventions
 * Spring Data JPA generates the SQL queries automatically!
 */

// @Repository marks this as a Data Access Object (DAO)
// Spring will:
// - Automatically detect and register this as a bean
// - Generate implementation at runtime
// - Enable exception translation
@Repository

// JpaRepository<Role, Long> is a generic interface
// - Role: The entity type this repository manages
// - Long: The type of the entity's primary key (id field)
//
// By extending JpaRepository, we get these methods for FREE:
// - save(Role): Insert or update a role
// - findById(Long): Find role by ID
// - findAll(): Get all roles
// - deleteById(Long): Delete role by ID
// - count(): Count total roles
// - existsById(Long): Check if role exists
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Find a role by its name
     *
     * Method Naming Convention:
     * -------------------------
     * Spring Data JPA uses METHOD NAMES to generate SQL queries!
     * Pattern: findBy + FieldName
     *
     * This method name "findByName" tells Spring Data JPA:
     * - "find" = SELECT query
     * - "By" = WHERE clause
     * - "Name" = field in Role entity
     *
     * Generated SQL:
     * --------------
     * SELECT * FROM roles WHERE name = ?
     *
     * The '?' is replaced by the method parameter (name)
     *
     * @param name The role name to search for (e.g., "ROLE_USER", "ROLE_ADMIN")
     * @return Optional<Role>
     *         - Optional.of(role) if found
     *         - Optional.empty() if not found
     *
     * Why Optional?
     * -------------
     * Optional is a container that may or may not contain a value.
     * Benefits:
     * 1. Prevents NullPointerException
     * 2. Forces caller to explicitly handle "not found" case
     * 3. Cleaner code: role.ifPresent(...) instead of if(role != null)
     *
     * Usage Examples:
     * ---------------
     *
     * Example 1: Get role or throw exception
     * Role role = roleRepository.findByName("ROLE_USER")
     *     .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));
     *
     * Example 2: Get role or use default
     * Role role = roleRepository.findByName("ROLE_ADMIN")
     *     .orElse(defaultRole);
     *
     * Example 3: Check if role exists
     * Optional<Role> roleOpt = roleRepository.findByName("ROLE_USER");
     * if (roleOpt.isPresent()) {
     *     Role role = roleOpt.get();
     *     // Use role
     * } else {
     *     // Handle not found
     * }
     *
     * Example 4: Functional style
     * roleRepository.findByName("ROLE_USER")
     *     .ifPresent(role -> user.addRole(role));
     *
     * Why is this query needed?
     * -------------------------
     * 1. User Registration:
     *    - New users are automatically assigned ROLE_USER
     *    - Need to find this role to assign it
     *
     * 2. Admin Promotion:
     *    - When promoting user to admin
     *    - Need to find ROLE_ADMIN and assign it
     *
     * 3. Role-based Logic:
     *    - Check if user has specific role
     *    - Conditional permissions based on role
     *
     * 4. Authorization Checks:
     *    - Validate role exists before creating permissions
     *    - Security configurations
     */
    Optional<Role> findByName(String name);

    /**
     * Check if a role exists with the given name
     *
     * Method Naming Convention:
     * -------------------------
     * Pattern: existsBy + FieldName
     *
     * Generated SQL:
     * --------------
     * SELECT COUNT(*) > 0 FROM roles WHERE name = ?
     *
     * This is more efficient than findByName() when you only need to check existence
     * because it doesn't load the entire Role object from database.
     *
     * @param name The role name to check
     * @return true if a role with this name exists, false otherwise
     *
     * Usage Example:
     * --------------
     * if (roleRepository.existsByName("ROLE_ADMIN")) {
     *     // Admin role exists, can proceed
     * } else {
     *     // Need to create admin role
     *     roleRepository.save(new Role(null, "ROLE_ADMIN"));
     * }
     *
     * Performance Comparison:
     * -----------------------
     * existsByName("ROLE_USER"):
     * - Executes: SELECT COUNT(*) > 0 FROM roles WHERE name = 'ROLE_USER'
     * - Returns: boolean (true/false)
     * - Fast: Only counts, doesn't load data
     *
     * findByName("ROLE_USER").isPresent():
     * - Executes: SELECT * FROM roles WHERE name = 'ROLE_USER'
     * - Loads entire Role object with all fields
     * - Returns: boolean (after checking Optional)
     * - Slower: Loads unnecessary data
     *
     * When to use which?
     * ------------------
     * - Use existsByName() when you only need to check if role exists
     * - Use findByName() when you need the actual Role object
     *
     * Example Use Cases:
     * ------------------
     * 1. System Initialization:
     *    if (!roleRepository.existsByName("ROLE_USER")) {
     *        roleRepository.save(new Role(null, "ROLE_USER"));
     *    }
     *
     * 2. Validation:
     *    if (!roleRepository.existsByName(roleName)) {
     *        throw new IllegalArgumentException("Invalid role: " + roleName);
     *    }
     */
    boolean existsByName(String name);

    // =========================================================================
    // ADVANCED: Custom Query Methods (for future use)
    // =========================================================================
    //
    // If you need more complex queries, you can use @Query annotation:
    //
    // Example 1: Find all roles with name containing keyword
    // @Query("SELECT r FROM Role r WHERE r.name LIKE %:keyword%")
    // List<Role> findByNameContaining(@Param("keyword") String keyword);
    //
    // Example 2: Count users with specific role
    // @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName")
    // long countUsersByRole(@Param("roleName") String roleName);
    //
    // Example 3: Find roles not assigned to any user
    // @Query("SELECT r FROM Role r WHERE r NOT IN " +
    //        "(SELECT DISTINCT role FROM User u JOIN u.roles role)")
    // List<Role> findUnusedRoles();
    //
    // =========================================================================
    // Spring Data JPA Query Method Keywords
    // =========================================================================
    //
    // You can build queries just by naming methods correctly:
    //
    // findBy: SELECT * FROM table WHERE
    // existsBy: SELECT COUNT(*) > 0 FROM table WHERE
    // countBy: SELECT COUNT(*) FROM table WHERE
    // deleteBy: DELETE FROM table WHERE
    //
    // Conditions:
    // - And: WHERE field1 = ? AND field2 = ?
    // - Or: WHERE field1 = ? OR field2 = ?
    // - Like: WHERE field LIKE ?
    // - In: WHERE field IN (?)
    // - OrderBy: ORDER BY field ASC/DESC
    //
    // Examples:
    // - findByNameAndActive(String name, Boolean active)
    //   WHERE name = ? AND active = ?
    //
    // - findByNameContainingIgnoreCase(String keyword)
    //   WHERE LOWER(name) LIKE LOWER(?)
    //
    // - findAllByOrderByNameAsc()
    //   SELECT * FROM roles ORDER BY name ASC
    //
    // =========================================================================
    // Best Practices
    // =========================================================================
    //
    // 1. Role Names:
    //    - Always use ROLE_ prefix for Spring Security
    //    - Use UPPERCASE: ROLE_USER, ROLE_ADMIN
    //    - Be consistent across the application
    //
    // 2. Role Creation:
    //    - Create roles during application startup
    //    - Use CommandLineRunner or @PostConstruct
    //    - Check existence before creating
    //
    // 3. Error Handling:
    //    - Always handle Optional.empty() case
    //    - Throw meaningful exceptions
    //    - Don't assume roles exist
    //
    // 4. Performance:
    //    - Use existsByName() for existence checks
    //    - Cache frequently accessed roles (optional)
    //    - Roles table is usually small, queries are fast
    //
    // =========================================================================
    // System Initialization Example
    // =========================================================================
    //
    // @Component
    // public class RoleInitializer implements CommandLineRunner {
    //
    //     @Autowired
    //     private RoleRepository roleRepository;
    //
    //     @Override
    //     public void run(String... args) {
    //         // Create default roles if they don't exist
    //         createRoleIfNotExists("ROLE_USER");
    //         createRoleIfNotExists("ROLE_ADMIN");
    //         createRoleIfNotExists("ROLE_MODERATOR");
    //     }
    //
    //     private void createRoleIfNotExists(String roleName) {
    //         if (!roleRepository.existsByName(roleName)) {
    //             Role role = new Role();
    //             role.setName(roleName);
    //             roleRepository.save(role);
    //             System.out.println("Created role: " + roleName);
    //         }
    //     }
    // }
}
