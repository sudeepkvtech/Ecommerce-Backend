package com.ecommerce.inventoryservice.dto;

// Import Lombok annotations
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Import time class
import java.time.LocalDateTime;

/**
 * Inventory Response DTO
 *
 * Returned when retrieving inventory information.
 * Contains current stock levels and product details.
 *
 * When is this returned?
 * ======================
 * 1. GET /api/inventory/{productId}
 *    - Check stock for a product
 *    - Show available quantity
 *
 * 2. After stock operations:
 *    - POST /api/inventory/{productId}/reserve
 *    - POST /api/inventory/{productId}/release
 *    - POST /api/inventory/{productId}/add
 *    - Returns updated inventory
 *
 * 3. GET /api/inventory/low-stock
 *    - List of low stock items
 *    - Returns array of InventoryResponse
 *
 * Example JSON response:
 * ======================
 * {
 *   "id": 1,
 *   "productId": 123,
 *   "availableQuantity": 85,
 *   "reservedQuantity": 15,
 *   "totalQuantity": 100,
 *   "lowStockThreshold": 10,
 *   "isLowStock": false,
 *   "createdAt": "2024-01-01T10:00:00",
 *   "updatedAt": "2024-01-15T14:30:00"
 * }
 *
 * Who uses this?
 * ==============
 * - Product Service: Display stock on product page
 * - Order Service: Check stock before creating order
 * - Frontend: Show "X in stock" to customer
 * - Admin Panel: Monitor inventory levels
 */
@Data               // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: no-arg constructor
@AllArgsConstructor // Lombok: all-args constructor
@Builder            // Lombok: builder pattern
public class InventoryResponse {

    /**
     * Inventory ID
     *
     * Unique identifier for inventory record.
     * Database-generated value.
     *
     * Example: 1
     */
    private Long id;

    /**
     * Product ID
     *
     * Reference to product in Product Service.
     * Which product this inventory is for.
     *
     * Example: 123 (Blue T-Shirt)
     */
    private Long productId;

    /**
     * Available Quantity
     *
     * How many units can be sold right now.
     * This is what customer sees as "in stock".
     *
     * Formula: total - reserved
     *
     * Display to customer:
     * - "85 in stock"
     * - "Only 5 left!"
     * - "Out of stock" (if 0)
     *
     * Example: 85
     */
    private Integer availableQuantity;

    /**
     * Reserved Quantity
     *
     * How many units are reserved for pending orders.
     * Not available for sale until released.
     *
     * Why show this?
     * - Admin dashboard needs full picture
     * - Understand pending order volume
     * - Track order pipeline
     *
     * Not typically shown to customers.
     *
     * Example: 15
     */
    private Integer reservedQuantity;

    /**
     * Total Quantity
     *
     * Total physical stock in warehouse.
     * Sum of available and reserved.
     *
     * Formula: available + reserved
     *
     * Uses:
     * - Reordering decisions
     * - Inventory value calculation
     * - Warehouse planning
     *
     * Example: 100
     */
    private Integer totalQuantity;

    /**
     * Low Stock Threshold
     *
     * Minimum stock level before alert.
     * When available < threshold, needs restocking.
     *
     * Can be configured per product:
     * - Popular items: higher threshold (50)
     * - Regular items: medium threshold (20)
     * - Slow movers: lower threshold (5)
     *
     * Example: 10
     */
    private Integer lowStockThreshold;

    /**
     * Is Low Stock
     *
     * Convenience field indicating if stock is low.
     * True if available < threshold.
     *
     * Calculation:
     * isLowStock = availableQuantity < lowStockThreshold
     *
     * Uses:
     * - Quick visual indicator
     * - Filter low stock items
     * - Alert badges in UI
     *
     * Display:
     * - Red badge: "Low Stock!"
     * - Warning icon
     * - Alert in admin dashboard
     *
     * Example: false (85 >= 10, so not low)
     */
    private Boolean isLowStock;

    /**
     * Created At
     *
     * When this inventory record was created.
     * Usually when product was first added.
     *
     * Format: ISO 8601 date-time
     * Example: "2024-01-01T10:00:00"
     */
    private LocalDateTime createdAt;

    /**
     * Updated At
     *
     * When inventory was last modified.
     * Updated on every stock change.
     *
     * Uses:
     * - See when stock last changed
     * - Audit trail
     * - Freshness indicator
     *
     * Example: "2024-01-15T14:30:00"
     */
    private LocalDateTime updatedAt;

    /**
     * Additional Fields (Not Included)
     * =================================
     *
     * Could add in future:
     *
     * 1. Product name (from Product Service):
     *    private String productName;
     *    // Avoid extra service call on frontend
     *
     * 2. Stock status enum:
     *    private StockStatus status;  // IN_STOCK, LOW_STOCK, OUT_OF_STOCK
     *    // Easier frontend logic
     *
     * 3. Next restock date:
     *    private LocalDate nextRestockDate;
     *    // When new stock expected
     *
     * 4. Location/warehouse:
     *    private String warehouseLocation;
     *    // Multi-warehouse support
     */

    /**
     * Example Usage
     * =============
     *
     * Frontend display:
     * -----------------
     * if (inventory.isLowStock) {
     *     badge = "Low Stock!";
     *     color = "red";
     * } else if (inventory.availableQuantity > 0) {
     *     badge = inventory.availableQuantity + " in stock";
     *     color = "green";
     * } else {
     *     badge = "Out of Stock";
     *     color = "gray";
     *     disableAddToCart = true;
     * }
     *
     * Order validation:
     * -----------------
     * if (requestedQuantity > inventory.availableQuantity) {
     *     throw new BadRequestException(
     *         "Insufficient stock. Available: " + inventory.availableQuantity +
     *         ", Requested: " + requestedQuantity
     *     );
     * }
     *
     * Admin dashboard:
     * ----------------
     * Product: Blue T-Shirt (#123)
     * Available: 85 units
     * Reserved: 15 units (pending orders)
     * Total: 100 units
     * Threshold: 10 units
     * Status: ✓ In Stock
     * Last Updated: Jan 15, 2024 2:30 PM
     */
}
