// Package declaration - this interface belongs to the repository package
package com.ecommerce.userservice.repository;

// Import Spring Data JPA repository interface
import org.springframework.data.jpa.repository.JpaRepository;
// Import Query annotation for custom queries
import org.springframework.data.jpa.repository.Query;
// Import parameter annotation
import org.springframework.data.repository.query.Param;
// Import stereotype annotation
import org.springframework.stereotype.Repository;
// Import the entity class
import com.ecommerce.userservice.entity.User;

// Import Optional and List for return types
import java.util.Optional;
import java.util.List;

/**
 * User Repository Interface
 *
 * This interface provides database operations for the User entity.
 * Spring Data JPA automatically implements this interface at runtime.
 *
 * What is this repository used for?
 * ----------------------------------
 * - Find users by email (for login)
 * - Check if email already exists (prevent duplicate registrations)
 * - Find users by roles (get all admins)
 * - Find enabled/disabled users
 * - Search users by name
 *
 * Critical for Authentication:
 * ----------------------------
 * The findByEmail() method is used by Spring Security
 * to load user details during login
 *
 * Example: Login Flow
 * --------------------
 * 1. User enters email and password
 * 2. Spring Security calls UserDetailsService.loadUserByUsername(email)
 * 3. UserDetailsService uses this repository: findByEmail(email)
 * 4. Returns User object with password hash and roles
 * 5. Spring Security compares passwords
 * 6. If match: Generate JWT token
 * 7. If no match: Throw BadCredentialsException
 */

// @Repository marks this as a Data Access Object (DAO)
@Repository

