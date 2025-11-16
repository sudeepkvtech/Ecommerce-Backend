package com.ecommerce.inventoryservice.dto;

// Import validation annotations
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Import Lombok annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Release Stock Request DTO
 *
 * Used when releasing a previous stock reservation.
 * Called when payment fails or order is cancelled.
 *
 * API Endpoint:
 * =============
 * POST /api/inventory/{productId}/release
 *
 * Request Body:
 * {
 *   "quantity": 2,
 *   "referenceId": "ORDER-123",
 *   "reason": "Payment failed"
 * }
 *
 * What happens when this is called?
 * ==================================
 * 1. Payment Service processes payment → FAILED
 * 2. Payment Service notifies Order Service
 * 3. Order Service calls Inventory Service to release stock
 * 4. Inventory Service:
 *    - Moves stock from reserved back to available
 *    - Creates RELEASE stock movement
 *    - Stock is now available for other customers
 * 5. Order remains PENDING (can retry payment)
 *
 * When to release stock?
 * ======================
 * 1. Payment failed:
 *    - Card declined
 *    - Insufficient funds
 *    - Payment gateway error
 *
 * 2. Order cancelled:
 *    - Customer cancels before payment
 *    - Admin cancels order
 *
 * 3. Reservation timeout:
 *    - Payment took too long
 *    - Automatic release after 30 minutes
 *
 * Example Scenario:
 * =================
 * Product: Blue T-Shirt
 * Available: 98 units
 * Reserved: 2 units (for ORDER-123)
 *
 * Customer payment fails:
 * - Release 2 units
 * - Available: 98 → 100
 * - Reserved: 2 → 0
 * - Total: 100 (unchanged)
 *
 * Stock is now available for other customers again.
 */
@Data               // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: no-arg constructor for Jackson deserialization
@AllArgsConstructor // Lombok: all-args constructor for testing
public class ReleaseStockRequest {

    /**
     * Quantity
     *
     * How many units to release.
     * Must match previously reserved quantity.
     *
     * Validations:
     * - @NotNull: Required field
     * - @Min(1): Must be at least 1
     *
     * Business validations (in service layer):
     * - Must not exceed reserved quantity
     * - Cannot release more than was reserved
     *
     * Example: 2 (release 2 previously reserved units)
     *
     * Error scenarios:
     * - quantity = 5, reserved = 2: "Cannot release more than reserved. Reserved: 2, Requested: 5"
     */
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    /**
     * Reference ID
     *
     * External reference for this release.
     * Should match the original reservation's referenceId.
     *
     * Validations:
     * - @NotNull: Required field
     *
     * Why match original?
     * - Ensures releasing correct reservation
     * - Links to original order/transaction
     * - Audit trail shows RESERVATION → RELEASE
     * - Prevents releasing wrong stock
     *
     * Example: "ORDER-123"
     *
     * Usage:
     * - Find original RESERVATION movement by referenceId
     * - Create matching RELEASE movement
     * - Update inventory
     */
    @NotNull(message = "Reference ID is required")
    private String referenceId;

    /**
     * Reason
     *
     * Why the stock is being released.
     * Helps with analytics and debugging.
     *
     * Not required (optional field).
     *
     * Common reasons:
     * - "Payment failed - card declined"
     * - "Payment failed - insufficient funds"
     * - "Order cancelled by customer"
     * - "Order cancelled by admin"
     * - "Reservation timeout expired"
     * - "Payment gateway timeout"
     *
     * Uses:
     * - Analytics: Why do reservations fail?
     * - Debugging: Trace stock issues
     * - Reporting: Conversion funnel analysis
     * - Optimization: Improve checkout process
     *
     * Example: "Payment failed - card declined"
     *
     * Analytics examples:
     * - High "card declined" rate → payment flow issue
     * - High "timeout" rate → checkout too slow
     * - High "customer cancelled" → pricing/trust issue
     */
    private String reason;

    /**
     * Example Usage
     * =============
     *
     * In Payment Service (after payment failure):
     * --------------------------------------------
     * @PostMapping("/webhook")
     * public void handlePaymentWebhook(PaymentWebhook webhook) {
     *     Payment payment = findPayment(webhook.getPaymentId());
     *
     *     if (webhook.getStatus().equals("failed")) {
     *         // Payment failed - release reservations
     *         Order order = orderService.getOrder(payment.getOrderId());
     *
     *         for (OrderItem item : order.getItems()) {
     *             ReleaseStockRequest releaseRequest = new ReleaseStockRequest();
     *             releaseRequest.setQuantity(item.getQuantity());
     *             releaseRequest.setReferenceId(order.getOrderNumber());
     *             releaseRequest.setReason("Payment failed - " + webhook.getFailureReason());
     *
     *             inventoryService.releaseStock(item.getProductId(), releaseRequest);
     *         }
     *
     *         // Update order status
     *         order.setStatus(OrderStatus.PENDING);  // Can retry
     *     }
     * }
     *
     * In Order Service (when customer cancels):
     * ------------------------------------------
     * @PostMapping("/orders/{id}/cancel")
     * public void cancelOrder(@PathVariable Long id) {
     *     Order order = orderRepository.findById(id)
     *         .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
     *
     *     // Only can cancel if pending (not yet shipped)
     *     if (order.getStatus() != OrderStatus.PENDING) {
     *         throw new BadRequestException("Cannot cancel order in status: " + order.getStatus());
     *     }
     *
     *     // Release all reserved stock
     *     for (OrderItem item : order.getItems()) {
     *         ReleaseStockRequest releaseRequest = new ReleaseStockRequest();
     *         releaseRequest.setQuantity(item.getQuantity());
     *         releaseRequest.setReferenceId(order.getOrderNumber());
     *         releaseRequest.setReason("Order cancelled by customer");
     *
     *         inventoryService.releaseStock(item.getProductId(), releaseRequest);
     *     }
     *
     *     // Update order status
     *     order.setStatus(OrderStatus.CANCELLED);
     *     orderRepository.save(order);
     * }
     *
     * Automated timeout handling (background job):
     * ---------------------------------------------
     * @Scheduled(fixedDelay = 60000)  // Run every minute
     * public void releaseExpiredReservations() {
     *     // Find reservations older than 30 minutes
     *     LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
     *     List<StockMovement> expiredReservations =
     *         stockMovementRepository.findExpiredReservations(cutoff);
     *
     *     for (StockMovement reservation : expiredReservations) {
     *         ReleaseStockRequest releaseRequest = new ReleaseStockRequest();
     *         releaseRequest.setQuantity(Math.abs(reservation.getQuantityChange()));
     *         releaseRequest.setReferenceId(reservation.getReferenceId());
     *         releaseRequest.setReason("Reservation timeout expired (30 minutes)");
     *
     *         inventoryService.releaseStock(reservation.getProductId(), releaseRequest);
     *     }
     * }
     *
     * Validation Example:
     * -------------------
     * // Valid
     * ReleaseStockRequest request = new ReleaseStockRequest();
     * request.setQuantity(2);
     * request.setReferenceId("ORDER-123");
     * request.setReason("Payment failed - card declined");
     * // ✓ Passes validation
     *
     * // Valid without reason
     * request = new ReleaseStockRequest(2, "ORDER-123", null);
     * // ✓ Reason is optional
     *
     * // Invalid: null quantity
     * request = new ReleaseStockRequest(null, "ORDER-123", "Failed");
     * // Validation error: "Quantity is required"
     */
}
