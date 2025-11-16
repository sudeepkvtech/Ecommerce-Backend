# Payment Service

Payment processing microservice for the e-commerce platform. Handles payment transactions, refunds, and payment history.

## Overview

The Payment Service manages all payment-related operations including:
- Processing payments via payment gateways
- Recording payment transactions
- Processing refunds
- Retrieving payment history
- Payment status tracking

## Technology Stack

- **Java 17** - Programming language
- **Spring Boot 3.2.0** - Application framework
- **Spring Data JPA** - Database access
- **Spring Security** - Authentication/Authorization with JWT
- **MySQL 8.0** - Database
- **Eureka Client** - Service discovery
- **Lombok** - Code generation
- **Maven** - Build tool
- **Docker** - Containerization

## Architecture

```
┌─────────────┐
│ Order       │──┐
│ Service     │  │
└─────────────┘  │
                 │    ┌────────────────┐      ┌──────────────┐
                 ├───>│ Payment        │─────>│ Payment      │
┌─────────────┐  │    │ Service        │      │ Gateway      │
│ Frontend    │──┘    │ (Port 8084)    │      │ (Stripe,     │
└─────────────┘       └────────────────┘      │  PayPal)     │
                             │                 └──────────────┘
                             v
                      ┌─────────────┐
                      │ MySQL       │
                      │ (Port 3309) │
                      └─────────────┘
```

## Database Schema

### payments Table
```sql
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    payment_method VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255),
    failure_reason TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_order_id (order_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_transaction_id (transaction_id)
);
```

## API Endpoints

| Method | Endpoint                         | Description                  | Auth Required |
|--------|----------------------------------|------------------------------|---------------|
| POST   | /api/payments                    | Process payment              | Yes           |
| GET    | /api/payments/{id}               | Get payment by ID            | Yes           |
| GET    | /api/payments/order/{orderId}    | Get payment by order ID      | Yes           |
| GET    | /api/payments/user/{userId}      | Get user payment history     | Yes           |
| POST   | /api/payments/{id}/refund        | Process refund (Admin)       | Yes (Admin)   |
| GET    | /api/payments/status/{status}    | Get payments by status (Admin)| Yes (Admin)   |

## Request/Response Examples

### Process Payment

**Request:**
```json
POST /api/payments
{
  "orderId": 123,
  "userId": 456,
  "amount": 99.99,
  "paymentMethod": "CREDIT_CARD",
  "paymentToken": "tok_1234567890abcdef"
}
```

**Success Response (201 CREATED):**
```json
{
  "id": 1,
  "orderId": 123,
  "userId": 456,
  "amount": 99.99,
  "paymentMethod": "CREDIT_CARD",
  "status": "COMPLETED",
  "transactionId": "ch_1234567890abcdef",
  "failureReason": null,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:15"
}
```

**Failure Response (402 PAYMENT REQUIRED):**
```json
{
  "id": 1,
  "orderId": 123,
  "userId": 456,
  "amount": 99.99,
  "paymentMethod": "CREDIT_CARD",
  "status": "FAILED",
  "transactionId": null,
  "failureReason": "Your card was declined",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:05"
}
```

### Process Refund

**Request:**
```json
POST /api/payments/1/refund
{
  "amount": 99.99,
  "reason": "Customer returned product - defective"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "orderId": 123,
  "userId": 456,
  "amount": 99.99,
  "paymentMethod": "CREDIT_CARD",
  "status": "REFUNDED",
  "transactionId": "ch_1234567890abcdef",
  "failureReason": null,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T14:20:00"
}
```

## Payment Status Lifecycle

```
PENDING → PROCESSING → COMPLETED
    ↓          ↓
    └─── FAILED

COMPLETED → REFUNDED
```

**Status Descriptions:**
- **PENDING**: Payment initiated, not yet processed
- **PROCESSING**: Payment being processed by gateway
- **COMPLETED**: Payment successful, funds captured
- **FAILED**: Payment failed (card declined, insufficient funds, etc.)
- **REFUNDED**: Payment refunded to customer

## Payment Methods

| Method           | Description                    | Processing Time |
|------------------|--------------------------------|-----------------|
| CREDIT_CARD      | Visa, Mastercard, Amex, etc.  | Instant         |
| DEBIT_CARD       | Direct debit from bank         | Instant         |
| PAYPAL           | PayPal account payment         | Instant         |
| BANK_TRANSFER    | Wire transfer/ACH              | 1-3 business days|
| CASH_ON_DELIVERY | Pay cash at delivery           | At delivery     |

## Configuration

### application.yml
```yaml
server:
  port: 8084

spring:
  application:
    name: payment-service
  datasource:
    url: jdbc:mysql://localhost:3309/paymentdb
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

app:
  jwt:
    secret: mySecretKey123456789012345678901234567890
```

## Running the Service

### Prerequisites
- Java 17 or higher
- MySQL 8.0
- Eureka Server running on port 8761

