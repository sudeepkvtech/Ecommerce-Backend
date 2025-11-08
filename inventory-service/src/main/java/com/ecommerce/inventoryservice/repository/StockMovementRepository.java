package com.ecommerce.inventoryservice.repository;

// Import the StockMovement entity and MovementType enum
import com.ecommerce.inventoryservice.entity.StockMovement;
import com.ecommerce.inventoryservice.entity.MovementType;

// Import Spring Data JPA repository interface
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Import stereotype annotation
import org.springframework.stereotype.Repository;

// Import List and time classes
import java.time.LocalDateTime;
import java.util.List;

/**
 * Stock Movement Repository
 *
 * Data access layer for StockMovement entity.
 * Provides methods to track and query inventory change history.
 *
 * What is this repository for?
 * ============================
 * StockMovement records every change to inventory.
 * This repository provides queries to:
 * - Get movement history for a product
 * - Filter movements by type (SALE, PURCHASE, etc.)
 * - Find movements by date range
 * - Track movements by reference (order ID, etc.)
 * - Generate reports and analytics
 *
 * Why track movements?
 * ====================
 * 1. Audit Trail:
 *    - Complete history of stock changes
 *    - Required for financial audits
 *    - Compliance and accountability
 *
 * 2. Analytics:
 *    - Sales velocity
 *    - Return rates
 *    - Damage patterns
 *    - Stock turnover
 *
 * 3. Debugging:
 *    - Find discrepancies
 *    - Trace stock issues
 *    - Verify calculations
 *
 * 4. Reporting:
 *    - Movement summaries
 *    - Historical trends
 *    - Operational insights
 */

