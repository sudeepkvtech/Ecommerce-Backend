// Package declaration - this class belongs to the entity package
package com.ecommerce.userservice.entity;

// Import JPA annotations for database mapping
import jakarta.persistence.*;
// Import Lombok annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Import Java time class for timestamps
import java.time.LocalDateTime;
// Import collections for roles and addresses
import java.util.HashSet;
import java.util.Set;

/**
 * User Entity Class
 *
 * Represents a user account in the e-commerce system
 * This class is mapped to a database table called "users"
 *
 * Database Table Structure:
 * --------------------------
 * | id | first_name | last_name | email | password | phone | enabled | created_at | updated_at |
 * --------------------------
 *
 * Relationships:
 * - Many-to-Many with Role (user can have multiple roles)
 * - One-to-Many with Address (user can have multiple addresses)
 *
 * Security Features:
 * ------------------
 * - Password is stored as BCrypt hash (never plain text)
 * - Email is unique (used as username for login)
 * - Enabled flag for account activation/deactivation
 * - Timestamps track when user registered and last updated
 */

// @Entity marks this class as a JPA entity
@Entity

// @Table specifies the table name and indexes
@Table(
    name = "users",
    indexes = {
        // Index on email for fast login queries
        // SELECT * FROM users WHERE email = ?
        @Index(name = "idx_email", columnList = "email"),

        // Index on phone for user lookup by phone
        @Index(name = "idx_phone", columnList = "phone")
    }
)

// @Data generates getters, setters, toString, equals, hashCode
@Data

// @NoArgsConstructor generates a no-argument constructor
@NoArgsConstructor

// @AllArgsConstructor generates a constructor with all fields
@AllArgsConstructor
public class User {

    /**
     * Primary Key - Unique identifier for each user
     *
     * @Id marks this field as the primary key
     * @GeneratedValue with IDENTITY strategy means database auto-increments
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * First Name
     *
     * @Column constraints:
     * - nullable = false: First name is required
     * - length = 100: Maximum 100 characters
     *
     * Used for:
     * - Display name in UI
     * - Personalized greetings: "Hello, John!"
     * - Shipping labels
     * - Email salutations
     */
    @Column(nullable = false, length = 100)
    private String firstName;

    /**
     * Last Name
     *
     * @Column constraints:
     * - nullable = false: Last name is required
     * - length = 100: Maximum 100 characters
     *
     * Combined with firstName for:
     * - Full name display: "John Doe"
     * - Formal communications
     * - Shipping labels
     */
    @Column(nullable = false, length = 100)
    private String lastName;

    /**
     * Email Address
     *
     * @Column constraints:
     * - nullable = false: Email is required
     * - unique = true: No two users can have the same email
     * - length = 255: Standard email length
     *
     * Purpose:
     * --------
     * - Used as username for login (authentication)
     * - Primary contact method
     * - Password reset
     * - Order confirmations
     * - Marketing communications
     *
     * Why unique?
     * -----------
     * - One account per email address
     * - Prevents duplicate registrations
     * - Used as unique identifier for login
     *
     * Validation:
     * -----------
     * Should match email format: user@example.com
     * Validated in DTO layer with @Email annotation
     *
     * Security Note:
     * --------------
     * Email should be stored in lowercase to prevent case-sensitivity issues
     * Example: User@Example.com → user@example.com
     */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Password Hash
     *
     * @Column constraints:
     * - nullable = false: Password is required
     * - length = 255: BCrypt hash length
     *
     * CRITICAL SECURITY:
     * ------------------
     * - NEVER store plain text passwords!
     * - This field stores BCrypt hash, not the actual password
     * - BCrypt automatically includes salt (prevents rainbow table attacks)
     * - Hash is one-way (cannot reverse to get original password)
     *
     * Example:
     * --------
     * Plain password: "myPassword123"
     * BCrypt hash: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
     *
     * How it works:
     * -------------
     * Registration:
     * 1. User provides password: "myPassword123"
     * 2. Server hashes with BCrypt: passwordEncoder.encode("myPassword123")
     * 3. Hash stored in database: "$2a$10$N9qo8uLOickgx..."
     *
     * Login:
     * 1. User provides password: "myPassword123"
     * 2. Server loads hash from database
     * 3. BCrypt compares: passwordEncoder.matches("myPassword123", storedHash)
     * 4. Returns true if match, false otherwise
     *
     * Why BCrypt?
     * -----------
     * - Intentionally slow (prevents brute force attacks)
     * - Automatic salt generation
     * - Adaptive (can increase rounds as CPUs get faster)
     * - Industry standard for password hashing
     *
     * Password Requirements (enforced in DTO):
     * -----------------------------------------
     * - Minimum 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character
     */
    @Column(nullable = false, length = 255)
    private String password;

