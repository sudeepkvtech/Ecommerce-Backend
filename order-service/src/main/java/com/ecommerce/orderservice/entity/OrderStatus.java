package com.ecommerce.orderservice.entity;

/**
 * Order Status Enum
 *
 * Represents the lifecycle states of an order in the e-commerce system.
 *
 * What is an Enum?
 * ----------------
 * An enum (enumeration) is a special Java type that represents a fixed set of constants.
 * It's used when a variable can only take one value from a predefined list.
 *
 * Why use Enum vs String?
 * ------------------------
 * Bad approach: status = "pending" (String)
 * Problems:
 * - Typos: "pending" vs "peding" (runtime error!)
 * - Case sensitivity: "PENDING" vs "pending"
 * - No validation: Can set to invalid values like "xyz"
 * - No IDE autocomplete
 *
 * Good approach: status = OrderStatus.PENDING (Enum)
 * Benefits:
 * - Type-safe: Compiler catches errors
 * - No typos: IDE autocomplete prevents mistakes
 * - Limited values: Can only be one of the defined constants
 * - Self-documenting: Easy to see all possible values
 *
 * Order Lifecycle Flow:
 * ---------------------
 * PENDING → Order created, awaiting payment/confirmation
 *    ↓
 * CONFIRMED → Payment received, order confirmed
 *    ↓
 * PROCESSING → Order being prepared/packaged
 *    ↓
 * SHIPPED → Order dispatched for delivery
 *    ↓
 * DELIVERED → Order successfully delivered to customer
 *
 * Alternative flow:
 * PENDING/CONFIRMED/PROCESSING → CANCELLED (User or admin cancels order)
 *
 * State Transitions:
 * ------------------
 * Valid transitions:
 * - PENDING → CONFIRMED (payment successful)
 * - PENDING → CANCELLED (user cancels before payment)
 * - CONFIRMED → PROCESSING (warehouse starts preparing)
 * - CONFIRMED → CANCELLED (admin cancels)
 * - PROCESSING → SHIPPED (package dispatched)
 * - PROCESSING → CANCELLED (order cannot be fulfilled)
 * - SHIPPED → DELIVERED (delivery confirmed)
 *
 * Invalid transitions (business logic should prevent these):
 * - DELIVERED → CANCELLED (cannot cancel delivered order)
 * - SHIPPED → PENDING (cannot go backwards)
 * - CANCELLED → any state (cancelled is final)
 */
public enum OrderStatus {

    /**
     * PENDING
     *
     * Initial state when order is first created.
     *
     * Characteristics:
     * - Order has been placed by customer
     * - Payment may be pending
     * - Inventory not yet reserved
     * - Can be cancelled by customer
     * - Awaiting payment confirmation
     *
     * Next states: CONFIRMED, CANCELLED
     *
     * Example scenario:
     * 1. Customer adds items to cart
     * 2. Customer clicks "Place Order"
     * 3. Order created with status PENDING
     * 4. Awaiting payment gateway response
     */
    PENDING,

    /**
     * CONFIRMED
     *
     * Order has been confirmed, payment received.
     *
     * Characteristics:
     * - Payment successfully processed
     * - Order confirmed with customer
     * - Inventory reserved
     * - Warehouse notified
     * - Awaiting processing
     *
     * Next states: PROCESSING, CANCELLED
     *
     * Example scenario:
     * 1. Payment gateway confirms payment
     * 2. Inventory service reserves items
     * 3. Order status updated to CONFIRMED
     * 4. Confirmation email sent to customer
     */
    CONFIRMED,

    /**
     * PROCESSING
     *
     * Order is being prepared for shipment.
     *
     * Characteristics:
     * - Warehouse is picking items
     * - Items being packaged
     * - Shipping label being generated
     * - Cannot be cancelled by customer (must contact support)
     * - Preparation in progress
     *
     * Next states: SHIPPED, CANCELLED (admin only)
     *
     * Example scenario:
     * 1. Warehouse receives order
     * 2. Staff picks items from shelves
     * 3. Items packaged in box
     * 4. Order status updated to PROCESSING
     */
    PROCESSING,

    /**
     * SHIPPED
     *
     * Order has been dispatched for delivery.
     *
     * Characteristics:
     * - Package handed to courier/delivery service
     * - Tracking number generated
     * - Customer can track shipment
     * - Estimated delivery date available
     * - Cannot be cancelled (in transit)
     *
     * Next states: DELIVERED
     *
     * Example scenario:
     * 1. Package picked up by courier
     * 2. Tracking number: ABC123456
     * 3. Order status updated to SHIPPED
     * 4. Shipping notification sent to customer
     * 5. Customer can track package online
     */
    SHIPPED,

    /**
     * DELIVERED
     *
     * Order successfully delivered to customer.
     *
     * Characteristics:
     * - Package delivered to customer
     * - Delivery confirmed (signature, photo, etc.)
     * - Order lifecycle complete
     * - Final state (no further transitions)
     * - Can initiate return/refund process
     *
     * Next states: None (terminal state)
     *
     * Example scenario:
     * 1. Courier delivers package
     * 2. Customer signs/confirms delivery
     * 3. Order status updated to DELIVERED
     * 4. Delivery confirmation sent to customer
     * 5. Order complete
     */
    DELIVERED,

