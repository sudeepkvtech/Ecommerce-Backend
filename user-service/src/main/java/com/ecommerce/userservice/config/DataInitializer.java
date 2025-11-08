package com.ecommerce.userservice.config;

// Import entity classes
import com.ecommerce.userservice.entity.Role;

// Import repository
import com.ecommerce.userservice.repository.RoleRepository;

// Import Spring annotations
import org.springframework.boot.CommandLineRunner;  // Runs code on application startup
import org.springframework.stereotype.Component;     // Makes this a Spring-managed bean

// Import Lombok
import lombok.RequiredArgsConstructor;

// Import logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data Initializer
 *
 * This component initializes default data when the application starts.
 * Currently, it creates default roles (ROLE_USER and ROLE_ADMIN) if they don't exist.
 *
 * What is CommandLineRunner?
 * ---------------------------
 * CommandLineRunner is a Spring Boot interface that provides a single method:
 * run(String... args)
 *
 * This method is executed automatically when the application starts,
 * AFTER all Spring beans are initialized but BEFORE the application is ready to serve requests.
 *
 * Why use CommandLineRunner?
 * ---------------------------
 * 1. DATABASE INITIALIZATION: Create default data needed for application to work
 * 2. DATA MIGRATION: Transform or migrate data on startup
 * 3. VALIDATION: Check that database is in correct state
 * 4. CLEANUP: Remove temporary or stale data
 * 5. WARMUP: Pre-load caches or perform initial computations
 *
 * When does this run?
 * -------------------
 * Application startup sequence:
 * 1. Load configuration (application.yml)
 * 2. Initialize Spring beans (services, repositories, etc.)
 * 3. Connect to database
 * 4. Run JPA schema generation (if enabled)
 * 5. Execute CommandLineRunner.run() ← THIS RUNS HERE
 * 6. Application ready to accept requests
 *
 * Why initialize roles here?
 * --------------------------
 * Roles must exist before users can register:
 * - Registration assigns ROLE_USER to new users
 * - If ROLE_USER doesn't exist, registration fails
 * - Creating roles here ensures they exist when needed
 *
 * Alternative approaches:
 * -----------------------
 * 1. Manual SQL script:
 *    INSERT INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN');
 *    Problem: Must remember to run script, easy to forget
 *
 * 2. Database migration tool (Flyway, Liquibase):
 *    Good for production, but adds complexity for small projects
 *
 * 3. Application initialization (this approach):
 *    ✓ Automatic, runs every time
 *    ✓ Idempotent (safe to run multiple times)
 *    ✓ No manual steps required
 *
 * Idempotency:
 * ------------
 * This code is idempotent - running it multiple times has the same effect as running once:
 * - First run: Roles don't exist → Creates them
 * - Subsequent runs: Roles exist → Skips creation
 * - Result: Always have exactly 2 roles, never duplicates
 */

// @Component marks this as a Spring bean
// Spring will automatically detect and instantiate it on startup
@Component

