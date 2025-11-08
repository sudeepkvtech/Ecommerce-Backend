package com.ecommerce.orderservice.repository;

// Import the OrderItem entity
import com.ecommerce.orderservice.entity.OrderItem;

// Import Spring Data JPA repository interface
import org.springframework.data.jpa.repository.JpaRepository;

// Import query annotation
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Import stereotype annotation
import org.springframework.stereotype.Repository;

// Import List for multiple results
import java.util.List;

/**
 * OrderItem Repository
 *
 * Data access layer for OrderItem entity.
 * Provides methods to interact with the order_items table in the database.
 *
 * What is OrderItem Repository used for?
 * ---------------------------------------
 * While Order repository handles orders, OrderItem repository provides
 * additional queries specific to order items:
 * - Find all items in an order
 * - Find items for a specific product
 * - Analytics on product sales
 * - Reporting on popular products
 *
 * Why separate repository for OrderItem?
 * ---------------------------------------
 * 1. Separation of concerns:
 *    - OrderRepository: Order-level operations
 *    - OrderItemRepository: Item-level operations
 *
 * 2. Query flexibility:
 *    - Can query items independently of orders
 *    - Useful for product-centric queries
 *
 * 3. Performance:
 *    - Direct item queries without loading entire order
 *    - More efficient for specific use cases
 *
 * Note: Most item operations go through Order.items relationship
 * This repository is for specialized queries
 *
 * JpaRepository<OrderItem, Long>:
 * - OrderItem: Entity type this repository manages
 * - Long: Type of the entity's ID field
 */