    /**
     * Phone Number
     *
     * @Column constraints:
     * - length = 20: Accommodates international formats
     * - nullable: Optional (can be null)
     *
     * Format Examples:
     * ----------------
     * - US: +1-555-123-4567
     * - UK: +44-20-7123-4567
     * - India: +91-98765-43210
     *
     * Used for:
     * ---------
     * - SMS notifications
     * - Order status updates
     * - Two-factor authentication (2FA)
     * - Delivery coordination
     *
     * Validation:
     * -----------
     * Should match phone number pattern in DTO
     * Can use @Pattern annotation with regex
     */
    @Column(length = 20)
    private String phone;

    /**
     * Account Enabled Flag
     *
     * Indicates whether the user account is active
     * - true: Account is active (user can login)
     * - false: Account is disabled (user cannot login)
     *
     * Default value: true (new accounts are active by default)
     *
     * Use Cases:
     * ----------
     * 1. Email Verification:
     *    - Set enabled = false on registration
     *    - Send verification email
     *    - Set enabled = true after email verified
     *
     * 2. Account Suspension:
     *    - Admin can disable account for policy violations
     *    - Temporary ban
     *    - Fraud prevention
     *
     * 3. Account Deletion (Soft Delete):
     *    - Instead of deleting user, set enabled = false
     *    - Preserves order history and data integrity
     *    - Can be reactivated later
     *
     * Spring Security Integration:
     * ----------------------------
     * UserDetails interface has isEnabled() method
     * Spring Security checks this during authentication
     * If false, throws DisabledException
     */
    @Column(nullable = false)
    private Boolean enabled = true;

