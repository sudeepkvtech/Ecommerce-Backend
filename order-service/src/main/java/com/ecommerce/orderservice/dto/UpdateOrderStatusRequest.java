package com.ecommerce.orderservice.dto;

// Import OrderStatus enum
import com.ecommerce.orderservice.entity.OrderStatus;

// Import validation annotations
import jakarta.validation.constraints.NotNull;

// Import Lombok annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update Order Status Request DTO
 *
 * Used by administrators to update order status.
 * Allows changing order state in the lifecycle.
 *
 * Typical status transitions:
 * - PENDING → CONFIRMED (payment received)
 * - CONFIRMED → PROCESSING (warehouse preparing order)
 * - PROCESSING → SHIPPED (order dispatched)
 * - SHIPPED → DELIVERED (order arrived)
 * - Any non-final state → CANCELLED (order cancelled)
 *
 * Example JSON request:
 * {
 *   "status": "SHIPPED"
 * }
 *
 * Security:
 * - Only admins should be able to update order status
 * - Regular users can only cancel their own orders
 * - Enforced with @PreAuthorize("hasRole('ADMIN')") in controller
 *
 * Business validations (in service layer):
 * - Validate transition is allowed
 *   (e.g., cannot go from DELIVERED to PENDING)
 * - Cannot change status of cancelled or delivered orders
 * - May require additional data for certain transitions
 *   (e.g., tracking number when marking as SHIPPED)
 */
@Data               // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: no-arg constructor for Jackson
@AllArgsConstructor // Lombok: all-args constructor for testing
public class UpdateOrderStatusRequest {

    /**
     * New Status
     *
     * The status to update the order to.
     *
     * Validations:
     * - @NotNull: Status is required
     *
     * Valid values (from OrderStatus enum):
     * - PENDING
     * - CONFIRMED
     * - PROCESSING
     * - SHIPPED
     * - DELIVERED
     * - CANCELLED
     *
     * Example: SHIPPED
     *
     * Business rules (validated in service):
     * 1. Status transition must be valid:
     *    - PENDING → CONFIRMED, CANCELLED
     *    - CONFIRMED → PROCESSING, CANCELLED
     *    - PROCESSING → SHIPPED, CANCELLED
     *    - SHIPPED → DELIVERED
     *    - DELIVERED → (no transitions, final state)
     *    - CANCELLED → (no transitions, final state)
     *
     * 2. Invalid transitions throw BadRequestException:
     *    - DELIVERED → PENDING (cannot revert delivered order)
     *    - CANCELLED → SHIPPED (cannot ship cancelled order)
     *    - SHIPPED → CONFIRMED (cannot go backwards)
     *
     * 3. Additional data may be required:
     *    - SHIPPED: Should include tracking number (future enhancement)
     *    - CANCELLED: Should include cancellation reason (future enhancement)
     */
    @NotNull(message = "Status is required")
    private OrderStatus status;

    // =========================================================================
    // Future Enhancements
    // =========================================================================
    //
    // Additional fields that could be added:
    //
    // /**
    //  * Tracking Number
    //  * Required when updating to SHIPPED status
    //  */
    // @NotBlank(message = "Tracking number required when shipping")
    // private String trackingNumber;
    //
    // /**
    //  * Carrier Name
    //  * Shipping company (UPS, FedEx, DHL, etc.)
    //  */
    // private String carrierName;
    //
    // /**
    //  * Cancellation Reason
    //  * Required when updating to CANCELLED status
    //  */
    // private String cancellationReason;
    //
    // /**
    //  * Notes
    //  * Optional notes about the status change
    //  */
    // private String notes;
    //
    // =========================================================================
    // Example Usage
    // =========================================================================
    //
    // Admin marks order as shipped:
    // PUT /api/orders/123/status
    // {
    //   "status": "SHIPPED"
    // }
    //
    // Admin marks order as delivered:
    // PUT /api/orders/123/status
    // {
    //   "status": "DELIVERED"
    // }
    //
    // Admin cancels order:
    // PUT /api/orders/123/status
    // {
    //   "status": "CANCELLED"
    // }
    //
    // =========================================================================
    // Controller Example
    // =========================================================================
    //
    // @PutMapping("/{id}/status")
    // @PreAuthorize("hasRole('ADMIN')")
    // public ResponseEntity<OrderResponse> updateOrderStatus(
    //     @PathVariable Long id,
    //     @Valid @RequestBody UpdateOrderStatusRequest request
    // ) {
    //     OrderResponse order = orderService.updateOrderStatus(id, request.getStatus());
    //     return ResponseEntity.ok(order);
    // }
}
