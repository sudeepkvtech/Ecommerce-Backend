# Order Service

Order management microservice for the E-commerce application. Handles order creation, order lifecycle management, and order tracking.

## Features

- **Order Creation**: Create orders with multiple products
- **Order Tracking**: Track orders by ID or order number
- **Order History**: View user's complete order history
- **Status Management**: Track order lifecycle (PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED)
- **Order Cancellation**: Users can cancel their own orders
- **Admin Operations**: Administrators can update order status
- **Product Snapshots**: Preserves product details at time of purchase
- **JWT Authentication**: Secure authentication with User Service
- **Service Discovery**: Registers with Eureka for microservices architecture

## Architecture

### Microservices Integration

```
┌──────────────┐
│ User Service │ ← Authentication, User/Address validation
└──────┬───────┘
       │
       ↓ (Future)
┌────────────────┐     ┌──────────────────┐
│ Order Service  │ → │ Product Service  │ ← Product validation, prices
└────────┬───────┘     └──────────────────┘
       │
       ↓ (Future)
┌─────────────────┐
│ Inventory       │ ← Stock reservation
│ Service         │
└─────────────────┘
```

### Layered Architecture

```
Controller Layer (REST API)
    ↓
Service Layer (Business Logic)
    ↓
Repository Layer (Data Access)
    ↓
Database (MySQL)
```

## Technology Stack

- **Java 17**: Programming language
- **Spring Boot 3.2.0**: Application framework
- **Spring Cloud 2023.0.0**: Microservices infrastructure
- **Spring Security**: Authentication and authorization
- **Netflix Eureka Client**: Service discovery
- **JWT (JJWT 0.12.3)**: Token validation
- **Spring Data JPA**: Database access
- **MySQL 8.0**: Database
- **Lombok**: Reduce boilerplate code
- **Maven**: Build and dependency management
- **Docker**: Containerization

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+
- Eureka Server running on port 8761
- User Service running (for JWT validation)

### Configuration

The service runs on **port 8083** and uses MySQL on **port 3308**.

Key configuration (in `application.yml`):

```yaml
server:
  port: 8083
  context-path: /order-service

spring:
  datasource:
    url: jdbc:mysql://localhost:3308/orderdb
    username: root
    password: root

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

jwt:
  secret: ThisIsAVeryLongSecretKeyForJWTTokenGenerationAndValidation
```

### Running with Maven

```bash
cd order-service
mvn spring-boot:run
```

### Running with Docker Compose

```bash
cd order-service
docker-compose up -d
```

This starts:
- MySQL database (port 3308)
- Order Service application (port 8083)

## API Endpoints

Base URL: `http://localhost:8083/order-service/api/orders`

All endpoints require JWT authentication (Bearer token in Authorization header).

### User Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/orders` | Create new order | User |
| GET | `/api/orders` | Get user's order history | User |
| GET | `/api/orders/{id}` | Get order by ID | User (owner) |
| GET | `/api/orders/number/{orderNumber}` | Get order by order number | User (owner) |
| PUT | `/api/orders/{id}/cancel` | Cancel order | User (owner) |

### Admin Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| PUT | `/api/orders/{id}/status` | Update order status | Admin |
| GET | `/api/orders/status/{status}` | Get orders by status | Admin |

### Example Requests

#### Create Order

```bash
POST /api/orders
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "items": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 2,
      "quantity": 1
    }
  ],
  "shippingAddressId": 1,
  "paymentMethod": "CREDIT_CARD"
}
```

Response:
```json
{
  "id": 1,
  "orderNumber": "ORD-20240115-001",
  "userId": 1,
  "status": "PENDING",
  "totalAmount": 299.97,
  "shippingAddressId": 1,
  "paymentMethod": "CREDIT_CARD",
  "items": [
    {
      "id": 1,
      "productId": 1,
      "productName": "Product #1",
      "productPrice": 99.99,
      "quantity": 2,
      "subtotal": 199.98
    },
    {
      "id": 2,
      "productId": 2,
      "productName": "Product #2",
      "productPrice": 99.99,
      "quantity": 1,
      "subtotal": 99.99
    }
  ],
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

#### Get User Orders

```bash
GET /api/orders
Authorization: Bearer YOUR_JWT_TOKEN
```

#### Cancel Order

```bash
PUT /api/orders/1/cancel
Authorization: Bearer YOUR_JWT_TOKEN
```

#### Update Order Status (Admin)

```bash
PUT /api/orders/1/status
Authorization: Bearer ADMIN_JWT_TOKEN
Content-Type: application/json

{
  "status": "SHIPPED"
}
```

## Database Schema

### Orders Table

```sql
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    shipping_address_id BIGINT,
    payment_method VARCHAR(50),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_order_number (order_number),
    INDEX idx_status (status),
    INDEX idx_user_created (user_id, created_at)
);
```

### Order Items Table

```sql
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT,
    product_name VARCHAR(255) NOT NULL,
    product_price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_order_id (order_id),
    INDEX idx_product_id (product_id)
);
```

## Order Lifecycle

Orders follow a defined lifecycle with status transitions:

```
PENDING
   ↓ (Payment confirmed)
CONFIRMED
   ↓ (Warehouse processing)
PROCESSING
   ↓ (Shipped to customer)
SHIPPED
   ↓ (Delivered to customer)
DELIVERED

(Can be cancelled from PENDING, CONFIRMED, or PROCESSING)
   ↓
