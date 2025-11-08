package com.ecommerce.orderservice.controller;

// Import DTOs
import com.ecommerce.orderservice.dto.*;

// Import OrderStatus enum
import com.ecommerce.orderservice.entity.OrderStatus;

// Import service
import com.ecommerce.orderservice.service.OrderService;

// Import Spring annotations
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Import validation
import jakarta.validation.Valid;

// Import Lombok
import lombok.RequiredArgsConstructor;

// Import List
import java.util.List;

/**
 * Order Controller
 *
 * REST API endpoints for order management.
 *
 * Base URL: /api/orders
 * All endpoints require authentication (JWT token)
 *
 * Endpoints:
 * - POST /api/orders - Create new order
 * - GET /api/orders - Get user's orders
 * - GET /api/orders/{id} - Get order by ID
 * - GET /api/orders/number/{orderNumber} - Get order by order number
 * - PUT /api/orders/{id}/status - Update order status (admin)
 * - PUT /api/orders/{id}/cancel - Cancel order
 * - GET /api/orders/status/{status} - Get orders by status (admin)
 *
 * Authentication:
 * - User identity extracted from JWT token
 * - Users can only access their own orders
 * - Admins can access all orders and update status
 */
@RestController  // Marks this as a REST controller
@RequestMapping("/api/orders")  // Base URL for all endpoints
@RequiredArgsConstructor  // Lombok generates constructor for final fields
public class OrderController {

    // OrderService dependency injected via constructor
    private final OrderService orderService;

