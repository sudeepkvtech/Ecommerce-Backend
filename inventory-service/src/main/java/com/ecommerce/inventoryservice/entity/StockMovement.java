package com.ecommerce.inventoryservice.entity;

// Import JPA annotations
import jakarta.persistence.*;

// Import Lombok annotations
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Import time class
import java.time.LocalDateTime;

/**
 * Stock Movement Entity
 *
 * Records every change to inventory.
 * Provides complete audit trail for stock changes.
 *
 * What is a Stock Movement?
 * =========================
 * Every time inventory quantity changes, a stock movement record is created.
 * This provides:
 * - Complete history of all stock changes
 * - Audit trail for compliance
 * - Ability to track inventory over time
 * - Debugging for inventory discrepancies
 *
 * Why Track Movements?
 * ====================
 * 1. Audit Trail:
 *    - Required for financial audits
 *    - Track every stock change
 *    - Know who changed what and when
 *
 * 2. Analytics:
 *    - Sales velocity
 *    - Return rates
 *    - Damage patterns
 *    - Stock turnover
 *
 * 3. Debugging:
 *    - Find discrepancies
 *    - Track missing stock
 *    - Verify calculations
 *
 * 4. Reporting:
 *    - Stock reports
 *    - Movement summaries
 *    - Historical trends
 *
 * Database Table: stock_movements
 * ================================
 * Columns:
 * - id: BIGINT PRIMARY KEY AUTO_INCREMENT
 * - product_id: BIGINT NOT NULL
 * - movement_type: VARCHAR(255) NOT NULL
 * - quantity_change: INT NOT NULL (positive or negative)
 * - quantity_before: INT NOT NULL
 * - quantity_after: INT NOT NULL
 * - reference_id: VARCHAR(255) (order ID, etc.)
 * - notes: TEXT
 * - created_at: DATETIME NOT NULL
 *
 * Indexes:
 * - idx_product_id: Fast product movement lookup
 * - idx_movement_type: Filter by movement type
 * - idx_created_at: Sort by date
 * - idx_reference_id: Find movements for specific order/transaction
 *
 * Example Records:
 * ================
 * Product ID: 123 (Blue T-Shirt)
 *
 * 1. Initial stock:
 *    - Type: PURCHASE
 *    - Quantity Change: +100
 *    - Before: 0, After: 100
 *    - Reference: "PO-2024-001"
 *    - Notes: "Initial stock from supplier"
 *
 * 2. Customer order:
 *    - Type: RESERVATION
 *    - Quantity Change: -2 (from available, to reserved)
 *    - Before: 100, After: 100 (total unchanged)
 *    - Reference: "ORDER-123"
 *    - Notes: "Reserved for order"
 *
 * 3. Payment success:
 *    - Type: SALE
 *    - Quantity Change: -2
 *    - Before: 100, After: 98
 *    - Reference: "ORDER-123"
 *    - Notes: "Sold to customer"
 *
 * 4. Damage:
 *    - Type: DAMAGE
 *    - Quantity Change: -5
 *    - Before: 98, After: 93
 *    - Reference: "INCIDENT-456"
 *    - Notes: "Water damage in warehouse"
 *
 * Relationship with Inventory:
 * =============================
 * - Inventory: Current state (snapshot)
 * - StockMovement: Historical changes (timeline)
 *
 * Inventory shows: "We have 93 units now"
 * StockMovement shows: "How we got to 93 units"
 */

// @Entity marks this as a JPA entity
@Entity

// @Table specifies table name and indexes
@Table(
    name = "stock_movements",
    indexes = {
        // Index for product lookup
        @Index(name = "idx_product_id", columnList = "product_id"),

        // Index for filtering by type
        @Index(name = "idx_movement_type", columnList = "movement_type"),

        // Index for date sorting
        @Index(name = "idx_created_at", columnList = "created_at"),

        // Index for reference lookup
        @Index(name = "idx_reference_id", columnList = "reference_id")
    }
)

// Lombok annotations
@Data                // Getters, setters, toString, equals, hashCode
@NoArgsConstructor   // No-argument constructor (required by JPA)
@AllArgsConstructor  // Constructor with all fields
@Builder             // Builder pattern
public class StockMovement {

