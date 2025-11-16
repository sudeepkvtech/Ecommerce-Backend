package com.ecommerce.inventoryservice.entity;

/**
 * Movement Type Enum
 *
 * Represents the different types of stock movements in the inventory system.
 * Each movement type indicates why stock quantity changed.
 *
 * Why use enum instead of String?
 * ================================
 * 1. Type Safety: Compiler enforces valid movement types
 * 2. Code Completion: IDE shows all available types
 * 3. Refactoring: Easy to rename across entire codebase
 * 4. Documentation: All movement types defined in one place
 * 5. Validation: Cannot use invalid movement types
 *
 * Database Storage
 * ================
 * Stored as VARCHAR in database using @Enumerated(EnumType.STRING)
 * Example: "PURCHASE", "SALE", "RETURN"
 *
 * Stock Movement Tracking
 * =======================
 * Every change to inventory must be recorded with:
 * - Movement type (why did stock change?)
 * - Quantity change (how much changed?)
 * - Timestamp (when did it change?)
 * - Reference ID (which order/adjustment caused it?)
 *
 * This provides complete audit trail for inventory.
 */
public enum MovementType {

    /**
     * PURCHASE
     *
     * Stock added through new purchase from supplier.
     *
     * When this occurs:
     * - New products received from supplier
     * - Restocking existing products
     * - Initial stock for new product
     *
     * Stock change: INCREASE (+)
     *
     * Example scenarios:
     * - Ordered 100 units from supplier → +100 PURCHASE
     * - Received shipment of new products → +500 PURCHASE
     * - Initial stock for new product → +50 PURCHASE
     *
     * Reference:
     * - Purchase order ID
     * - Supplier ID
     * - Receipt number
     *
     * Typical flow:
     * 1. Place purchase order with supplier
     * 2. Receive shipment
     * 3. Verify quantity
     * 4. Record PURCHASE movement
     * 5. Update inventory quantity
     *
     * Accounting impact:
     * - Increases inventory value
     * - May update cost of goods
     */
    PURCHASE,

    /**
     * SALE
     *
     * Stock reduced through customer sale.
     *
     * When this occurs:
     * - Customer completes order
     * - Payment confirmed
     * - Stock committed to order
     *
     * Stock change: DECREASE (-)
     *
     * Example scenarios:
     * - Customer orders 2 units → -2 SALE
     * - Bulk order of 50 units → -50 SALE
     *
     * Reference:
     * - Order ID
     * - Customer ID
     * - Invoice number
     *
     * Typical flow:
     * 1. Customer places order
     * 2. Stock reserved (RESERVATION)
     * 3. Payment processed successfully
     * 4. Record SALE movement
     * 5. Reduce inventory quantity
     *
     * Important notes:
     * - Only record SALE after payment confirms
     * - If payment fails, RELEASE reservation instead
     * - SALE is final, cannot be undone (but can be RETURN)
     *
     * Accounting impact:
     * - Decreases inventory value
     * - Increases cost of goods sold
     * - Affects revenue calculation
     */
    SALE,

    /**
     * RETURN
     *
     * Stock added back through customer return.
     *
     * When this occurs:
     * - Customer returns product
     * - Return approved by admin
     * - Product inspected and accepted
     *
     * Stock change: INCREASE (+)
     *
     * Example scenarios:
     * - Customer returns defective product → +1 RETURN
     * - Customer changes mind, returns item → +2 RETURN
     * - Wrong item shipped, returned → +1 RETURN
     *
     * Reference:
     * - Original order ID
     * - Return authorization ID
     * - Reason for return
     *
     * Typical flow:
     * 1. Customer requests return
     * 2. Admin approves return
     * 3. Customer ships product back
     * 4. Warehouse receives and inspects
     * 5. If acceptable, record RETURN movement
     * 6. Increase inventory quantity
     * 7. Process refund
     *
     * Important notes:
     * - Only add back to stock if product is resellable
     * - Damaged returns may be DAMAGE instead
     * - Return may be partial (some items from order)
     *
     * Accounting impact:
     * - Increases inventory value
     * - Decreases revenue (refund)
     * - May affect cost of goods sold
     */
    RETURN,

    /**
     * DAMAGE
     *
     * Stock reduced due to damage or loss.
     *
     * When this occurs:
     * - Product damaged in warehouse
     * - Product lost or stolen
     * - Product expired (perishables)
     * - Product deemed unsellable
     *
     * Stock change: DECREASE (-)
     *
     * Example scenarios:
     * - Dropped during handling → -5 DAMAGE
     * - Water damage in warehouse → -20 DAMAGE
     * - Expired food items → -10 DAMAGE
     * - Theft → -3 DAMAGE
     *
     * Reference:
     * - Incident report ID
     * - Insurance claim ID
     * - Reason for damage
     *
     * Typical flow:
     * 1. Damage discovered
     * 2. Incident documented
     * 3. Items marked as unsellable
     * 4. Record DAMAGE movement
     * 5. Reduce inventory quantity
     * 6. File insurance claim if applicable
     *
     * Important notes:
     * - Damages should be investigated
     * - May indicate process improvement needs
     * - Track damage rate per product
     * - High damage rate = quality or handling issue
     *
     * Accounting impact:
     * - Decreases inventory value
     * - Recorded as loss/expense
     * - May be offset by insurance
     */
    DAMAGE,