    /**
     * Create new order
     *
     * Allows authenticated users to place a new order.
     *
     * URL: POST /api/orders
     * Authentication: Required (JWT token)
     * Authorization: Any authenticated user
     *
     * Request body:
     * {
     *   "items": [
     *     {"productId": 123, "quantity": 2},
     *     {"productId": 456, "quantity": 1}
     *   ],
     *   "shippingAddressId": 789,
     *   "paymentMethod": "CREDIT_CARD"
     * }
     *
     * Response: 201 Created
     * {
     *   "id": 1,
     *   "orderNumber": "ORD-20240115-001",
     *   "userId": 1,
     *   "status": "PENDING",
     *   "totalAmount": 299.97,
     *   "shippingAddressId": 789,
     *   "paymentMethod": "CREDIT_CARD",
     *   "items": [...],
     *   "createdAt": "2024-01-15T10:30:00",
     *   "updatedAt": "2024-01-15T10:30:00"
     * }
     *
     * Error responses:
     * - 400 Bad Request: Invalid request data (validation errors)
     * - 401 Unauthorized: No JWT token provided
     * - 404 Not Found: Product not found, Address not found
     *
     * @param request OrderRequest with items and shipping details
     * @param authentication Current authenticated user (from JWT)
     * @return ResponseEntity with created OrderResponse and HTTP 201
     */
    @PostMapping  // Maps to POST /api/orders
    public ResponseEntity<OrderResponse> createOrder(
            // @Valid triggers validation on OrderRequest
            // Validates: items not empty, each item valid, address not null
            @Valid @RequestBody OrderRequest request,

            // Authentication injected by Spring Security
            // Contains user information from JWT token
            Authentication authentication) {

        // Extract user email from JWT token
        // This is the username stored in the token
        String userEmail = authentication.getName();

        // Create order via service
        // Service will:
        // 1. Get user ID from email
        // 2. Validate products and addresses
        // 3. Calculate totals
        // 4. Generate order number
        // 5. Save to database
        OrderResponse order = orderService.createOrder(userEmail, request);

        // Return HTTP 201 Created with order details
        // HTTP 201 indicates successful resource creation
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    /**
     * Get user's orders
     *
     * Retrieves order history for the authenticated user.
     * Orders sorted by creation date (newest first).
     *
     * URL: GET /api/orders
     * Authentication: Required (JWT token)
     * Authorization: Any authenticated user
     *
     * Response: 200 OK
     * [
     *   {
     *     "id": 2,
     *     "orderNumber": "ORD-20240116-001",
     *     "status": "SHIPPED",
     *     ...
     *   },
     *   {
     *     "id": 1,
     *     "orderNumber": "ORD-20240115-001",
     *     "status": "DELIVERED",
     *     ...
     *   }
     * ]
     *
     * Returns empty array [] if user has no orders.
     *
     * @param authentication Current authenticated user
     * @return ResponseEntity with List of OrderResponse and HTTP 200
     */
    @GetMapping  // Maps to GET /api/orders
    public ResponseEntity<List<OrderResponse>> getUserOrders(
            Authentication authentication) {

        // Extract user email from token
        String userEmail = authentication.getName();

        // Get user's orders from service
        // Returns orders sorted by creation date (newest first)
        List<OrderResponse> orders = orderService.getUserOrders(userEmail);

        // Return HTTP 200 OK with orders list
        return ResponseEntity.ok(orders);
    }

    /**
     * Get order by ID
     *
     * Retrieves a specific order by its database ID.
     * Users can only access their own orders.
     *
     * URL: GET /api/orders/{id}
     * Authentication: Required (JWT token)
     * Authorization: Order owner only
     *
     * Request: GET /api/orders/123
     *
     * Response: 200 OK
     * {
     *   "id": 123,
     *   "orderNumber": "ORD-20240115-001",
     *   "status": "CONFIRMED",
     *   ...
     * }
     *
     * Error responses:
     * - 404 Not Found: Order doesn't exist
     * - 400 Bad Request: Order doesn't belong to user
     *
     * @param id The order ID
     * @param authentication Current authenticated user
     * @return ResponseEntity with OrderResponse and HTTP 200
     */
    @GetMapping("/{id}")  // Maps to GET /api/orders/{id}
    public ResponseEntity<OrderResponse> getOrderById(
            // @PathVariable extracts {id} from URL
            // Example: GET /api/orders/123 → id = 123
            @PathVariable Long id,

            Authentication authentication) {

        // Extract user email
        String userEmail = authentication.getName();

        // Get order by ID
        // Service validates order belongs to user
        OrderResponse order = orderService.getOrderById(id, userEmail);

        // Return HTTP 200 OK with order details
        return ResponseEntity.ok(order);
    }

    /**
     * Get order by order number
     *
     * Retrieves order using customer-facing order number.
     * Useful for order tracking.
     *
     * URL: GET /api/orders/number/{orderNumber}
     * Authentication: Required (JWT token)
     * Authorization: Order owner only
     *
     * Request: GET /api/orders/number/ORD-20240115-001
     *
     * Response: 200 OK
     * {
     *   "id": 1,
     *   "orderNumber": "ORD-20240115-001",
     *   "status": "SHIPPED",
     *   ...
     * }
     *
     * Error responses:
     * - 404 Not Found: Order number doesn't exist
     * - 400 Bad Request: Order doesn't belong to user
     *
     * @param orderNumber The order number (e.g., ORD-20240115-001)
     * @param authentication Current authenticated user
     * @return ResponseEntity with OrderResponse and HTTP 200
     */
    @GetMapping("/number/{orderNumber}")  // Maps to GET /api/orders/number/{orderNumber}
    public ResponseEntity<OrderResponse> getOrderByOrderNumber(
            @PathVariable String orderNumber,
            Authentication authentication) {

        // Extract user email
        String userEmail = authentication.getName();

        // Get order by order number
        // Service validates order belongs to user
        OrderResponse order = orderService.getOrderByOrderNumber(orderNumber, userEmail);

        // Return HTTP 200 OK with order details
        return ResponseEntity.ok(order);
    }

    /**
     * Update order status (Admin only)
     *
     * Allows administrators to change order status.
     * Used for order lifecycle management.
     *
     * URL: PUT /api/orders/{id}/status
     * Authentication: Required (JWT token)
     * Authorization: ROLE_ADMIN only
     *
     * Request: PUT /api/orders/123/status
     * {
     *   "status": "SHIPPED"
     * }
     *
     * Response: 200 OK
     * {
     *   "id": 123,
     *   "orderNumber": "ORD-20240115-001",
     *   "status": "SHIPPED",  // Updated status
     *   ...
     * }
     *
     * Valid status transitions:
     * - PENDING → CONFIRMED, CANCELLED
     * - CONFIRMED → PROCESSING, CANCELLED
     * - PROCESSING → SHIPPED, CANCELLED
     * - SHIPPED → DELIVERED
     *
     * Error responses:
     * - 403 Forbidden: User is not an admin
     * - 404 Not Found: Order doesn't exist
     * - 400 Bad Request: Invalid status transition
     *
     * @PreAuthorize annotation:
     * - Checks user roles BEFORE executing method
     * - Only users with ROLE_ADMIN can call this
     * - Returns HTTP 403 if user lacks permission
     *
     * @param id The order ID
     * @param request UpdateOrderStatusRequest with new status
     * @return ResponseEntity with updated OrderResponse and HTTP 200
     */
    @PutMapping("/{id}/status")  // Maps to PUT /api/orders/{id}/status
    @PreAuthorize("hasRole('ADMIN')")  // Admin only
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,

            // @Valid triggers validation on UpdateOrderStatusRequest
            // Validates: status is not null
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        // Update order status via service
        // Service validates status transition is allowed
        OrderResponse order = orderService.updateOrderStatus(id, request.getStatus());

        // Return HTTP 200 OK with updated order
        return ResponseEntity.ok(order);
    }

