# Inventory Service

> Real-time inventory management and stock reservation system for e-commerce platform

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Stock Reservation Flow](#stock-reservation-flow)
- [Configuration](#configuration)
- [Docker Deployment](#docker-deployment)
- [Integration Guide](#integration-guide)
- [Troubleshooting](#troubleshooting)

---

## Overview

The Inventory Service is a critical microservice in the e-commerce platform that manages product inventory levels, stock reservations, and stock movements. It ensures accurate stock tracking and prevents overselling by implementing a robust reservation system.

### Key Responsibilities

1. **Stock Management**: Track available, reserved, and total inventory quantities
2. **Stock Reservation**: Reserve stock for pending orders to prevent overselling
3. **Stock Operations**: Add, reduce, and adjust inventory levels
4. **Movement Tracking**: Complete audit trail of all stock changes
5. **Low Stock Alerts**: Monitor and report low inventory levels
6. **Stock Validation**: Ensure stock availability before order processing

### Business Logic

**Stock States:**
- **Available Quantity**: Items that can be sold immediately
- **Reserved Quantity**: Items held for pending orders (payment processing)
- **Total Quantity**: Physical stock in warehouse (available + reserved)

**Invariant:** `total = available + reserved` (always maintained)

**Reservation Lifecycle:**
```
1. Order Created → Reserve Stock (available → reserved)
2. Payment Success → Commit Reservation (deduct from reserved and total)
3. Payment Failed → Release Reservation (reserved → available)
```

---

## Features

### Core Features

- ✅ **Real-time stock tracking** with three-state model (available/reserved/total)
- ✅ **Stock reservation system** prevents overselling during checkout
- ✅ **Complete audit trail** tracks all stock movements with reasons
- ✅ **Low stock monitoring** with configurable thresholds per product
- ✅ **Concurrent safety** with optimistic locking
- ✅ **Business validations** prevent invalid stock operations
- ✅ **Movement types** categorize stock changes (purchase, sale, return, damage, etc.)

### Admin Features

- 🔐 **Add stock** for new inventory arrivals
- 🔐 **Reduce stock** for damages or losses
- 🔐 **Adjust stock** for inventory corrections
- 🔐 **Low stock reports** for restocking decisions
- 🔐 **Movement history** for audit and analytics

### Integration Features

- 🔌 **REST API** for service-to-service communication
- 🔌 **JWT authentication** with role-based access control
- 🔌 **Eureka registration** for service discovery
- 🔌 **Transaction management** ensures data consistency

---

## Architecture

### Service Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Inventory Service                       │
│                         (Port 8085)                          │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐      ┌──────────────┐                     │
│  │  Controller  │─────▶│   Service    │                     │
│  │   (REST)     │      │  (Business)  │                     │
│  └──────────────┘      └──────┬───────┘                     │
│                               │                              │
│                               ▼                              │
│                    ┌─────────────────────┐                  │
│                    │    Repository       │                  │
│                    │  (Data Access)      │                  │
│                    └──────────┬──────────┘                  │
│                               │                              │
│                               ▼                              │
│                    ┌─────────────────────┐                  │
│                    │    MySQL Database   │                  │
│                    │     (Port 3310)     │                  │
│                    │                     │                  │
│                    │  • inventory        │                  │
│                    │  • stock_movement   │                  │
│                    └─────────────────────┘                  │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### Microservices Integration

```
┌─────────────────┐         ┌─────────────────┐
│  Order Service  │────1───▶│ Inventory       │
│                 │         │ Service         │
│                 │◀───2────│                 │
└─────────────────┘         └─────────────────┘
     Reserve Stock                Check & Reserve

┌─────────────────┐         ┌─────────────────┐
│ Payment Service │────3───▶│ Inventory       │
│                 │         │ Service         │
│                 │◀───4────│                 │
└─────────────────┘         └─────────────────┘
     Commit/Release              Update Stock

┌─────────────────┐         ┌─────────────────┐
│ Product Service │────5───▶│ Inventory       │
│                 │         │ Service         │
│                 │◀───6────│                 │
└─────────────────┘         └─────────────────┘
     Display Stock             Get Availability
```

**Integration Flow:**
1. Order Service checks stock availability before order creation
2. Inventory Service reserves stock and returns confirmation
3. Payment Service commits reservation on payment success
4. Inventory Service deducts stock from total
5. Product Service displays current stock levels to customers
6. Inventory Service returns available quantity for display

### Entity Relationship

```
┌─────────────────────────────┐
│        Inventory            │
│─────────────────────────────│
│ id (PK)                     │
│ product_id (UNIQUE)         │◀──┐
│ available_quantity          │   │
│ reserved_quantity           │   │  One-to-Many
│ total_quantity              │   │
│ low_stock_threshold         │   │
│ created_at                  │   │
│ updated_at                  │   │
└─────────────────────────────┘   │
                                  │
                                  │
┌─────────────────────────────┐   │
│     Stock Movement          │   │
│─────────────────────────────│   │
│ id (PK)                     │   │
│ product_id (FK)             │───┘
│ movement_type (ENUM)        │
│ quantity_change             │
│ quantity_before             │
│ quantity_after              │
│ reference_id                │
│ notes                       │
│ created_at                  │
└─────────────────────────────┘
```

---

## Technology Stack

### Core Technologies

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Programming language |
| Spring Boot | 3.2.0 | Application framework |
| Spring Cloud | 2023.0.0 | Microservices framework |
| Spring Data JPA | 3.2.0 | Data persistence |
| Hibernate | 6.4.0 | ORM framework |
| MySQL | 8.0 | Database |

### Spring Modules

- **Spring Web**: REST API development
- **Spring Security**: JWT authentication
- **Spring Data JPA**: Database access
- **Netflix Eureka Client**: Service discovery

### Libraries

- **Lombok**: Reduce boilerplate code
- **JJWT**: JWT token handling
- **MySQL Connector**: Database connectivity
- **Maven**: Dependency management

---

## Getting Started

### Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **MySQL 8.0** (or use Docker)
- **Eureka Server** running on port 8761

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd inventory-service
   ```

2. **Configure database** (option 1: Local MySQL)
   ```bash
   # Create database
   mysql -u root -p
   CREATE DATABASE inventorydb;
   exit;
   ```

3. **Configure database** (option 2: Docker)
   ```bash
   # Start MySQL container
   docker-compose up -d mysql-inventory
   ```

4. **Update configuration** (if needed)
   ```bash
   # Edit src/main/resources/application.yml
   # Update database credentials, ports, etc.
   ```

5. **Build the application**
   ```bash
   mvn clean package
   ```

6. **Run the application**
   ```bash
   java -jar target/inventory-service-0.0.1-SNAPSHOT.jar
   ```

   Or using Maven:
   ```bash
   mvn spring-boot:run
   ```

7. **Verify service is running**
   ```bash
   curl http://localhost:8085/api/inventory/1
   ```

### Quick Start with Docker

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

---

## API Documentation

### Base URL

```
http://localhost:8085/api/inventory
```

### Authentication

All endpoints require JWT authentication. Include the token in the request header:

```
Authorization: Bearer <jwt-token>
```

Admin endpoints require `ROLE_ADMIN` authority.

---

### Endpoints

#### 1. Get Inventory

Get current inventory levels for a product.

**Endpoint:** `GET /api/inventory/{productId}`

**Authorization:** User or Admin

**Path Parameters:**
- `productId` (Long): Product ID

**Response:** `200 OK`
```json
{
  "id": 1,
  "productId": 123,
  "availableQuantity": 85,
  "reservedQuantity": 15,
  "totalQuantity": 100,
  "lowStockThreshold": 10,
  "isLowStock": false,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-15T14:30:00"
}
```

**Example:**
```bash
curl -X GET http://localhost:8085/api/inventory/123 \
  -H "Authorization: Bearer <token>"
```

---

#### 2. Create Inventory

Create new inventory record for a product.

**Endpoint:** `POST /api/inventory`

**Authorization:** Admin only

**Request Body:**
```json
{
  "productId": 123,
  "initialQuantity": 100,
  "lowStockThreshold": 10
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "productId": 123,
  "availableQuantity": 100,
  "reservedQuantity": 0,
  "totalQuantity": 100,
  "lowStockThreshold": 10,
  "isLowStock": false,
  "createdAt": "2024-01-15T10:00:00",
  "updatedAt": "2024-01-15T10:00:00"
}
```

---

#### 3. Reserve Stock

Reserve stock for a pending order.

**Endpoint:** `POST /api/inventory/{productId}/reserve`

**Authorization:** User or Admin

**Path Parameters:**
- `productId` (Long): Product ID

**Request Body:**
```json
{
  "quantity": 5,
  "referenceId": "ORDER-12345"
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "productId": 123,
  "availableQuantity": 80,
  "reservedQuantity": 20,
  "totalQuantity": 100,
  "lowStockThreshold": 10,
  "isLowStock": false,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-15T14:35:00"
}
```

**Business Logic:**
- Validates sufficient stock available
- Moves quantity from `available` to `reserved`
- Creates `RESERVATION` stock movement
- `total` remains unchanged

**Error Cases:**
- `404 Not Found`: Product inventory not found
- `400 Bad Request`: Insufficient stock available

---

#### 4. Release Reservation

Release reserved stock (order cancelled or payment failed).

**Endpoint:** `POST /api/inventory/{productId}/release`

**Authorization:** User or Admin

**Path Parameters:**
- `productId` (Long): Product ID

**Request Body:**
```json
{
  "quantity": 5,
  "referenceId": "ORDER-12345",
  "reason": "Payment failed"
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "productId": 123,
  "availableQuantity": 85,
  "reservedQuantity": 15,
  "totalQuantity": 100,
  "lowStockThreshold": 10,
  "isLowStock": false,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-15T14:40:00"
}
```

**Business Logic:**
- Moves quantity from `reserved` to `available`
- Creates `RELEASE` stock movement
- `total` remains unchanged

---

#### 5. Commit Reservation

Commit reserved stock (order completed, deduct from inventory).

**Endpoint:** `POST /api/inventory/{productId}/commit`

**Authorization:** User or Admin

**Path Parameters:**
- `productId` (Long): Product ID

**Request Body:**
```json
{
  "quantity": 5,
  "referenceId": "ORDER-12345",
  "notes": "Order completed and shipped"
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "productId": 123,
  "availableQuantity": 85,
  "reservedQuantity": 10,
  "totalQuantity": 95,
  "lowStockThreshold": 10,
  "isLowStock": false,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-15T14:45:00"
}
```

**Business Logic:**
- Deducts quantity from `reserved`
- Deducts quantity from `total`
- Creates `SALE` stock movement
- `available` remains unchanged

---

#### 6. Add Stock

Add new stock (inventory arrival from supplier).

**Endpoint:** `POST /api/inventory/{productId}/add`

**Authorization:** Admin only

**Path Parameters:**
- `productId` (Long): Product ID

**Request Body:**
```json
{
  "quantity": 50,
  "referenceId": "PO-67890",
  "notes": "Stock received from Supplier XYZ"
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "productId": 123,
  "availableQuantity": 135,
  "reservedQuantity": 10,
  "totalQuantity": 145,
  "lowStockThreshold": 10,
  "isLowStock": false,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-15T15:00:00"
}
```

**Business Logic:**
- Increases `available` by quantity
- Increases `total` by quantity
- Creates `PURCHASE` stock movement
- `reserved` remains unchanged

---

#### 7. Reduce Stock

Reduce stock (damage, loss, theft).

**Endpoint:** `POST /api/inventory/{productId}/reduce`

**Authorization:** Admin only

**Path Parameters:**
- `productId` (Long): Product ID

**Request Body:**
```json
{
  "quantity": 10,
  "referenceId": "INC-111",
  "notes": "Water damage in warehouse"
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "productId": 123,
  "availableQuantity": 125,
  "reservedQuantity": 10,
  "totalQuantity": 135,
  "lowStockThreshold": 10,
  "isLowStock": false,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-15T15:30:00"
}
```

**Business Logic:**
- Decreases `available` by quantity
- Decreases `total` by quantity
- Creates `DAMAGE` stock movement
- `reserved` remains unchanged

---

#### 8. Adjust Stock

Manual stock adjustment (inventory reconciliation).

**Endpoint:** `POST /api/inventory/{productId}/adjust`

**Authorization:** Admin only

**Path Parameters:**
- `productId` (Long): Product ID

**Request Body:**
```json
{
  "quantity": 130,
  "referenceId": "AUDIT-2024-01",
  "notes": "Physical count correction"
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "productId": 123,
  "availableQuantity": 120,
  "reservedQuantity": 10,
  "totalQuantity": 130,
  "lowStockThreshold": 10,
  "isLowStock": false,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-15T16:00:00"
}
```

**Business Logic:**
- Sets `total` to specified quantity
- Adjusts `available` to maintain invariant (total - reserved)
- Creates `ADJUSTMENT` stock movement
- `reserved` remains unchanged

---

#### 9. Get Low Stock Items

List all products with low stock levels.

**Endpoint:** `GET /api/inventory/low-stock`

**Authorization:** Admin only

**Response:** `200 OK`
```json
[
  {
    "id": 5,
    "productId": 789,
    "availableQuantity": 8,
    "reservedQuantity": 2,
    "totalQuantity": 10,
    "lowStockThreshold": 10,
    "isLowStock": true,
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-15T14:30:00"
  },
  {
    "id": 12,
    "productId": 456,
    "availableQuantity": 3,
    "reservedQuantity": 0,
    "totalQuantity": 3,
    "lowStockThreshold": 20,
    "isLowStock": true,
    "createdAt": "2024-01-05T10:00:00",
    "updatedAt": "2024-01-15T12:00:00"
  }
]
```

**Business Logic:**
- Returns items where `availableQuantity < lowStockThreshold`
- Used for restocking alerts
- Admin dashboard feature

---

#### 10. Get Out of Stock Items

List all products that are out of stock.

**Endpoint:** `GET /api/inventory/out-of-stock`

**Authorization:** Admin only

**Response:** `200 OK`
```json
[
  {
    "id": 8,
    "productId": 321,
    "availableQuantity": 0,
    "reservedQuantity": 5,
    "totalQuantity": 5,
    "lowStockThreshold": 10,
    "isLowStock": true,
    "createdAt": "2024-01-03T10:00:00",
    "updatedAt": "2024-01-15T11:00:00"
  }
]
```

**Business Logic:**
- Returns items where `availableQuantity <= 0`
- Critical for preventing overselling
- Admin dashboard feature

---

#### 11. Get Movement History

Get complete stock movement history for a product.

**Endpoint:** `GET /api/inventory/movements/{productId}`

**Authorization:** User or Admin

**Path Parameters:**
- `productId` (Long): Product ID

**Response:** `200 OK`
```json
[
  {
    "id": 45,
    "productId": 123,
    "movementType": "SALE",
    "quantityChange": -5,
    "quantityBefore": 100,
    "quantityAfter": 95,
    "referenceId": "ORDER-12345",
    "notes": "Order completed and shipped",
    "createdAt": "2024-01-15T14:45:00"
  },
  {
    "id": 44,
    "productId": 123,
    "movementType": "RESERVATION",
    "quantityChange": 0,
    "quantityBefore": 100,
    "quantityAfter": 100,
    "referenceId": "ORDER-12345",
    "notes": "Stock reserved for order ORDER-12345",
    "createdAt": "2024-01-15T14:35:00"
  },
  {
    "id": 43,
    "productId": 123,
    "movementType": "PURCHASE",
    "quantityChange": 50,
    "quantityBefore": 50,
    "quantityAfter": 100,
    "referenceId": "PO-67890",
    "notes": "Stock received from Supplier XYZ",
    "createdAt": "2024-01-15T09:00:00"
  }
]
```

**Business Logic:**
- Returns all movements ordered by newest first
- Complete audit trail
- Shows quantity before/after each change

**Movement Types:**
- `PURCHASE`: Stock added from supplier (+)
- `SALE`: Stock sold to customer (-)
- `RETURN`: Customer return (+)
- `DAMAGE`: Damaged or lost stock (-)
- `ADJUSTMENT`: Manual correction (+/-)
- `RESERVATION`: Stock reserved (no quantity change)
- `RELEASE`: Reservation released (no quantity change)

---

#### 12. Check Availability

Check if sufficient stock is available for an order.

**Endpoint:** `GET /api/inventory/check/{productId}?quantity=5`

**Authorization:** User or Admin

**Path Parameters:**
- `productId` (Long): Product ID

**Query Parameters:**
- `quantity` (Integer): Quantity to check

**Response:** `200 OK`
```json
{
  "available": true,
  "productId": 123,
  "requestedQuantity": 5,
  "availableQuantity": 85
}
```

**Business Logic:**
- Returns `true` if `availableQuantity >= requestedQuantity`
- Used by Order Service before creating order
- Fast stock validation

---

## Database Schema

### Tables

#### 1. inventory

Stores current inventory levels for each product.

```sql
CREATE TABLE inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    available_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    total_quantity INT NOT NULL DEFAULT 0,
    low_stock_threshold INT DEFAULT 10,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT DEFAULT 0,
    INDEX idx_product_id (product_id),
    INDEX idx_available_quantity (available_quantity),
    CONSTRAINT chk_quantities CHECK (total_quantity = available_quantity + reserved_quantity)
);
```

**Columns:**
- `id`: Primary key
- `product_id`: Reference to product (one inventory per product)
- `available_quantity`: Can be sold now
- `reserved_quantity`: Held for pending orders
- `total_quantity`: Physical stock (available + reserved)
- `low_stock_threshold`: Alert threshold
- `created_at`: Creation timestamp
- `updated_at`: Last modification timestamp
- `version`: Optimistic locking version

**Constraints:**
- `UNIQUE(product_id)`: One inventory record per product
- `CHECK`: Ensures total = available + reserved

---

#### 2. stock_movement

Complete audit trail of all stock changes.

```sql
CREATE TABLE stock_movement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    quantity_change INT NOT NULL,
    quantity_before INT NOT NULL,
    quantity_after INT NOT NULL,
    reference_id VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_product_id (product_id),
    INDEX idx_movement_type (movement_type),
    INDEX idx_reference_id (reference_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (product_id) REFERENCES inventory(product_id)
);
```

**Columns:**
- `id`: Primary key
- `product_id`: Which product this movement is for
- `movement_type`: Type of movement (enum: PURCHASE, SALE, etc.)
- `quantity_change`: How much changed (positive or negative)
- `quantity_before`: Total quantity before change
- `quantity_after`: Total quantity after change
- `reference_id`: Links to source (order ID, PO number, etc.)
- `notes`: Human-readable explanation
- `created_at`: When movement occurred

**Movement Types:**
- `PURCHASE`: New stock from supplier (+)
- `SALE`: Stock sold (-)
- `RETURN`: Customer return (+)
- `DAMAGE`: Damaged/lost stock (-)
- `ADJUSTMENT`: Manual correction (+/-)
- `RESERVATION`: Reserved for order (no total change)
- `RELEASE`: Reservation released (no total change)

---

### Sample Data

```sql
-- Insert inventory records
INSERT INTO inventory (product_id, available_quantity, reserved_quantity, total_quantity, low_stock_threshold)
VALUES
    (1, 100, 0, 100, 10),
    (2, 50, 5, 55, 20),
    (3, 8, 2, 10, 10),
    (4, 0, 5, 5, 15);

-- Insert stock movements
INSERT INTO stock_movement (product_id, movement_type, quantity_change, quantity_before, quantity_after, reference_id, notes)
VALUES
    (1, 'PURCHASE', 100, 0, 100, 'PO-001', 'Initial stock from supplier'),
    (2, 'PURCHASE', 55, 0, 55, 'PO-002', 'Initial stock from supplier'),
    (2, 'RESERVATION', 0, 55, 55, 'ORDER-123', 'Reserved for order'),
    (3, 'PURCHASE', 50, 0, 50, 'PO-003', 'Initial stock'),
    (3, 'SALE', -40, 50, 10, 'ORDER-456', 'Sold to customer'),
    (4, 'PURCHASE', 20, 0, 20, 'PO-004', 'Initial stock'),
    (4, 'DAMAGE', -15, 20, 5, 'INC-100', 'Water damage');
```

---

## Stock Reservation Flow

### Complete Order Lifecycle

```
┌──────────────────────────────────────────────────────────────┐
│                    Order Lifecycle                            │
└──────────────────────────────────────────────────────────────┘

Step 1: Customer adds to cart
────────────────────────────────
┌─────────────┐                 ┌──────────────┐
│   Frontend  │────────────────▶│   Inventory  │
│             │  Check stock    │   Service    │
│             │◀────────────────│              │
└─────────────┘   available: 85 └──────────────┘

Step 2: Customer proceeds to checkout
──────────────────────────────────────
┌─────────────┐     ┌──────────┐     ┌──────────────┐
│   Frontend  │────▶│  Order   │────▶│   Inventory  │
│             │     │ Service  │     │   Service    │
│             │     │          │     │              │
│             │     │          │     │ Reserve: 5   │
│             │     │          │     │ available→   │
│             │     │          │     │   reserved   │
│             │◀────│          │◀────│              │
└─────────────┘     └──────────┘     └──────────────┘
  Order created    Reserve stock    available: 80
  Status: PENDING  Reference:       reserved: 5
                   ORDER-123

Step 3a: Payment succeeds
─────────────────────────
┌──────────────┐          ┌──────────────┐
│   Payment    │─────────▶│   Inventory  │
│   Service    │          │   Service    │
│              │          │              │
│ Status: PAID │  Commit  │ Commit: 5    │
│              │  ORDER-  │ reserved→    │
│              │  123     │   total--    │
│              │◀─────────│              │
└──────────────┘          └──────────────┘
                         available: 80 (unchanged)
                         reserved: 0
                         total: 95 (decreased)

Step 3b: Payment fails
──────────────────────
┌──────────────┐          ┌──────────────┐
│   Payment    │─────────▶│   Inventory  │
│   Service    │          │   Service    │
│              │          │              │
│Status: FAILED│  Release │ Release: 5   │
│              │  ORDER-  │ reserved→    │
│              │  123     │   available  │
│              │◀─────────│              │
└──────────────┘          └──────────────┘
                         available: 85 (restored)
                         reserved: 0
                         total: 100 (unchanged)
```

### State Transitions

#### Initial State
```
available: 100
reserved: 0
total: 100
```

#### After Reserve (5 units)
```
available: 95  (100 - 5)
reserved: 5    (0 + 5)
total: 100     (unchanged)
```

#### After Commit (5 units)
```
available: 95  (unchanged)
reserved: 0    (5 - 5)
total: 95      (100 - 5)
```

#### After Release (5 units)
```
available: 100 (95 + 5)
reserved: 0    (5 - 5)
total: 100     (unchanged)
```

### Stock Movement Records

Each operation creates a stock movement record:

**Reserve:**
```json
{
  "movementType": "RESERVATION",
  "quantityChange": 0,
  "quantityBefore": 100,
  "quantityAfter": 100,
  "referenceId": "ORDER-123",
  "notes": "Reserved 5 units for order"
}
```

**Commit:**
```json
{
  "movementType": "SALE",
  "quantityChange": -5,
  "quantityBefore": 100,
  "quantityAfter": 95,
  "referenceId": "ORDER-123",
  "notes": "Order completed and shipped"
}
```

**Release:**
```json
{
  "movementType": "RELEASE",
  "quantityChange": 0,
  "quantityBefore": 100,
  "quantityAfter": 100,
  "referenceId": "ORDER-123",
  "notes": "Payment failed, stock released"
}
```

---

## Configuration

### Application Properties

**Location:** `src/main/resources/application.yml`

#### Server Configuration
```yaml
server:
  port: 8085
```

#### Database Configuration
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3310/inventorydb
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
```

#### Service Discovery
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    hostname: localhost
    prefer-ip-address: true
```

#### JWT Configuration
```yaml
app:
  jwt:
    secret: mySecretKey12345
    expiration: 86400000
```

#### Inventory Configuration
```yaml
app:
  inventory:
    low-stock-threshold: 10
    allow-negative-stock: false
```

### Environment Variables

Override configuration using environment variables:

```bash
# Database
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3310/inventorydb
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=root

# Service Discovery
export EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://localhost:8761/eureka/

# JWT
export APP_JWT_SECRET=your-secret-key

# Inventory
export APP_INVENTORY_LOW_STOCK_THRESHOLD=10
export APP_INVENTORY_ALLOW_NEGATIVE_STOCK=false
```

---

## Docker Deployment

### Using Docker Compose

**Start services:**
```bash
docker-compose up -d
```

**View logs:**
```bash
docker-compose logs -f inventory-service
docker-compose logs -f mysql-inventory
```

**Stop services:**
```bash
docker-compose down
```

**Rebuild and restart:**
```bash
docker-compose up -d --build
```

**Remove all data (fresh start):**
```bash
docker-compose down -v
```

### Using Dockerfile Only

**Build image:**
```bash
docker build -t inventory-service:latest .
```

**Run container:**
```bash
docker run -d \
  --name inventory-service \
  -p 8085:8085 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/inventorydb \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=root \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://host.docker.internal:8761/eureka/ \
  inventory-service:latest
```

### Accessing Services

**Inventory Service:**
- URL: http://localhost:8085
- Health: http://localhost:8085/actuator/health (if actuator enabled)
- API: http://localhost:8085/api/inventory

**MySQL Database:**
- Host: localhost
- Port: 3310
- Database: inventorydb
- Username: root
- Password: root

**Connect to MySQL:**
```bash
mysql -h localhost -P 3310 -u root -p
```

**Connect from Docker:**
```bash
docker exec -it mysql-inventory mysql -u root -p
```

---

## Integration Guide

### For Order Service

**1. Check stock availability before creating order:**

```java
// GET /api/inventory/check/{productId}?quantity=5
RestTemplate restTemplate = new RestTemplate();
String url = "http://inventory-service:8085/api/inventory/check/" + productId + "?quantity=" + quantity;
Boolean available = restTemplate.getForObject(url, Boolean.class);

if (!available) {
    throw new BadRequestException("Insufficient stock");
}
```

**2. Reserve stock when order is created:**

```java
// POST /api/inventory/{productId}/reserve
ReserveStockRequest request = new ReserveStockRequest();
request.setQuantity(5);
request.setReferenceId("ORDER-123");

String url = "http://inventory-service:8085/api/inventory/" + productId + "/reserve";
InventoryResponse response = restTemplate.postForObject(url, request, InventoryResponse.class);
```

### For Payment Service

**1. Commit reservation when payment succeeds:**

```java
// POST /api/inventory/{productId}/commit
StockOperationRequest request = new StockOperationRequest();
request.setQuantity(5);
request.setReferenceId("ORDER-123");
request.setNotes("Payment completed");

String url = "http://inventory-service:8085/api/inventory/" + productId + "/commit";
InventoryResponse response = restTemplate.postForObject(url, request, InventoryResponse.class);
```

**2. Release reservation when payment fails:**

```java
// POST /api/inventory/{productId}/release
ReleaseStockRequest request = new ReleaseStockRequest();
request.setQuantity(5);
request.setReferenceId("ORDER-123");
request.setReason("Payment failed");

String url = "http://inventory-service:8085/api/inventory/" + productId + "/release";
InventoryResponse response = restTemplate.postForObject(url, request, InventoryResponse.class);
```

### For Product Service

**1. Display stock levels on product page:**

```java
// GET /api/inventory/{productId}
String url = "http://inventory-service:8085/api/inventory/" + productId;
InventoryResponse inventory = restTemplate.getForObject(url, InventoryResponse.class);

// Display to customer
if (inventory.getAvailableQuantity() > 0) {
    System.out.println(inventory.getAvailableQuantity() + " in stock");
} else {
    System.out.println("Out of stock");
}

if (inventory.getIsLowStock()) {
    System.out.println("Low stock - order soon!");
}
```

---

## Troubleshooting

### Common Issues

#### 1. Service won't start

**Symptoms:**
- Service fails to start
- Application crashes on startup

**Solutions:**
```bash
# Check logs
docker-compose logs -f inventory-service

# Common causes:
# - Port 8085 already in use
# - Database connection failed
# - Eureka server not running

# Check port availability
netstat -an | grep 8085
lsof -i :8085

# Verify MySQL is running
docker-compose ps mysql-inventory

# Test database connection
mysql -h localhost -P 3310 -u root -p
```

#### 2. Database connection errors

**Symptoms:**
- `Communications link failure`
- `Connection refused`
- `Unknown database 'inventorydb'`

**Solutions:**
```bash
# Wait for MySQL to be ready (takes 30-60 seconds)
docker-compose logs -f mysql-inventory

# Verify database exists
mysql -h localhost -P 3310 -u root -p -e "SHOW DATABASES;"

# Recreate database
mysql -h localhost -P 3310 -u root -p
DROP DATABASE IF EXISTS inventorydb;
CREATE DATABASE inventorydb;
exit;

# Restart service
docker-compose restart inventory-service
```

#### 3. Insufficient stock errors

**Symptoms:**
- Reserve stock fails with 400 error
- "Insufficient stock available" message

**Solutions:**
```bash
# Check current inventory
curl http://localhost:8085/api/inventory/123 \
  -H "Authorization: Bearer <token>"

# Add more stock (admin)
curl -X POST http://localhost:8085/api/inventory/123/add \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 100,
    "referenceId": "RESTOCK-001",
    "notes": "Emergency restock"
  }'
```

#### 4. Stock inconsistencies

**Symptoms:**
- Total ≠ available + reserved
- Negative stock quantities
- Missing reservations

**Solutions:**
```bash
# Check stock movements
curl http://localhost:8085/api/inventory/movements/123 \
  -H "Authorization: Bearer <token>"

# Perform manual adjustment (admin)
curl -X POST http://localhost:8085/api/inventory/123/adjust \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 100,
    "referenceId": "AUDIT-2024-01",
    "notes": "Physical inventory count correction"
  }'
```

#### 5. Eureka registration fails

**Symptoms:**
- Service not visible in Eureka dashboard
- Other services can't discover inventory service

**Solutions:**
```bash
# Verify Eureka server is running
curl http://localhost:8761

# Check Eureka configuration
docker-compose exec inventory-service env | grep EUREKA

# Check service logs for registration errors
docker-compose logs -f inventory-service | grep -i eureka

# Restart service
docker-compose restart inventory-service
```

### Performance Issues

#### Slow API responses

**Diagnosis:**
```bash
# Check database indexes
mysql -h localhost -P 3310 -u root -p inventorydb
SHOW INDEXES FROM inventory;
SHOW INDEXES FROM stock_movement;

# Check query performance
EXPLAIN SELECT * FROM inventory WHERE product_id = 123;
```

**Solutions:**
- Add indexes on frequently queried columns
- Enable database query caching
- Implement Redis caching layer
- Scale horizontally with load balancer

#### High database load

**Diagnosis:**
```bash
# Check MySQL process list
mysql -h localhost -P 3310 -u root -p
SHOW FULL PROCESSLIST;

# Check slow query log
docker-compose exec mysql-inventory cat /var/log/mysql/slow-query.log
```

**Solutions:**
- Optimize slow queries
- Add database read replicas
- Implement connection pooling
- Use database caching (Redis)

---

## Production Checklist

### Security
- [ ] Change default passwords (MySQL, JWT secret)
- [ ] Use environment variables or secrets management
- [ ] Enable HTTPS/TLS
- [ ] Implement rate limiting
- [ ] Add input validation
- [ ] Enable SQL injection prevention
- [ ] Use non-root MySQL user
- [ ] Restrict network access

### Performance
- [ ] Configure connection pooling
- [ ] Add database indexes
- [ ] Implement caching (Redis)
- [ ] Enable query optimization
- [ ] Set resource limits (CPU, memory)
- [ ] Use production-grade database config

### Reliability
- [ ] Configure health checks
- [ ] Set up monitoring (Prometheus, Grafana)
- [ ] Enable logging (ELK stack)
- [ ] Configure backup strategy
- [ ] Test disaster recovery
- [ ] Set up alerting
- [ ] Document runbooks

### Scalability
- [ ] Separate database to dedicated server
- [ ] Use managed database service (AWS RDS, Azure Database)
- [ ] Configure auto-scaling
- [ ] Add load balancer
- [ ] Implement circuit breakers
- [ ] Use message queue for async operations

---

## Support

For issues, questions, or contributions:
- Create an issue in the repository
- Contact the development team
- Check the project wiki for additional documentation

---

## License

[Specify your license here]

---

**Version:** 1.0.0
**Last Updated:** 2024-01-15
