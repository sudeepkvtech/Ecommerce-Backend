package com.ecommerce.inventoryservice.dto;

// Import validation annotations
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Import Lombok annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reserve Stock Request DTO
 *
 * Used when reserving stock for a pending order.
 * Called by Order Service when customer creates order.
 *
 * API Endpoint:
 * =============
 * POST /api/inventory/{productId}/reserve
 *
 * Request Body:
 * {
 *   "quantity": 2,
 *   "referenceId": "ORDER-123"
 * }
 *
 * What happens when this is called?
 * ==================================
 * 1. Order Service creates order (status: PENDING)
 * 2. Order Service calls Inventory Service to reserve stock
 * 3. Inventory Service:
 *    - Checks if sufficient stock available
 *    - Moves stock from available to reserved
 *    - Creates RESERVATION stock movement
 *    - Returns updated inventory
 * 4. Stock is now held for this order
 * 5. Other customers cannot buy this stock
 * 6. If payment succeeds: commit reservation (stock leaves warehouse)
 * 7. If payment fails: release reservation (back to available)
 *
 * Why reserve stock?
 * ==================
 * - Prevents overselling (selling more than available)
 * - Holds stock during payment processing
 * - Fair to customers (first to order gets the item)
 * - Avoids disappointing customers (order accepted but can't fulfill)
 *
 * Example Scenario:
 * =================
 * Product: Blue T-Shirt
 * Available: 100 units
 *
 * Customer A orders 2 units:
 * - Reserve 2 units
 * - Available: 100 → 98
 * - Reserved: 0 → 2
 * - Total: 100 (unchanged)
 *
 * Customer B tries to order 99 units:
 * - Only 98 available
 * - Order rejected: "Insufficient stock"
 * - Prevents overselling
 *
 * Customer A payment succeeds:
 * - Commit 2 units
 * - Available: 98 (unchanged)
 * - Reserved: 2 → 0
 * - Total: 100 → 98 (stock shipped)
 *
 * Timeout Handling:
 * =================
 * Reservations should have timeout (e.g., 30 minutes).
 * If payment not completed in time:
 * - Automatically release reservation
 * - Stock returns to available
 * - Prevents stuck reservations
 *
 * Implementation Note:
 * This simple version doesn't include timeout.
 * In production, add:
 * - reservedAt timestamp
 * - expiresAt timestamp
 * - Background job to release expired reservations
 */
@Data               // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: no-arg constructor for Jackson deserialization
@AllArgsConstructor // Lombok: all-args constructor for testing
public class ReserveStockRequest {

    /**
     * Quantity
     *
     * How many units to reserve.
     * Must be positive and not exceed available stock.
     *
     * Validations:
     * - @NotNull: Required field
     * - @Min(1): Must be at least 1
     *
     * Business validations (in service layer):
     * - Must not exceed available quantity
     * - Available stock must be sufficient
     *
     * Example: 2 (reserve 2 units)
     *
     * Error scenarios:
     * - quantity = 0: "Quantity must be at least 1"
     * - quantity = -1: "Quantity must be at least 1"
     * - quantity = 100, available = 50: "Insufficient stock. Available: 50, Requested: 100"
     */
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    /**
     * Reference ID
     *
     * External reference for this reservation.
     * Typically the order ID that triggered this reservation.
     *
     * Validations:
     * - @NotNull: Required field
     *
     * Why required?
     * - Links reservation to order
     * - Needed for release/commit operations
     * - Audit trail
     * - Tracking and debugging
     *
     * Format:
     * - Order ID: "ORDER-123"
     * - Could be other references depending on use case
     *
     * Example: "ORDER-123"
     *
     * Usage in service:
     * - Create StockMovement with this referenceId
     * - Later: find reservation by referenceId to release/commit
     * - Track which order reserved this stock
     */
    @NotNull(message = "Reference ID is required")
    private String referenceId;

    /**
     * Additional Fields (Not Implemented)
     * ====================================
     *
     * For more advanced reservation system:
     *
     * 1. Reservation timeout:
     *    private Integer reservationTimeoutMinutes;
     *    // How long to hold before auto-release
     *    // Default: 30 minutes
     *
     * 2. Priority:
     *    private ReservationPriority priority;
     *    // HIGH, NORMAL, LOW
     *    // Premium customers get HIGH priority
     *
     * 3. Customer ID:
     *    private Long customerId;
     *    // Track which customer reserved
     *    // Prevent same customer hoarding stock
     *
     * 4. Notes:
     *    private String notes;
     *    // Additional context
     *    // "Flash sale order", "VIP customer", etc.
     */

    /**
     * Example Usage
     * =============
     *
     * In Order Service (when creating order):
     * ----------------------------------------
     * // Customer places order
     * Order order = createOrder(orderRequest);
     *
     * // Reserve stock for each item
     * for (OrderItem item : order.getItems()) {
     *     ReserveStockRequest reserveRequest = new ReserveStockRequest();
     *     reserveRequest.setQuantity(item.getQuantity());
     *     reserveRequest.setReferenceId(order.getOrderNumber());
     *
     *     try {
     *         inventoryService.reserveStock(item.getProductId(), reserveRequest);
     *     } catch (InsufficientStockException e) {
     *         // Rollback: release already reserved items
     *         rollbackReservations(order);
     *         throw new BadRequestException("Insufficient stock for " + item.getProductName());
     *     }
     * }
     *
     * // All items reserved successfully
     * // Proceed with payment
     *
     * In Payment Service callback:
     * ----------------------------
     * if (payment.getStatus() == PaymentStatus.COMPLETED) {
     *     // Payment succeeded - commit reservations
     *     CommitReservationRequest commitRequest = new CommitReservationRequest();
     *     commitRequest.setQuantity(item.getQuantity());
     *     commitRequest.setReferenceId(order.getOrderNumber());
     *     inventoryService.commitReservation(item.getProductId(), commitRequest);
     * } else {
     *     // Payment failed - release reservations
     *     ReleaseStockRequest releaseRequest = new ReleaseStockRequest();
     *     releaseRequest.setQuantity(item.getQuantity());
     *     releaseRequest.setReferenceId(order.getOrderNumber());
     *     inventoryService.releaseReservation(item.getProductId(), releaseRequest);
     * }
     *
     * Validation Example:
     * -------------------
     * // Invalid: quantity = 0
     * ReserveStockRequest request = new ReserveStockRequest(0, "ORDER-123");
     * // Validation error: "Quantity must be at least 1"
     *
     * // Invalid: null quantity
     * request = new ReserveStockRequest(null, "ORDER-123");
     * // Validation error: "Quantity is required"
     *
     * // Invalid: null reference
     * request = new ReserveStockRequest(2, null);
     * // Validation error: "Reference ID is required"
     *
     * // Valid
     * request = new ReserveStockRequest(2, "ORDER-123");
     * // ✓ Passes validation
     */
}