// @Repository marks this as a Spring Data repository
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Find all items for a specific order
     *
     * Retrieves all products/items in a single order.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - Order_Id: Navigates to Order entity and uses its id field
     *   - In OrderItem, we have: private Order order;
     *   - order.id is accessed as Order_Id in method name
     *
     * Generated SQL:
     * SELECT * FROM order_items WHERE order_id = ?
     *
     * Alternative approach:
     * Instead of this query, you can also use:
     * Order order = orderRepository.findById(orderId).get();
     * List<OrderItem> items = order.getItems();
     *
     * When to use which approach?
     * ----------------------------
     * Use Order.getItems():
     * - When you already have the Order object
     * - When you need both order and items
     * - Simpler code
     *
     * Use orderItemRepository.findByOrder_Id():
     * - When you only need items, not the order
     * - When you don't want to load the entire order
     * - More efficient for item-only operations
     *
     * Usage:
     * List<OrderItem> items = orderItemRepository.findByOrder_Id(123L);
     * for (OrderItem item : items) {
     *     System.out.println(item.getProductName() + " x " + item.getQuantity());
     * }
     *
     * @param orderId The ID of the order
     * @return List of items in the order (empty if order has no items)
     */
    List<OrderItem> findByOrder_Id(Long orderId);

    /**
     * Find all items for a specific product (across all orders)
     *
     * Retrieves all order items that reference a specific product.
     * Useful for product analytics and reporting.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - ProductId: Filter by productId field
     *
     * Generated SQL:
     * SELECT * FROM order_items WHERE product_id = ?
     *
     * Use cases:
     * ----------
     * 1. Product sales report:
     *    - How many times has this product been ordered?
     *    - Total quantity sold
     *    - Revenue generated
     *
     * 2. Product recalls:
     *    - Which orders contain this product?
     *    - Need to notify customers
     *
     * 3. Product analytics:
     *    - Track product performance
     *    - Identify best sellers
     *
     * Usage example - Calculate total quantity sold:
     * List<OrderItem> items = orderItemRepository.findByProductId(456L);
     * int totalQuantity = items.stream()
     *     .mapToInt(OrderItem::getQuantity)
     *     .sum();
     * System.out.println("Product 456 sold: " + totalQuantity + " units");
     *
     * Usage example - Calculate total revenue:
     * List<OrderItem> items = orderItemRepository.findByProductId(456L);
     * BigDecimal totalRevenue = items.stream()
     *     .map(OrderItem::getSubtotal)
     *     .reduce(BigDecimal.ZERO, BigDecimal::add);
     * System.out.println("Product 456 revenue: $" + totalRevenue);
     *
     * @param productId The ID of the product
     * @return List of order items for this product (empty if product never ordered)
     */
    List<OrderItem> findByProductId(Long productId);

    /**
     * Count order items for a specific product
     *
     * Returns how many times a product has been ordered (number of order items, not quantity).
     * More efficient than loading all items and counting.
     *
     * Method name breakdown:
     * - countBy: COUNT query
     * - ProductId: Filter by productId
     *
     * Generated SQL:
     * SELECT COUNT(*) FROM order_items WHERE product_id = ?
     *
     * Note: This counts the number of order items, NOT total quantity sold.
     *
     * Example:
     * - Order 1: 2 x Product #456 (1 order item)
     * - Order 2: 3 x Product #456 (1 order item)
     * - Order 3: 1 x Product #456 (1 order item)
     * countByProductId(456) = 3 (3 order items)
     * Total quantity = 2 + 3 + 1 = 6 units
     *
     * For total quantity, use getTotalQuantitySoldForProduct()
     *
     * Usage:
     * long orderCount = orderItemRepository.countByProductId(456L);
     * System.out.println("Product 456 appeared in " + orderCount + " orders");
     *
     * @param productId The ID of the product
     * @return Number of order items for this product
     */
    long countByProductId(Long productId);

    /**
     * Calculate total quantity sold for a product
     *
     * Custom JPQL query that sums the quantity field for all items of a product.
     *
     * @Query annotation with aggregate function:
     * - SUM(oi.quantity): Aggregate function to sum quantities
     * - FROM OrderItem oi: Alias 'oi' for OrderItem
     * - WHERE oi.productId = :productId: Filter by product
     *
     * Generated SQL:
     * SELECT SUM(quantity) FROM order_items WHERE product_id = ?
     *
     * Return type Long:
     * - SUM returns null if no items found
     * - We default to 0L if null (see usage in service)
     *
     * Example calculation:
     * - Order 1: 2 x Product #456
     * - Order 2: 3 x Product #456
     * - Order 3: 1 x Product #456
     * getTotalQuantitySoldForProduct(456) = 2 + 3 + 1 = 6
     *
     * Usage:
     * Long totalSold = orderItemRepository.getTotalQuantitySoldForProduct(456L);
     * if (totalSold == null) {
     *     totalSold = 0L; // No items sold
     * }
     * System.out.println("Total units sold: " + totalSold);
     *
     * Analytics use case:
     * // Find top selling products
     * List<Long> productIds = getAllProductIds();
     * Map<Long, Long> salesByProduct = new HashMap<>();
     * for (Long productId : productIds) {
     *     Long quantity = orderItemRepository.getTotalQuantitySoldForProduct(productId);
     *     salesByProduct.put(productId, quantity != null ? quantity : 0L);
     * }
     * // Sort by quantity to find best sellers
     *
     * @param productId The ID of the product
     * @return Total quantity sold (null if product never ordered)
     */
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.productId = :productId")
    Long getTotalQuantitySoldForProduct(@Param("productId") Long productId);

    /**
     * Calculate total revenue for a product
     *
     * Custom JPQL query that sums the subtotal field for all items of a product.
     *
     * @Query annotation:
     * - SUM(oi.subtotal): Aggregate function to sum subtotals
     * - Returns total revenue generated by this product
     *
     * Generated SQL:
     * SELECT SUM(subtotal) FROM order_items WHERE product_id = ?
     *
     * Return type BigDecimal:
     * - SUM returns null if no items found
     * - Service should default to BigDecimal.ZERO if null
     *
     * Example calculation:
     * - Order 1: 2 x Product #456 @ $29.99 = $59.98
     * - Order 2: 3 x Product #456 @ $29.99 = $89.97
     * - Order 3: 1 x Product #456 @ $29.99 = $29.99
     * getTotalRevenueForProduct(456) = $59.98 + $89.97 + $29.99 = $179.94
     *
     * Note: Handles price changes correctly!
     * If product price changed over time, each order item has the price
     * at time of purchase, so revenue is accurate.
     *
     * Usage:
     * BigDecimal revenue = orderItemRepository.getTotalRevenueForProduct(456L);
     * if (revenue == null) {
     *     revenue = BigDecimal.ZERO;
     * }
     * System.out.println("Total revenue: $" + revenue);
     *
     * Business intelligence use case:
     * // Product profitability analysis
     * BigDecimal revenue = getTotalRevenueForProduct(productId);
     * BigDecimal cost = getProductCost(productId); // From Product Service
     * Long quantity = getTotalQuantitySoldForProduct(productId);
     * BigDecimal profit = revenue.subtract(cost.multiply(new BigDecimal(quantity)));
     * System.out.println("Profit margin: $" + profit);
     *
     * @param productId The ID of the product
     * @return Total revenue from this product (null if never ordered)
     */
    @Query("SELECT SUM(oi.subtotal) FROM OrderItem oi WHERE oi.productId = :productId")
    java.math.BigDecimal getTotalRevenueForProduct(@Param("productId") Long productId);

    /**
     * Delete all items for a specific order
     *
     * Removes all order items associated with an order.
     *
     * Method name breakdown:
     * - deleteBy: DELETE query
     * - Order_Id: Filter by order.id
     *
     * Generated SQL:
     * DELETE FROM order_items WHERE order_id = ?
     *
     * When is this used?
     * ------------------
     * Usually not needed because of cascade delete:
     * - Order entity has: cascade = CascadeType.ALL, orphanRemoval = true
     * - When order is deleted, items are automatically deleted
     *
     * However, this method can be useful for:
     * - Manually clearing order items without deleting order
     * - Testing and cleanup operations
     * - Bulk operations
     *
     * Note: Be careful! This bypasses cascade rules and lifecycle callbacks.
     *
     * Usage:
     * orderItemRepository.deleteByOrder_Id(123L);
     * // All items for order 123 are deleted
     *
     * @param orderId The ID of the order whose items to delete
     */
    void deleteByOrder_Id(Long orderId);

    // =========================================================================
    // Additional Query Methods (Can be added as needed)
    // =========================================================================
    //
    // /**
    //  * Find items with quantity greater than specified value
    //  * (Bulk orders)
    //  */
    // List<OrderItem> findByQuantityGreaterThan(Integer quantity);
    //
    // /**
    //  * Find items with price in a specific range
    //  */
    // List<OrderItem> findByProductPriceBetween(
    //     BigDecimal minPrice,
    //     BigDecimal maxPrice
    // );
    //
    // /**
    //  * Find top selling products (by quantity)
    //  */
    // @Query("SELECT oi.productId, SUM(oi.quantity) as total " +
    //        "FROM OrderItem oi " +
    //        "GROUP BY oi.productId " +
    //        "ORDER BY total DESC")
    // List<Object[]> findTopSellingProducts(Pageable pageable);
    // // Usage: findTopSellingProducts(PageRequest.of(0, 10))
    //
    // /**
    //  * Find top revenue generating products
    //  */
    // @Query("SELECT oi.productId, SUM(oi.subtotal) as revenue " +
    //        "FROM OrderItem oi " +
    //        "GROUP BY oi.productId " +
    //        "ORDER BY revenue DESC")
    // List<Object[]> findTopRevenueProducts(Pageable pageable);
    //
    // /**
    //  * Find items for specific product and order status
    //  * (Items in pending orders, shipped orders, etc.)
    //  */
    // @Query("SELECT oi FROM OrderItem oi " +
    //        "WHERE oi.productId = :productId " +
    //        "AND oi.order.status = :status")
    // List<OrderItem> findByProductIdAndOrderStatus(
    //     @Param("productId") Long productId,
    //     @Param("status") OrderStatus status
    // );
    //
    // =========================================================================
    // Performance Notes
    // =========================================================================
    //
    // 1. Indexes:
    //    - idx_order_id: Fast order item lookups
    //    - idx_product_id: Fast product analytics queries
    //
    // 2. Aggregate queries:
    //    - SUM, COUNT operations are database-optimized
    //    - Much faster than loading all data and calculating in Java
    //    - Scales well with large datasets
    //
    // 3. N+1 Query Problem:
    //    Bad: Loading items individually
    //      Order order = orderRepository.findById(id).get();
    //      for (OrderItem item : order.getItems()) {
    //          // Each item loaded separately (N+1 queries!)
    //      }
    //
    //    Good: Fetch join to load items with order
    //      @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    //      Order findByIdWithItems(@Param("id") Long id);
    //
    // 4. Batch operations:
    //    Use saveAll() for bulk inserts
    //    List<OrderItem> items = ...;
    //    orderItemRepository.saveAll(items);
    //    // More efficient than individual save() calls
}
