package com.ecommerce.inventoryservice.dto;

// Import Lombok annotations
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Import movement type enum
import com.ecommerce.inventoryservice.entity.MovementType;

// Import time class
import java.time.LocalDateTime;

/**
 * Stock Movement Response DTO
 *
 * Returned when retrieving stock movement history.
 * Shows a single inventory change record.
 *
 * When is this returned?
 * ======================
 * 1. GET /api/inventory/movements/{productId}
 *    - Get all movements for a product
 *    - Returns array of StockMovementResponse
 *
 * 2. GET /api/inventory/movements/recent
 *    - Get recent movements across all products
 *    - Activity feed for admin dashboard
 *
 * 3. GET /api/inventory/movements/type/{type}
 *    - Get movements by type (SALE, PURCHASE, etc.)
 *    - Type-specific analysis
 *
 * Example JSON response:
 * ======================
 * {
 *   "id": 1,
 *   "productId": 123,
 *   "movementType": "SALE",
 *   "quantityChange": -2,
 *   "quantityBefore": 100,
 *   "quantityAfter": 98,
 *   "referenceId": "ORDER-123",
 *   "notes": "Sold to customer John Doe",
 *   "createdAt": "2024-01-15T14:30:00"
 * }
 *
 * Who uses this?
 * ==============
 * - Admin Panel: View inventory change history
 * - Reports: Movement summaries and analytics
 * - Audits: Compliance and verification
 * - Debugging: Investigate discrepancies
 */
@Data               // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: no-arg constructor
@AllArgsConstructor // Lombok: all-args constructor
@Builder            // Lombok: builder pattern
public class StockMovementResponse {

    /**
     * Movement ID
     *
     * Unique identifier for this stock movement.
     * Database-generated value.
     *
     * Example: 1
     */
    private Long id;

    /**
     * Product ID
     *
     * Which product's stock changed.
     * Reference to product in Product Service.
     *
     * Example: 123 (Blue T-Shirt)
     */
    private Long productId;

    /**
     * Movement Type
     *
     * Why the stock changed.
     *
     * Values:
     * - PURCHASE: New stock received
     * - SALE: Stock sold to customer
     * - RETURN: Customer returned product
     * - DAMAGE: Stock damaged or lost
     * - ADJUSTMENT: Manual correction
     * - RESERVATION: Stock reserved for order
     * - RELEASE: Reservation released
     *
     * Display:
     * - Color code by type (green for add, red for reduce)
     * - Icon for each type
     * - Filter buttons in UI
     *
     * Example: "SALE"
     */
    private MovementType movementType;

    /**
     * Quantity Change
     *
     * How much the stock changed.
     * - Positive: Stock increased (+)
     * - Negative: Stock decreased (-)
     *
     * Examples:
     * - +100: Added 100 units (PURCHASE)
     * - -2: Removed 2 units (SALE)
     * - +1: Added 1 unit (RETURN)
     * - -5: Removed 5 units (DAMAGE)
     *
     * Display:
     * - Show with sign: "+100" or "-2"
     * - Color: green for positive, red for negative
     * - Arrow: ↑ for increase, ↓ for decrease
     *
     * Example: -2
     */
    private Integer quantityChange;

    /**
     * Quantity Before
     *
     * Stock quantity before this movement.
     * Snapshot of inventory before change.
     *
     * Uses:
     * - Verify calculation
     * - Timeline visualization
     * - Debugging
     *
     * Example: 100 (had 100 units before)
     *
     * Display:
     * - "Was: 100 units"
     * - Part of timeline: "100 → 98"
     */
    private Integer quantityBefore;

    /**
     * Quantity After
     *
     * Stock quantity after this movement.
     * Snapshot of inventory after change.
     *
     * Calculation check:
     * quantityAfter = quantityBefore + quantityChange
     *
     * Example: 98 (now have 98 units)
     *
     * Display:
     * - "Now: 98 units"
     * - Timeline: "100 → 98"
     * - Change indicator: "-2 (100 → 98)"
     */
    private Integer quantityAfter;

    /**
     * Reference ID
     *
     * External reference for this movement.
     * Links to source transaction.
     *
     * Examples:
     * - "ORDER-123" (sale)
     * - "PO-2024-001" (purchase)
     * - "RMA-456" (return)
     * - "INCIDENT-789" (damage)
     * - "AUDIT-2024-01" (adjustment)
     *
     * Display:
     * - As clickable link (navigate to order/PO/etc.)
     * - Tooltip with full details
     *
     * Example: "ORDER-123"
     */
    private String referenceId;

    /**
     * Notes
     *
     * Human-readable explanation.
     * Additional context and details.
     *
     * Examples:
     * - "Sold to customer John Doe"
     * - "Received shipment from Supplier XYZ"
     * - "Customer returned - defective product"
     * - "Water damage in warehouse"
     * - "Physical count adjustment"
     *
     * Display:
     * - Show in expandable section
     * - Tooltip on hover
     * - Full text in details view
     *
     * Example: "Sold to customer John Doe"
     */
    private String notes;

