// Package declaration - defines the namespace for this class
package com.ecommerce.userservice;

// Import Spring Boot's main annotation for auto-configuration
import org.springframework.boot.SpringApplication;
// Import annotation that marks this as a Spring Boot application
import org.springframework.boot.autoconfigure.SpringBootApplication;
// Import annotation to enable Eureka client functionality
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main Application Class for User Service
 *
 * This is the entry point of the Spring Boot application.
 * When you run this class, it starts the entire User Service microservice.
 *
 * User Service Responsibilities:
 * ------------------------------
 * 1. User Registration - Create new user accounts
 * 2. User Authentication - Login with email/password, generate JWT token
 * 3. User Authorization - Role-based access control (RBAC)
 * 4. User Profile Management - Update user information, addresses
 * 5. Password Management - Change password, reset password
 * 6. Service Discovery - Register with Eureka Server
 *
 * Key Features:
 * -------------
 * - JWT-based authentication (stateless, scalable)
 * - BCrypt password hashing (secure password storage)
 * - Role-based authorization (USER, ADMIN roles)
 * - Spring Security integration
 * - Eureka service registration
 * - RESTful API endpoints
 */

// @SpringBootApplication is a convenience annotation that combines:
// 1. @Configuration - Marks this class as a source of bean definitions
// 2. @EnableAutoConfiguration - Tells Spring Boot to automatically configure beans based on classpath
// 3. @ComponentScan - Tells Spring to scan this package and sub-packages for components
@SpringBootApplication

// @EnableDiscoveryClient enables this service to register with Eureka Server
// What happens when this annotation is present:
// 1. On startup, this service will contact Eureka Server at http://localhost:8761/eureka/
// 2. It will register itself with the name "user-service" (from application.yml)
// 3. It will send heartbeat signals every 30 seconds to Eureka Server
// 4. Other services can discover this service by asking Eureka "where is user-service?"
// 5. If this service crashes, Eureka will detect missing heartbeats and mark it as DOWN
//
// Why Service Discovery for User Service?
// ----------------------------------------
// - Order Service needs to validate users before creating orders
// - Payment Service needs to get user billing information
// - Notification Service needs user email/phone for sending notifications
// - Instead of hard-coding URLs, services discover user-service via Eureka
//
// Example: Order Service calling User Service
// --------------------------------------------
// Without Eureka (hard-coded URL):
// String userServiceUrl = "http://localhost:8082/user-service/api/users/1";
//
// With Eureka (service discovery):
// String userServiceUrl = "http://user-service/user-service/api/users/1";
// Spring Cloud LoadBalancer resolves "user-service" to actual instance
@EnableDiscoveryClient
public class UserServiceApplication {

    /**
     * Main method - the entry point of the Java application
     *
     * @param args Command line arguments passed when starting the application
     *
     * Execution flow:
     * 1. JVM starts and calls this main method
     * 2. SpringApplication.run() is called with:
     *    - UserServiceApplication.class: The main configuration class
     *    - args: Command line arguments
     * 3. Spring Boot starts initializing:
     *    - Loads application.yml configuration
     *    - Initializes database connection to MySQL
     *    - Creates all Spring beans (Controllers, Services, Repositories)
     *    - Initializes Spring Security (authentication/authorization)
     *    - Starts embedded Tomcat server on port 8082
     *    - Registers with Eureka Server
     * 4. Application is ready to handle HTTP requests
     *
     * Security Initialization:
     * -----------------------
     * On startup, Spring Security:
     * - Loads SecurityConfig.java configuration
     * - Sets up JWT authentication filter
     * - Configures which endpoints are public (e.g., /api/auth/login)
     * - Configures which endpoints require authentication
     * - Sets up password encoder (BCrypt)
     * - Initializes UserDetailsService for loading users
     *
     * To run this application:
     * - From IDE: Right-click and select "Run"
     * - From command line: mvn spring-boot:run
     * - From JAR: java -jar user-service-1.0.0.jar
     */
    public static void main(String[] args) {
        // SpringApplication.run() bootstraps the Spring Boot application
        // It returns an ApplicationContext which contains all Spring beans
        SpringApplication.run(UserServiceApplication.class, args);

        // After this line executes successfully, you'll see logs like:
        // "Started UserServiceApplication in X.XXX seconds"
        // "Tomcat started on port(s): 8082"
        // "Registered with Eureka Server"
        //
        // The service is now:
        // - Listening for HTTP requests on port 8082
        // - Registered with Eureka Server (visible at http://localhost:8761)
        // - Ready to authenticate users and generate JWT tokens
    }
}
