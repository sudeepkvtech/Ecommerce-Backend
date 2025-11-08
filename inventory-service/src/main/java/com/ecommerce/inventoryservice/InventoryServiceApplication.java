package com.ecommerce.inventoryservice;

// Import Spring Boot annotations
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Import Eureka client annotation
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Inventory Service Application
 *
 * Main entry point for the Inventory Service microservice.
 * This service manages product inventory and stock levels for the e-commerce platform.
 *
 * What does Inventory Service do?
 * ================================
 * 1. Track Stock Levels:
 *    - Monitor available quantity for each product
 *    - Real-time stock updates
 *    - Low stock alerts
 *
 * 2. Stock Reservations:
 *    - Reserve items when order is placed
 *    - Release reservations if payment fails
 *    - Commit reservations when payment succeeds
 *
 * 3. Stock Movements:
 *    - Record all inventory changes
 *    - Track reasons for changes (purchase, sale, return, adjustment)
 *    - Audit trail for inventory management
 *
 * 4. Stock Management:
 *    - Add stock (new products, restocking)
 *    - Reduce stock (sales, damages)
 *    - Adjust stock (corrections, audits)
 *    - Transfer stock (between warehouses - future)
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
 *   1. @Configuration: Marks class as configuration source
 *   2. @EnableAutoConfiguration: Auto-configures beans based on classpath
 *   3. @ComponentScan: Scans for Spring components
 *
 * @EnableDiscoveryClient:
 * - Enables service discovery with Eureka Server
 * - Registers this service with Eureka on startup
 * - Allows other services to discover Inventory Service
 *
 * Database
 * ========
 * - Database name: inventorydb
 * - Port: 3310 (different from other services)
 * - Tables:
 *   - inventory: Current stock levels per product
 *   - stock_movements: History of all stock changes
 *
 * API Endpoints (to be implemented)
 * =================================
 * - GET    /api/inventory/{productId}           - Get stock level
 * - POST   /api/inventory/{productId}/reserve   - Reserve stock
 * - POST   /api/inventory/{productId}/release   - Release reservation
 * - POST   /api/inventory/{productId}/commit    - Commit reservation
 * - POST   /api/inventory/{productId}/add       - Add stock (admin)
 * - POST   /api/inventory/{productId}/reduce    - Reduce stock (admin)
 * - GET    /api/inventory/low-stock             - Get low stock items (admin)
 * - GET    /api/inventory/movements/{productId} - Get stock movement history (admin)
 *
 * Integration with Other Services
 * ===============================
 * - Product Service:
 *   - Inventory created when new product is added
 *   - Stock levels displayed with product details
 *
 * - Order Service:
 *   - Reserves stock when order is placed
 *   - Releases stock if payment fails or order is cancelled
 *   - Commits stock when payment succeeds
 *
 * - Payment Service:
 *   - Notifies Inventory Service when payment succeeds/fails
 *   - Inventory updates stock accordingly
 *
 * Port Configuration
 * ==================
 * - Inventory Service: 8085
 * - Product Service: 8081
 * - User Service: 8082
 * - Order Service: 8083
 * - Payment Service: 8084
 * - Eureka Server: 8761
 *
 * How to Run
 * ==========
 * 1. Start Eureka Server (port 8761)
 * 2. Start MySQL on port 3310
 * 3. Run this application:
 *    - IDE: Run this main method
 *    - Maven: mvn spring-boot:run
 *    - JAR: java -jar inventory-service.jar
 * 4. Service registers with Eureka automatically
 * 5. Access API at http://localhost:8085/api/inventory
 *
 * Docker Support
 * ==============
 * Can be containerized with Docker:
 * - Dockerfile builds the application
 * - docker-compose.yml orchestrates MySQL + Inventory Service
 * - Isolated network for service communication
 */

// @SpringBootApplication enables auto-configuration and component scanning
@SpringBootApplication

// @EnableDiscoveryClient enables Eureka service registration
@EnableDiscoveryClient
public class InventoryServiceApplication {

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
     * 5. Starts embedded Tomcat server on port 8085
     * 6. Connects to Eureka Server and registers as "inventory-service"
     * 7. Application is ready to accept HTTP requests
     *
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {
        // Start Spring Boot application
        // Returns ApplicationContext (Spring container with all beans)
        SpringApplication.run(InventoryServiceApplication.class, args);

        // After this line executes, the application is running and ready
        // Console will show:
        // - Tomcat started on port(s): 8085
        // - DiscoveryClient registered with Eureka Server
        // - Application startup completed
    }

    /**
     * Future Enhancements
     * ===================
     *
     * 1. Multi-Warehouse Support:
     *    - Track stock across multiple warehouses
     *    - Stock transfer between warehouses
     *    - Warehouse-specific reservations
     *
     * 2. Advanced Features:
     *    - Automatic reordering when stock is low
     *    - Stock forecasting based on sales trends
     *    - Batch operations for stock updates
     *    - Stock alerts and notifications
     *
     * 3. Integration:
     *    - Real-time stock sync with physical warehouses
     *    - Third-party logistics integration
     *    - Supplier integration for automatic ordering
     *
     * 4. Analytics:
     *    - Stock turnover rate
     *    - Most/least popular products
     *    - Seasonal demand patterns
     *    - Stock value reporting
     *
     * 5. Safety Stock:
     *    - Minimum stock levels per product
     *    - Buffer stock for high-demand items
     *    - Safety stock calculations
     */
}