    /**
     * ADJUSTMENT
     *
     * Manual stock adjustment to correct discrepancies.
     *
     * When this occurs:
     * - Physical count differs from system count
     * - Correction of data entry error
     * - Reconciliation after audit
     * - Migration from old system
     *
     * Stock change: INCREASE (+) or DECREASE (-)
     *
     * Example scenarios:
     * - Physical count shows 95, system shows 100 → -5 ADJUSTMENT
     * - Found 10 units in different location → +10 ADJUSTMENT
     * - Data entry error correction → +/- X ADJUSTMENT
     *
     * Reference:
     * - Audit ID
     * - Reason for adjustment
     * - Approver ID (should require approval)
     *
     * Typical flow:
     * 1. Discrepancy discovered
     * 2. Investigation to find cause
     * 3. Admin approval for adjustment
     * 4. Record ADJUSTMENT movement
     * 5. Update inventory to match reality
     *
     * Important notes:
     * - Should require admin approval
     * - Frequent adjustments indicate process issues
     * - Document reason for every adjustment
     * - Investigate root cause of discrepancies
     *
     * Best practices:
     * - Minimize adjustments through good processes
     * - Regular cycle counting to catch issues early
     * - Train staff on proper inventory procedures
     * - Audit adjustment patterns
     *
     * Accounting impact:
     * - Adjusts inventory value
     * - May affect cost of goods sold
     * - Should be explained in financial reports
     */
    ADJUSTMENT,

    /**
     * RESERVATION
     *
     * Stock temporarily reserved for pending order.
     *
     * When this occurs:
     * - Customer places order
     * - Payment pending
     * - Items held for this order
     *
     * Stock change: NEUTRAL (tracked separately)
     *
     * Important: This doesn't immediately reduce available stock,
     * but marks stock as "reserved" or "allocated" to an order.
     *
     * Example scenarios:
     * - Customer places order, payment processing → RESERVATION
     * - Items put aside for pending payment → RESERVATION
     * - Stock held during checkout process → RESERVATION
     *
     * Reference:
     * - Order ID
     * - Customer ID
     * - Reservation expiry time
     *
     * Typical flow:
     * 1. Customer adds items to cart and checks out
     * 2. Record RESERVATION movement
     * 3. Mark stock as reserved (not available for others)
     * 4. Process payment:
     *    - Success: Convert to SALE
     *    - Failure: Convert to RELEASE
     * 5. Remove reservation
     *
     * Important notes:
     * - Prevents overselling (selling same item twice)
     * - Should have timeout (release if payment takes too long)
     * - Reserved stock still counts in total inventory
     * - Available stock = Total - Reserved
     *
     * States:
     * - Reserved: Stock held for order
     * - Committed: Payment success, converted to SALE
     * - Released: Payment failed, back to available
     *
     * Accounting impact:
     * - No immediate accounting impact
     * - Affects "available to sell" metrics
     * - Important for order fulfillment planning
     */
    RESERVATION,

    /**
     * RELEASE
     *
     * Previously reserved stock released back to available pool.
     *
     * When this occurs:
     * - Payment failed
     * - Order cancelled before payment
     * - Reservation timeout expired
     *
     * Stock change: NEUTRAL (reverses reservation)
     *
     * This makes reserved stock available again for other customers.
     *
     * Example scenarios:
     * - Payment declined → RELEASE reservation
     * - Customer cancels order → RELEASE reservation
     * - 30-minute timeout expired → RELEASE reservation
     *
     * Reference:
     * - Original order ID
     * - Original reservation ID
     * - Reason for release
     *
     * Typical flow:
     * 1. Stock was previously RESERVED
     * 2. Payment failed or order cancelled
     * 3. Record RELEASE movement
     * 4. Mark stock as available again
     * 5. Other customers can now order this stock
     *
     * Important notes:
     * - Must match previous RESERVATION
     * - Cannot release more than was reserved
     * - Automatic release on timeout prevents stuck reservations
     * - High release rate = payment issues or cart abandonment
     *
     * Metrics to track:
     * - Reservation-to-sale conversion rate
     * - Average time from reservation to release
     * - Common reasons for release
     * - Impact on customer experience
     *
     * Accounting impact:
     * - No accounting impact
     * - Stock returns to "available to sell"
     * - May indicate lost sale opportunity
     */
    RELEASE;

    /**
     * Helper Methods (Could be added)
     * ================================
     *
     * Check if movement increases stock:
     * public boolean increasesStock() {
     *     return this == PURCHASE || this == RETURN ||
     *            (this == ADJUSTMENT); // ADJUSTMENT can be +/-
     * }
     *
     * Check if movement decreases stock:
     * public boolean decreasesStock() {
     *     return this == SALE || this == DAMAGE;
     * }
     *
     * Check if movement is a reservation operation:
     * public boolean isReservation() {
     *     return this == RESERVATION || this == RELEASE;
     * }
     *
     * Get display name:
     * public String getDisplayName() {
     *     return switch (this) {
     *         case PURCHASE -> "Purchase";
     *         case SALE -> "Sale";
     *         case RETURN -> "Return";
     *         case DAMAGE -> "Damage/Loss";
     *         case ADJUSTMENT -> "Manual Adjustment";
     *         case RESERVATION -> "Reserved";
     *         case RELEASE -> "Released";
     *     };
     * }
     *
     * Get description:
     * public String getDescription() {
     *     return switch (this) {
     *         case PURCHASE -> "Stock received from supplier";
     *         case SALE -> "Stock sold to customer";
     *         case RETURN -> "Stock returned by customer";
     *         case DAMAGE -> "Stock damaged or lost";
     *         case ADJUSTMENT -> "Manual stock correction";
     *         case RESERVATION -> "Stock reserved for order";
     *         case RELEASE -> "Reserved stock released";
     *     };
     * }
     */
}
