package com.ecommerce.orderservice;

// Import Spring Boot annotations
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Order Service Application
 *
 * Main entry point for the Order Service microservice.
 * This service handles order management, order items, and order status tracking.
 *
 * Port: 8083
 * Database: MySQL (port 3308, database: orderdb)
 *
 * Features:
 * - Create and manage orders
 * - Track order status (PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED)
 * - Order item management
 * - Integration with User Service and Product Service
 * - JWT authentication
 * - Eureka service discovery
 *
 * @SpringBootApplication:
 * This is a meta-annotation that combines three annotations:
 * 1. @Configuration: Marks this as a configuration class
 * 2. @EnableAutoConfiguration: Auto-configure based on dependencies
 * 3. @ComponentScan: Scan for components in this package and sub-packages
 *
 * @EnableDiscoveryClient:
 * Enables service discovery with Eureka Server
 * - Registers this service with Eureka at startup
 * - Service name: "order-service" (from application.yml)
 * - Allows other services to discover this service
 */
@SpringBootApplication  // Makes this a Spring Boot application
@EnableDiscoveryClient  // Enables Eureka service discovery
public class OrderServiceApplication {

    /**
     * Main method - application entry point
     *
     * This method is executed when you run:
     * - java -jar order-service.jar
     * - mvn spring-boot:run
     * - IDE run button
     *
     * @param args Command-line arguments (usually empty for web applications)
     *
     * Startup sequence:
     * 1. Load application.yml configuration
     * 2. Auto-configure based on dependencies (JPA, Security, etc.)
     * 3. Scan for @Component, @Service, @Repository, @Controller classes
     * 4. Initialize Spring beans
     * 5. Connect to MySQL database
     * 6. Register with Eureka Server
     * 7. Start embedded Tomcat server on port 8083
     * 8. Application ready to accept HTTP requests
     */
    public static void main(String[] args) {
        // SpringApplication.run() starts the Spring Boot application
        // Parameters:
        // 1. OrderServiceApplication.class - Configuration class
        // 2. args - Command-line arguments passed to main()
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