### Using Maven
```bash
# Navigate to payment-service directory
cd payment-service

# Run the application
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/payment-service-1.0.0-SNAPSHOT.jar
```

### Using Docker
```bash
# Build and start services
docker-compose up -d

# View logs
docker-compose logs -f payment-service

# Stop services
docker-compose down
```

The service will be available at `http://localhost:8084`

## Development

### Project Structure
```
payment-service/
├── src/main/java/com/ecommerce/paymentservice/
│   ├── entity/          # Database entities
│   │   ├── Payment.java
│   │   ├── PaymentStatus.java
│   │   └── PaymentMethod.java
│   ├── repository/      # Data access layer
│   │   └── PaymentRepository.java
│   ├── service/         # Business logic
│   │   └── PaymentService.java
│   ├── controller/      # REST controllers
│   │   └── PaymentController.java
│   ├── dto/             # Data Transfer Objects
│   │   ├── ProcessPaymentRequest.java
│   │   ├── PaymentResponse.java
│   │   └── RefundRequest.java
│   ├── exception/       # Exception handling
│   │   ├── ResourceNotFoundException.java
│   │   ├── BadRequestException.java
│   │   ├── DuplicateResourceException.java
│   │   ├── ErrorResponse.java
│   │   └── GlobalExceptionHandler.java
│   ├── security/        # Security configuration
│   │   ├── JwtUtil.java
│   │   └── JwtAuthenticationFilter.java
│   └── config/          # Configuration classes
│       └── SecurityConfig.java
├── src/main/resources/
│   └── application.yml  # Application configuration
├── Dockerfile           # Docker image definition
├── docker-compose.yml   # Docker Compose configuration
├── pom.xml             # Maven dependencies
└── README.md           # This file
```

## Security

### Authentication
All endpoints require JWT authentication. Include the JWT token in the Authorization header:
```
Authorization: Bearer <jwt-token>
```

### Role-Based Access Control
- **USER**: Can process payments and view their own payment history
- **ADMIN**: Can process refunds and view all payments

## Payment Gateway Integration

**IMPORTANT**: The current implementation uses placeholder payment processing. In production, integrate with actual payment gateways:

### Stripe Integration Example
```java
// Add dependency
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>24.0.0</version>
</dependency>

// Initialize
Stripe.apiKey = "sk_live_...";

// Process payment
Map<String, Object> params = new HashMap<>();
params.put("amount", amount.multiply(new BigDecimal("100")).longValue());
params.put("currency", "usd");
params.put("source", paymentToken);
Charge charge = Charge.create(params);

// Process refund
Map<String, Object> refundParams = new HashMap<>();
refundParams.put("charge", transactionId);
refundParams.put("amount", refundAmount.multiply(new BigDecimal("100")).longValue());
Refund refund = Refund.create(refundParams);
```

## Testing

### Test Payment Scenarios
The placeholder implementation simulates different payment scenarios:

1. **Successful Payment**: All payment methods complete successfully
2. **Failed Payment**: Uncomment failure simulation in PaymentService

### Production Testing
When integrating with real gateways, use test cards:

**Stripe Test Cards:**
- Success: 4242 4242 4242 4242
- Declined: 4000 0000 0000 0002
- Insufficient funds: 4000 0000 0000 9995

## Monitoring

### Health Check
```bash
curl http://localhost:8084/actuator/health
```

### Metrics
```bash
curl http://localhost:8084/actuator/metrics
```

### Service Discovery
Check Eureka dashboard: http://localhost:8761

## Troubleshooting

### Payment Service won't start
- Check if port 8084 is available
- Verify MySQL is running on port 3309
- Check database credentials in application.yml

### Can't connect to database
```bash
# Test MySQL connection
mysql -h localhost -P 3309 -u root -p
# Password: root

# Check if database exists
SHOW DATABASES;
USE paymentdb;
SHOW TABLES;
```

### Can't register with Eureka
- Verify Eureka Server is running: http://localhost:8761
- Check EUREKA_CLIENT_SERVICEURL_DEFAULTZONE in application.yml
- Review payment-service logs for connection errors

### Payment processing fails
- Check payment gateway API credentials
- Verify payment token format
- Review PaymentService logs for detailed errors
- Ensure payment gateway API is accessible

## Future Enhancements

1. **Payment Gateway Integration**:
   - Stripe API integration
   - PayPal API integration
   - Support for multiple payment gateways

2. **Advanced Features**:
   - Saved payment methods
   - Recurring payments/subscriptions
   - Multi-currency support
   - Split payments

3. **Security Enhancements**:
   - PCI DSS compliance
   - Payment card tokenization
   - Fraud detection
   - 3D Secure support

4. **Reporting**:
   - Payment analytics dashboard
   - Revenue reports
   - Failed payment analysis
   - Reconciliation tools

5. **Webhooks**:
   - Payment confirmation webhooks
   - Refund notifications
   - Failed payment alerts

## License

This project is part of the E-commerce Backend microservices system.