    /**
     * Movement ID
     *
     * Primary key, auto-generated.
     * Unique identifier for each stock movement.
     *
     * Database column: id BIGINT PRIMARY KEY AUTO_INCREMENT
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Product ID
     *
     * Reference to product in Product Service.
     * Which product's stock changed.
     *
     * Database column: product_id BIGINT NOT NULL
     *
     * Example: 123 (Blue T-Shirt)
     *
     * Usage:
     * // Get all movements for a product
     * List<StockMovement> movements =
     *     stockMovementRepository.findByProductIdOrderByCreatedAtDesc(123L);
     */
    @Column(name = "product_id", nullable = false)
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
     * Database column: movement_type VARCHAR(255) NOT NULL
     *
     * Example: SALE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private MovementType movementType;

    /**
     * Quantity Change
     *
     * How much the stock changed.
     * - Positive: Stock increased (+)
     * - Negative: Stock decreased (-)
     * - Zero: No change (should not happen)
     *
     * Examples:
     * - PURCHASE: +100 (added 100 units)
     * - SALE: -2 (sold 2 units)
     * - RETURN: +1 (1 unit returned)
     * - DAMAGE: -5 (5 units damaged)
     * - ADJUSTMENT: +10 or -10 (depending on correction)
     *
     * Special cases:
     * - RESERVATION: Tracked separately (moves available to reserved)
     * - RELEASE: Tracked separately (moves reserved to available)
     *
     * Database column: quantity_change INT NOT NULL
     *
     * Example: -2 (2 units removed)
     */
    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    /**
     * Quantity Before
     *
     * Stock quantity before this movement.
     * Snapshot of inventory before change.
     *
     * Why track this?
     * - Verify calculations
     * - Reconstruct history
     * - Debug discrepancies
     * - Audit trail
     *
     * Database column: quantity_before INT NOT NULL
     *
     * Example: 100 (had 100 units before sale)
     *
     * Verification:
     * quantityBefore + quantityChange = quantityAfter
     */
    @Column(name = "quantity_before", nullable = false)
    private Integer quantityBefore;

    /**
     * Quantity After
     *
     * Stock quantity after this movement.
     * Snapshot of inventory after change.
     *
     * Why track this?
     * - Current state at time of movement
     * - Verify calculation: before + change = after
     * - Audit trail
     * - Debugging
     *
     * Database column: quantity_after INT NOT NULL
     *
     * Example: 98 (now have 98 units after sale)
     *
     * Invariant:
     * quantityAfter = quantityBefore + quantityChange
     *
     * This should ALWAYS be true!
     */
    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    /**
     * Reference ID
     *
     * External reference for this movement.
     * Links movement to source transaction.
     *
     * Examples by type:
     * - PURCHASE: Purchase order ID ("PO-2024-001")
     * - SALE: Order ID ("ORDER-123")
     * - RETURN: Return authorization ID ("RMA-456")
     * - DAMAGE: Incident report ID ("INCIDENT-789")
     * - ADJUSTMENT: Audit ID ("AUDIT-2024-01")
     * - RESERVATION: Order ID ("ORDER-124")
     * - RELEASE: Order ID ("ORDER-124")
     *
     * Why important?
     * - Trace back to source
     * - Link to other systems
     * - Customer support lookups
     * - Financial reconciliation
     *
     * Database column: reference_id VARCHAR(255)
     *
     * Example: "ORDER-123"
     *
     * Usage:
     * // Find all movements for an order
     * List<StockMovement> orderMovements =
     *     stockMovementRepository.findByReferenceId("ORDER-123");
     */
    @Column(name = "reference_id")
    private String referenceId;

