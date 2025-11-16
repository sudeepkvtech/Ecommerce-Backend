// Package declaration - defines the namespace for this class
package com.ecommerce.productservice;

// Import Spring Boot's main annotation for auto-configuration
import org.springframework.boot.SpringApplication;
// Import annotation that marks this as a Spring Boot application
import org.springframework.boot.autoconfigure.SpringBootApplication;
// Import annotation to enable Eureka client functionality
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main Application Class for Product Service
 *
 * This is the entry point of the Spring Boot application.
 * When you run this class, it starts the entire Product Service microservice.
 *
 * Key Responsibilities:
 * 1. Bootstraps the Spring Boot application
 * 2. Enables auto-configuration of Spring components
 * 3. Starts embedded Tomcat server on port 8081
 * 4. Registers this service with Eureka Server for service discovery
 * 5. Scans all packages under com.ecommerce.productservice for Spring components
 */

// @SpringBootApplication is a convenience annotation that combines:
// 1. @Configuration - Marks this class as a source of bean definitions
// 2. @EnableAutoConfiguration - Tells Spring Boot to automatically configure beans based on classpath
// 3. @ComponentScan - Tells Spring to scan this package and sub-packages for components
@SpringBootApplication

// @EnableDiscoveryClient enables this service to register with Eureka Server
// What happens when this annotation is present:
// 1. On startup, this service will contact Eureka Server at http://localhost:8761/eureka/
// 2. It will register itself with the name "product-service" (from application.yml)
// 3. It will send heartbeat signals every 30 seconds to Eureka Server
// 4. Other services can discover this service by asking Eureka "where is product-service?"
// 5. If this service crashes, Eureka will detect missing heartbeats and mark it as DOWN
//
// Benefits of Service Discovery:
// - No need to hardcode URLs of other services
// - Automatic load balancing when multiple instances exist
// - Health monitoring and failover support
@EnableDiscoveryClient
public class ProductServiceApplication {

    /**
     * Main method - the entry point of the Java application
     *
     * @param args Command line arguments passed when starting the application
     *
     * Execution flow:
     * 1. JVM starts and calls this main method
     * 2. SpringApplication.run() is called with:
     *    - ProductServiceApplication.class: The main configuration class
     *    - args: Command line arguments
     * 3. Spring Boot starts initializing:
     *    - Loads application.yml configuration
     *    - Initializes database connection to MySQL
     *    - Creates all Spring beans (Controllers, Services, Repositories)
     *    - Starts embedded Tomcat server on port 8081
     *    - Registers with Eureka Server
     * 4. Application is ready to handle HTTP requests
     *
     * To run this application:
     * - From IDE: Right-click and select "Run"
     * - From command line: mvn spring-boot:run
     * - From JAR: java -jar product-service-1.0.0.jar
     */
    public static void main(String[] args) {
        // SpringApplication.run() bootstraps the Spring Boot application
        // It returns an ApplicationContext which contains all Spring beans
        SpringApplication.run(ProductServiceApplication.class, args);

        // After this line executes successfully, you'll see logs like:
        // "Started ProductServiceApplication in X.XXX seconds"
        // "Tomcat started on port(s): 8081"
        // "Registered with Eureka Server"
    }
}