// @RequiredArgsConstructor (Lombok) generates constructor for final fields
// Enables dependency injection of RoleRepository
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    // Logger for logging startup messages
    // Helps track initialization and debug issues
    // SLF4J (Simple Logging Facade for Java) is the logging API
    // Logback is the actual logging implementation (included in Spring Boot)
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    // RoleRepository dependency for database operations
    // Injected via constructor by Spring
    private final RoleRepository roleRepository;

    /**
     * Run method - executed on application startup
     *
     * This method is called by Spring Boot after all beans are initialized.
     * It creates default roles if they don't already exist.
     *
     * @param args Command-line arguments passed to the application
     *             (same args from public static void main(String[] args))
     *             Usually empty for web applications
     *
     * @throws Exception Any exception that occurs during initialization
     *                   If exception is thrown, application startup fails
     */
    @Override
    public void run(String... args) throws Exception {
        // Log startup message
        // INFO level: Normal informational messages
        // Shows in console and log files
        logger.info("Starting data initialization...");

        // Initialize roles
        // This method creates ROLE_USER and ROLE_ADMIN if they don't exist
        initializeRoles();

        // Log completion message
        logger.info("Data initialization completed successfully");

        // Note: Could add more initialization methods here:
        // initializeDefaultAdmin();
        // initializeCategories();
        // etc.
    }

    /**
     * Initialize default roles
     *
     * This method creates two roles if they don't exist:
     * - ROLE_USER: Assigned to all registered users
     * - ROLE_ADMIN: Assigned to administrators
     *
     * Why these specific role names?
     * -------------------------------
     * Spring Security expects role names to start with "ROLE_" prefix:
     * - When checking permissions: hasRole('USER')
     * - Spring Security adds prefix: checks for 'ROLE_USER'
     * - Database stores: 'ROLE_USER'
     *
     * This is a Spring Security convention for distinguishing roles from authorities.
     *
     * Idempotency:
     * ------------
     * This method is safe to run multiple times:
     * - Checks if role exists before creating
     * - If exists: Logs message, skips creation
     * - If doesn't exist: Creates role
     * - Result: Roles exist after running (whether created now or previously)
     */
    private void initializeRoles() {
        // Create ROLE_USER if it doesn't exist
        // This role is assigned to all registered users
        createRoleIfNotExists("ROLE_USER");

        // Create ROLE_ADMIN if it doesn't exist
        // This role is assigned to administrators
        // Admins can view all users, activate/deactivate accounts, etc.
        createRoleIfNotExists("ROLE_ADMIN");

        // Note: Could add more roles here if needed:
        // createRoleIfNotExists("ROLE_MODERATOR");
        // createRoleIfNotExists("ROLE_MANAGER");
    }

    /**
     * Create a role if it doesn't already exist
     *
     * This method checks if a role exists in the database.
     * If it exists, logs a message and does nothing.
     * If it doesn't exist, creates and saves the role.
     *
     * @param roleName The name of the role to create (e.g., "ROLE_USER")
     *
     * Example execution:
     * ------------------
     * First application startup (empty database):
     * 1. Check: ROLE_USER exists? → No
     * 2. Create new Role(name="ROLE_USER")
     * 3. Save to database → ID auto-generated
     * 4. Log: "Created role: ROLE_USER"
     *
     * Subsequent startups (roles exist):
     * 1. Check: ROLE_USER exists? → Yes
     * 2. Log: "Role already exists: ROLE_USER"
     * 3. Skip creation
     *
     * Database state after first run:
     * -------------------------------
     * Table: roles
     * +----+------------+
     * | id | name       |
     * +----+------------+
     * | 1  | ROLE_USER  |
     * | 2  | ROLE_ADMIN |
     * +----+------------+
     */
    private void createRoleIfNotExists(String roleName) {
        // Check if role already exists in database
        // roleRepository.existsByName() executes:
        // SELECT COUNT(*) FROM roles WHERE name = ?
        // Returns true if count > 0, false if count = 0
        if (!roleRepository.existsByName(roleName)) {
            // Role doesn't exist - create it

            // Create new Role entity
            Role role = new Role();

            // Set the role name
            // This is the only field we need to set
            // ID is auto-generated by database
            role.setName(roleName);

            // Save role to database
            // JPA will:
            // 1. Generate INSERT SQL:
            //    INSERT INTO roles (name) VALUES (?)
            // 2. Execute SQL with roleName parameter
            // 3. Get auto-generated ID from database
            // 4. Set ID on role object
            // 5. Return the saved role
            roleRepository.save(role);

            // Log success message
            // Shows in console and logs
            // Helps confirm initialization worked
            logger.info("Created role: {}", roleName);
        } else {
            // Role already exists - skip creation

            // Log informational message
            // Not an error - this is expected on subsequent startups
            logger.info("Role already exists: {}", roleName);
        }
    }

    // =========================================================================
    // Additional Initialization Methods (Optional)
    // =========================================================================
    //
    // You can add more initialization methods here as needed:
    //
    // /**
    //  * Initialize default admin user
    //  *
    //  * Creates a default admin account if it doesn't exist.
    //  * Useful for initial setup and testing.
    //  */
    // private void initializeDefaultAdmin() {
    //     String adminEmail = "admin@example.com";
    //
    //     if (!userRepository.existsByEmail(adminEmail)) {
    //         User admin = new User();
    //         admin.setFirstName("Admin");
    //         admin.setLastName("User");
    //         admin.setEmail(adminEmail);
    //         admin.setPassword(passwordEncoder.encode("admin123"));  // Change in production!
    //         admin.setEnabled(true);
    //
    //         // Assign ROLE_ADMIN
    //         Role adminRole = roleRepository.findByName("ROLE_ADMIN")
    //             .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));
    //         admin.setRoles(Set.of(adminRole));
    //
    //         userRepository.save(admin);
    //         logger.info("Created default admin user: {}", adminEmail);
    //     }
    // }
    //
    // /**
    //  * Initialize sample data for testing
    //  *
    //  * Creates sample users, addresses, etc. for development/testing.
    //  * Should only run in dev/test environments, not production.
    //  */
    // @Profile({"dev", "test"})  // Only run in dev/test environments
    // private void initializeSampleData() {
    //     // Create sample users
    //     // Create sample addresses
    //     // etc.
    // }
    //
    // =========================================================================
    // Error Handling
    // =========================================================================
    //
    // What happens if initialization fails?
    // --------------------------------------
    // If this method throws an exception:
    // 1. Spring Boot catches the exception
    // 2. Logs the error with stack trace
    // 3. Application startup FAILS
    // 4. Application does not start serving requests
    // 5. Process exits with error code
    //
    // This is intentional:
    // - If roles can't be created, app can't function properly
    // - Better to fail fast than run with missing data
    // - Alerts developers to configuration/database issues
    //
    // Common causes of initialization failure:
    // ----------------------------------------
    // 1. Database not running: Connection refused
    // 2. Wrong database credentials: Authentication failed
    // 3. Database schema mismatch: Table doesn't exist
    // 4. Network issues: Timeout connecting to database
    //
    // Solution: Fix the underlying issue, restart application
    //
    // =========================================================================
    // Logging Levels
    // =========================================================================
    //
    // SLF4J logging levels (from most to least severe):
    //
    // ERROR: Serious problems, application may not work correctly
    //   logger.error("Database connection failed", exception);
    //
    // WARN: Potentially harmful situations, but application continues
    //   logger.warn("Using default configuration, none specified");
    //
    // INFO: Informational messages highlighting application progress
    //   logger.info("Created role: ROLE_USER");  ← We use this
    //
    // DEBUG: Detailed diagnostic information useful for debugging
    //   logger.debug("Checking if role exists: {}", roleName);
    //
    // TRACE: Very detailed diagnostic information
    //   logger.trace("SQL: SELECT * FROM roles WHERE name = ?");
    //
    // Configuration (in application.yml):
    // -----------------------------------
    // logging:
    //   level:
    //     com.ecommerce.userservice: INFO  # Show INFO and above
    //     root: INFO  # Default level for all packages
    //
    // In production: Set to WARN or ERROR (less verbose)
    // In development: Set to DEBUG or TRACE (more verbose)
}
