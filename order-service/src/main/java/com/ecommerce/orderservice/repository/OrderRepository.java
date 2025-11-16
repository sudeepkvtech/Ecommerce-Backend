package com.ecommerce.orderservice.repository;

// Import the Order entity
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderStatus;

// Import Spring Data JPA repository interface
import org.springframework.data.jpa.repository.JpaRepository;

// Import query annotation for custom SQL
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Import stereotype annotation
import org.springframework.stereotype.Repository;

// Import Optional for null-safe returns
import java.util.Optional;

// Import List for multiple results
import java.util.List;

// Import LocalDateTime for date queries
import java.time.LocalDateTime;

/**
 * Order Repository
 *
 * Data access layer for Order entity.
 * Provides methods to interact with the orders table in the database.
 *
 * What is a Repository?
 * ---------------------
 * The Repository pattern provides an abstraction layer between business logic
 * and data access logic.
 *
 * Benefits:
 * - Separates business logic from database operations
 * - Makes code testable (can mock repository)
 * - Provides clean API for data access
 * - Hides SQL complexity from service layer
 *
 * Spring Data JPA Repository:
 * ----------------------------
 * By extending JpaRepository, we get these methods for FREE:
 * - save(Order order): Insert or update
 * - findById(Long id): Find by primary key
 * - findAll(): Get all orders
 * - deleteById(Long id): Delete by ID
 * - count(): Count total orders
 * - existsById(Long id): Check if exists
 * And many more...
 *
 * No implementation needed! Spring generates it at runtime.
 *
 * Custom Query Methods:
 * ---------------------
 * We can define custom methods using two approaches:
 *
 * 1. Method Name Query Derivation:
 *    Spring parses method name and generates SQL automatically
 *    Example: findByUserId(Long userId)
 *    Generated SQL: SELECT * FROM orders WHERE user_id = ?
 *
 * 2. @Query Annotation:
 *    We write JPQL (Java Persistence Query Language) explicitly
 *    More complex queries that can't be expressed in method names
 *    Example: @Query("SELECT o FROM Order o WHERE o.totalAmount > :amount")
 *
 * JpaRepository<Order, Long>:
 * - Order: Entity type this repository manages
 * - Long: Type of the entity's ID field
 */