CANCELLED
```

### Status Descriptions

- **PENDING**: Order created, awaiting payment confirmation
- **CONFIRMED**: Payment received, order confirmed
- **PROCESSING**: Warehouse preparing order for shipment
- **SHIPPED**: Order dispatched to customer
- **DELIVERED**: Order successfully delivered
- **CANCELLED**: Order cancelled (by user or admin)

### Valid Status Transitions

- PENDING → CONFIRMED, CANCELLED
- CONFIRMED → PROCESSING, CANCELLED
- PROCESSING → SHIPPED, CANCELLED
- SHIPPED → DELIVERED
- DELIVERED → (final state)
- CANCELLED → (final state)

## Order Number Format

Format: `ORD-YYYYMMDD-NNN`

Examples:
- `ORD-20240115-001` - First order on January 15, 2024
- `ORD-20240115-002` - Second order on January 15, 2024
- `ORD-20240116-001` - First order on January 16, 2024

## Integration Points

### User Service Integration (Future)

```java
// Validate user by email
UserResponse user = userServiceClient.getUserByEmail(email);

// Validate shipping address
AddressResponse address = userServiceClient.getAddress(userId, addressId);
```

### Product Service Integration (Future)

```java
// Validate product exists and get details
ProductResponse product = productServiceClient.getProduct(productId);

// Get current price
BigDecimal price = product.getPrice();
```

### Inventory Service Integration (Future)

```java
// Reserve inventory
inventoryServiceClient.reserveStock(productId, quantity);

// Release inventory (on cancellation)
inventoryServiceClient.releaseStock(productId, quantity);
```

## Security

### JWT Authentication

All endpoints require JWT token authentication. The token must be obtained from User Service.

Include token in requests:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Authorization Rules

- **Users**: Can only access their own orders
- **Admins**: Can access all orders and update status

### Shared JWT Secret

Order Service validates JWT tokens using the same secret as User Service:
```yaml
jwt:
  secret: ThisIsAVeryLongSecretKeyForJWTTokenGenerationAndValidation
```

## Eureka Service Discovery

Order Service registers with Eureka Server for service discovery.

### Registration

```yaml
eureka:
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### Service Name

`order-service` (used by other services to discover this service)

### Health Check

Eureka monitors service health via:
```
http://localhost:8083/order-service/actuator/health
```

## Monitoring

### Actuator Endpoints

- Health: `http://localhost:8083/order-service/actuator/health`
- Info: `http://localhost:8083/order-service/actuator/info`

### Logging

Configure logging levels in `application.yml`:

```yaml
logging:
  level:
    com.ecommerce.orderservice: DEBUG
    org.hibernate.SQL: DEBUG
```

## Development

### Project Structure

```
order-service/
├── src/main/java/com/ecommerce/orderservice/
│   ├── OrderServiceApplication.java
│   ├── controller/
│   │   └── OrderController.java
│   ├── service/
│   │   └── OrderService.java
│   ├── repository/
│   │   ├── OrderRepository.java
│   │   └── OrderItemRepository.java
│   ├── entity/
│   │   ├── Order.java
│   │   ├── OrderItem.java
│   │   └── OrderStatus.java
│   ├── dto/
│   │   ├── OrderRequest.java
│   │   ├── OrderResponse.java
│   │   ├── OrderItemRequest.java
│   │   ├── OrderItemResponse.java
│   │   └── UpdateOrderStatusRequest.java
│   ├── exception/
│   │   ├── ResourceNotFoundException.java
│   │   ├── BadRequestException.java
│   │   ├── DuplicateResourceException.java
│   │   ├── ErrorResponse.java
│   │   └── GlobalExceptionHandler.java
│   ├── security/
│   │   ├── JwtUtil.java
│   │   └── JwtAuthenticationFilter.java
│   └── config/
│       └── SecurityConfig.java
├── src/main/resources/
│   └── application.yml
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

### Build

```bash
mvn clean package
```

### Run Tests

```bash
mvn test
```

## Docker

### Build Image

```bash
docker build -t order-service:latest .
```

### Run Container

```bash
docker run -p 8083:8083 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3308/orderdb \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://host.docker.internal:8761/eureka/ \
  order-service:latest
```

### Docker Compose

```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

## Troubleshooting

### Service won't start

1. Check Eureka Server is running: `curl http://localhost:8761`
2. Check MySQL is running: `mysql -h localhost -P 3308 -u root -p`
3. Check port 8083 is available: `lsof -i :8083`

### Can't create orders

1. Verify JWT token is valid
2. Check User Service is running (for authentication)
3. Verify shipping address exists in User Service
4. Check database connection

### Database errors

1. Verify MySQL is running on port 3308
2. Check database `orderdb` exists
3. Verify credentials in application.yml

## Future Enhancements

- [ ] Integrate with User Service for user/address validation
- [ ] Integrate with Product Service for product validation and pricing
- [ ] Integrate with Inventory Service for stock management
- [ ] Integrate with Payment Service for payment processing
- [ ] Add order tracking with status history
- [ ] Implement order notifications (email, SMS)
- [ ] Add order search and filtering
- [ ] Implement order returns and refunds
- [ ] Add order analytics and reporting
- [ ] Support multiple payment methods
- [ ] Add shipping cost calculation
- [ ] Implement discount codes and promotions

## License

This project is part of the E-commerce microservices application.