    /**
     * Cancel order
     *
     * Allows users to cancel their own orders.
     * Only PENDING or CONFIRMED orders can be cancelled.
     *
     * URL: PUT /api/orders/{id}/cancel
     * Authentication: Required (JWT token)
     * Authorization: Order owner only
     *
     * Request: PUT /api/orders/123/cancel
     *
     * Response: 200 OK
     * {
     *   "id": 123,
     *   "orderNumber": "ORD-20240115-001",
     *   "status": "CANCELLED",  // Updated to CANCELLED
     *   ...
     * }
     *
     * Business rules:
     * - Can only cancel own orders
     * - Can only cancel PENDING or CONFIRMED orders
     * - Cannot cancel PROCESSING, SHIPPED, or DELIVERED orders
     * - Already CANCELLED orders return error
     *
     * Error responses:
     * - 404 Not Found: Order doesn't exist
     * - 400 Bad Request: Order doesn't belong to user
     * - 400 Bad Request: Order cannot be cancelled (wrong status)
     *
     * @param id The order ID
     * @param authentication Current authenticated user
     * @return ResponseEntity with cancelled OrderResponse and HTTP 200
     */
    @PutMapping("/{id}/cancel")  // Maps to PUT /api/orders/{id}/cancel
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id,
            Authentication authentication) {

        // Extract user email
        String userEmail = authentication.getName();

        // Cancel order via service
        // Service validates:
        // - Order belongs to user
        // - Order can be cancelled (PENDING or CONFIRMED)
        OrderResponse order = orderService.cancelOrder(id, userEmail);

        // Return HTTP 200 OK with cancelled order
        return ResponseEntity.ok(order);
    }

    /**
     * Get orders by status (Admin only)
     *
     * Retrieves all orders with a specific status.
     * Used for administrative dashboards.
     *
     * URL: GET /api/orders/status/{status}
     * Authentication: Required (JWT token)
     * Authorization: ROLE_ADMIN only
     *
     * Request: GET /api/orders/status/PENDING
     *
     * Response: 200 OK
     * [
     *   {
     *     "id": 123,
     *     "orderNumber": "ORD-20240115-001",
     *     "status": "PENDING",
     *     ...
     *   },
     *   {
     *     "id": 124,
     *     "orderNumber": "ORD-20240115-002",
     *     "status": "PENDING",
     *     ...
     *   }
     * ]
     *
     * Valid status values:
     * - PENDING
     * - CONFIRMED
     * - PROCESSING
     * - SHIPPED
     * - DELIVERED
     * - CANCELLED
     *
     * Use cases:
     * - Show all pending orders (awaiting processing)
     * - Show all orders being processed (warehouse queue)
     * - Show all shipped orders (in transit)
     * - Show cancelled orders (for analytics)
     *
     * @param status The order status to filter by
     * @return ResponseEntity with List of OrderResponse and HTTP 200
     */
    @GetMapping("/status/{status}")  // Maps to GET /api/orders/status/{status}
    @PreAuthorize("hasRole('ADMIN')")  // Admin only
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(
            // @PathVariable automatically converts string to OrderStatus enum
            // Example: GET /api/orders/status/PENDING → status = OrderStatus.PENDING
            @PathVariable OrderStatus status) {

        // Get orders with specified status
        List<OrderResponse> orders = orderService.getOrdersByStatus(status);

        // Return HTTP 200 OK with orders list
        return ResponseEntity.ok(orders);
    }

    // =========================================================================
    // HTTP Method Summary
    // =========================================================================
    //
    // POST /api/orders
    //   - Create new order
    //   - Returns HTTP 201 Created
    //
    // GET /api/orders
    //   - Get user's orders (order history)
    //   - Returns HTTP 200 OK
    //
    // GET /api/orders/{id}
    //   - Get specific order by ID
    //   - Returns HTTP 200 OK
    //
    // GET /api/orders/number/{orderNumber}
    //   - Get order by order number
    //   - Returns HTTP 200 OK
    //
    // PUT /api/orders/{id}/status
    //   - Update order status (admin only)
    //   - Returns HTTP 200 OK
    //
    // PUT /api/orders/{id}/cancel
    //   - Cancel order (user's own order)
    //   - Returns HTTP 200 OK
    //
    // GET /api/orders/status/{status}
    //   - Get orders by status (admin only)
    //   - Returns HTTP 200 OK
    //
    // =========================================================================
    // Security Summary
    // =========================================================================
    //
    // Authentication:
    // - All endpoints require JWT token
    // - Token validated by JwtAuthenticationFilter
    // - User email extracted from token
    //
    // Authorization:
    // - Regular users: Can only access their own orders
    // - Admins: Can access all orders, update status
    //
    // Endpoint permissions:
    // - Create order: Any user
    // - Get own orders: Any user
    // - Get order by ID: Order owner only
    // - Get order by number: Order owner only
    // - Update status: Admin only (@PreAuthorize)
    // - Cancel order: Order owner only
    // - Get by status: Admin only (@PreAuthorize)
    //
    // =========================================================================
    // Example cURL Commands
    // =========================================================================
    //
    // Create order:
    // curl -X POST http://localhost:8083/order-service/api/orders \
    //   -H "Authorization: Bearer YOUR_JWT_TOKEN" \
    //   -H "Content-Type: application/json" \
    //   -d '{
    //     "items": [
    //       {"productId": 1, "quantity": 2},
    //       {"productId": 2, "quantity": 1}
    //     ],
    //     "shippingAddressId": 1,
    //     "paymentMethod": "CREDIT_CARD"
    //   }'
    //
    // Get user orders:
    // curl http://localhost:8083/order-service/api/orders \
    //   -H "Authorization: Bearer YOUR_JWT_TOKEN"
    //
    // Get order by ID:
    // curl http://localhost:8083/order-service/api/orders/1 \
    //   -H "Authorization: Bearer YOUR_JWT_TOKEN"
    //
    // Cancel order:
    // curl -X PUT http://localhost:8083/order-service/api/orders/1/cancel \
    //   -H "Authorization: Bearer YOUR_JWT_TOKEN"
    //
    // Update status (admin):
    // curl -X PUT http://localhost:8083/order-service/api/orders/1/status \
    //   -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
    //   -H "Content-Type: application/json" \
    //   -d '{"status": "SHIPPED"}'
}
