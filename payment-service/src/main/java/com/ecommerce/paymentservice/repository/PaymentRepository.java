package com.ecommerce.paymentservice.repository;

// Import the Payment entity
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentStatus;

// Import Spring Data JPA repository interface
import org.springframework.data.jpa.repository.JpaRepository;

// Import query annotation
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Import stereotype annotation
import org.springframework.stereotype.Repository;

// Import List and Optional for query results
import java.util.List;
import java.util.Optional;

/**
 * Payment Repository
 *
 * Data access layer for Payment entity.
 * Provides methods to interact with the payments table in the database.
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
 *    - findBy... → SELECT query with WHERE clause
 *    - countBy... → COUNT query
 *    - deleteBy... → DELETE query
 *
 * 2. Naming Convention:
 *    - findByOrderId → WHERE order_id = ?
 *    - findByUserId → WHERE user_id = ?
 *    - findByStatus → WHERE status = ?
 *    - findByUserIdAndStatus → WHERE user_id = ? AND status = ?
 *
 * 3. Automatic Implementation:
 *    - Spring generates implementation at runtime
 *    - No need to write SQL or implementation class
 *    - Type-safe (compiler checks parameter types)
 *
 * JpaRepository<Payment, Long>:
 * =============================
 * - Payment: Entity type this repository manages
 * - Long: Type of the entity's ID field
 *
 * Inherited methods from JpaRepository:
 * - save(Payment) - Insert or update
 * - findById(Long) - Find by primary key
 * - findAll() - Get all records
 * - deleteById(Long) - Delete by ID
 * - count() - Count all records
 * - existsById(Long) - Check if exists
 * And many more...
 *
 * When to use custom queries vs method names?
 * ============================================
 * Method names: Simple queries (findBy, countBy)
 * @Query: Complex queries, joins, aggregates
 *
 * Example:
 * Simple: findByOrderId(Long orderId)
 * Complex: @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED'")
 */

