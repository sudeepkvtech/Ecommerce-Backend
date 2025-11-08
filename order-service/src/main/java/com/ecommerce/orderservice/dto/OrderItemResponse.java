package com.ecommerce.orderservice.dto;

// Import Lombok annotations
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Import BigDecimal for monetary values
import java.math.BigDecimal;

// Import time class
import java.time.LocalDateTime;

/**
 * Order Item Response DTO
 *
 * Returned when retrieving order details.
 * Contains complete information about a product in an order.
 *
 * This DTO shows the product snapshot at time of order:
 * - Product name and price as they were when ordered
 * - Not current product details (which may have changed)
 *
 * Example JSON response:
 * {
 *   "id": 1,
 *   "productId": 123,
 *   "productName": "Wireless Mouse",
 *   "productPrice": 29.99,
 *   "quantity": 2,
 *   "subtotal": 59.98,
 *   "createdAt": "2024-01-15T10:30:00",
 *   "updatedAt": "2024-01-15T10:30:00"
 * }
 */
@Data               // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: no-arg constructor
@AllArgsConstructor // Lombok: all-args constructor
@Builder            // Lombok: builder pattern for clean object creation
public class OrderItemResponse {

    /**
     * Order Item ID
     *
     * Unique identifier for this order item.
     * Database-generated value.
     */
    private Long id;

    /**
     * Product ID
     *
     * Reference to product in Product Service.
     * Can be used to fetch current product details if needed.
     *
     * Note: May be null if product was deleted from catalog
     * but order item preserves historical data.
     */
    private Long productId;

    /**
     * Product Name (Snapshot)
     *
     * Name of the product at time of purchase.
     * Preserved even if product is renamed or deleted.
     *
     * Example: "MacBook Pro 2023"
     * Even if product later renamed to "MacBook Pro M3",
     * this order shows the original name.
     */
    private String productName;

    /**
     * Product Price (Snapshot)
     *
     * Price per unit at time of purchase.
     * Preserved even if product price changes later.
     *
     * Example: 29.99
     * If product price later increases to 34.99,
     * this order shows the original price of 29.99.
     */
    private BigDecimal productPrice;

    /**
     * Quantity
     *
     * Number of units ordered.
     *
     * Example: 2
     */
    private Integer quantity;

    /**
     * Subtotal
     *
     * Total price for this line item.
     * Calculated as: productPrice × quantity
     *
     * Example:
     * productPrice = 29.99
     * quantity = 2
     * subtotal = 59.98
     */
    private BigDecimal subtotal;

    /**
     * Created Timestamp
     *
     * When this order item was created.
     */
    private LocalDateTime createdAt;

    /**
     * Updated Timestamp
     *
     * When this order item was last modified.
     */
    private LocalDateTime updatedAt;
}
