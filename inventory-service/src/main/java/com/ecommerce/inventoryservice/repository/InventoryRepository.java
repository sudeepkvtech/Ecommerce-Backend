package com.ecommerce.inventoryservice.repository;

// Import the Inventory entity
import com.ecommerce.inventoryservice.entity.Inventory;

// Import Spring Data JPA repository interface
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Import stereotype annotation
import org.springframework.stereotype.Repository;

// Import List and Optional for query results
import java.util.List;
import java.util.Optional;

/**
 * Inventory Repository
 *
 * Data access layer for Inventory entity.
 * Provides methods to interact with the inventory table.
 *
 * What is a Repository?
 * =====================
 * Repository is part of the Data Access Layer in Spring applications.
 * It provides an abstraction for database operations (CRUD).
 *
 * Spring Data JPA automatically implements this interface.
 * You just define method signatures, Spring generates the implementation.
 *
 * How does Spring Data JPA work?
 * ===============================
 * 1. Method Name Parsing:
 *    - Spring parses method names to generate queries
 *    - findBy... → SELECT with WHERE clause
 *    - existsBy... → Check existence
 *    - countBy... → COUNT query
 *
 * 2. Naming Convention:
 *    - findByProductId → WHERE product_id = ?
 *    - findByAvailableQuantityLessThan → WHERE available_quantity < ?
 *
 * 3. Automatic Implementation:
 *    - Spring generates implementation at runtime
 *    - No need to write SQL or implementation class
 *    - Type-safe (compiler checks parameter types)
 *
 * JpaRepository<Inventory, Long>:
 * ================================
 * - Inventory: Entity type this repository manages
 * - Long: Type of the entity's ID field
 *
 * Inherited methods from JpaRepository:
 * - save(Inventory) - Insert or update
 * - findById(Long) - Find by primary key
 * - findAll() - Get all records
 * - deleteById(Long) - Delete by ID
 * - count() - Count all records
 * - existsById(Long) - Check if exists
 */