// JpaRepository<User, Long>
// - User: The entity type this repository manages
// - Long: Type of User's primary key (id field)
//
// Inherited methods (available automatically):
// - save(User): Insert/update a user
// - findById(Long): Find by ID
// - findAll(): Get all users
// - deleteById(Long): Delete by ID
// - count(): Count total users
// - existsById(Long): Check if user exists
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by email address
     *
     * Method Naming Convention:
     * -------------------------
     * "findByEmail" tells Spring Data JPA to:
     * - Generate: SELECT * FROM users WHERE email = ?
     *
     * This is THE MOST IMPORTANT query in authentication!
     * Used by Spring Security to load user during login
     *
     * @param email The email address (username) for login
     * @return Optional<User> - Contains user if found, empty otherwise
     *
     * Why Optional?
     * -------------
     * - Prevents NullPointerException
     * - Forces explicit handling of "user not found" case
     * - Better than returning null
     *
     * Usage Examples:
     * ---------------
     *
     * Example 1: Load user for authentication (Spring Security)
     * @Override
     * public UserDetails loadUserByUsername(String email) {
     *     User user = userRepository.findByEmail(email)
     *         .orElseThrow(() -> new UsernameNotFoundException("User not found"));
     *     return buildUserDetails(user);
     * }
     *
     * Example 2: Check if email exists during registration
     * if (userRepository.findByEmail(email).isPresent()) {
     *     throw new DuplicateEmailException("Email already registered");
     * }
     *
     * Example 3: Get user profile
     * User user = userRepository.findByEmail(currentUserEmail)
     *     .orElseThrow(() -> new ResourceNotFoundException("User not found"));
     *
     * Why indexed?
     * ------------
     * - Email is used for every login
     * - Without index, database would scan entire table (slow)
     * - With index (defined in User entity), lookup is very fast
     * - Index created in User entity: @Index(name = "idx_email", columnList = "email")
     *
     * Security Consideration:
     * -----------------------
     * - Email should be stored in lowercase
     * - Convert to lowercase before saving: email.toLowerCase()
     * - Prevents case-sensitivity issues:
     *   - User registers: User@Example.com
     *   - Tries to login: user@example.com
     *   - Without lowercase: Login fails (case mismatch)
     *   - With lowercase: Login succeeds
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists with the given email
     *
     * Method Naming Convention:
     * -------------------------
     * "existsByEmail" generates: SELECT COUNT(*) > 0 FROM users WHERE email = ?
     *
     * More efficient than findByEmail() when you only need to check existence
     * Doesn't load the entire User object (faster, less memory)
     *
     * @param email The email address to check
     * @return true if a user with this email exists, false otherwise
     *
     * Usage Example:
     * --------------
     * During user registration, check if email is already taken:
     *
     * public void registerUser(UserRegistrationRequest request) {
     *     // Check if email already exists
     *     if (userRepository.existsByEmail(request.getEmail())) {
     *         throw new DuplicateResourceException(
     *             "Email already registered: " + request.getEmail()
     *         );
     *     }
     *
     *     // Proceed with registration
     *     User user = new User();
     *     user.setEmail(request.getEmail().toLowerCase());
     *     // ... set other fields
     *     userRepository.save(user);
     * }
     *
     * Performance Comparison:
     * -----------------------
     * existsByEmail("user@example.com"):
     * - Query: SELECT COUNT(*) > 0 FROM users WHERE email = ?
     * - Returns: boolean
     * - Fast: Only counts, doesn't load data
     *
     * findByEmail("user@example.com").isPresent():
     * - Query: SELECT * FROM users WHERE email = ?
     * - Loads: User object with all fields, roles, etc.
     * - Returns: boolean (after checking Optional)
     * - Slower: Loads unnecessary data
     *
     * When to use which?
     * ------------------
     * - Use existsByEmail() when you only need to check if email is registered
     * - Use findByEmail() when you need the actual User object
     */
    boolean existsByEmail(String email);

    /**
     * Find all enabled users
     *
     * Method Naming Convention:
     * -------------------------
     * "findByEnabledTrue" generates: SELECT * FROM users WHERE enabled = true
     *
     * The "True" suffix tells Spring Data JPA to check for boolean = true
     *
     * @return List<User> - All enabled (active) users
     *
     * Usage Examples:
     * ---------------
     *
     * Example 1: Get all active users
     * List<User> activeUsers = userRepository.findByEnabledTrue();
     *
     * Example 2: Count active users
     * long activeCount = userRepository.findByEnabledTrue().size();
     *
     * Example 3: Send newsletter to active users
     * List<User> users = userRepository.findByEnabledTrue();
     * users.forEach(user -> emailService.sendNewsletter(user.getEmail()));
     *
     * Why filter by enabled?
     * ----------------------
     * - Only show active users in admin panel
     * - Send communications only to active accounts
     * - Statistics on active vs inactive users
     * - Exclude disabled accounts from user list
     */
    List<User> findByEnabledTrue();

    /**
     * Find all disabled users
     *
     * Method Naming Convention:
     * -------------------------
     * "findByEnabledFalse" generates: SELECT * FROM users WHERE enabled = false
     *
     * @return List<User> - All disabled (inactive) users
     *
     * Usage Examples:
     * ---------------
     *
     * Example 1: Review disabled accounts
     * List<User> disabledUsers = userRepository.findByEnabledFalse();
     * // Admin can review and re-enable if needed
     *
     * Example 2: Clean up old disabled accounts
     * List<User> disabledUsers = userRepository.findByEnabledFalse();
     * disabledUsers.stream()
     *     .filter(user -> user.getUpdatedAt().isBefore(LocalDateTime.now().minusMonths(6)))
     *     .forEach(user -> userRepository.delete(user));
     *
     * Example 3: Statistics
     * long disabledCount = userRepository.findByEnabledFalse().size();
     * System.out.println("Disabled accounts: " + disabledCount);
     */
    List<User> findByEnabledFalse();

    /**
     * Find users by first name or last name (case-insensitive)
     *
     * Method Naming Convention:
     * -------------------------
     * "findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase"
     * - "Containing": SQL LIKE with wildcards (%keyword%)
     * - "IgnoreCase": Case-insensitive search (LOWER())
     * - "Or": Combines conditions with OR
     *
     * Generated SQL:
     * --------------
     * SELECT * FROM users
     * WHERE LOWER(first_name) LIKE LOWER(?)
     * OR LOWER(last_name) LIKE LOWER(?)
     *
     * @param firstName Keyword to search in first name
     * @param lastName Keyword to search in last name
     * @return List<User> - Users matching the search criteria
     *
     * Usage Example:
     * --------------
     * // Search for "john" in first or last name
     * List<User> users = userRepository
     *     .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase("john", "john");
     *
     * Results:
     * - "John Doe" ✓ (first name match)
     * - "Jane Johnson" ✓ (last name match)
     * - "JOHNNY Smith" ✓ (case-insensitive)
     * - "Mike Jones" ✗ (no match)
     *
     * Use Cases:
     * ----------
     * - Admin user search
     * - Customer lookup
     * - Autocomplete suggestions
     * - User directory search
     */
    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
        String firstName,
        String lastName
    );

    /**
     * Find users by role
     *
     * Custom JPQL Query:
     * ------------------
     * This query is more complex, so we use @Query annotation
     *
     * @Query annotation allows writing custom JPQL queries
     * JPQL (Java Persistence Query Language) is object-oriented query language
     *
     * Query Explanation:
     * ------------------
     * SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName
     *
     * - "User u": Select from User entity (alias 'u')
     * - "JOIN u.roles r": Join with roles collection (alias 'r')
     * - "WHERE r.name = :roleName": Filter by role name
     * - ":roleName": Named parameter (value from method parameter)
     *
     * Why use JOIN?
     * -------------
     * - User and Role have Many-to-Many relationship
     * - Need to traverse relationship to filter by role name
     * - JOIN combines data from both tables
     *
     * @param roleName The role name to filter by (e.g., "ROLE_ADMIN")
     * @return List<User> - All users with the specified role
     *
     * Usage Examples:
     * ---------------
     *
     * Example 1: Get all admins
     * List<User> admins = userRepository.findByRoleName("ROLE_ADMIN");
     *
     * Example 2: Send admin notification
     * List<User> admins = userRepository.findByRoleName("ROLE_ADMIN");
     * admins.forEach(admin ->
     *     emailService.sendAdminNotification(admin.getEmail(), "Important update")
     * );
     *
     * Example 3: Count users by role
     * long adminCount = userRepository.findByRoleName("ROLE_ADMIN").size();
     * long userCount = userRepository.findByRoleName("ROLE_USER").size();
     *
     * Example 4: Check if user has role
     * User currentUser = getCurrentUser();
     * boolean isAdmin = userRepository.findByRoleName("ROLE_ADMIN")
     *     .contains(currentUser);
     *
     * Performance Note:
     * -----------------
     * - Uses JOIN which can be expensive for large datasets
     * - Consider caching if called frequently
     * - For checking single user's role, use user.getRoles() instead
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    /**
     * Count users by role
     *
     * Custom JPQL Query for counting:
     * --------------------------------
     * SELECT COUNT(u) instead of SELECT u
     *
     * @param roleName The role name to count
     * @return long - Number of users with the specified role
     *
     * Usage Example:
     * --------------
     * long adminCount = userRepository.countByRoleName("ROLE_ADMIN");
     * long userCount = userRepository.countByRoleName("ROLE_USER");
     *
     * System.out.println("Total admins: " + adminCount);
     * System.out.println("Total users: " + userCount);
     *
     * Why separate count query?
     * -------------------------
     * - More efficient than loading all users and calling .size()
     * - Database only counts, doesn't return data
     * - Faster for large datasets
     *
     * Performance Comparison:
     * -----------------------
     * userRepository.findByRoleName("ROLE_ADMIN").size():
     * - Loads all admin users from database
     * - Loads all fields, roles, etc.
     * - Counts in Java
     * - Slow for many users
     *
     * userRepository.countByRoleName("ROLE_ADMIN"):
     * - Executes COUNT query in database
     * - No data transfer
     * - Returns single number
     * - Fast regardless of user count
     */
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName")
    long countByRoleName(@Param("roleName") String roleName);

    // =========================================================================
    // Additional Custom Queries (for future use)
    // =========================================================================
    //
    // Example 1: Find users created after a certain date
    // List<User> findByCreatedAtAfter(LocalDateTime date);
    //
    // Example 2: Find users by email domain
    // @Query("SELECT u FROM User u WHERE u.email LIKE %:domain")
    // List<User> findByEmailDomain(@Param("domain") String domain);
    // Usage: findByEmailDomain("@gmail.com")
    //
    // Example 3: Find users with multiple roles
    // @Query("SELECT u FROM User u WHERE SIZE(u.roles) > :count")
    // List<User> findUsersWithMultipleRoles(@Param("count") int count);
    //
    // Example 4: Find recently updated users
    // List<User> findByUpdatedAtAfter(LocalDateTime date);
    //
    // Example 5: Search by phone number
    // Optional<User> findByPhone(String phone);
    //
    // =========================================================================
    // Best Practices
    // =========================================================================
    //
    // 1. Email Handling:
    //    - Always convert to lowercase before querying
    //    - Store emails in lowercase in database
    //    - Prevents case-sensitivity issues
    //
    // 2. Password Security:
    //    - NEVER query by password
    //    - NEVER return password in API responses
    //    - Use UserDetailsService for authentication
    //
    // 3. Performance:
    //    - Use existsByEmail() for existence checks
    //    - Use count queries instead of loading all data
    //    - Add indexes on frequently queried fields
    //
    // 4. Error Handling:
    //    - Always handle Optional.empty() case
    //    - Throw meaningful exceptions (UsernameNotFoundException, etc.)
    //    - Don't expose sensitive information in error messages
    //
    // 5. Pagination:
    //    - For large datasets, use Pageable
    //    - Example: Page<User> findAll(Pageable pageable)
    //    - Don't load thousands of users at once
}