// @Repository marks this as a Spring Data repository
// Spring will:
// 1. Create a proxy implementation at runtime
// 2. Register it as a Spring bean
// 3. Enable exception translation (SQLException → DataAccessException)
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find order by order number
     *
     * Order numbers are unique identifiers for customer-facing order tracking.
     * Example: ORD-20240115-001
     *
     * Method name breakdown:
     * - findBy: Query method prefix (tells Spring to generate SELECT query)
     * - OrderNumber: Property name in Order entity (orderNumber field)
     *
     * Generated SQL:
     * SELECT * FROM orders WHERE order_number = ?
     *
     * Return type Optional<Order>:
     * - Optional.of(order): If order found
     * - Optional.empty(): If order not found
     * - Null-safe: No NullPointerException
     *
     * Usage:
     * Optional<Order> order = orderRepository.findByOrderNumber("ORD-20240115-001");
     * if (order.isPresent()) {
     *     // Order found
     *     Order foundOrder = order.get();
     * } else {
     *     // Order not found
     *     throw new ResourceNotFoundException("Order not found");
     * }
     *
     * @param orderNumber The unique order number to search for
     * @return Optional containing Order if found, empty otherwise
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Find all orders for a specific user
     *
     * Returns all orders placed by a user, useful for order history.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - UserId: Property name (userId field in Order entity)
     *
     * Generated SQL:
     * SELECT * FROM orders WHERE user_id = ?
     *
     * Result ordering:
     * Orders are returned in no specific order.
     * For chronological order, use findByUserIdOrderByCreatedAtDesc()
     *
     * Usage:
     * List<Order> userOrders = orderRepository.findByUserId(123L);
     * System.out.println("User has " + userOrders.size() + " orders");
     *
     * @param userId The ID of the user whose orders to retrieve
     * @return List of orders (empty list if user has no orders)
     */
    List<Order> findByUserId(Long userId);

    /**
     * Find orders for a user, sorted by creation date (newest first)
     *
     * Same as findByUserId but with sorting applied.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - UserId: Filter by userId
     * - OrderBy: Add ORDER BY clause
     * - CreatedAt: Sort by createdAt field
     * - Desc: Descending order (newest first)
     *
     * Generated SQL:
     * SELECT * FROM orders
     * WHERE user_id = ?
     * ORDER BY created_at DESC
     *
     * Sorting options:
     * - Desc: Descending (newest first) - Most recent orders at top
     * - Asc: Ascending (oldest first) - Historical orders at top
     *
     * We use DESC because users typically want to see recent orders first.
     *
     * Usage:
     * List<Order> recentOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(123L);
     * // recentOrders[0] is the most recent order
     * // recentOrders[n-1] is the oldest order
     *
     * @param userId The ID of the user
     * @return List of orders sorted by creation date (newest first)
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find orders by status
     *
     * Retrieves all orders in a specific state.
     * Useful for administrative dashboards and reports.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - Status: Filter by status field (OrderStatus enum)
     *
     * Generated SQL:
     * SELECT * FROM orders WHERE status = ?
     *
     * Note: status is stored as String in database (EnumType.STRING)
     * Example: findByStatus(OrderStatus.PENDING) → WHERE status = 'PENDING'
     *
     * Common use cases:
     * - findByStatus(OrderStatus.PENDING): Orders awaiting payment
     * - findByStatus(OrderStatus.PROCESSING): Orders being prepared
     * - findByStatus(OrderStatus.SHIPPED): Orders in transit
     *
     * Usage:
     * List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
     * System.out.println("There are " + pendingOrders.size() + " pending orders");
     *
     * @param status The order status to filter by
     * @return List of orders with the specified status
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Find orders created between two dates
     *
     * Retrieves orders within a specific time range.
     * Useful for reporting and analytics.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - CreatedAt: Filter by createdAt field
     * - Between: SQL BETWEEN operator
     * - Requires two parameters: start and end dates
     *
     * Generated SQL:
     * SELECT * FROM orders
     * WHERE created_at BETWEEN ? AND ?
     *
     * BETWEEN operator:
     * - Inclusive of both start and end values
     * - Example: BETWEEN '2024-01-01' AND '2024-01-31'
     *   Includes both January 1st and January 31st
     *
     * Usage examples:
     *
     * 1. Orders from last 30 days:
     *    LocalDateTime end = LocalDateTime.now();
     *    LocalDateTime start = end.minusDays(30);
     *    List<Order> recentOrders = orderRepository.findByCreatedAtBetween(start, end);
     *
     * 2. Orders for a specific month (January 2024):
     *    LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
     *    LocalDateTime end = LocalDateTime.of(2024, 1, 31, 23, 59);
     *    List<Order> januaryOrders = orderRepository.findByCreatedAtBetween(start, end);
     *
     * 3. Orders for today:
     *    LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0);
     *    LocalDateTime end = LocalDateTime.now().withHour(23).withMinute(59);
     *    List<Order> todayOrders = orderRepository.findByCreatedAtBetween(start, end);
     *
     * @param startDate Start of the time range (inclusive)
     * @param endDate End of the time range (inclusive)
     * @return List of orders created within the date range
     */
    List<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Count orders for a specific user
     *
     * Returns the number of orders placed by a user.
     * More efficient than loading all orders and counting them.
     *
     * Method name breakdown:
     * - countBy: COUNT query (instead of SELECT)
     * - UserId: Filter by userId
     *
     * Generated SQL:
     * SELECT COUNT(*) FROM orders WHERE user_id = ?
     *
     * Why use count instead of findByUserId().size()?
     * ------------------------------------------------
     * Bad approach:
     *   List<Order> orders = orderRepository.findByUserId(userId);
     *   int count = orders.size();
     * Problems:
     *   - Loads ALL order data from database (memory intensive)
     *   - Slow for users with many orders
     *   - Unnecessary data transfer
     *
     * Good approach:
     *   long count = orderRepository.countByUserId(userId);
     * Benefits:
     *   - Only returns count (single number)
     *   - Fast (database does counting)
     *   - Minimal memory usage
     *   - Database optimized for COUNT queries
     *
     * Usage:
     * long userOrderCount = orderRepository.countByUserId(123L);
     * if (userOrderCount > 10) {
     *     // Loyal customer with 10+ orders
     *     applyDiscountForLoyalCustomer();
     * }
     *
     * @param userId The ID of the user
     * @return Number of orders for the user
     */
    long countByUserId(Long userId);

    /**
     * Check if an order number exists
     *
     * Quickly checks if an order number is already used.
     * Used for validation when generating order numbers.
     *
     * Method name breakdown:
     * - existsBy: EXISTS query (returns boolean)
     * - OrderNumber: Check by orderNumber field
     *
     * Generated SQL:
     * SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
     * FROM orders
     * WHERE order_number = ?
     *
     * Why use exists instead of findByOrderNumber()?
     * -----------------------------------------------
     * existsByOrderNumber:
     *   - Returns only boolean (true/false)
     *   - Fast (database stops at first match)
     *   - Minimal memory usage
     *
     * findByOrderNumber:
     *   - Loads entire Order object
     *   - Slower (retrieves all data)
     *   - More memory usage
     *
     * Use exists when you only need to know if something exists,
     * not the actual data.
     *
     * Usage:
     * String newOrderNumber = generateOrderNumber();
     * while (orderRepository.existsByOrderNumber(newOrderNumber)) {
     *     // Order number already exists, generate a new one
     *     newOrderNumber = generateOrderNumber();
     * }
     * // newOrderNumber is unique, safe to use
     *
     * @param orderNumber The order number to check
     * @return true if order number exists, false otherwise
     */
    boolean existsByOrderNumber(String orderNumber);

    /**
     * Find orders by user and status
     *
     * Retrieves orders for a specific user with a specific status.
     * Useful for filtering user's order history.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - UserId: Filter by userId
     * - And: Combine conditions with AND operator
     * - Status: Filter by status
     *
     * Generated SQL:
     * SELECT * FROM orders
     * WHERE user_id = ? AND status = ?
     *
     * Usage examples:
     *
     * 1. Show user's pending orders:
     *    List<Order> pending = orderRepository.findByUserIdAndStatus(
     *        userId, OrderStatus.PENDING
     *    );
     *
     * 2. Show user's delivered orders:
     *    List<Order> delivered = orderRepository.findByUserIdAndStatus(
     *        userId, OrderStatus.DELIVERED
     *    );
     *
     * 3. Show user's cancelled orders:
     *    List<Order> cancelled = orderRepository.findByUserIdAndStatus(
     *        userId, OrderStatus.CANCELLED
     *    );
     *
     * @param userId The ID of the user
     * @param status The order status to filter by
     * @return List of orders matching both criteria
     */
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

    /**
     * Find orders with total amount greater than specified value
     *
     * Custom JPQL query for finding high-value orders.
     *
     * @Query annotation:
     * - Allows writing custom JPQL (not SQL!)
     * - JPQL uses entity names and properties (not table/column names)
     * - More flexibility than method name queries
     *
     * JPQL vs SQL:
     * JPQL: SELECT o FROM Order o WHERE o.totalAmount > :amount
     * SQL:  SELECT * FROM orders WHERE total_amount > ?
     *
     * :amount is a named parameter (matches @Param("amount"))
     *
     * Why use JPQL?
     * - Database independent (works with MySQL, PostgreSQL, etc.)
     * - Type-safe (uses Java types, not SQL types)
     * - Object-oriented (works with entities, not tables)
     *
     * Usage:
     * BigDecimal threshold = new BigDecimal("1000.00");
     * List<Order> highValueOrders = orderRepository.findByTotalAmountGreaterThan(threshold);
     * // Returns all orders over $1000
     *
     * @param amount The minimum total amount
     * @return List of orders with total amount greater than specified value
     */
    @Query("SELECT o FROM Order o WHERE o.totalAmount > :amount")
    List<Order> findByTotalAmountGreaterThan(@Param("amount") java.math.BigDecimal amount);

    // =========================================================================
    // Additional Query Methods (Can be added as needed)
    // =========================================================================
    //
    // /**
    //  * Find recent orders (last N days)
    //  */
    // @Query("SELECT o FROM Order o WHERE o.createdAt >= :startDate")
    // List<Order> findRecentOrders(@Param("startDate") LocalDateTime startDate);
    //
    // /**
    //  * Find orders by user and date range
    //  */
    // List<Order> findByUserIdAndCreatedAtBetween(
    //     Long userId,
    //     LocalDateTime startDate,
    //     LocalDateTime endDate
    // );
    //
    // /**
    //  * Count orders by status
    //  */
    // long countByStatus(OrderStatus status);
    //
    // /**
    //  * Find orders with specific payment method
    //  */
    // List<Order> findByPaymentMethod(String paymentMethod);
    //
    // /**
    //  * Delete old cancelled orders (cleanup)
    //  */
    // @Modifying
    // @Query("DELETE FROM Order o WHERE o.status = :status AND o.createdAt < :date")
    // void deleteOldCancelledOrders(
    //     @Param("status") OrderStatus status,
    //     @Param("date") LocalDateTime date
    // );
    //
    // =========================================================================
    // Performance Considerations
    // =========================================================================
    //
    // 1. Indexes:
    //    - We defined indexes in Order entity
    //    - idx_user_id: Fast user order lookups
    //    - idx_order_number: Fast order number lookups
    //    - idx_status: Fast status filtering
    //    - idx_user_created: Fast user + date queries
    //
    // 2. Fetch strategy:
    //    - Order.items uses LAZY loading
    //    - Items only loaded when accessed
    //    - Prevents unnecessary queries
    //
    // 3. Query optimization:
    //    - Use count() instead of loading and counting
    //    - Use exists() instead of loading and checking
    //    - Use pagination for large result sets
    //
    // 4. Pagination example (for large datasets):
    //    Page<Order> findByUserId(Long userId, Pageable pageable);
    //    // Usage:
    //    Pageable pageable = PageRequest.of(0, 10); // Page 0, size 10
    //    Page<Order> firstPage = orderRepository.findByUserId(userId, pageable);
}
