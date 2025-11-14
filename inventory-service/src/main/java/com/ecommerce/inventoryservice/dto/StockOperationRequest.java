package com.ecommerce.inventoryservice.dto;

// Import validation annotations
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Import Lombok annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stock Operation Request DTO
 *
 * General-purpose DTO for stock operations.
 * Used for adding stock, reducing stock, and adjusting stock.
 *
 * API Endpoints:
 * ==============
 * POST /api/inventory/{productId}/add      - Add stock (PURCHASE, RETURN)
 * POST /api/inventory/{productId}/reduce   - Reduce stock (DAMAGE)
 * POST /api/inventory/{productId}/adjust   - Adjust stock (ADJUSTMENT)
 * POST /api/inventory/{productId}/commit   - Commit reservation (SALE)
 *
 * Request Body:
 * {
 *   "quantity": 100,
 *   "referenceId": "PO-2024-001",
 *   "notes": "Received shipment from Supplier XYZ"
 * }
 *
 * Operation Types:
 * ================
 *
 * 1. Add Stock (PURCHASE, RETURN):
 *    - New stock from supplier
 *    - Customer returns
 *    - Increases total and available
 *
 * 2. Reduce Stock (DAMAGE):
 *    - Damaged items
 *    - Lost items
 *    - Expired items
 *    - Decreases total and available
 *
 * 3. Adjust Stock (ADJUSTMENT):
 *    - Physical count correction
 *    - Data entry fix
 *    - Can increase or decrease
 *
 * 4. Commit Reservation (SALE):
 *    - Payment succeeded
 *    - Stock leaves warehouse
 *    - Decreases reserved and total
 *
 * Who can use this?
 * =================
 * - Add/Reduce/Adjust: Admin only (dangerous operations)
 * - Commit: Order/Payment Service (automated)
 *
 * Example Scenarios:
 * ==================
 *
 * New stock received:
 * -------------------
 * POST /api/inventory/123/add
 * {
 *   "quantity": 100,
 *   "referenceId": "PO-2024-001",
 *   "notes": "Received shipment from Supplier XYZ - Blue T-Shirts"
 * }
 * Result:
 * - Total: 100 → 200
 * - Available: 85 → 185
 * - Reserved: 15 (unchanged)
 *
 * Damaged items:
 * --------------
 * POST /api/inventory/123/reduce
 * {
 *   "quantity": 5,
 *   "referenceId": "INCIDENT-789",
 *   "notes": "Water damage in Warehouse A, Section B3"
 * }
 * Result:
 * - Total: 200 → 195
 * - Available: 185 → 180
 * - Reserved: 15 (unchanged)
 *
 * Physical count adjustment:
 * --------------------------
 * POST /api/inventory/123/adjust
 * {
 *   "quantity": 195,  // New total from physical count
 *   "referenceId": "AUDIT-2024-01",
 *   "notes": "Annual physical inventory count - found discrepancy"
 * }
 *
 * Payment succeeded (commit):
 * ---------------------------
 * POST /api/inventory/123/commit
 * {
 *   "quantity": 2,
 *   "referenceId": "ORDER-123",
 *   "notes": "Order shipped to customer"
 * }
 * Result:
 * - Total: 195 → 193 (stock left warehouse)
 * - Available: 180 (unchanged, was already reduced during reservation)
 * - Reserved: 15 → 13
 */
@Data               // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: no-arg constructor for Jackson deserialization
@AllArgsConstructor // Lombok: all-args constructor for testing
public class StockOperationRequest {

    /**
     * Quantity
     *
     * Amount to add, reduce, adjust, or commit.
     *
     * Validations:
     * - @NotNull: Required field
     * - @Min(1): Must be at least 1 for add/reduce/commit
     *
     * For adjust operation:
     * - This is the NEW total quantity (not change)
     * - Can be higher or lower than current
     * - Service calculates the difference
     *
     * Example: 100 (add 100 units, or new total is 100)
     *
     * Business validations (in service layer):
     * - Add: No limit (can add as much as needed)
     * - Reduce: Cannot exceed available quantity
     * - Adjust: Can be any positive number
     * - Commit: Cannot exceed reserved quantity
     */
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    /**
     * Reference ID
     *
     * External reference for this operation.
     * Links to source transaction/document.
     *
     * Validations:
     * - @NotBlank: Required and non-empty
     *
     * Examples by operation:
     * - Add: "PO-2024-001" (Purchase Order)
     * - Reduce: "INCIDENT-789" (Damage Report)
     * - Adjust: "AUDIT-2024-01" (Physical Count)
     * - Commit: "ORDER-123" (Order Number)
     * - Return: "RMA-456" (Return Authorization)
     *
     * Why required?
     * - Audit trail
     * - Traceability
     * - Compliance
     * - Debugging
     * - Financial reconciliation
     *
     * Example: "PO-2024-001"
     */
    @NotBlank(message = "Reference ID is required")
    private String referenceId;

