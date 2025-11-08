package com.ecommerce.orderservice.dto;

// Import validation annotations
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Import Lombok annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Order Item Request DTO
 *
 * Used when creating or updating order items.
 * Represents a single product in an order with quantity.
 *
 * This DTO is used in the OrderRequest to specify which products
 * and how many of each to include in an order.
 *
 * Example JSON:
 * {
 *   "productId": 123,
 *   "quantity": 2
 * }
 *
 * The service will fetch product details (name, price) from Product Service
 * using the productId.
 */
@Data               // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: no-argument constructor for Jackson deserialization
@AllArgsConstructor // Lombok: constructor with all fields for testing
public class OrderItemRequest {

    /**
     * Product ID
     *
     * Reference to the product in Product Service.
     * Service will validate this product exists and get its details.
     *
     * Validations:
     * - @NotNull: Product ID is required
     *
     * Example: 123 (refers to product with ID 123)
     */
    @NotNull(message = "Product ID is required")
    private Long productId;

    /**
     * Quantity
     *
     * How many units of this product to order.
     *
     * Validations:
     * - @NotNull: Quantity is required
     * - @Min(1): Must order at least 1 unit
     *
     * Business rules (validated in service layer):
     * - Should not exceed available inventory
     * - May have maximum order quantity limits
     *
     * Example: 2 (order 2 units of this product)
     */
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    // Note: Price is not included here
    // Service fetches current price from Product Service
    // This ensures order uses the latest price at time of placement
}
