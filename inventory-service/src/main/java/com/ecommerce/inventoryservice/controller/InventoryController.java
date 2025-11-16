package com.ecommerce.inventoryservice.controller;

// Import service layer
import com.ecommerce.inventoryservice.service.InventoryService;

// Import DTOs
import com.ecommerce.inventoryservice.dto.*;

// Import Spring Web annotations
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Import validation
import jakarta.validation.Valid;

// Import Lombok for logging
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Import List for collections
import java.util.List;

/**
 * Inventory Controller
 *
 * REST API controller for inventory operations.
 * Handles HTTP requests for stock tracking, reservations, and movements.
 *
 * Base URL: /api/inventory
 *
 * Available Endpoints:
 * ====================
 * 1. GET    /api/inventory/{productId}                - Get inventory by product ID
 * 2. POST   /api/inventory                            - Create inventory (admin)
 * 3. POST   /api/inventory/{productId}/reserve        - Reserve stock
 * 4. POST   /api/inventory/{productId}/release        - Release reservation
 * 5. POST   /api/inventory/{productId}/commit         - Commit reservation
 * 6. POST   /api/inventory/{productId}/add            - Add stock (admin)
 * 7. POST   /api/inventory/{productId}/reduce         - Reduce stock (admin)
 * 8. POST   /api/inventory/{productId}/adjust         - Adjust stock (admin)
 * 9. GET    /api/inventory/low-stock                  - Get low stock items (admin)
 * 10. GET   /api/inventory/out-of-stock               - Get out of stock items (admin)
 * 11. GET   /api/inventory/movements/{productId}      - Get movement history
 * 12. GET   /api/inventory/check/{productId}          - Check availability
 *
 * Who calls these endpoints?
 * ==========================
 * 1. Product Service:
 *    - Creates inventory when new product added
 *    - Gets stock level for product display
 *
 * 2. Order Service:
 *    - Checks availability before creating order
 *    - Reserves stock when order placed
 *    - Commits when payment succeeds
 *    - Releases when payment fails
 *
 * 3. Frontend:
 *    - Shows "X in stock" on product page
 *    - Enables/disables "Add to Cart" button
 *
 * 4. Admin Panel:
 *    - Views inventory levels
 *    - Adds/reduces/adjusts stock
 *    - Monitors low stock items
 *    - Views movement history
 *
 * Security:
 * =========
 * All endpoints require JWT authentication.
 * Some endpoints have role-based access control:
 * - Regular users/services: Can view and reserve stock
 * - Admins: Can add/reduce/adjust stock
 *
 * HTTP Status Codes:
 * ==================
 * 200 OK: Request successful
 * 201 CREATED: Inventory created successfully
 * 400 BAD REQUEST: Invalid request or insufficient stock
 * 401 UNAUTHORIZED: Not authenticated
 * 403 FORBIDDEN: Not authorized (requires admin)
 * 404 NOT FOUND: Inventory not found
 * 409 CONFLICT: Duplicate inventory
 * 500 INTERNAL SERVER ERROR: Server error
 */

// @RestController marks this as a REST API controller
@RestController

// @RequestMapping sets base path for all endpoints
@RequestMapping("/api/inventory")

// @RequiredArgsConstructor generates constructor for dependency injection
@RequiredArgsConstructor

// @Slf4j generates logger
@Slf4j
public class InventoryController {

    /**
     * Inventory Service
     *
     * Injected via constructor
     */
    private final InventoryService inventoryService;

