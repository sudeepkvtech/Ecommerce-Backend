# Product Service - E-commerce Microservices

A comprehensive microservice for managing products and categories in an e-commerce platform, built with Spring Boot 3.2, MySQL, and Netflix Eureka for service discovery.

## Table of Contents

- [Overview](#overview)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Eureka Service Discovery](#eureka-service-discovery)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Configuration](#configuration)
- [Docker Setup](#docker-setup)
- [Testing](#testing)
- [Monitoring](#monitoring)

---

## Overview

The Product Service is a microservice responsible for:
- Managing product catalog (CRUD operations)
- Managing product categories
- Product search and filtering
- Inventory tracking (stock management)
- Product activation/deactivation (soft delete)

### Key Features

- RESTful API for product and category management
- Full CRUD operations with validation
- Advanced search and filtering capabilities
- Service discovery with Netflix Eureka
- MySQL database with JPA/Hibernate
- Docker support for easy deployment
- Comprehensive error handling
- API documentation with Swagger/OpenAPI
- Health checks and monitoring with Spring Actuator

---

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Programming language |
| Spring Boot | 3.2.0 | Application framework |
| Spring Data JPA | 3.2.0 | Database operations |
| Spring Cloud | 2023.0.0 | Microservices infrastructure |
| Netflix Eureka | 4.x | Service discovery |
| MySQL | 8.0 | Relational database |
| Maven | 3.9+ | Build tool |
| Docker | Latest | Containerization |
| Lombok | Latest | Reduce boilerplate code |
| SpringDoc OpenAPI | 2.3.0 | API documentation |

---

## Architecture

### Layered Architecture

```
┌─────────────────────────────────────────┐
│         REST Controllers                │  ← HTTP Layer
│  (CategoryController, ProductController)│
└─────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│       Service Layer                     │  ← Business Logic
│  (CategoryService, ProductService)      │
└─────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│       Repository Layer                  │  ← Data Access
│  (CategoryRepository, ProductRepository)│
└─────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│         Database (MySQL)                │  ← Data Storage
└─────────────────────────────────────────┘
```

### Microservices Architecture

```
┌──────────────────────────────────────────────────────┐
│                   API Gateway                        │
│              (Port: 8080 - Future)                   │
└──────────────────────────────────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────────┐
│              Eureka Service Discovery                │
│                   (Port: 8761)                       │
│  - Service Registry                                  │
│  - Health Monitoring                                 │
│  - Load Balancing                                    │
└──────────────────────────────────────────────────────┘
                         ↓
        ┌────────────────┴────────────────┐
        ↓                                  ↓
┌──────────────────┐            ┌──────────────────┐
│ Product Service  │            │  Other Services  │
│   (Port: 8081)   │            │  (Future)        │
│                  │            │  - User Service  │
│ ┌──────────────┐ │            │  - Order Service │
│ │   MySQL DB   │ │            │  - Payment Svc   │
│ │ (Port: 3306) │ │            │  - etc.          │
│ └──────────────┘ │            └──────────────────┘
└──────────────────┘
```

---

## Eureka Service Discovery

### What is Service Discovery?

In a microservices architecture, services need to communicate with each other. But how does **Product Service** know where **Order Service** is running? What if services scale up/down dynamically?

**Service Discovery** solves this problem by maintaining a registry of all available services and their locations.

### How Eureka Works

Netflix Eureka is a service registry that enables:
1. **Service Registration**: Services register themselves with Eureka on startup
2. **Service Discovery**: Services query Eureka to find other services
3. **Health Monitoring**: Eureka tracks which services are healthy
4. **Load Balancing**: Distributes requests across multiple instances

### Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                  Eureka Server                          │
│                  (localhost:8761)                       │
│  ┌───────────────────────────────────────────────────┐ │
│  │          Service Registry                         │ │
│  ├───────────────────────────────────────────────────┤ │
│  │ product-service → [Instance1: localhost:8081]    │ │
│  │ user-service    → [Instance1: localhost:8082]    │ │
│  │ order-service   → [Instance1: localhost:8083]    │ │
│  │                   [Instance2: localhost:8084]    │ │
│  └───────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                    ↑           ↓
         Registration        Discovery
                    ↑           ↓
┌─────────────────────────────────────────────────────────┐
│              Product Service (8081)                     │
│  - Registers as "product-service"                       │
│  - Sends heartbeat every 30s                            │
│  - Can discover other services                          │
└─────────────────────────────────────────────────────────┘
```

### Eureka Configuration in Product Service

#### 1. Maven Dependency (pom.xml)

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

#### 2. Enable Discovery Client (ProductServiceApplication.java)

```java
@SpringBootApplication
@EnableDiscoveryClient  // ← Enables Eureka client
public class ProductServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
```

#### 3. Eureka Configuration (application.yml)

```yaml
spring:
  application:
    name: product-service  # ← Service name in Eureka registry

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/  # ← Eureka server URL
    register-with-eureka: true  # ← Register this service
    fetch-registry: true        # ← Fetch other services
  instance:
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 30    # ← Heartbeat interval
    lease-expiration-duration-in-seconds: 90 # ← Timeout before removal
```

### Registration Process

1. **Service Starts**
   ```
   Product Service starts on port 8081
   ```

2. **Registration with Eureka**
   ```
   POST http://localhost:8761/eureka/apps/PRODUCT-SERVICE
   {
     "instance": {
       "hostName": "localhost",
       "app": "PRODUCT-SERVICE",
       "ipAddr": "192.168.1.100",
       "status": "UP",
       "port": {"$": 8081, "@enabled": true},
       "healthCheckUrl": "http://localhost:8081/product-service/actuator/health"
     }
   }
   ```

3. **Eureka Acknowledges**
   ```
   HTTP 204 No Content
   Service registered successfully
   ```

4. **Heartbeat Monitoring**
   ```
   Every 30 seconds:
   PUT http://localhost:8761/eureka/apps/PRODUCT-SERVICE/localhost:product-service:8081

   If no heartbeat for 90 seconds:
   → Eureka marks service as DOWN
   → Other services won't route to this instance
   ```

### Service Discovery Example

When **Order Service** needs to call **Product Service**:

#### Without Service Discovery (Hard-coded URL)
```java
// ❌ Bad: Hard-coded URL
String productServiceUrl = "http://localhost:8081/product-service/api/products/1";
RestTemplate restTemplate = new RestTemplate();
Product product = restTemplate.getForObject(productServiceUrl, Product.class);

// Problems:
// - What if Product Service moves to different host/port?
// - What if we have multiple Product Service instances?
// - What if Product Service is down?
```

#### With Service Discovery (Dynamic Discovery)
```java
// ✅ Good: Service discovery
@LoadBalanced  // Enables client-side load balancing
RestTemplate restTemplate = new RestTemplate();

// Use service name instead of URL
String url = "http://product-service/product-service/api/products/1";
Product product = restTemplate.getForObject(url, Product.class);

// Benefits:
// - No hard-coded URLs
// - Automatic load balancing across instances
// - Automatic failover if instance is down
// - Scales transparently
```

### Eureka Dashboard

Access Eureka dashboard at: **http://localhost:8761**

The dashboard shows:
- All registered services
- Service instances (hostname, port, status)
- Health status (UP, DOWN, STARTING)
- Last heartbeat time
- Service metadata

Example dashboard view:
```
┌───────────────────────────────────────────────────────┐
│          Eureka Server Dashboard                      │
├───────────────────────────────────────────────────────┤
│ Registered Instances                                  │
│                                                       │
│ PRODUCT-SERVICE (1 instance)                         │
│   ├─ Instance ID: localhost:product-service:8081    │
│   ├─ Status: UP                                      │
│   ├─ Zone: default                                   │
│   └─ Health: http://localhost:8081/.../health       │
│                                                       │
│ USER-SERVICE (2 instances)                           │
│   ├─ Instance 1: localhost:user-service:8082 (UP)   │
│   └─ Instance 2: localhost:user-service:8085 (UP)   │
└───────────────────────────────────────────────────────┘
```

### Health Monitoring

Eureka uses Spring Actuator health endpoint to monitor service health:

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info  # ← Expose health endpoint
```

Health check endpoint:
```
GET http://localhost:8081/product-service/actuator/health

Response:
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### Load Balancing

When multiple instances are registered, Eureka provides client-side load balancing:

```
Order Service wants to call Product Service
         ↓
Queries Eureka: "Where is product-service?"
         ↓
Eureka returns: [Instance1: 8081, Instance2: 8082, Instance3: 8083]
         ↓
Ribbon (load balancer) picks instance using algorithm:
- Round Robin (default): Request 1 → 8081, Request 2 → 8082, Request 3 → 8083, Request 4 → 8081...
- Random: Randomly selects an instance
- Weighted: Based on instance response time
         ↓
Request sent to selected instance
```

### High Availability

For production, run multiple Eureka servers:

```yaml
# Eureka Server 1
eureka:
  client:
    service-url:
      defaultZone: http://eureka2:8761/eureka/,http://eureka3:8761/eureka/

# Eureka Server 2
eureka:
  client:
    service-url:
      defaultZone: http://eureka1:8761/eureka/,http://eureka3:8761/eureka/

# Eureka Server 3
eureka:
  client:
    service-url:
      defaultZone: http://eureka1:8761/eureka/,http://eureka2:8761/eureka/
```

Eureka servers replicate registry data among themselves for fault tolerance.

### Common Issues and Troubleshooting

#### 1. Service Not Registering

**Symptom**: Service starts but doesn't appear in Eureka dashboard

**Solutions**:
- Check Eureka URL: `eureka.client.service-url.defaultZone`
- Verify network connectivity: `curl http://localhost:8761/eureka/apps`
- Check logs for registration errors
- Ensure `@EnableDiscoveryClient` annotation is present

#### 2. Service Marked as DOWN

**Symptom**: Service shows as DOWN in Eureka

**Solutions**:
- Check health endpoint: `/actuator/health`
- Verify database connectivity
- Check application logs for errors
- Restart the service

#### 3. Service Not Discovered

**Symptom**: Other services can't find this service

**Solutions**:
- Ensure `fetch-registry: true`
- Wait 30-60 seconds for registration to propagate
- Check service name matches exactly (case-sensitive)
- Verify `@LoadBalanced` annotation on RestTemplate

### Benefits of Using Eureka

1. **Dynamic Service Discovery**
   - No hard-coded URLs
   - Services find each other automatically
   - Easy to add/remove instances

2. **Fault Tolerance**
   - Automatic failover to healthy instances
   - Unhealthy services removed from registry
   - Circuit breaker integration (Resilience4j)

3. **Load Balancing**
   - Requests distributed across instances
   - Client-side load balancing (faster than server-side)
   - Multiple algorithms available

4. **Scalability**
   - Scale services up/down dynamically
   - No configuration changes needed
   - Automatic discovery of new instances

5. **Monitoring**
   - Visual dashboard
   - Service health status
   - Instance metrics

---

## Prerequisites

Before running the Product Service, ensure you have:

- **Java 17** or higher
- **Maven 3.9+** (or use Maven wrapper: `./mvnw`)
- **MySQL 8.0+** (or Docker for containerized setup)
- **Docker & Docker Compose** (optional, for containerized deployment)
- **Eureka Server** running on port 8761

---

## Getting Started

### 1. Set Up Eureka Server

First, you need a running Eureka Server. Create a simple Eureka Server:

```xml
<!-- Eureka Server pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

```yaml
# Eureka Server application.yml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

Run Eureka Server:
```bash
mvn spring-boot:run
```

Access Eureka Dashboard: http://localhost:8761

### 2. Set Up MySQL Database

#### Option A: Using Docker (Recommended)

```bash
cd product-service
docker-compose up -d
```

This starts MySQL on port 3306 with database `productdb`.

#### Option B: Local MySQL Installation

```sql
CREATE DATABASE productdb;
CREATE USER 'root'@'localhost' IDENTIFIED BY 'root';
GRANT ALL PRIVILEGES ON productdb.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Build the Application

```bash
cd product-service
mvn clean install
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

Or run the JAR:
```bash
java -jar target/product-service-1.0.0.jar
```

### 5. Verify Registration

- Check Eureka Dashboard: http://localhost:8761
- You should see `PRODUCT-SERVICE` listed
- Status should be `UP`

### 6. Test the API

```bash
# Health check
curl http://localhost:8081/product-service/actuator/health

# Create a category
curl -X POST http://localhost:8081/product-service/api/categories \
  -H "Content-Type: application/json" \
  -d '{"name": "Electronics", "description": "Electronic devices"}'

# Get all categories
curl http://localhost:8081/product-service/api/categories
```

---

## API Documentation

### Base URL

```
http://localhost:8081/product-service/api
```

### Swagger UI

Access interactive API documentation:
```
http://localhost:8081/product-service/swagger-ui.html
```

### Category Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/categories` | Create category |
| GET | `/categories` | Get all categories |
| GET | `/categories/{id}` | Get category by ID |
| GET | `/categories/name/{name}` | Get category by name |
| PUT | `/categories/{id}` | Update category |
| DELETE | `/categories/{id}` | Delete category |

### Product Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/products` | Create product |
| GET | `/products` | Get all products |
| GET | `/products/{id}` | Get product by ID |
| GET | `/products/sku/{sku}` | Get product by SKU |
| GET | `/products/active` | Get active products |
| GET | `/products/category/{id}` | Get products by category |
| GET | `/products/search?keyword=X` | Search products |
| GET | `/products/filter/price` | Filter by price range |
| GET | `/products/low-stock` | Get low stock products |
| GET | `/products/out-of-stock` | Get out of stock products |
| PUT | `/products/{id}` | Update product |
| PATCH | `/products/{id}/stock` | Update stock quantity |
| PATCH | `/products/{id}/activate` | Activate product |
| PATCH | `/products/{id}/deactivate` | Deactivate product |
| DELETE | `/products/{id}` | Delete product |

### Example Requests

#### Create Category
```bash
POST /api/categories
Content-Type: application/json

{
  "name": "Electronics",
  "description": "Electronic devices and accessories",
  "imageUrl": "https://example.com/electronics.jpg"
}
```

#### Create Product
```bash
POST /api/products
Content-Type: application/json

{
  "name": "Dell XPS 13",
  "description": "Ultra-thin laptop",
  "price": 1299.99,
  "stockQuantity": 50,
  "sku": "LAPTOP-DELL-XPS13",
  "imageUrl": "laptop.jpg",
  "categoryId": 1,
  "active": true
}
```

---

## Database Schema

### Categories Table

```sql
CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    image_url VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

### Products Table

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    stock_quantity INT NOT NULL,
    sku VARCHAR(100) NOT NULL UNIQUE,
    image_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    category_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    INDEX idx_sku (sku),
    INDEX idx_name (name),
    INDEX idx_category_id (category_id)
);
```

---

## Configuration

Key configuration properties in `application.yml`:

```yaml
# Server Configuration
server.port: 8081
server.servlet.context-path: /product-service

# Database Configuration
spring.datasource.url: jdbc:mysql://localhost:3306/productdb
spring.datasource.username: root
spring.datasource.password: root

# Eureka Configuration
eureka.client.service-url.defaultZone: http://localhost:8761/eureka/
eureka.instance.prefer-ip-address: true
```

### Environment Variables

Override configuration with environment variables:

```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/productdb
export SPRING_DATASOURCE_USERNAME=admin
export SPRING_DATASOURCE_PASSWORD=secretpassword
export EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka/
```

---

## Docker Setup

### Build Docker Image

```bash
docker build -t product-service:1.0.0 .
```

### Run with Docker Compose

```bash
docker-compose up --build
```

This starts:
- MySQL database
- Product Service
- Automatically connects them via Docker network

---

## Testing

### Run Tests

```bash
mvn test
```

### Manual Testing

Use Postman, curl, or Swagger UI to test endpoints.

---

## Monitoring

### Health Check

```bash
curl http://localhost:8081/product-service/actuator/health
```

### Metrics

```bash
curl http://localhost:8081/product-service/actuator/metrics
```

### Eureka Dashboard

Monitor service registration: http://localhost:8761

---

## Next Steps

1. **Set up other microservices** (User Service, Order Service, etc.)
2. **Add API Gateway** (Spring Cloud Gateway) for routing
3. **Implement security** (OAuth2, JWT)
4. **Add distributed tracing** (Sleuth + Zipkin)
5. **Set up centralized logging** (ELK Stack)
6. **Implement circuit breaker** (Resilience4j)

---

## License

This project is part of an E-commerce Microservices architecture learning project.

---

## Contact

For questions or issues, please create an issue in the repository.