    /**
     * Created At
     *
     * When this movement occurred.
     * Timestamp of stock change.
     *
     * Format: ISO 8601 date-time
     * Example: "2024-01-15T14:30:00"
     *
     * Display:
     * - Relative: "2 hours ago", "Yesterday"
     * - Absolute: "Jan 15, 2024 2:30 PM"
     * - Sort chronologically (newest first usually)
     */
    private LocalDateTime createdAt;

    /**
     * Derived/Helper Fields (Not in DTO but useful in UI)
     * ====================================================
     *
     * These could be calculated on frontend:
     *
     * 1. Display color:
     *    function getColor(movement) {
     *        if (movement.quantityChange > 0) return 'green';
     *        if (movement.quantityChange < 0) return 'red';
     *        return 'gray';
     *    }
     *
     * 2. Icon:
     *    function getIcon(movement) {
     *        switch (movement.movementType) {
     *            case 'PURCHASE': return '📦';  // Package
     *            case 'SALE': return '🛒';      // Shopping cart
     *            case 'RETURN': return '↩️';    // Return arrow
     *            case 'DAMAGE': return '⚠️';    // Warning
     *            case 'ADJUSTMENT': return '🔧'; // Wrench
     *            case 'RESERVATION': return '🔒'; // Lock
     *            case 'RELEASE': return '🔓';   // Unlock
     *        }
     *    }
     *
     * 3. Display text:
     *    function getDisplayText(movement) {
     *        const sign = movement.quantityChange > 0 ? '+' : '';
     *        return `${sign}${movement.quantityChange} units (${movement.quantityBefore} → ${movement.quantityAfter})`;
     *    }
     */

    /**
     * Example UI Rendering
     * ====================
     *
     * Timeline view:
     * --------------
     * Jan 15, 2024 2:30 PM  🛒 SALE
     * -2 units (100 → 98)
     * Order: ORDER-123
     * Sold to customer John Doe
     *
     * Jan 14, 2024 10:00 AM  📦 PURCHASE
     * +100 units (0 → 100)
     * PO: PO-2024-001
     * Received shipment from Supplier XYZ
     *
     * Table view:
     * -----------
     * | Date     | Type     | Change | Before | After | Reference  | Notes            |
     * |----------|----------|--------|--------|-------|------------|------------------|
     * | 1/15 2PM | SALE     | -2     | 100    | 98    | ORDER-123  | Sold to customer |
     * | 1/14 10AM| PURCHASE | +100   | 0      | 100   | PO-2024-001| From supplier    |
     *
     * Chart view:
     * -----------
     * Stock Level Over Time
     * 100 ┤         ●────────●
     *  98 ┤                   ●
     *     └─────────────────────
     *       1/14    1/15    1/16
     *
     * Filter by type:
     * ---------------
     * [All] [Sales] [Purchases] [Returns] [Damage] [Adjustments]
     *
     * When "Sales" selected:
     * - Show only SALE movements
     * - Calculate total sold
     * - Show sales velocity
     */

    /**
     * Example Usage
     * =============
     *
     * In Admin Panel (viewing product history):
     * ------------------------------------------
     * @GetMapping("/admin/inventory/{productId}/movements")
     * public List<StockMovementResponse> getMovementHistory(@PathVariable Long productId) {
     *     return inventoryService.getMovementHistory(productId);
     * }
     *
     * // Frontend renders:
     * movements.forEach(movement => {
     *     const color = movement.quantityChange > 0 ? 'green' : 'red';
     *     const sign = movement.quantityChange > 0 ? '+' : '';
     *
     *     console.log(`${movement.createdAt} - ${movement.movementType}`);
     *     console.log(`${sign}${movement.quantityChange} (${movement.quantityBefore} → ${movement.quantityAfter})`);
     *     console.log(`Reference: ${movement.referenceId}`);
     *     if (movement.notes) {
     *         console.log(`Notes: ${movement.notes}`);
     *     }
     *     console.log('---');
     * });
     *
     * In Reports (sales analysis):
     * ----------------------------
     * // Get all sales movements
     * List<StockMovementResponse> sales = inventoryService.getMovementsByType(MovementType.SALE);
     *
     * // Calculate total sold
     * int totalSold = sales.stream()
     *     .mapToInt(m -> Math.abs(m.getQuantityChange()))
     *     .sum();
     *
     * // Group by date
     * Map<LocalDate, Integer> salesByDate = sales.stream()
     *     .collect(Collectors.groupingBy(
     *         m -> m.getCreatedAt().toLocalDate(),
     *         Collectors.summingInt(m -> Math.abs(m.getQuantityChange()))
     *     ));
     *
     * In Audit (compliance check):
     * ----------------------------
     * // Get all adjustments
     * List<StockMovementResponse> adjustments =
     *     inventoryService.getMovementsByType(MovementType.ADJUSTMENT);
     *
     * // Review each adjustment
     * for (StockMovementResponse adj : adjustments) {
     *     System.out.println("Adjustment on " + adj.getCreatedAt());
     *     System.out.println("Change: " + adj.getQuantityChange());
     *     System.out.println("Reference: " + adj.getReferenceId());
     *     System.out.println("Notes: " + adj.getNotes());
     *     System.out.println("Requires review: " + (Math.abs(adj.getQuantityChange()) > 10));
     * }
     */
}