    /**
     * Notes
     *
     * Human-readable explanation for this operation.
     * Provides context and details.
     *
     * Optional but recommended.
     *
     * Examples:
     * - "Received shipment from Supplier XYZ - 100 Blue T-Shirts size M"
     * - "Water damage in Warehouse A, Section B3 - 5 units damaged beyond repair"
     * - "Annual physical inventory count - system showed 200, actual count 195"
     * - "Order #123 shipped to customer John Doe"
     * - "Customer returned defective product - refurbished and added back to stock"
     *
     * Uses:
     * - Admin reference
     * - Customer support
     * - Audits
     * - Investigations
     * - Process improvement
     *
     * Example: "Received shipment from Supplier XYZ"
     *
     * Best practices:
     * - Be specific and detailed
     * - Include relevant context
     * - Mention responsible person if applicable
     * - Note any issues or anomalies
     */
    private String notes;

    /**
     * Example Usage
     * =============
     *
     * In Admin Panel (adding new stock):
     * -----------------------------------
     * @PostMapping("/admin/inventory/{productId}/add")
     * @PreAuthorize("hasRole('ADMIN')")
     * public InventoryResponse addStock(
     *     @PathVariable Long productId,
     *     @Valid @RequestBody StockOperationRequest request
     * ) {
     *     return inventoryService.addStock(productId, request);
     * }
     *
     * // Admin fills form:
     * // Quantity: 100
     * // Reference: PO-2024-001
     * // Notes: "Received shipment from Supplier XYZ - Blue T-Shirts size M"
     *
     * In Admin Panel (recording damage):
     * -----------------------------------
     * @PostMapping("/admin/inventory/{productId}/reduce")
     * @PreAuthorize("hasRole('ADMIN')")
     * public InventoryResponse reduceStock(
     *     @PathVariable Long productId,
     *     @Valid @RequestBody StockOperationRequest request
     * ) {
     *     return inventoryService.reduceStock(productId, request);
     * }
     *
     * // Warehouse manager reports damage:
     * // Quantity: 5
     * // Reference: INCIDENT-789
     * // Notes: "Forklift accident - damaged 5 units in pallet"
     *
     * In Payment Service (committing reservation):
     * ---------------------------------------------
     * @PostMapping("/webhook")
     * public void handlePaymentSuccess(PaymentWebhook webhook) {
     *     Payment payment = findPayment(webhook.getPaymentId());
     *
     *     if (webhook.getStatus().equals("succeeded")) {
     *         Order order = orderService.getOrder(payment.getOrderId());
     *
     *         for (OrderItem item : order.getItems()) {
     *             StockOperationRequest commitRequest = new StockOperationRequest();
     *             commitRequest.setQuantity(item.getQuantity());
     *             commitRequest.setReferenceId(order.getOrderNumber());
     *             commitRequest.setNotes("Payment succeeded - order shipped to " + order.getCustomerName());
     *
     *             inventoryService.commitReservation(item.getProductId(), commitRequest);
     *         }
     *     }
     * }
     *
     * In Admin Panel (physical count adjustment):
     * --------------------------------------------
     * @PostMapping("/admin/inventory/{productId}/adjust")
     * @PreAuthorize("hasRole('ADMIN')")
     * public InventoryResponse adjustStock(
     *     @PathVariable Long productId,
     *     @Valid @RequestBody StockOperationRequest request
     * ) {
     *     return inventoryService.adjustStock(productId, request);
     * }
     *
     * // After physical count:
     * // Current system: 200 units
     * // Actual count: 195 units
     * // Quantity: 195 (new total, not -5)
     * // Reference: AUDIT-2024-Q1
     * // Notes: "Q1 physical inventory - 5 unit discrepancy, cause unknown"
     *
     * Validation Examples:
     * --------------------
     * // Valid - Add stock
     * StockOperationRequest request = new StockOperationRequest(
     *     100,
     *     "PO-2024-001",
     *     "Received shipment from Supplier XYZ"
     * );
     * // ✓ Passes validation
     *
     * // Valid - Without notes
     * request = new StockOperationRequest(5, "INCIDENT-789", null);
     * // ✓ Notes are optional
     *
     * // Invalid - Zero quantity
     * request = new StockOperationRequest(0, "PO-001", "Test");
     * // Validation error: "Quantity must be at least 1"
     *
     * // Invalid - Empty reference
     * request = new StockOperationRequest(100, "", "Test");
     * // Validation error: "Reference ID is required"
     *
     * // Invalid - Null reference
     * request = new StockOperationRequest(100, null, "Test");
     * // Validation error: "Reference ID is required"
     */
}