// @Repository marks this as a Spring Data repository
// Spring will create a bean implementing this interface
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by order ID
     *
     * Retrieves the payment associated with a specific order.
     * Typically, one order has one payment.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - OrderId: Filter by orderId field
     *
     * Generated SQL:
     * SELECT * FROM payments WHERE order_id = ?
     *
     * Return type Optional<Payment>:
     * - Optional means payment may or may not exist
     * - Safer than returning null
     * - Prevents NullPointerException
     *
     * Why Optional?
     * -------------
     * Instead of:
     * Payment payment = paymentRepository.findByOrderId(123L);
     * if (payment == null) {  // Easy to forget this check!
     *     throw new ResourceNotFoundException("Payment not found");
     * }
     *
     * With Optional:
     * Payment payment = paymentRepository.findByOrderId(123L)
     *     .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
     *
     * Use cases:
     * ----------
     * 1. Order Service checks payment status:
     *    - Order created, check if payment completed
     *    - Update order status based on payment status
     *
     * 2. Get payment details for an order:
     *    - Customer views order details
     *    - Show payment method and status
     *
     * 3. Refund processing:
     *    - Admin wants to refund order
     *    - Fetch payment by order ID
     *    - Process refund via payment gateway
     *
     * Example usage:
     * --------------
     * // In PaymentService
     * public PaymentResponse getPaymentByOrderId(Long orderId) {
     *     Payment payment = paymentRepository.findByOrderId(orderId)
     *         .orElseThrow(() -> new ResourceNotFoundException(
     *             "Payment not found for order: " + orderId
     *         ));
     *     return mapToResponse(payment);
     * }
     *
     * // Check if payment exists and is completed
     * Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);
     * if (paymentOpt.isPresent() && paymentOpt.get().getStatus() == PaymentStatus.COMPLETED) {
     *     // Order can be shipped
     * }
     *
     * @param orderId The ID of the order
     * @return Optional containing payment if found, empty otherwise
     */
    Optional<Payment> findByOrderId(Long orderId);

    /**
     * Find all payments by user ID
     *
     * Retrieves payment history for a specific user.
     * Returns all payments made by the user, sorted by most recent first.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - UserId: Filter by userId field
     * - OrderBy: Sort results
     * - CreatedAt: Sort by createdAt field
     * - Desc: Descending order (newest first)
     *
     * Generated SQL:
     * SELECT * FROM payments
     * WHERE user_id = ?
     * ORDER BY created_at DESC
     *
     * Return type List<Payment>:
     * - Returns empty list if no payments found (never null)
     * - List is mutable, can be modified
     *
     * Use cases:
     * ----------
     * 1. Payment history page:
     *    - User views "My Payments" page
     *    - Shows all past transactions
     *    - Most recent payments first
     *
     * 2. User analytics:
     *    - Total amount spent by user
     *    - Average transaction value
     *    - Payment method preferences
     *
     * 3. Fraud detection:
     *    - Check for unusual payment patterns
     *    - Multiple failed payments
     *    - Velocity checks (too many payments too fast)
     *
     * Example usage:
     * --------------
     * // Get user's payment history
     * List<Payment> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(456L);
     *
     * // Show in UI
     * for (Payment payment : payments) {
     *     System.out.println(payment.getCreatedAt() + " - $" + payment.getAmount()
     *         + " - " + payment.getStatus());
     * }
     * // Output:
     * // 2024-01-15 10:30 - $99.99 - COMPLETED
     * // 2024-01-10 14:20 - $49.99 - COMPLETED
     * // 2024-01-05 09:15 - $29.99 - REFUNDED
     *
     * // Calculate total spent
     * BigDecimal totalSpent = payments.stream()
     *     .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
     *     .map(Payment::getAmount)
     *     .reduce(BigDecimal.ZERO, BigDecimal::add);
     *
     * @param userId The ID of the user
     * @return List of payments by this user, newest first (empty if none found)
     */
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find all payments by status
     *
     * Retrieves all payments with a specific status.
     * Useful for admin operations and reporting.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - Status: Filter by status field
     *
     * Generated SQL:
     * SELECT * FROM payments WHERE status = ?
     *
     * Use cases:
     * ----------
     * 1. Admin dashboard:
     *    - View all pending payments
     *    - Monitor failed payments
     *    - Track completed payments
     *
     * 2. Batch processing:
     *    - Retry failed payments
     *    - Cancel stuck pending payments
     *    - Generate reports
     *
     * 3. Reconciliation:
     *    - Match completed payments with bank deposits
     *    - Verify all orders have payments
     *
     * Example usage:
     * --------------
     * // Get all failed payments for investigation
     * List<Payment> failedPayments = paymentRepository.findByStatus(PaymentStatus.FAILED);
     * for (Payment payment : failedPayments) {
     *     System.out.println("Order " + payment.getOrderId()
     *         + " failed: " + payment.getFailureReason());
     * }
     *
     * // Get all pending payments (might be stuck)
     * List<Payment> pendingPayments = paymentRepository.findByStatus(PaymentStatus.PENDING);
     * for (Payment payment : pendingPayments) {
     *     long minutesSincCreation = ChronoUnit.MINUTES.between(
     *         payment.getCreatedAt(), LocalDateTime.now()
     *     );
     *     if (minutesSincCreation > 30) {
     *         // Payment stuck for > 30 minutes, needs investigation
     *         payment.setStatus(PaymentStatus.FAILED);
     *         payment.setFailureReason("Payment timeout");
     *     }
     * }
     *
     * @param status The payment status to filter by
     * @return List of payments with this status (empty if none found)
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Find payment by transaction ID
     *
     * Retrieves payment using the transaction ID from payment gateway.
     * Transaction ID is unique per payment gateway.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - TransactionId: Filter by transactionId field
     *
     * Generated SQL:
     * SELECT * FROM payments WHERE transaction_id = ?
     *
     * Return type Optional<Payment>:
     * - Transaction ID may not exist (null for pending payments)
     * - Optional prevents null pointer issues
     *
     * Use cases:
     * ----------
     * 1. Webhook processing:
     *    - Payment gateway sends webhook notification
     *    - Includes transaction ID
     *    - Lookup payment and update status
     *
     * 2. Refund processing:
     *    - Need original transaction to process refund
     *    - Fetch by transaction ID
     *    - Call gateway refund API
     *
     * 3. Dispute resolution:
     *    - Customer disputes charge
     *    - Gateway provides transaction ID
     *    - Lookup payment in system
     *
     * 4. Reconciliation:
     *    - Match gateway transactions with system records
     *    - Download gateway report
     *    - Lookup each transaction ID
     *
     * Example usage:
     * --------------
     * // Process webhook from Stripe
     * @PostMapping("/webhook")
     * public void handleWebhook(@RequestBody StripeEvent event) {
     *     String transactionId = event.getData().getObject().getId();
     *     Payment payment = paymentRepository.findByTransactionId(transactionId)
     *         .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
     *
     *     if (event.getType().equals("charge.succeeded")) {
     *         payment.setStatus(PaymentStatus.COMPLETED);
     *     } else if (event.getType().equals("charge.failed")) {
     *         payment.setStatus(PaymentStatus.FAILED);
     *     }
     *     paymentRepository.save(payment);
     * }
     *
     * @param transactionId The transaction ID from payment gateway
     * @return Optional containing payment if found, empty otherwise
     */
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * Check if payment exists for an order
     *
     * Checks if an order already has a payment record.
     * Prevents duplicate payments for the same order.
     *
     * Method name breakdown:
     * - existsBy: Boolean query (true/false)
     * - OrderId: Check orderId field
     *
     * Generated SQL:
     * SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
     * FROM payments WHERE order_id = ?
     *
     * Return type boolean:
     * - true if payment exists
     * - false if no payment found
     *
     * Why this is important:
     * ----------------------
     * Prevents duplicate charges if:
     * - User clicks "Pay" button multiple times
     * - Network timeout causes retry
     * - Browser back button confusion
     *
     * Use cases:
     * ----------
     * 1. Prevent duplicate payments:
     *    - Before creating payment, check if exists
     *    - If exists, return existing payment
     *    - If not, create new payment
     *
     * 2. Validation:
     *    - Order Service checks if order has payment
     *    - Before allowing cancellation
     *    - Before creating another payment
     *
     * Example usage:
     * --------------
     * // In PaymentService.processPayment()
     * public PaymentResponse processPayment(ProcessPaymentRequest request) {
     *     // Check if payment already exists for this order
     *     if (paymentRepository.existsByOrderId(request.getOrderId())) {
     *         // Payment already exists, don't create duplicate
     *         Payment existingPayment = paymentRepository.findByOrderId(request.getOrderId()).get();
     *         throw new DuplicateResourceException(
     *             "Payment already exists for order " + request.getOrderId()
     *             + " with status: " + existingPayment.getStatus()
     *         );
     *     }
     *
     *     // Safe to create new payment
     *     Payment payment = new Payment();
     *     // ... create payment
     * }
     *
     * @param orderId The ID of the order
     * @return true if payment exists, false otherwise
     */
    boolean existsByOrderId(Long orderId);

    /**
     * Find payments by user ID and status
     *
     * Retrieves payments for a specific user with a specific status.
     * Combines user and status filtering.
     *
     * Method name breakdown:
     * - findBy: SELECT query
     * - UserId: Filter by userId field
     * - And: Logical AND
     * - Status: Filter by status field
     * - OrderBy: Sort results
     * - CreatedAt: Sort by createdAt field
     * - Desc: Descending order
     *
     * Generated SQL:
     * SELECT * FROM payments
     * WHERE user_id = ? AND status = ?
     * ORDER BY created_at DESC
     *
     * Use cases:
     * ----------
     * 1. User's failed payments:
     *    - Show user their failed transactions
     *    - Let them retry with different payment method
     *
     * 2. User's completed payments:
     *    - Show successful transaction history
     *    - Generate receipts
     *
     * 3. User's pending payments:
     *    - Show awaiting confirmation
     *    - Allow cancellation
     *
     * Example usage:
     * --------------
     * // Get user's failed payments
     * List<Payment> failedPayments = paymentRepository
     *     .findByUserIdAndStatusOrderByCreatedAtDesc(456L, PaymentStatus.FAILED);
     *
     * // Show to user: "You have X failed payments"
     * if (!failedPayments.isEmpty()) {
     *     System.out.println("You have " + failedPayments.size() + " failed payments:");
     *     for (Payment payment : failedPayments) {
     *         System.out.println("Order #" + payment.getOrderId()
     *             + " - " + payment.getFailureReason());
     *     }
     * }
     *
     * @param userId The ID of the user
     * @param status The payment status to filter by
     * @return List of user's payments with this status, newest first
     */
    List<Payment> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, PaymentStatus status);

    /**
     * Count payments by status
     *
     * Returns the number of payments with a specific status.
     * Useful for dashboards and reports.
     *
     * Method name breakdown:
     * - countBy: COUNT query
     * - Status: Filter by status field
     *
     * Generated SQL:
     * SELECT COUNT(*) FROM payments WHERE status = ?
     *
     * Return type long:
     * - Number of payments (0 if none found)
     * - Never null
     *
     * Use cases:
     * ----------
     * 1. Admin dashboard metrics:
     *    - Total completed payments today
     *    - Number of failed payments
     *    - Pending payments needing attention
     *
     * 2. Success rate calculation:
     *    - completedCount / (completedCount + failedCount)
     *    - Track payment success over time
     *
     * 3. Alerts:
     *    - Alert if failed payments exceed threshold
     *    - Alert if too many pending payments
     *
     * Example usage:
     * --------------
     * // Dashboard statistics
     * long completedCount = paymentRepository.countByStatus(PaymentStatus.COMPLETED);
     * long failedCount = paymentRepository.countByStatus(PaymentStatus.FAILED);
     * long pendingCount = paymentRepository.countByStatus(PaymentStatus.PENDING);
     *
     * double successRate = (double) completedCount / (completedCount + failedCount) * 100;
     *
     * System.out.println("Payment Statistics:");
     * System.out.println("Completed: " + completedCount);
     * System.out.println("Failed: " + failedCount);
     * System.out.println("Pending: " + pendingCount);
     * System.out.println("Success Rate: " + successRate + "%");
     *
     * // Alert if too many failures
     * if (failedCount > 100) {
     *     sendAlert("High number of failed payments: " + failedCount);
     * }
     *
     * @param status The payment status to count
     * @return Number of payments with this status
     */
    long countByStatus(PaymentStatus status);

    /**
     * Calculate total amount for completed payments
     *
     * Custom JPQL query that sums the amount of all completed payments.
     * Useful for revenue reporting.
     *
     * @Query annotation:
     * - Custom JPQL (Java Persistence Query Language)
     * - SUM(p.amount): Aggregate function
     * - FROM Payment p: Alias 'p' for Payment entity
     * - WHERE p.status = :status: Filter by status parameter
     *
     * Generated SQL:
     * SELECT SUM(amount) FROM payments WHERE status = ?
     *
     * Return type BigDecimal:
     * - Sum can be null if no payments found
     * - Service should handle null (default to zero)
     *
     * Use cases:
     * ----------
     * 1. Revenue reporting:
     *    - Total revenue for the day
     *    - Total revenue for the month
     *    - Total refunded amount
     *
     * 2. Financial reconciliation:
     *    - Compare with bank deposits
     *    - Verify all payments accounted for
     *
     * 3. Analytics:
     *    - Revenue trends over time
     *    - Average transaction value
     *
     * Example usage:
     * --------------
     * // Calculate total revenue
     * BigDecimal totalRevenue = paymentRepository
     *     .getTotalAmountByStatus(PaymentStatus.COMPLETED);
     * if (totalRevenue == null) {
     *     totalRevenue = BigDecimal.ZERO;
     * }
     * System.out.println("Total Revenue: $" + totalRevenue);
     *
     * // Calculate refunded amount
     * BigDecimal refundedAmount = paymentRepository
     *     .getTotalAmountByStatus(PaymentStatus.REFUNDED);
     * if (refundedAmount == null) {
     *     refundedAmount = BigDecimal.ZERO;
     * }
     *
     * // Net revenue = completed - refunded
     * BigDecimal netRevenue = totalRevenue.subtract(refundedAmount);
     * System.out.println("Net Revenue: $" + netRevenue);
     *
     * @param status The payment status to sum
     * @return Total amount for this status (null if no payments)
     */
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = :status")
    java.math.BigDecimal getTotalAmountByStatus(@Param("status") PaymentStatus status);

    // =========================================================================
    // Additional Query Methods (Can be added as needed)
    // =========================================================================
    //
    // /**
    //  * Find payments by payment method
    //  */
    // List<Payment> findByPaymentMethod(PaymentMethod paymentMethod);
    //
    // /**
    //  * Find payments created within a date range
    //  */
    // List<Payment> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    //
    // /**
    //  * Find payments for a user within a date range
    //  */
    // @Query("SELECT p FROM Payment p WHERE p.userId = :userId " +
    //        "AND p.createdAt BETWEEN :start AND :end " +
    //        "ORDER BY p.createdAt DESC")
    // List<Payment> findUserPaymentsInDateRange(
    //     @Param("userId") Long userId,
    //     @Param("start") LocalDateTime start,
    //     @Param("end") LocalDateTime end
    // );
    //
    // /**
    //  * Find high-value payments (amount greater than threshold)
    //  */
    // @Query("SELECT p FROM Payment p WHERE p.amount > :threshold " +
    //        "AND p.status = :status ORDER BY p.amount DESC")
    // List<Payment> findHighValuePayments(
    //     @Param("threshold") BigDecimal threshold,
    //     @Param("status") PaymentStatus status
    // );
    //
    // /**
    //  * Count payments by user and status
    //  */
    // long countByUserIdAndStatus(Long userId, PaymentStatus status);
    //
    // =========================================================================
    // Performance Notes
    // =========================================================================
    //
    // 1. Indexes:
    //    - idx_order_id: Fast lookup by order
    //    - idx_user_id: Fast user payment history
    //    - idx_status: Fast status filtering
    //    - idx_transaction_id: Fast webhook processing
    //
    // 2. Query Optimization:
    //    - Use existsBy instead of findBy when only checking presence
    //    - Use COUNT queries for statistics (faster than loading all records)
    //    - Use aggregate functions (SUM, AVG) in database, not Java
    //
    // 3. Avoiding N+1 Queries:
    //    - No relationships in Payment entity (orderId/userId are IDs only)
    //    - If relationships existed, use @Query with JOIN FETCH
    //
    // 4. Pagination:
    //    For large datasets, use pagination:
    //    Page<Payment> findByUserId(Long userId, Pageable pageable);
    //    // Usage: findByUserId(123L, PageRequest.of(0, 20))
}