    /**
     * CANCELLED
     *
     * Order has been cancelled.
     *
     * Characteristics:
     * - Order no longer active
     * - Inventory released (if reserved)
     * - Payment refunded (if applicable)
     * - Final state (no further transitions)
     * - Reason for cancellation recorded
     *
     * Next states: None (terminal state)
     *
     * Cancellation can occur from:
     * - PENDING: Customer cancels before payment
     * - CONFIRMED: Customer/admin cancels after payment (refund issued)
     * - PROCESSING: Admin cancels (cannot fulfill order, refund issued)
     *
     * Cannot cancel from:
     * - SHIPPED: Already in transit
     * - DELIVERED: Already delivered (use return/refund instead)
     *
     * Example scenarios:
     *
     * 1. Customer cancellation (PENDING):
     *    - Customer decides not to purchase
     *    - Clicks "Cancel Order"
     *    - Order status updated to CANCELLED
     *    - No payment to refund
     *
     * 2. Payment failure (PENDING):
     *    - Payment gateway declines payment
     *    - System automatically cancels order
     *    - Order status updated to CANCELLED
     *
     * 3. Admin cancellation (CONFIRMED/PROCESSING):
     *    - Item out of stock
     *    - Cannot fulfill order
     *    - Admin cancels order
     *    - Payment refunded to customer
     *    - Order status updated to CANCELLED
     */
    CANCELLED;

    // ===========================================================================
    // Usage in Order Entity
    // ===========================================================================
    //
    // @Enumerated(EnumType.STRING)
    // private OrderStatus status;
    //
    // Why EnumType.STRING?
    // --------------------
    // Database storage options:
    //
    // 1. EnumType.ORDINAL (stores position number):
    //    Database: 0 (PENDING), 1 (CONFIRMED), 2 (PROCESSING), etc.
    //    Problems:
    //    - If you add new status in middle, numbers change!
    //    - Database data becomes inconsistent
    //    - Hard to debug (what does "2" mean?)
    //
    // 2. EnumType.STRING (stores name):
    //    Database: "PENDING", "CONFIRMED", "PROCESSING", etc.
    //    Benefits:
    //    - Human-readable in database
    //    - Safe to add new statuses
    //    - Safe to reorder statuses
    //    - Easy to debug
    //
    // Example database row:
    // +----+--------------+--------+
    // | id | order_number | status |
    // +----+--------------+--------+
    // | 1  | ORD-20240115 | PENDING|
    // | 2  | ORD-20240116 | SHIPPED|
    // +----+--------------+--------+
    //
    // ===========================================================================
    // Business Logic Examples
    // ===========================================================================
    //
    // 1. Check if order can be cancelled:
    //    public boolean canBeCancelled() {
    //        return status == OrderStatus.PENDING ||
    //               status == OrderStatus.CONFIRMED;
    //    }
    //
    // 2. Check if order is in final state:
    //    public boolean isFinalState() {
    //        return status == OrderStatus.DELIVERED ||
    //               status == OrderStatus.CANCELLED;
    //    }
    //
    // 3. Validate status transition:
    //    public boolean canTransitionTo(OrderStatus newStatus) {
    //        return switch (this.status) {
    //            case PENDING -> newStatus == CONFIRMED || newStatus == CANCELLED;
    //            case CONFIRMED -> newStatus == PROCESSING || newStatus == CANCELLED;
    //            case PROCESSING -> newStatus == SHIPPED || newStatus == CANCELLED;
    //            case SHIPPED -> newStatus == DELIVERED;
    //            case DELIVERED, CANCELLED -> false; // Terminal states
    //        };
    //    }
    //
    // ===========================================================================
    // Notification/Event Examples
    // ===========================================================================
    //
    // Different statuses trigger different actions:
    //
    // PENDING → CONFIRMED:
    //   - Send confirmation email
    //   - Reserve inventory
    //   - Notify warehouse
    //
    // CONFIRMED → PROCESSING:
    //   - Generate packing slip
    //   - Send "being prepared" email
    //
    // PROCESSING → SHIPPED:
    //   - Generate tracking number
    //   - Send shipping notification
    //   - Update inventory (mark as sold)
    //
    // SHIPPED → DELIVERED:
    //   - Send delivery confirmation
    //   - Request review/feedback
    //
    // Any → CANCELLED:
    //   - Refund payment
    //   - Release inventory
    //   - Send cancellation notification
    //
    // ===========================================================================
    // Database Index Consideration
    // ===========================================================================
    //
    // Status is frequently queried:
    // - "Show all PENDING orders"
    // - "Count SHIPPED orders today"
    // - "Find CANCELLED orders this month"
    //
    // Good to add database index on status column:
    // @Table(name = "orders", indexes = {
    //     @Index(name = "idx_status", columnList = "status")
    // })
    //
    // This makes status-based queries much faster.
}