    /**
     * User Roles
     *
     * @ManyToMany defines a many-to-many relationship:
     * - One user can have many roles (USER + ADMIN)
     * - One role can belong to many users (many users have ROLE_USER)
     *
     * @JoinTable creates a join table to store the relationship
     * Join Table Structure:
     * ----------------------
     * user_roles table:
     * | user_id | role_id |
     * |---------|---------|
     * | 1       | 1       | ← User 1 has Role 1 (ROLE_USER)
     * | 1       | 2       | ← User 1 has Role 2 (ROLE_ADMIN)
     * | 2       | 1       | ← User 2 has Role 1 (ROLE_USER)
     *
     * @JoinTable parameters:
     * - name: Name of the join table
     * - joinColumns: Foreign key to User table
     * - inverseJoinColumns: Foreign key to Role table
     *
     * fetch = FetchType.EAGER:
     * - Loads roles immediately when loading user
     * - Important for Spring Security (needs roles for authorization)
     * - Alternative: LAZY would load roles only when accessed
     *
     * cascade = CascadeType.PERSIST:
     * - When saving a user, also persist new roles
     * - Does NOT delete roles if user is deleted (roles are shared)
     *
     * HashSet vs List:
     * ----------------
     * - HashSet prevents duplicate roles
     * - Order doesn't matter for roles
     * - Better performance for contains() checks
     *
     * Example Usage:
     * --------------
     * User user = new User();
     * Role userRole = roleRepository.findByName("ROLE_USER").get();
     * Role adminRole = roleRepository.findByName("ROLE_ADMIN").get();
     * user.setRoles(Set.of(userRole, adminRole));
     * // User now has both USER and ADMIN roles
     */
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinTable(
        name = "user_roles",  // Join table name
        joinColumns = @JoinColumn(name = "user_id"),  // Foreign key to users table
        inverseJoinColumns = @JoinColumn(name = "role_id")  // Foreign key to roles table
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * Created Timestamp
     *
     * When this user account was created (registration date)
     *
     * @Column properties:
     * - nullable = false: Must have a creation timestamp
     * - updatable = false: Once set, cannot be changed
     *
     * Use Cases:
     * ----------
     * - Display "Member since: January 2024"
     * - Calculate customer lifetime value
     * - Identify new vs. returning customers
     * - Analytics and reporting
     * - Account age verification
     *
     * Automatically set by @PrePersist callback
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Updated Timestamp
     *
     * When this user account was last modified
     *
     * Use Cases:
     * ----------
     * - Track when profile was last updated
     * - Detect inactive accounts
     * - Audit trail
     * - Cache invalidation
     *
     * Automatically updated by @PreUpdate callback
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Lifecycle Callback - Before Insert
     *
     * @PrePersist is called automatically before saving a NEW user to database
     * This method sets timestamps when user is first created
     *
     * Why automatic timestamps?
     * -------------------------
     * - Developers don't have to remember to set them
     * - Consistent across all user creations
     * - Prevents missing timestamps
     */
    @PrePersist
    protected void onCreate() {
        // Get current date and time
        LocalDateTime now = LocalDateTime.now();

        // Set both timestamps to current time
        // For a new user, created and updated times are the same
        createdAt = now;
        updatedAt = now;
    }

    /**
     * Lifecycle Callback - Before Update
     *
     * @PreUpdate is called automatically before updating an EXISTING user
     * This method updates the updatedAt timestamp
     *
     * Triggers when:
     * --------------
     * - User updates profile (name, phone, etc.)
     * - Admin changes user roles
     * - Password is changed
     * - Any field is modified
     *
     * Note: createdAt is NOT updated because it's marked as updatable = false
     */
    @PreUpdate
    protected void onUpdate() {
        // Update the updatedAt timestamp to current time
        // createdAt remains unchanged (preserves registration date)
        updatedAt = LocalDateTime.now();
    }

    // =========================================================================
    // Helper Methods (could be added if needed)
    // =========================================================================
    //
    // Get full name:
    // public String getFullName() {
    //     return firstName + " " + lastName;
    // }
    //
    // Add role:
    // public void addRole(Role role) {
    //     roles.add(role);
    // }
    //
    // Remove role:
    // public void removeRole(Role role) {
    //     roles.remove(role);
    // }
    //
    // Has role:
    // public boolean hasRole(String roleName) {
    //     return roles.stream()
    //         .anyMatch(role -> role.getName().equals(roleName));
    // }
    //
    // =========================================================================
    // Spring Security Integration
    // =========================================================================
    //
    // This entity is used with UserDetailsService:
    //
    // @Service
    // public class CustomUserDetailsService implements UserDetailsService {
    //     @Override
    //     public UserDetails loadUserByUsername(String email) {
    //         User user = userRepository.findByEmail(email)
    //             .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    //
    //         return org.springframework.security.core.userdetails.User
    //             .withUsername(user.getEmail())
    //             .password(user.getPassword())  // BCrypt hash
    //             .authorities(user.getRoles().stream()
    //                 .map(role -> new SimpleGrantedAuthority(role.getName()))
    //                 .toList())
    //             .accountExpired(false)
    //             .accountLocked(false)
    //             .credentialsExpired(false)
    //             .disabled(!user.getEnabled())
    //             .build();
    //     }
    // }
}
