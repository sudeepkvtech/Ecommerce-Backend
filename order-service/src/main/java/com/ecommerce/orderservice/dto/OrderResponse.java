package com.ecommerce.orderservice.dto;

// Import OrderStatus enum
import com.ecommerce.orderservice.entity.OrderStatus;

// Import Lombok annotations
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Import BigDecimal for monetary values
import java.math.BigDecimal;

// Import time class
import java.time.LocalDateTime;

// Import List
import java.util.List;

/**
 * Order Response DTO
 *
 * Returned when retrieving order details.
 * Contains complete order information including all order items.
 *
 * This DTO is used for:
 * - Order creation response (after POST /api/orders)
 * - Order retrieval (GET /api/orders/{id})
 * - Order list (GET /api/orders)
 * - Order updates (after PUT /api/orders/{id}/status)
 *
 * Example JSON response:
 * {
 *   "id": 1,
 *   "orderNumber": "ORD-20240115-001",
 *   "userId": 123,
 *   "status": "CONFIRMED",
 *   "totalAmount": 1059.97,
 *   "shippingAddressId": 789,
 *   "paymentMethod": "CREDIT_CARD",
 *   "items": [
 *     {
 *       "id": 1,
 *       "productId": 456,
 *       "productName": "Wireless Mouse",
 *       "productPrice": 29.99,
 *       "quantity": 2,
 *       "subtotal": 59.98
 *     },
 *     {
 *       "id": 2,
 *       "productId": 789,
 *       "productName": "Laptop",
 *       "productPrice": 999.99,
 *       "quantity": 1,
 *       "subtotal": 999.99
 *     }
 *   ],
 *   "createdAt": "2024-01-15T10:30:00",
 *   "updatedAt": "2024-01-15T10:35:00"
 * }
 */
@Data               // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: no-arg constructor
@AllArgsConstructor // Lombok: all-args constructor
@Builder            // Lombok: builder pattern for clean object creation
public class OrderResponse {

    /**
     * Order ID
     *
     * Unique database identifier for the order.
     * Internal ID, not shown to customers.
     *
     * Example: 1
     */
    private Long id;

    /**
     * Order Number
     *
     * Human-readable unique identifier.
     * Customer-facing order tracking number.
     *
     * Format: ORD-YYYYMMDD-NNN
     * Example: ORD-20240115-001
     *
     * This is what customers use to:
     * - Track their order
     * - Contact customer service
     * - Reference in communications
     */
    private String orderNumber;

    /**
     * User ID
     *
     * ID of the user who placed this order.
     * References user in User Service.
     *
     * Example: 123
     *
     * Frontend can use this to:
     * - Verify order belongs to current user
     * - Fetch user details if needed
     * - Display user name/email
     */
    private Long userId;

    /**
     * Order Status
     *
     * Current state of the order.
     * See OrderStatus enum for all possible values.
     *
     * Possible values:
     * - PENDING: Order created, awaiting payment
     * - CONFIRMED: Payment received, order confirmed
     * - PROCESSING: Order being prepared
     * - SHIPPED: Order dispatched
     * - DELIVERED: Order delivered to customer
     * - CANCELLED: Order cancelled
     *
     * Example: "CONFIRMED"
     *
     * Frontend uses this to:
     * - Show order progress
     * - Enable/disable actions (e.g., cancel button)
     * - Display status badge with color
     */
    private OrderStatus status;

    /**
     * Total Amount
     *
     * Total cost of the order.
     * Sum of all order item subtotals.
     *
     * Example: 1059.97
     * (59.98 + 999.99 = 1059.97)
     *
     * Note: In production, might also include:
     * - Shipping costs
     * - Taxes
     * - Discounts
     */
    private BigDecimal totalAmount;

    /**
     * Shipping Address ID
     *
     * Reference to delivery address in User Service.
     *
     * Example: 789
     *
     * Frontend can use this to:
     * - Fetch full address details
     * - Display shipping address
     * - Show delivery location
     */
    private Long shippingAddressId;

    /**
     * Payment Method
     *
     * How customer paid for this order.
     *
     * Example: "CREDIT_CARD"
     *
     * Frontend uses this to:
     * - Display payment method on order details
     * - Show payment icon
     */
    private String paymentMethod;

    /**
     * Order Items
     *
     * List of products in this order.
     * Each item contains product details and quantity.
     *
     * Example:
     * [
     *   {"productId": 456, "productName": "Mouse", "quantity": 2, "subtotal": 59.98},
     *   {"productId": 789, "productName": "Laptop", "quantity": 1, "subtotal": 999.99}
     * ]
     *
     * Frontend uses this to:
     * - Display order contents
     * - Show itemized breakdown
     * - Calculate item count
     */
    private List<OrderItemResponse> items;

    /**
     * Created Timestamp
     *
     * When the order was placed.
     *
     * Example: "2024-01-15T10:30:00"
     *
     * Frontend uses this to:
     * - Display order date
     * - Sort orders chronologically
     * - Calculate estimated delivery date
     */
    private LocalDateTime createdAt;

    /**
     * Updated Timestamp
     *
     * When the order was last modified.
     *
     * Example: "2024-01-15T10:35:00"
     *
     * Updated when:
     * - Order status changes
     * - Order details modified
     * - Items added/removed
     *
     * Frontend uses this to:
     * - Show last update time
     * - Track order modifications
     */
    private LocalDateTime updatedAt;

    // =========================================================================
    // Builder Pattern Usage Example
    // =========================================================================
    //
    // OrderResponse response = OrderResponse.builder()
    //     .id(order.getId())
    //     .orderNumber(order.getOrderNumber())
    //     .userId(order.getUserId())
    //     .status(order.getStatus())
    //     .totalAmount(order.getTotalAmount())
    //     .shippingAddressId(order.getShippingAddressId())
    //     .paymentMethod(order.getPaymentMethod())
    //     .items(itemResponses)
    //     .createdAt(order.getCreatedAt())
    //     .updatedAt(order.getUpdatedAt())
    //     .build();
    //
    // =========================================================================
    // Frontend Display Examples
    // =========================================================================
    //
    // Order Summary Card:
    // -------------------
    // Order #ORD-20240115-001
    // Status: CONFIRMED
    // Total: $1,059.97
    // Placed: Jan 15, 2024 10:30 AM
    //
    // Order Details Page:
    // -------------------
    // Order Number: ORD-20240115-001
    // Status: CONFIRMED
    // Order Date: Jan 15, 2024 10:30 AM
    // Payment: Credit Card
    //
    // Items:
    // 1. Wireless Mouse x 2 - $59.98
    // 2. Laptop x 1 - $999.99
    //
    // Subtotal: $1,059.97
    // Shipping: FREE
    // Total: $1,059.97
    //
    // =========================================================================
    // Status Badge Colors (Frontend)
    // =========================================================================
    //
    // PENDING: Yellow/Orange (awaiting action)
    // CONFIRMED: Blue (in progress)
    // PROCESSING: Blue (in progress)
    // SHIPPED: Purple (in transit)
    // DELIVERED: Green (complete)
    // CANCELLED: Red (terminated)
}