    /**
     * Notes
     *
     * Additional information about this movement.
     * Human-readable explanation.
     *
     * Examples:
     * - "Initial stock from Supplier XYZ"
     * - "Sold to customer John Doe"
     * - "Customer returned - defective product"
     * - "Water damage in Warehouse A, Section B3"
     * - "Physical count adjustment - found extra stock"
     * - "Reserved for pending payment"
     * - "Payment failed - stock released"
     *
     * Use cases:
     * - Customer support
     * - Investigation
     * - Compliance
     * - Audits
     *
     * Database column: notes TEXT
     *
     * Example: "Sold to customer via Order #123"
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Created At
     *
     * When this movement occurred.
     * Timestamp of stock change.
     *
     * Important for:
     * - Chronological history
     * - Date range queries
     * - Time-based analytics
     * - Audit trail
     *
     * Database column: created_at DATETIME NOT NULL
     *
     * Example: 2024-01-15T10:30:00
     *
     * Usage:
     * // Get movements in date range
     * List<StockMovement> movements =
     *     stockMovementRepository.findByProductIdAndCreatedAtBetween(
     *         123L,
     *         startDate,
     *         endDate
     *     );
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Pre-Persist Callback
     *
     * Called automatically before INSERT.
     * Sets timestamp and validates invariant.
     */
    @PrePersist
    protected void onCreate() {
        // Set creation timestamp
        createdAt = LocalDateTime.now();

        // Validate invariant: before + change = after
        validateQuantityCalculation();
    }

    /**
     * Validate Quantity Calculation
     *
     * Ensures: quantityBefore + quantityChange = quantityAfter
     * This is a critical invariant that must always hold.
     *
     * @throws IllegalStateException if invariant violated
     */
    private void validateQuantityCalculation() {
        if (quantityBefore + quantityChange != quantityAfter) {
            throw new IllegalStateException(
                "Stock movement calculation error! " +
                "Before: " + quantityBefore +
                ", Change: " + quantityChange +
                ", After: " + quantityAfter +
                " (Expected: " + (quantityBefore + quantityChange) + ")"
            );
        }
    }

    /**
     * Helper Methods
     * ==============
     */

    /**
     * Check if movement increased stock
     *
     * @return true if quantity change is positive
     */
    public boolean isIncrease() {
        return quantityChange > 0;
    }

    /**
     * Check if movement decreased stock
     *
     * @return true if quantity change is negative
     */
    public boolean isDecrease() {
        return quantityChange < 0;
    }

    /**
     * Get absolute quantity change
     *
     * @return absolute value of quantity change
     */
    public int getAbsoluteQuantityChange() {
        return Math.abs(quantityChange);
    }

    /**
     * Create Stock Movement (Static Factory Method)
     * ==============================================
     *
     * Creates a stock movement record with validation.
     *
     * @param productId Product ID
     * @param movementType Type of movement
     * @param quantityChange How much changed
     * @param quantityBefore Stock before
     * @param quantityAfter Stock after
     * @param referenceId External reference
     * @param notes Additional notes
     * @return StockMovement instance
     *
     * Usage example:
     * StockMovement movement = StockMovement.create(
     *     123L,                      // product ID
     *     MovementType.SALE,         // type
     *     -2,                        // change (-2)
     *     100,                       // before (100)
     *     98,                        // after (98)
     *     "ORDER-123",               // reference
     *     "Sold to customer"         // notes
     * );
     */
    public static StockMovement create(
            Long productId,
            MovementType movementType,
            int quantityChange,
            int quantityBefore,
            int quantityAfter,
            String referenceId,
            String notes
    ) {
        return StockMovement.builder()
                .productId(productId)
                .movementType(movementType)
                .quantityChange(quantityChange)
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .referenceId(referenceId)
                .notes(notes)
                .build();
    }

    /**
     * Analytics Use Cases
     * ===================
     *
     * 1. Sales Velocity:
     *    SELECT product_id, SUM(ABS(quantity_change))
     *    FROM stock_movements
     *    WHERE movement_type = 'SALE'
     *    AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
     *    GROUP BY product_id;
     *
     * 2. Return Rate:
     *    SELECT
     *        product_id,
     *        (SUM(CASE WHEN movement_type = 'RETURN' THEN ABS(quantity_change) END) /
     *         SUM(CASE WHEN movement_type = 'SALE' THEN ABS(quantity_change) END)) * 100 as return_rate
     *    FROM stock_movements
     *    GROUP BY product_id;
     *
     * 3. Damage Analysis:
     *    SELECT product_id, SUM(ABS(quantity_change)) as total_damaged
     *    FROM stock_movements
     *    WHERE movement_type = 'DAMAGE'
     *    GROUP BY product_id
     *    ORDER BY total_damaged DESC;
     *
     * 4. Stock Timeline:
     *    SELECT created_at, quantity_after
     *    FROM stock_movements
     *    WHERE product_id = 123
     *    ORDER BY created_at
     *    -- Plot this to see stock level over time
     */
}
