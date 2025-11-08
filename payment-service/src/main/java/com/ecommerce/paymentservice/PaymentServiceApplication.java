package com.ecommerce.paymentservice;

// Import Spring Boot annotations
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Import Eureka client annotation
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Payment Service Application
 *
 * Main entry point for the Payment Service microservice.
 * This service handles payment processing and transaction management for e-commerce orders.
 *
 * What does Payment Service do?
 * ==============================
 * 1. Process Payments:
 *    - Accept payment requests from Order Service
 *    - Process different payment methods (credit card, PayPal, etc.)
 *    - Return payment confirmation or failure
 *
 * 2. Transaction Management:
 *    - Record all payment transactions
 *    - Track payment status (PENDING, PROCESSING, COMPLETED, FAILED)
 *    - Store payment method information
 *
 * 3. Refund Processing:
 *    - Handle refund requests
 *    - Process partial or full refunds
 *    - Update payment records
 *
 * 4. Payment History:
 *    - Retrieve payment details by order ID
 *    - Get payment history for a user
 *    - Track failed payment attempts
 *
 * Architecture
 * ============
 * This service follows a layered architecture:
 * - Controller: REST API endpoints (@RestController)
 * - Service: Business logic and validation
 * - Repository: Database access (Spring Data JPA)
 * - Entity: Database models with JPA annotations
 *
 * Annotations Explained:
 * ======================
 *
 * @SpringBootApplication:
 * - Meta-annotation that combines three important annotations:
 *   1. @Configuration: Marks class as configuration source (for @Bean definitions)
 *   2. @EnableAutoConfiguration: Enables Spring Boot's auto-configuration
 *      - Automatically configures beans based on classpath dependencies
 *      - Example: Detects MySQL driver → configures DataSource
 *      - Example: Detects Spring Web → configures DispatcherServlet
 *   3. @ComponentScan: Scans for Spring components in current package and sub-packages
 *      - Finds @Controller, @Service, @Repository, @Component classes
 *      - Registers them as Spring beans
 *
 * @EnableDiscoveryClient:
 * - Enables service discovery with Eureka Server
 * - Registers this service with Eureka Server on startup
 * - Allows other services to discover Payment Service
 * - How it works:
 *   1. On startup, connects to Eureka Server (localhost:8761)
 *   2. Registers with service name from application.yml
 *   3. Sends heartbeat every 30 seconds to stay registered
 *   4. Other services can discover this service via Eureka
 *   5. Enables load balancing and failover
 *
 * Service Discovery Benefits:
 * - No hardcoded URLs between services
 * - Automatic load balancing
 * - Service health monitoring
 * - Automatic failover if instance goes down
 * - Easy horizontal scaling
 *
 * Example: Order Service calling Payment Service
 * -----------------------------------------------
 * Without service discovery:
 *   String url = "http://localhost:8084/api/payments";  // Hardcoded!
 *   // What if port changes? What if we add more instances?
 *
 * With service discovery:
 *   String url = "http://payment-service/api/payments";  // Service name!
 *   // Eureka resolves to actual instance
 *   // Automatically load balances if multiple instances
 *   // Fails over to another instance if one is down
 *
 * Database
 * ========
 * - Database name: paymentdb
 * - Port: 3309 (different from other services)
 * - Tables:
 *   - payments: Payment transaction records
 *
 * API Endpoints (to be implemented)
 * =================================
 * - POST   /api/payments          - Process payment
 * - GET    /api/payments/{id}     - Get payment by ID
 * - GET    /api/payments/order/{orderId} - Get payment by order ID
 * - POST   /api/payments/{id}/refund - Process refund
 * - GET    /api/payments/user/{userId} - Get user's payment history
 *
 * Integration with Other Services
 * ===============================
 * - Order Service:
 *   - Receives payment requests when order is created
 *   - Returns payment confirmation or failure
 *   - Updates order status based on payment result
 *
 * - User Service:
 *   - Validates user exists
 *   - May retrieve payment methods stored for user
 *
 * Port Configuration
 * ==================
 * - Payment Service: 8084
 * - Product Service: 8081
 * - User Service: 8082
 * - Order Service: 8083
 * - Eureka Server: 8761
 *
 * How to Run
 * ==========
 * 1. Start Eureka Server (port 8761)
 * 2. Start MySQL on port 3309
 * 3. Run this application:
 *    - IDE: Run this main method
 *    - Maven: mvn spring-boot:run
 *    - JAR: java -jar payment-service.jar
 * 4. Service registers with Eureka automatically
 * 5. Access API at http://localhost:8084/api/payments
 *
 * Docker Support
 * ==============
 * Can be containerized with Docker:
 * - Dockerfile builds the application
 * - docker-compose.yml orchestrates MySQL + Payment Service
 * - Isolated network for service communication
 */

// @SpringBootApplication enables auto-configuration and component scanning
@SpringBootApplication

// @EnableDiscoveryClient enables Eureka service registration
@EnableDiscoveryClient
public class PaymentServiceApplication {

    /**
     * Main Method
     *
     * Application entry point. Starts the Spring Boot application.
     *
     * What happens when this runs:
     * ----------------------------
     * 1. SpringApplication.run() is called
     * 2. Spring Boot auto-configuration begins:
     *    a. Scans classpath for dependencies
     *    b. Configures beans automatically:
     *       - DataSource (MySQL connection)
     *       - EntityManagerFactory (JPA/Hibernate)
     *       - TransactionManager (database transactions)
     *       - DispatcherServlet (handles HTTP requests)
     *       - JwtAuthenticationFilter (security)
     *       - EurekaClient (service discovery)
     * 3. Component scanning finds all @Controller, @Service, @Repository
     * 4. Creates Spring beans and manages their lifecycle
     * 5. Starts embedded Tomcat server on port 8084
     * 6. Connects to Eureka Server and registers as "payment-service"
     * 7. Application is ready to accept HTTP requests
     *
     * @param args Command-line arguments (not used)
     *             Could be used for: --spring.profiles.active=prod
     */
    public static void main(String[] args) {
        // Start Spring Boot application
        // Returns ApplicationContext (Spring container with all beans)
        SpringApplication.run(PaymentServiceApplication.class, args);

        // After this line executes, the application is running and ready
        // Console will show:
        // - Tomcat started on port(s): 8084
        // - DiscoveryClient registered with Eureka Server
        // - Application startup completed
    }

    /**
     * Future Enhancements
     * ===================
     *
     * 1. Payment Gateway Integration:
     *    - Stripe API integration
     *    - PayPal API integration
     *    - Square API integration
     *
     * 2. Security:
     *    - PCI DSS compliance for card data
     *    - Tokenization for sensitive data
     *    - Encryption at rest and in transit
     *
     * 3. Fraud Detection:
     *    - Velocity checks (multiple attempts)
     *    - IP address validation
     *    - CVV verification
     *    - 3D Secure support
     *
     * 4. Webhooks:
     *    - Payment confirmation webhooks
     *    - Failed payment notifications
     *    - Refund notifications
     *
     * 5. Retry Logic:
     *    - Automatic retry for failed payments
     *    - Exponential backoff
     *    - Circuit breaker pattern
     *
     * 6. Reporting:
     *    - Daily payment summaries
     *    - Failed payment reports
     *    - Revenue analytics
     */
}