// @Repository marks this as a Spring Data repository
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * Find inventory by product ID
     *
     * Retrieves inventory record for a specific product.
     * Most commonly used query in this repository.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - ProductId: Filter by productId field
     *
     * Generated SQL:
     * SELECT * FROM inventory WHERE product_id = ?
     *
     * Return type Optional<Inventory>:
     * - Optional means inventory may or may not exist
     * - Safer than returning null
     * - Prevents NullPointerException
     *
     * Use cases:
     * ----------
     * 1. Check stock before order:
     *    - Customer wants to order product
     *    - Check if sufficient stock available
     *    - Show "X in stock" on product page
     *
     * 2. Reserve stock for order:
     *    - Get inventory for product
     *    - Call inventory.reserveStock(quantity)
     *    - Save updated inventory
     *
     * 3. Display stock on product page:
     *    - Product Service calls Inventory Service
     *    - Get available quantity
     *    - Show to customer
     *
     * Example usage:
     * --------------
     * // In InventoryService
     * public InventoryResponse getInventory(Long productId) {
     *     Inventory inventory = inventoryRepository.findByProductId(productId)
     *         .orElseThrow(() -> new ResourceNotFoundException(
     *             "Inventory not found for product: " + productId
     *         ));
     *     return mapToResponse(inventory);
     * }
     *
     * // Check stock availability
     * Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(123L);
     * if (inventoryOpt.isPresent()) {
     *     Inventory inventory = inventoryOpt.get();
     *     if (inventory.hasSufficientStock(5)) {
     *         // Can fulfill order
     *     }
     * }
     *
     * @param productId The ID of the product
     * @return Optional containing inventory if found, empty otherwise
     */
    Optional<Inventory> findByProductId(Long productId);

    /**
     * Check if inventory exists for product
     *
     * Checks if an inventory record exists for a product.
     * Faster than findByProductId when you only need to check existence.
     *
     * Method name breakdown:
     * - existsBy: Boolean query (true/false)
     * - ProductId: Check productId field
     *
     * Generated SQL:
     * SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
     * FROM inventory WHERE product_id = ?
     *
     * Return type boolean:
     * - true if inventory exists
     * - false if no inventory found
     *
     * Use cases:
     * ----------
     * 1. Prevent duplicate inventory:
     *    - Before creating inventory for new product
     *    - Check if already exists
     *    - Avoid unique constraint violation
     *
     * 2. Validation:
     *    - Product Service checks if inventory exists
     *    - Before deleting product
     *    - Ensure inventory is cleaned up
     *
     * Example usage:
     * --------------
     * // In InventoryService.createInventory()
     * if (inventoryRepository.existsByProductId(productId)) {
     *     throw new DuplicateResourceException(
     *         "Inventory already exists for product: " + productId
     *     );
     * }
     *
     * // Create new inventory
     * Inventory inventory = new Inventory();
     * // ... set fields
     *
     * @param productId The ID of the product
     * @return true if inventory exists, false otherwise
     */
    boolean existsByProductId(Long productId);

    /**
     * Find low stock items
     *
     * Retrieves all products where available quantity is below threshold.
     * Used for reordering alerts and low stock reports.
     *
     * Custom JPQL query using @Query annotation.
     * Compares available quantity with low stock threshold.
     *
     * Generated SQL:
     * SELECT * FROM inventory
     * WHERE available_quantity < low_stock_threshold
     * ORDER BY available_quantity ASC
     *
     * Return type List<Inventory>:
     * - Returns empty list if no low stock items
     * - Sorted by available quantity (lowest first)
     *
     * Use cases:
     * ----------
     * 1. Admin dashboard:
     *    - Show products needing restock
     *    - Prioritize by urgency (lowest stock first)
     *    - Generate reorder report
     *
     * 2. Automated alerts:
     *    - Daily job checks low stock
     *    - Send email to purchasing team
     *    - Create purchase orders automatically
     *
     * 3. Inventory planning:
     *    - Identify products to restock
     *    - Calculate reorder quantities
     *    - Plan warehouse space
     *
     * Example usage:
     * --------------
     * // Get all low stock products
     * List<Inventory> lowStockItems = inventoryRepository.findLowStockItems();
     *
     * // Send alert if any low stock
     * if (!lowStockItems.isEmpty()) {
     *     emailService.sendLowStockAlert(lowStockItems);
     * }
     *
     * // Display in admin dashboard
     * for (Inventory inv : lowStockItems) {
     *     System.out.println("Product " + inv.getProductId() +
     *         ": Only " + inv.getAvailableQuantity() + " left! " +
     *         "(Threshold: " + inv.getLowStockThreshold() + ")");
     * }
     *
     * @return List of low stock inventory items, sorted by quantity (lowest first)
     */
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity < i.lowStockThreshold ORDER BY i.availableQuantity ASC")
    List<Inventory> findLowStockItems();

    /**
     * Find out of stock items
     *
     * Retrieves all products with zero or negative available quantity.
     * Critical for preventing orders that cannot be fulfilled.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - AvailableQuantity: Filter by availableQuantity field
     * - LessThanEqual: <= comparison
     *
     * Generated SQL:
     * SELECT * FROM inventory WHERE available_quantity <= 0
     *
     * Use cases:
     * ----------
     * 1. Product display:
     *    - Hide out of stock products from listings
     *    - Show "Out of Stock" badge
     *    - Offer "Notify when available" option
     *
     * 2. Order validation:
     *    - Prevent orders for unavailable products
     *    - Show error: "This item is currently out of stock"
     *
     * 3. Reporting:
     *    - Track stockout frequency
     *    - Identify popular items needing faster restocking
     *    - Calculate lost sales due to stockouts
     *
     * Example usage:
     * --------------
     * // Get all out of stock products
     * List<Inventory> outOfStock = inventoryRepository.findByAvailableQuantityLessThanEqual(0);
     *
     * // Hide from product listings
     * List<Long> unavailableProductIds = outOfStock.stream()
     *     .map(Inventory::getProductId)
     *     .collect(Collectors.toList());
     *
     * @param quantity Threshold (typically 0 for out of stock)
     * @return List of inventory items with available quantity <= threshold
     */
    List<Inventory> findByAvailableQuantityLessThanEqual(Integer quantity);

    /**
     * Find items with available stock
     *
     * Retrieves all products that can be sold (available > 0).
     * Opposite of findByAvailableQuantityLessThanEqual.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - AvailableQuantity: Filter by availableQuantity field
     * - GreaterThan: > comparison
     *
     * Generated SQL:
     * SELECT * FROM inventory WHERE available_quantity > ?
     *
     * Use cases:
     * ----------
     * 1. Product filtering:
     *    - Show only in-stock products
     *    - "Available Now" filter
     *    - Quick ship options
     *
     * 2. Inventory summary:
     *    - Count products in stock
     *    - Calculate total inventory value
     *    - Stock coverage analysis
     *
     * Example usage:
     * --------------
     * // Get all products in stock
     * List<Inventory> inStock = inventoryRepository.findByAvailableQuantityGreaterThan(0);
     *
     * // Count in-stock products
     * long inStockCount = inStock.size();
     * System.out.println("Products in stock: " + inStockCount);
     *
     * @param quantity Threshold (typically 0 for any stock available)
     * @return List of inventory items with available quantity > threshold
     */
    List<Inventory> findByAvailableQuantityGreaterThan(Integer quantity);

    /**
     * Find items with reservations
     *
     * Retrieves all products that have stock reserved for pending orders.
     * Helps monitor order pipeline.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - ReservedQuantity: Filter by reservedQuantity field
     * - GreaterThan: > comparison
     *
     * Generated SQL:
     * SELECT * FROM inventory WHERE reserved_quantity > ?
     *
     * Use cases:
     * ----------
     * 1. Order pipeline monitoring:
     *    - See how much stock is "in limbo"
     *    - Pending payments
     *    - Orders being processed
     *
     * 2. Fulfillment planning:
     *    - Estimate upcoming shipments
     *    - Prepare packing materials
     *    - Staff scheduling
     *
     * 3. Cash flow forecasting:
     *    - Reserved stock = pending revenue
     *    - Estimate payment confirmations
     *    - Revenue projections
     *
     * Example usage:
     * --------------
     * // Get all products with reservations
     * List<Inventory> withReservations =
     *     inventoryRepository.findByReservedQuantityGreaterThan(0);
     *
     * // Calculate total reserved value
     * int totalReserved = withReservations.stream()
     *     .mapToInt(Inventory::getReservedQuantity)
     *     .sum();
     * System.out.println("Total reserved units: " + totalReserved);
     *
     * @param quantity Threshold (typically 0 for any reservations)
     * @return List of inventory items with reserved quantity > threshold
     */
    List<Inventory> findByReservedQuantityGreaterThan(Integer quantity);

    /**
     * Count total inventory value
     *
     * Calculates total units across all products.
     * Sum of all total quantities.
     *
     * Custom JPQL query using @Query annotation.
     *
     * Generated SQL:
     * SELECT SUM(total_quantity) FROM inventory
     *
     * Return type Long:
     * - Sum of all total quantities
     * - Can be null if no inventory records
     *
     * Use cases:
     * ----------
     * 1. Inventory overview:
     *    - Total units in warehouse
     *    - Dashboard summary
     *    - Warehouse capacity planning
     *
     * 2. Reporting:
     *    - Inventory level trends
     *    - Month-over-month comparison
     *    - Stock turnover calculations
     *
     * Example usage:
     * --------------
     * // Get total inventory units
     * Long totalUnits = inventoryRepository.getTotalInventoryUnits();
     * if (totalUnits == null) {
     *     totalUnits = 0L;
     * }
     * System.out.println("Total inventory: " + totalUnits + " units");
     *
     * @return Total units across all products (null if no inventory)
     */
    @Query("SELECT SUM(i.totalQuantity) FROM Inventory i")
    Long getTotalInventoryUnits();

    /**
     * Count available inventory
     *
     * Calculates total available units across all products.
     * Sum of all available quantities.
     *
     * Generated SQL:
     * SELECT SUM(available_quantity) FROM inventory
     *
     * Use cases:
     * ----------
     * 1. Available to sell:
     *    - How much can be sold right now
     *    - Exclude reserved stock
     *    - Real-time availability
     *
     * 2. Dashboard metrics:
     *    - Available vs reserved ratio
     *    - Fulfillment capacity
     *
     * Example usage:
     * --------------
     * Long availableUnits = inventoryRepository.getAvailableInventoryUnits();
     * Long reservedUnits = inventoryRepository.getReservedInventoryUnits();
     * Long totalUnits = inventoryRepository.getTotalInventoryUnits();
     *
     * System.out.println("Available: " + availableUnits);
     * System.out.println("Reserved: " + reservedUnits);
     * System.out.println("Total: " + totalUnits);
     *
     * @return Total available units (null if no inventory)
     */
    @Query("SELECT SUM(i.availableQuantity) FROM Inventory i")
    Long getAvailableInventoryUnits();

    /**
     * Count reserved inventory
     *
     * Calculates total reserved units across all products.
     * Sum of all reserved quantities.
     *
     * Generated SQL:
     * SELECT SUM(reserved_quantity) FROM inventory
     *
     * Use cases:
     * ----------
     * 1. Order pipeline:
     *    - Pending order volume
     *    - Revenue forecast
     *
     * 2. Operational metrics:
     *    - Order processing speed
     *    - Payment conversion rate
     *
     * @return Total reserved units (null if no inventory)
     */
    @Query("SELECT SUM(i.reservedQuantity) FROM Inventory i")
    Long getReservedInventoryUnits();

    /**
     * Count low stock products
     *
     * Returns number of products below their threshold.
     *
     * Generated SQL:
     * SELECT COUNT(*) FROM inventory
     * WHERE available_quantity < low_stock_threshold
     *
     * Use cases:
     * ----------
     * 1. Quick summary:
     *    - Dashboard badge: "5 products low on stock"
     *    - Alert indicator
     *
     * 2. Trend tracking:
     *    - Daily low stock count
     *    - Identify restocking issues
     *
     * Example usage:
     * --------------
     * long lowStockCount = inventoryRepository.countLowStockProducts();
     * if (lowStockCount > 0) {
     *     System.out.println("Warning: " + lowStockCount +
     *         " products are low on stock!");
     * }
     *
     * @return Number of low stock products
     */
    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.availableQuantity < i.lowStockThreshold")
    long countLowStockProducts();

    /**
     * Count out of stock products
     *
     * Returns number of products with zero availability.
     *
     * Generated SQL:
     * SELECT COUNT(*) FROM inventory WHERE available_quantity = 0
     *
     * Use cases:
     * ----------
     * 1. Stockout monitoring:
     *    - Track stockout frequency
     *    - Identify restocking bottlenecks
     *
     * 2. Performance metrics:
     *    - Stockout rate
     *    - Service level tracking
     *
     * Example usage:
     * --------------
     * long outOfStockCount = inventoryRepository.countOutOfStockProducts();
     * long totalProducts = inventoryRepository.count();
     * double stockoutRate = (outOfStockCount * 100.0) / totalProducts;
     * System.out.println("Stockout rate: " + stockoutRate + "%");
     *
     * @return Number of out of stock products
     */
    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.availableQuantity = 0")
    long countOutOfStockProducts();

    // =========================================================================
    // Additional Query Methods (Can be added as needed)
    // =========================================================================
    //
    // /**
    //  * Find inventory by ID list (bulk lookup)
    //  */
    // List<Inventory> findByProductIdIn(List<Long> productIds);
    //
    // /**
    //  * Find products needing immediate restock (available = 0 or very low)
    //  */
    // @Query("SELECT i FROM Inventory i WHERE i.availableQuantity <= 5 ORDER BY i.availableQuantity ASC")
    // List<Inventory> findCriticalStock();
    //
    // /**
    //  * Find overstocked items (available > threshold * 2)
    //  */
    // @Query("SELECT i FROM Inventory i WHERE i.availableQuantity > (i.lowStockThreshold * 2)")
    // List<Inventory> findOverstockedItems();
    //
    // /**
    //  * Delete inventory by product ID
    //  */
    // void deleteByProductId(Long productId);
    //
    // =========================================================================
    // Performance Notes
    // =========================================================================
    //
    // 1. Indexes:
    //    - idx_product_id: Fast product lookup (most common query)
    //    - idx_low_stock: Efficient low stock queries
    //
    // 2. Query Optimization:
    //    - Use existsBy instead of findBy when only checking presence
    //    - Use COUNT queries for statistics (faster than loading all records)
    //    - Use aggregate functions (SUM, COUNT) in database, not Java
    //
    // 3. Avoiding N+1 Queries:
    //    - No relationships in Inventory entity (productId is ID only)
    //    - If relationships existed, use @Query with JOIN FETCH
    //
    // 4. Pagination:
    //    For large datasets, use pagination:
    //    Page<Inventory> findAll(Pageable pageable);
    //    // Usage: findAll(PageRequest.of(0, 20))
}