    /**
     * Get Inventory by Product ID
     *
     * Endpoint: GET /api/inventory/{productId}
     * Description: Retrieve current inventory for a product
     *
     * Who calls this?
     * ---------------
     * - Product Service: Display stock on product page
     * - Frontend: Show "X in stock"
     * - Order Service: Pre-check before order
     *
     * URL Example:
     * GET /api/inventory/123
     *
     * Success Response (200 OK):
     * {
     *   "id": 1,
     *   "productId": 123,
     *   "availableQuantity": 85,
     *   "reservedQuantity": 15,
     *   "totalQuantity": 100,
     *   "lowStockThreshold": 10,
     *   "isLowStock": false,
     *   "createdAt": "2024-01-01T10:00:00",
     *   "updatedAt": "2024-01-15T14:30:00"
     * }
     *
     * Error Response:
     * - 404 NOT FOUND: Inventory doesn't exist for this product
     *
     * @param productId Product ID from URL path
     * @return ResponseEntity<InventoryResponse> with inventory details
     */
    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable Long productId) {
        // Log the request
        log.info("Getting inventory for product: {}", productId);

        // Get inventory from service
        InventoryResponse response = inventoryService.getInventory(productId);

        // Return inventory with 200 OK
        return ResponseEntity.ok(response);
    }

    /**
     * Create Inventory
     *
     * Endpoint: POST /api/inventory
     * Description: Create initial inventory for a new product
     *
     * Who calls this?
     * ---------------
     * - Product Service: When new product is added
     * - Admin: Manual inventory creation
     *
     * Security: Admin only
     *
     * Request Body:
     * {
     *   "productId": 123,
     *   "initialQuantity": 100,
     *   "lowStockThreshold": 10
     * }
     *
     * Success Response (201 CREATED):
     * {
     *   "id": 1,
     *   "productId": 123,
     *   "availableQuantity": 100,
     *   "reservedQuantity": 0,
     *   "totalQuantity": 100,
     *   "lowStockThreshold": 10,
     *   "isLowStock": false,
     *   ...
     * }
     *
     * Error Responses:
     * - 409 CONFLICT: Inventory already exists for this product
     *
     * @param productId Product ID
     * @param initialQuantity Initial stock quantity
     * @param lowStockThreshold Low stock alert threshold (optional, default 10)
     * @return ResponseEntity<InventoryResponse> with created inventory
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryResponse> createInventory(
            @RequestParam Long productId,
            @RequestParam Integer initialQuantity,
            @RequestParam(required = false) Integer lowStockThreshold
    ) {
        // Log the creation request
        log.info("Creating inventory for product: {} - initial: {}, threshold: {}",
                productId, initialQuantity, lowStockThreshold);

        // Create inventory via service
        InventoryResponse response = inventoryService.createInventory(
                productId,
                initialQuantity,
                lowStockThreshold
        );

        // Return created inventory with 201 CREATED
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Reserve Stock
     *
     * Endpoint: POST /api/inventory/{productId}/reserve
     * Description: Reserve stock for a pending order
     *
     * Who calls this?
     * ---------------
     * - Order Service: When customer places order
     *
     * URL Example:
     * POST /api/inventory/123/reserve
     *
     * Request Body:
     * {
     *   "quantity": 2,
     *   "referenceId": "ORDER-123"
     * }
     *
     * Success Response (200 OK):
     * {
     *   "id": 1,
     *   "productId": 123,
     *   "availableQuantity": 98,  // Was 100, now 98
     *   "reservedQuantity": 2,     // Was 0, now 2
     *   "totalQuantity": 100,      // Unchanged
     *   ...
     * }
     *
     * Error Responses:
     * - 400 BAD REQUEST: Insufficient stock
     * - 404 NOT FOUND: Inventory doesn't exist
     *
     * @param productId Product ID from URL
     * @param request Reserve stock request
     * @return ResponseEntity<InventoryResponse> with updated inventory
     */
    @PostMapping("/{productId}/reserve")
    public ResponseEntity<InventoryResponse> reserveStock(
            @PathVariable Long productId,
            @Valid @RequestBody ReserveStockRequest request
    ) {
        // Log the reservation request
        log.info("Reserving stock for product: {} - quantity: {}, reference: {}",
                productId, request.getQuantity(), request.getReferenceId());

        // Reserve stock via service
        InventoryResponse response = inventoryService.reserveStock(productId, request);

        // Return updated inventory with 200 OK
        return ResponseEntity.ok(response);
    }

    /**
     * Release Reservation
     *
     * Endpoint: POST /api/inventory/{productId}/release
     * Description: Release previously reserved stock
     *
     * Who calls this?
     * ---------------
     * - Payment Service: When payment fails
     * - Order Service: When order is cancelled
     *
     * URL Example:
     * POST /api/inventory/123/release
     *
     * Request Body:
     * {
     *   "quantity": 2,
     *   "referenceId": "ORDER-123",
     *   "reason": "Payment failed - card declined"
     * }
     *
     * Success Response (200 OK):
     * {
     *   "id": 1,
     *   "productId": 123,
     *   "availableQuantity": 100,  // Was 98, back to 100
     *   "reservedQuantity": 0,     // Was 2, back to 0
     *   "totalQuantity": 100,      // Unchanged
     *   ...
     * }
     *
     * Error Responses:
     * - 400 BAD REQUEST: Releasing more than reserved
     * - 404 NOT FOUND: Inventory doesn't exist
     *
     * @param productId Product ID from URL
     * @param request Release stock request
     * @return ResponseEntity<InventoryResponse> with updated inventory
     */
    @PostMapping("/{productId}/release")
    public ResponseEntity<InventoryResponse> releaseReservation(
            @PathVariable Long productId,
            @Valid @RequestBody ReleaseStockRequest request
    ) {
        // Log the release request
        log.info("Releasing reservation for product: {} - quantity: {}, reference: {}",
                productId, request.getQuantity(), request.getReferenceId());

        // Release reservation via service
        InventoryResponse response = inventoryService.releaseReservation(productId, request);

        // Return updated inventory with 200 OK
        return ResponseEntity.ok(response);
    }

    /**
     * Commit Reservation
     *
     * Endpoint: POST /api/inventory/{productId}/commit
     * Description: Commit reservation when payment succeeds
     *
     * Who calls this?
     * ---------------
     * - Payment Service: When payment succeeds
     * - Order Service: When order is confirmed
     *
     * URL Example:
     * POST /api/inventory/123/commit
     *
     * Request Body:
     * {
     *   "quantity": 2,
     *   "referenceId": "ORDER-123",
     *   "notes": "Order shipped to customer"
     * }
     *
     * Success Response (200 OK):
     * {
     *   "id": 1,
     *   "productId": 123,
     *   "availableQuantity": 98,   // Unchanged (was already reduced during reservation)
     *   "reservedQuantity": 0,     // Was 2, now 0
     *   "totalQuantity": 98,       // Was 100, now 98 (stock left warehouse)
     *   ...
     * }
     *
     * Error Responses:
     * - 400 BAD REQUEST: Committing more than reserved
     * - 404 NOT FOUND: Inventory doesn't exist
     *
     * @param productId Product ID from URL
     * @param request Stock operation request
     * @return ResponseEntity<InventoryResponse> with updated inventory
     */
    @PostMapping("/{productId}/commit")
    public ResponseEntity<InventoryResponse> commitReservation(
            @PathVariable Long productId,
            @Valid @RequestBody StockOperationRequest request
    ) {
        // Log the commit request
        log.info("Committing reservation for product: {} - quantity: {}, reference: {}",
                productId, request.getQuantity(), request.getReferenceId());

        // Commit reservation via service
        InventoryResponse response = inventoryService.commitReservation(productId, request);

        // Return updated inventory with 200 OK
        return ResponseEntity.ok(response);
    }

    /**
     * Add Stock
     *
     * Endpoint: POST /api/inventory/{productId}/add
     * Description: Add stock to inventory (PURCHASE, RETURN)
     *
     * Who calls this?
     * ---------------
     * - Admin: Manual stock addition
     * - Automated: When shipment received
     *
     * Security: Admin only
     *
     * URL Example:
     * POST /api/inventory/123/add
     *
     * Request Body:
     * {
     *   "quantity": 100,
     *   "referenceId": "PO-2024-001",
     *   "notes": "Received shipment from Supplier XYZ"
     * }
     *
     * Success Response (200 OK):
     * {
     *   "id": 1,
     *   "productId": 123,
     *   "availableQuantity": 198,  // Was 98, now 198
     *   "reservedQuantity": 0,     // Unchanged
     *   "totalQuantity": 198,      // Was 98, now 198
     *   ...
     * }
     *
     * @param productId Product ID from URL
     * @param request Stock operation request
     * @return ResponseEntity<InventoryResponse> with updated inventory
     */
    @PostMapping("/{productId}/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryResponse> addStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockOperationRequest request
    ) {
        // Log the add stock request
        log.info("Admin adding stock for product: {} - quantity: {}, reference: {}",
                productId, request.getQuantity(), request.getReferenceId());

        // Add stock via service
        InventoryResponse response = inventoryService.addStock(productId, request);

        // Return updated inventory with 200 OK
        return ResponseEntity.ok(response);
    }

    /**
     * Reduce Stock
     *
     * Endpoint: POST /api/inventory/{productId}/reduce
     * Description: Reduce stock from inventory (DAMAGE)
     *
     * Who calls this?
     * ---------------
     * - Admin: Manual stock reduction
     * - Automated: When damage reported
     *
     * Security: Admin only
     *
     * URL Example:
     * POST /api/inventory/123/reduce
     *
     * Request Body:
     * {
     *   "quantity": 5,
     *   "referenceId": "INCIDENT-789",
     *   "notes": "Water damage in warehouse"
     * }
     *
     * Success Response (200 OK):
     * {
     *   "id": 1,
     *   "productId": 123,
     *   "availableQuantity": 193,  // Was 198, now 193
     *   "reservedQuantity": 0,     // Unchanged
     *   "totalQuantity": 193,      // Was 198, now 193
     *   ...
     * }
     *
     * Error Responses:
     * - 400 BAD REQUEST: Reducing more than available
     *
     * @param productId Product ID from URL
     * @param request Stock operation request
     * @return ResponseEntity<InventoryResponse> with updated inventory
     */
    @PostMapping("/{productId}/reduce")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryResponse> reduceStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockOperationRequest request
    ) {
        // Log the reduce stock request
        log.info("Admin reducing stock for product: {} - quantity: {}, reference: {}",
                productId, request.getQuantity(), request.getReferenceId());

        // Reduce stock via service
        InventoryResponse response = inventoryService.reduceStock(productId, request);

        // Return updated inventory with 200 OK
        return ResponseEntity.ok(response);
    }

    /**
     * Adjust Stock
     *
     * Endpoint: POST /api/inventory/{productId}/adjust
     * Description: Manually adjust stock to match physical count
     *
     * Who calls this?
     * ---------------
     * - Admin: After physical inventory count
     *
     * Security: Admin only
     *
     * URL Example:
     * POST /api/inventory/123/adjust
     *
     * Request Body:
     * {
     *   "quantity": 195,  // NEW total (not change)
     *   "referenceId": "AUDIT-2024-Q1",
     *   "notes": "Physical count - found discrepancy"
     * }
     *
     * Success Response (200 OK):
     * {
     *   "id": 1,
     *   "productId": 123,
     *   "availableQuantity": 195,  // Adjusted to match physical count
     *   "reservedQuantity": 0,
     *   "totalQuantity": 195,      // NEW total from physical count
     *   ...
     * }
     *
     * @param productId Product ID from URL
     * @param request Stock operation request
     * @return ResponseEntity<InventoryResponse> with updated inventory
     */
    @PostMapping("/{productId}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryResponse> adjustStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockOperationRequest request
    ) {
        // Log the adjust stock request
        log.info("Admin adjusting stock for product: {} - new total: {}, reference: {}",
                productId, request.getQuantity(), request.getReferenceId());

        // Adjust stock via service
        InventoryResponse response = inventoryService.adjustStock(productId, request);

        // Return updated inventory with 200 OK
        return ResponseEntity.ok(response);
    }

    /**
     * Get Low Stock Items
     *
     * Endpoint: GET /api/inventory/low-stock
     * Description: Retrieve all products with low stock
     *
     * Who calls this?
     * ---------------
     * - Admin Panel: Dashboard alert
     * - Reports: Reordering report
     *
     * Security: Admin only
     *
     * URL Example:
     * GET /api/inventory/low-stock
     *
     * Success Response (200 OK):
     * [
     *   {
     *     "id": 1,
     *     "productId": 123,
     *     "availableQuantity": 5,
     *     "lowStockThreshold": 10,
     *     "isLowStock": true,
     *     ...
     *   },
     *   {
     *     "id": 2,
     *     "productId": 456,
     *     "availableQuantity": 8,
     *     "lowStockThreshold": 20,
     *     "isLowStock": true,
     *     ...
     *   }
     * ]
     *
     * Returns empty array if no low stock items.
     *
     * @return ResponseEntity<List<InventoryResponse>> with low stock items
     */
    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InventoryResponse>> getLowStockItems() {
        // Log the request
        log.info("Admin fetching low stock items");

        // Get low stock items from service
        List<InventoryResponse> items = inventoryService.getLowStockItems();

        // Return items with 200 OK
        return ResponseEntity.ok(items);
    }

    /**
     * Get Out of Stock Items
     *
     * Endpoint: GET /api/inventory/out-of-stock
     * Description: Retrieve all products with zero availability
     *
     * Who calls this?
     * ---------------
     * - Admin Panel: Urgent restock list
     * - Reports: Stockout analysis
     *
     * Security: Admin only
     *
     * URL Example:
     * GET /api/inventory/out-of-stock
     *
     * Success Response (200 OK):
     * [
     *   {
     *     "id": 1,
     *     "productId": 789,
     *     "availableQuantity": 0,
     *     "isLowStock": true,
     *     ...
     *   }
     * ]
     *
     * @return ResponseEntity<List<InventoryResponse>> with out of stock items
     */
    @GetMapping("/out-of-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InventoryResponse>> getOutOfStockItems() {
        // Log the request
        log.info("Admin fetching out of stock items");

        // Get out of stock items from service
        List<InventoryResponse> items = inventoryService.getOutOfStockItems();

        // Return items with 200 OK
        return ResponseEntity.ok(items);
    }

    /**
     * Get Movement History
     *
     * Endpoint: GET /api/inventory/movements/{productId}
     * Description: Retrieve complete stock movement history for a product
     *
     * Who calls this?
     * ---------------
     * - Admin Panel: View change history
     * - Reports: Movement analysis
     * - Audit: Compliance verification
     *
     * URL Example:
     * GET /api/inventory/movements/123
     *
     * Success Response (200 OK):
     * [
     *   {
     *     "id": 1,
     *     "productId": 123,
     *     "movementType": "SALE",
     *     "quantityChange": -2,
     *     "quantityBefore": 100,
     *     "quantityAfter": 98,
     *     "referenceId": "ORDER-123",
     *     "notes": "Sold to customer",
     *     "createdAt": "2024-01-15T14:30:00"
     *   },
     *   {
     *     "id": 2,
     *     "productId": 123,
     *     "movementType": "PURCHASE",
     *     "quantityChange": 100,
     *     "quantityBefore": 0,
     *     "quantityAfter": 100,
     *     "referenceId": "PO-2024-001",
     *     "notes": "Initial stock",
     *     "createdAt": "2024-01-01T10:00:00"
     *   }
     * ]
     *
     * Sorted by date (newest first).
     *
     * @param productId Product ID from URL
     * @return ResponseEntity<List<StockMovementResponse>> with movement history
     */
    @GetMapping("/movements/{productId}")
    public ResponseEntity<List<StockMovementResponse>> getMovementHistory(
            @PathVariable Long productId
    ) {
        // Log the request
        log.info("Getting movement history for product: {}", productId);

        // Get movement history from service
        List<StockMovementResponse> movements =
                inventoryService.getMovementHistory(productId);

        // Return movements with 200 OK
        return ResponseEntity.ok(movements);
    }

    /**
     * Check Availability
     *
     * Endpoint: GET /api/inventory/check/{productId}
     * Description: Quick check if sufficient stock is available
     *
     * Who calls this?
     * ---------------
     * - Order Service: Before creating order
     * - Frontend: Enable/disable "Add to Cart" button
     *
     * URL Example:
     * GET /api/inventory/check/123?quantity=5
     *
     * Success Response (200 OK):
     * {
     *   "available": true
     * }
     *
     * Or if insufficient:
     * {
     *   "available": false
     * }
     *
     * @param productId Product ID from URL
     * @param quantity Quantity to check
     * @return ResponseEntity with availability status
     */
    @GetMapping("/check/{productId}")
    public ResponseEntity<Boolean> checkAvailability(
            @PathVariable Long productId,
            @RequestParam Integer quantity
    ) {
        // Log the check
        log.info("Checking availability for product: {} - quantity: {}",
                productId, quantity);

        // Check availability via service
        boolean available = inventoryService.checkAvailability(productId, quantity);

        // Return availability status with 200 OK
        return ResponseEntity.ok(available);
    }
}