// @Repository marks this as a Spring Data repository
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    /**
     * Find all movements for a product
     *
     * Retrieves complete movement history for a product.
     * Sorted by date (newest first).
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - ProductId: Filter by productId field
     * - OrderBy: Sort results
     * - CreatedAt: Sort by createdAt field
     * - Desc: Descending order (newest first)
     *
     * Generated SQL:
     * SELECT * FROM stock_movements
     * WHERE product_id = ?
     * ORDER BY created_at DESC
     *
     * Use cases:
     * ----------
     * 1. Product movement history:
     *    - Admin views all changes for a product
     *    - Understand stock level changes over time
     *    - Investigate discrepancies
     *
     * 2. Timeline visualization:
     *    - Chart stock levels over time
     *    - Identify patterns
     *    - Seasonal trends
     *
     * Example usage:
     * --------------
     * // Get all movements for product
     * List<StockMovement> movements =
     *     stockMovementRepository.findByProductIdOrderByCreatedAtDesc(123L);
     *
     * // Display history
     * for (StockMovement movement : movements) {
     *     System.out.println(movement.getCreatedAt() + " - " +
     *         movement.getMovementType() + ": " +
     *         movement.getQuantityChange() + " units");
     * }
     *
     * @param productId The ID of the product
     * @return List of movements for this product, newest first
     */
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    /**
     * Find movements by type
     *
     * Retrieves all movements of a specific type (SALE, PURCHASE, etc.)
     * Useful for type-specific analysis.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - MovementType: Filter by movementType field
     * - OrderBy: Sort results
     * - CreatedAt: Sort by date
     * - Desc: Newest first
     *
     * Generated SQL:
     * SELECT * FROM stock_movements
     * WHERE movement_type = ?
     * ORDER BY created_at DESC
     *
     * Use cases:
     * ----------
     * 1. Sales analysis:
     *    - Get all SALE movements
     *    - Calculate total units sold
     *    - Sales velocity metrics
     *
     * 2. Returns analysis:
     *    - Get all RETURN movements
     *    - Calculate return rate
     *    - Identify problematic products
     *
     * 3. Damage tracking:
     *    - Get all DAMAGE movements
     *    - Monitor damage frequency
     *    - Identify handling issues
     *
     * Example usage:
     * --------------
     * // Get all sales
     * List<StockMovement> sales =
     *     stockMovementRepository.findByMovementTypeOrderByCreatedAtDesc(MovementType.SALE);
     *
     * // Calculate total sold
     * int totalSold = sales.stream()
     *     .mapToInt(m -> Math.abs(m.getQuantityChange()))
     *     .sum();
     *
     * @param movementType Type of movement to find
     * @return List of movements of this type, newest first
     */
    List<StockMovement> findByMovementTypeOrderByCreatedAtDesc(MovementType movementType);

    /**
     * Find movements by reference ID
     *
     * Retrieves all movements linked to a specific transaction.
     * Useful for tracking order-related movements.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - ReferenceId: Filter by referenceId field
     * - OrderBy: Sort results
     * - CreatedAt: Sort by date
     * - Desc: Newest first
     *
     * Generated SQL:
     * SELECT * FROM stock_movements
     * WHERE reference_id = ?
     * ORDER BY created_at DESC
     *
     * Use cases:
     * ----------
     * 1. Order tracking:
     *    - Get all movements for an order
     *    - See: RESERVATION, then SALE (or RELEASE if failed)
     *    - Verify order fulfillment
     *
     * 2. Return processing:
     *    - Find original SALE by order ID
     *    - Verify return quantity matches
     *    - Link RETURN to original order
     *
     * 3. Purchase order tracking:
     *    - Find all PURCHASE movements for PO
     *    - Verify received quantities
     *    - Match against PO
     *
     * Example usage:
     * --------------
     * // Get all movements for an order
     * List<StockMovement> orderMovements =
     *     stockMovementRepository.findByReferenceIdOrderByCreatedAtDesc("ORDER-123");
     *
     * // Should see: RESERVATION → SALE (if payment succeeded)
     * // or: RESERVATION → RELEASE (if payment failed)
     *
     * @param referenceId External reference (order ID, PO number, etc.)
     * @return List of movements with this reference, newest first
     */
    List<StockMovement> findByReferenceIdOrderByCreatedAtDesc(String referenceId);

    /**
     * Find movements by product and type
     *
     * Retrieves movements for a product filtered by type.
     * Combines product and type filtering.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - ProductId: Filter by productId
     * - And: Logical AND
     * - MovementType: Filter by movementType
     * - OrderBy: Sort results
     * - CreatedAt: Sort by date
     * - Desc: Newest first
     *
     * Generated SQL:
     * SELECT * FROM stock_movements
     * WHERE product_id = ? AND movement_type = ?
     * ORDER BY created_at DESC
     *
     * Use cases:
     * ----------
     * 1. Product sales history:
     *    - Get all SALE movements for a product
     *    - Calculate total units sold
     *    - Sales trend for this product
     *
     * 2. Product returns:
     *    - Get all RETURN movements for a product
     *    - Return rate for this product
     *    - Quality issues?
     *
     * 3. Product damages:
     *    - Get all DAMAGE movements for a product
     *    - High damage rate?
     *    - Packaging or handling issue?
     *
     * Example usage:
     * --------------
     * // Get all sales for a product
     * List<StockMovement> productSales =
     *     stockMovementRepository.findByProductIdAndMovementTypeOrderByCreatedAtDesc(
     *         123L,
     *         MovementType.SALE
     *     );
     *
     * // Calculate total sold for this product
     * int totalSold = productSales.stream()
     *     .mapToInt(m -> Math.abs(m.getQuantityChange()))
     *     .sum();
     *
     * @param productId The ID of the product
     * @param movementType Type of movement
     * @return List of movements for this product and type, newest first
     */
    List<StockMovement> findByProductIdAndMovementTypeOrderByCreatedAtDesc(
        Long productId,
        MovementType movementType
    );

    /**
     * Find movements in date range
     *
     * Retrieves movements created within a specific time period.
     * Essential for time-based reporting.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - CreatedAt: Filter by createdAt field
     * - Between: Date range filter (start <= date <= end)
     * - OrderBy: Sort results
     * - CreatedAt: Sort by date
     * - Desc: Newest first
     *
     * Generated SQL:
     * SELECT * FROM stock_movements
     * WHERE created_at BETWEEN ? AND ?
     * ORDER BY created_at DESC
     *
     * Use cases:
     * ----------
     * 1. Daily reports:
     *    - Get movements for today
     *    - Daily sales/returns summary
     *    - End-of-day reconciliation
     *
     * 2. Monthly reports:
     *    - Get movements for month
     *    - Monthly sales analysis
     *    - Inventory turnover
     *
     * 3. Custom date ranges:
     *    - Quarter-end reports
     *    - Year-to-date analysis
     *    - Campaign period tracking
     *
     * Example usage:
     * --------------
     * // Get today's movements
     * LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
     * LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
     * List<StockMovement> todayMovements =
     *     stockMovementRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
     *         startOfDay,
     *         endOfDay
     *     );
     *
     * // Get last 30 days
     * LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
     * LocalDateTime now = LocalDateTime.now();
     * List<StockMovement> last30Days =
     *     stockMovementRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
     *         thirtyDaysAgo,
     *         now
     *     );
     *
     * @param start Start of date range (inclusive)
     * @param end End of date range (inclusive)
     * @return List of movements in this date range, newest first
     */
    List<StockMovement> findByCreatedAtBetweenOrderByCreatedAtDesc(
        LocalDateTime start,
        LocalDateTime end
    );

    /**
     * Find movements for product in date range
     *
     * Retrieves movements for a specific product within a time period.
     * Combines product and date filtering.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - ProductId: Filter by productId
     * - And: Logical AND
     * - CreatedAt: Filter by date
     * - Between: Date range
     * - OrderBy: Sort results
     * - CreatedAt: Sort by date
     * - Desc: Newest first
     *
     * Generated SQL:
     * SELECT * FROM stock_movements
     * WHERE product_id = ? AND created_at BETWEEN ? AND ?
     * ORDER BY created_at DESC
     *
     * Use cases:
     * ----------
     * 1. Product activity report:
     *    - See all changes for a product in a period
     *    - Timeline visualization
     *    - Understand stock behavior
     *
     * 2. Product sales in period:
     *    - Filter by SALE type separately
     *    - Or process all movements
     *    - Period-specific analytics
     *
     * Example usage:
     * --------------
     * // Get product movements for last month
     * LocalDateTime lastMonth = LocalDateTime.now().minusMonths(1);
     * LocalDateTime now = LocalDateTime.now();
     * List<StockMovement> productMovements =
     *     stockMovementRepository.findByProductIdAndCreatedAtBetweenOrderByCreatedAtDesc(
     *         123L,
     *         lastMonth,
     *         now
     *     );
     *
     * @param productId The ID of the product
     * @param start Start of date range
     * @param end End of date range
     * @return List of movements for this product in date range, newest first
     */
    List<StockMovement> findByProductIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        Long productId,
        LocalDateTime start,
        LocalDateTime end
    );

    /**
     * Calculate total quantity change by type
     *
     * Sums quantity changes for a specific movement type.
     * Useful for aggregated statistics.
     *
     * Custom JPQL query using @Query annotation.
     *
     * Generated SQL:
     * SELECT SUM(quantity_change) FROM stock_movements
     * WHERE movement_type = ?
     *
     * Return type Integer:
     * - Sum of all quantity changes for this type
     * - Can be null if no movements of this type
     * - Positive for increases (PURCHASE, RETURN)
     * - Negative for decreases (SALE, DAMAGE)
     *
     * Use cases:
     * ----------
     * 1. Total sales:
     *    - Sum all SALE movements (will be negative)
     *    - Take absolute value for total sold
     *    - Sales volume reporting
     *
     * 2. Total purchases:
     *    - Sum all PURCHASE movements (positive)
     *    - Total units received from suppliers
     *    - Procurement tracking
     *
     * 3. Total damages:
     *    - Sum all DAMAGE movements (negative)
     *    - Take absolute value for total damaged
     *    - Quality/handling metrics
     *
     * Example usage:
     * --------------
     * // Get total sales
     * Integer totalSalesChange =
     *     stockMovementRepository.getTotalQuantityChangeByType(MovementType.SALE);
     * int totalSold = totalSalesChange != null ? Math.abs(totalSalesChange) : 0;
     * System.out.println("Total units sold: " + totalSold);
     *
     * // Get total purchases
     * Integer totalPurchased =
     *     stockMovementRepository.getTotalQuantityChangeByType(MovementType.PURCHASE);
     * System.out.println("Total units purchased: " +
     *     (totalPurchased != null ? totalPurchased : 0));
     *
     * @param movementType Type of movement to sum
     * @return Sum of quantity changes (null if no movements)
     */
    @Query("SELECT SUM(sm.quantityChange) FROM StockMovement sm WHERE sm.movementType = :type")
    Integer getTotalQuantityChangeByType(@Param("type") MovementType movementType);

    /**
     * Count movements by type
     *
     * Returns number of movements of a specific type.
     * Useful for frequency analysis.
     *
     * Method name breakdown:
     * - countBy: COUNT query
     * - MovementType: Filter by movementType
     *
     * Generated SQL:
     * SELECT COUNT(*) FROM stock_movements WHERE movement_type = ?
     *
     * Use cases:
     * ----------
     * 1. Transaction frequency:
     *    - How many sales occurred?
     *    - How many returns?
     *    - How many damages?
     *
     * 2. Activity monitoring:
     *    - Track movement frequency
     *    - Identify unusual patterns
     *    - Operational metrics
     *
     * Example usage:
     * --------------
     * // Count sales transactions
     * long salesCount = stockMovementRepository.countByMovementType(MovementType.SALE);
     * long returnsCount = stockMovementRepository.countByMovementType(MovementType.RETURN);
     *
     * // Calculate return rate
     * double returnRate = (returnsCount * 100.0) / salesCount;
     * System.out.println("Return rate: " + returnRate + "%");
     *
     * @param movementType Type of movement to count
     * @return Number of movements of this type
     */
    long countByMovementType(MovementType movementType);

    /**
     * Get recent movements
     *
     * Retrieves most recent stock movements across all products.
     * Limited to specified count.
     *
     * Custom JPQL query with LIMIT.
     *
     * Generated SQL:
     * SELECT * FROM stock_movements
     * ORDER BY created_at DESC
     * LIMIT ?
     *
     * Use cases:
     * ----------
     * 1. Activity feed:
     *    - Show recent inventory activity
     *    - Real-time dashboard
     *    - Movement monitoring
     *
     * 2. Quick overview:
     *    - See what's happening now
     *    - Spot check operations
     *    - Verify recent changes
     *
     * Example usage:
     * --------------
     * // Get last 10 movements
     * List<StockMovement> recentMovements =
     *     stockMovementRepository.findRecentMovements(10);
     *
     * // Display activity feed
     * System.out.println("Recent Activity:");
     * for (StockMovement movement : recentMovements) {
     *     System.out.println(movement.getCreatedAt() + " - " +
     *         "Product " + movement.getProductId() + ": " +
     *         movement.getMovementType() + " " +
     *         movement.getQuantityChange());
     * }
     *
     * @param limit Maximum number of movements to return
     * @return List of recent movements, newest first
     */
    @Query(value = "SELECT sm FROM StockMovement sm ORDER BY sm.createdAt DESC")
    List<StockMovement> findRecentMovements(@Param("limit") int limit);

    // =========================================================================
    // Additional Query Methods (Can be added as needed)
    // =========================================================================
    //
    // /**
    //  * Find movements by multiple products (bulk lookup)
    //  */
    // List<StockMovement> findByProductIdIn(List<Long> productIds);
    //
    // /**
    //  * Calculate net change for product in date range
    //  */
    // @Query("SELECT SUM(sm.quantityChange) FROM StockMovement sm " +
    //        "WHERE sm.productId = :productId " +
    //        "AND sm.createdAt BETWEEN :start AND :end")
    // Integer getNetChangeForProduct(
    //     @Param("productId") Long productId,
    //     @Param("start") LocalDateTime start,
    //     @Param("end") LocalDateTime end
    // );
    //
    // /**
    //  * Delete old movements (data retention)
    //  */
    // void deleteByCreatedAtBefore(LocalDateTime date);
    //
    // /**
    //  * Find movements with notes containing keyword (search)
    //  */
    // List<StockMovement> findByNotesContainingIgnoreCase(String keyword);
    //
    // =========================================================================
    // Performance Notes
    // =========================================================================
    //
    // 1. Indexes:
    //    - idx_product_id: Fast product movement lookup
    //    - idx_movement_type: Fast type filtering
    //    - idx_created_at: Efficient date range queries
    //    - idx_reference_id: Fast reference lookup
    //
    // 2. Query Optimization:
    //    - Date range queries use BETWEEN (index-friendly)
    //    - Aggregate functions (SUM, COUNT) execute in database
    //    - Composite conditions use AND (can use multiple indexes)
    //
    // 3. Data Retention:
    //    - StockMovement table can grow large over time
    //    - Consider archiving old movements (> 2 years)
    //    - Or partitioning by date
    //
    // 4. Pagination:
    //    For large result sets, use pagination:
    //    Page<StockMovement> findByProductId(Long productId, Pageable pageable);
    //    // Usage: findByProductId(123L, PageRequest.of(0, 20))
}
