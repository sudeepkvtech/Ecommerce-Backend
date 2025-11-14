package com.ecommerce.inventoryservice.service;

// Import entity classes
import com.ecommerce.inventoryservice.entity.Inventory;
import com.ecommerce.inventoryservice.entity.StockMovement;
import com.ecommerce.inventoryservice.entity.MovementType;

// Import repositories
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import com.ecommerce.inventoryservice.repository.StockMovementRepository;

// Import DTOs
import com.ecommerce.inventoryservice.dto.*;

// Import exceptions (will be created)
import com.ecommerce.inventoryservice.exception.ResourceNotFoundException;
import com.ecommerce.inventoryservice.exception.BadRequestException;
import com.ecommerce.inventoryservice.exception.DuplicateResourceException;

// Import Spring annotations
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Import Lombok
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Import List for collections
import java.util.List;
import java.util.stream.Collectors;

/**
 * Inventory Service
 *
 * Business logic layer for inventory operations.
 * Handles stock tracking, reservations, and movements.
 *
 * What does this service do?
 * ==========================
 * 1. Stock Tracking:
 *    - Get current inventory for products
 *    - Monitor low stock items
 *    - Track available vs reserved stock
 *
 * 2. Stock Reservations:
 *    - Reserve stock when order is placed
 *    - Release stock if payment fails
 *    - Commit stock when payment succeeds
 *
 * 3. Stock Operations:
 *    - Add stock (purchases, returns)
 *    - Reduce stock (damage, loss)
 *    - Adjust stock (corrections)
 *
 * 4. Movement History:
 *    - Record all stock changes
 *    - Retrieve movement history
 *    - Generate reports
 *
 * Integration Points:
 * ===================
 * - Product Service: Creates inventory for new products
 * - Order Service: Reserves stock during checkout
 * - Payment Service: Commits/releases based on payment result
 *
 * Transaction Management:
 * =======================
 * All stock-modifying methods use @Transactional to ensure:
 * - Inventory and StockMovement updated together
 * - Rollback if any error occurs
 * - Data consistency maintained
 */

// @Service marks this as a Spring service component
@Service

// @RequiredArgsConstructor generates constructor for final fields (dependency injection)
@RequiredArgsConstructor

// @Slf4j generates logger for logging
@Slf4j
public class InventoryService {

    /**
     * Repositories
     *
     * Injected via constructor (final fields + @RequiredArgsConstructor)
     */
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;

    // =========================================================================
    // Query Methods (Read Operations)
    // =========================================================================

    /**
     * Get Inventory by Product ID
     *
     * Retrieves current inventory for a product.
     * Most commonly used method - called on every product page view.
     *
     * @param productId The ID of the product
     * @return InventoryResponse with current stock levels
     * @throws ResourceNotFoundException if inventory not found
     */
    public InventoryResponse getInventory(Long productId) {
        // Log the request
        log.info("Getting inventory for product: {}", productId);

        // Find inventory by product ID
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Inventory not found for product: " + productId
                ));

        // Convert to DTO and return
        return mapToInventoryResponse(inventory);
    }

    /**
     * Get Low Stock Items
     *
     * Retrieves all products where stock is below threshold.
     * Used for admin dashboard and reordering alerts.
     *
     * @return List of low stock inventory items
     */
    public List<InventoryResponse> getLowStockItems() {
        // Log the request
        log.info("Getting low stock items");

        // Get low stock items from repository
        List<Inventory> lowStockItems = inventoryRepository.findLowStockItems();

        // Log how many found
        log.info("Found {} low stock items", lowStockItems.size());

        // Convert to DTOs and return
        return lowStockItems.stream()
                .map(this::mapToInventoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get Out of Stock Items
     *
     * Retrieves all products with zero availability.
     *
     * @return List of out of stock inventory items
     */
    public List<InventoryResponse> getOutOfStockItems() {
        // Log the request
        log.info("Getting out of stock items");

        // Get items with available quantity <= 0
        List<Inventory> outOfStockItems =
                inventoryRepository.findByAvailableQuantityLessThanEqual(0);

        // Log how many found
        log.info("Found {} out of stock items", outOfStockItems.size());

        // Convert to DTOs and return
        return outOfStockItems.stream()
                .map(this::mapToInventoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Check Stock Availability
     *
     * Quick check if sufficient stock is available.
     * Used by Order Service before creating order.
     *
     * @param productId Product to check
     * @param quantity Quantity needed
     * @return true if sufficient stock available
     */
    public boolean checkAvailability(Long productId, int quantity) {
        // Log the check
        log.debug("Checking availability for product {} - quantity: {}", productId, quantity);

        // Find inventory
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElse(null);

        // If no inventory, not available
        if (inventory == null) {
            log.warn("No inventory found for product: {}", productId);
            return false;
        }

        // Check if sufficient stock
        boolean available = inventory.hasSufficientStock(quantity);

        // Log result
        log.debug("Product {} availability check: {} (available: {}, requested: {})",
                productId, available, inventory.getAvailableQuantity(), quantity);

        return available;
    }

    // =========================================================================
    // Stock Reservation Methods
    // =========================================================================

    /**
     * Reserve Stock
     *
     * Reserves stock for a pending order.
     * Called by Order Service when customer places order.
     *
     * Workflow:
     * 1. Find inventory for product
     * 2. Validate sufficient stock available
     * 3. Move stock from available to reserved
     * 4. Record RESERVATION stock movement
     * 5. Save updated inventory
     * 6. Return updated inventory
     *
     * @param productId Product to reserve
     * @param request Reserve request with quantity and reference
     * @return Updated inventory response
     * @throws ResourceNotFoundException if inventory not found
     * @throws BadRequestException if insufficient stock
     *
     * @Transactional ensures inventory and movement are updated together
     */
    @Transactional
    public InventoryResponse reserveStock(Long productId, ReserveStockRequest request) {
        // Log the reservation request
        log.info("Reserving stock for product: {} - quantity: {}, reference: {}",
                productId, request.getQuantity(), request.getReferenceId());

        // =====================================================================
        // Step 1: Find inventory
        // =====================================================================

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Inventory not found for product: " + productId
                ));

        // =====================================================================
        // Step 2: Validate sufficient stock
        // =====================================================================

        if (!inventory.hasSufficientStock(request.getQuantity())) {
            // Not enough stock available
            log.warn("Insufficient stock for product: {} - available: {}, requested: {}",
                    productId, inventory.getAvailableQuantity(), request.getQuantity());

            throw new BadRequestException(
                "Insufficient stock. Available: " + inventory.getAvailableQuantity() +
                ", Requested: " + request.getQuantity()
            );
        }

        // =====================================================================
        // Step 3: Record state before reservation
        // =====================================================================

        int quantityBefore = inventory.getTotalQuantity();

        // =====================================================================
        // Step 4: Reserve stock (moves available to reserved)
        // =====================================================================

        // Use business logic method in entity
        // This updates: available (decrease), reserved (increase), total (unchanged)
        inventory.reserveStock(request.getQuantity());

        // =====================================================================
        // Step 5: Save updated inventory
        // =====================================================================

        inventory = inventoryRepository.save(inventory);

        // Log successful reservation
        log.info("Reserved {} units for product: {} - available: {}, reserved: {}",
                request.getQuantity(), productId,
                inventory.getAvailableQuantity(), inventory.getReservedQuantity());

        // =====================================================================
        // Step 6: Record stock movement
        // =====================================================================

        // RESERVATION doesn't change total quantity, just moves available to reserved
        // So quantityChange is 0 for total, but we track it separately
        StockMovement movement = StockMovement.create(
                productId,
                MovementType.RESERVATION,
                0,  // Total quantity unchanged
                quantityBefore,
                inventory.getTotalQuantity(),
                request.getReferenceId(),
                "Reserved " + request.getQuantity() + " units for " + request.getReferenceId()
        );

        stockMovementRepository.save(movement);

        // =====================================================================
        // Step 7: Return updated inventory
        // =====================================================================

        return mapToInventoryResponse(inventory);
    }

    /**
     * Release Reservation
     *
     * Releases previously reserved stock back to available.
     * Called when payment fails or order is cancelled.
     *
     * Workflow:
     * 1. Find inventory for product
     * 2. Validate sufficient reserved stock
     * 3. Move stock from reserved to available
     * 4. Record RELEASE stock movement
     * 5. Save updated inventory
     * 6. Return updated inventory
     *
     * @param productId Product to release
     * @param request Release request with quantity and reference
     * @return Updated inventory response
     * @throws ResourceNotFoundException if inventory not found
     * @throws BadRequestException if trying to release more than reserved
     */
    @Transactional
    public InventoryResponse releaseReservation(Long productId, ReleaseStockRequest request) {
        // Log the release request
        log.info("Releasing reservation for product: {} - quantity: {}, reference: {}, reason: {}",
                productId, request.getQuantity(), request.getReferenceId(), request.getReason());

        // Find inventory
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Inventory not found for product: " + productId
                ));

        // Record state before release
        int quantityBefore = inventory.getTotalQuantity();

        // Release reservation (moves reserved to available)
        // This validates that we're not releasing more than reserved
        inventory.releaseReservation(request.getQuantity());

        // Save updated inventory
        inventory = inventoryRepository.save(inventory);

        // Log successful release
        log.info("Released {} units for product: {} - available: {}, reserved: {}",
                request.getQuantity(), productId,
                inventory.getAvailableQuantity(), inventory.getReservedQuantity());

        // Record stock movement
        String notes = "Released " + request.getQuantity() + " units for " +
                request.getReferenceId();
        if (request.getReason() != null) {
            notes += " - Reason: " + request.getReason();
        }

        StockMovement movement = StockMovement.create(
                productId,
                MovementType.RELEASE,
                0,  // Total quantity unchanged
                quantityBefore,
                inventory.getTotalQuantity(),
                request.getReferenceId(),
                notes
        );

        stockMovementRepository.save(movement);

        // Return updated inventory
        return mapToInventoryResponse(inventory);
    }

    /**
     * Commit Reservation
     *
     * Commits a reservation when payment succeeds.
     * Stock leaves warehouse - reduces total and reserved.
     *
     * Workflow:
     * 1. Find inventory for product
     * 2. Validate sufficient reserved stock
     * 3. Remove stock from reserved and total
     * 4. Record SALE stock movement
     * 5. Save updated inventory
     * 6. Return updated inventory
     *
     * @param productId Product to commit
     * @param request Stock operation request
     * @return Updated inventory response
     * @throws ResourceNotFoundException if inventory not found
     * @throws BadRequestException if trying to commit more than reserved
     */
    @Transactional
    public InventoryResponse commitReservation(Long productId, StockOperationRequest request) {
        // Log the commit request
        log.info("Committing reservation for product: {} - quantity: {}, reference: {}",
                productId, request.getQuantity(), request.getReferenceId());

        // Find inventory
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Inventory not found for product: " + productId
                ));

        // Record state before commit
        int quantityBefore = inventory.getTotalQuantity();

        // Commit reservation (removes from reserved and total)
        inventory.commitReservation(request.getQuantity());

        // Save updated inventory
        inventory = inventoryRepository.save(inventory);

        // Log successful commit
        log.info("Committed {} units for product: {} - total: {}, reserved: {}",
                request.getQuantity(), productId,
                inventory.getTotalQuantity(), inventory.getReservedQuantity());

        // Record stock movement (SALE)
        StockMovement movement = StockMovement.create(
                productId,
                MovementType.SALE,
                -request.getQuantity(),  // Negative: stock decreased
                quantityBefore,
                inventory.getTotalQuantity(),
                request.getReferenceId(),
                request.getNotes() != null ? request.getNotes() :
                        "Sold " + request.getQuantity() + " units - " + request.getReferenceId()
        );

        stockMovementRepository.save(movement);

        // Return updated inventory
        return mapToInventoryResponse(inventory);
    }

    // =========================================================================
    // Stock Operation Methods (Admin)
    // =========================================================================

    /**
     * Add Stock
     *
     * Adds stock to inventory (PURCHASE, RETURN).
     * Increases total and available.
     *
     * @param productId Product to add stock to
     * @param request Stock operation request
     * @return Updated inventory response
     */
    @Transactional
    public InventoryResponse addStock(Long productId, StockOperationRequest request) {
        // Log the add stock request
        log.info("Adding stock for product: {} - quantity: {}, reference: {}",
                productId, request.getQuantity(), request.getReferenceId());

        // Find inventory
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Inventory not found for product: " + productId
                ));

        // Record state before adding
        int quantityBefore = inventory.getTotalQuantity();

        // Add stock
        inventory.addStock(request.getQuantity());

        // Save updated inventory
        inventory = inventoryRepository.save(inventory);

        // Log successful addition
        log.info("Added {} units to product: {} - total: {}",
                request.getQuantity(), productId, inventory.getTotalQuantity());

        // Record stock movement (determine type based on reference)
        MovementType movementType = request.getReferenceId().startsWith("RMA") ||
                request.getReferenceId().startsWith("RETURN") ?
                MovementType.RETURN : MovementType.PURCHASE;

        StockMovement movement = StockMovement.create(
                productId,
                movementType,
                request.getQuantity(),  // Positive: stock increased
                quantityBefore,
                inventory.getTotalQuantity(),
                request.getReferenceId(),
                request.getNotes() != null ? request.getNotes() :
                        "Added " + request.getQuantity() + " units - " + request.getReferenceId()
        );

        stockMovementRepository.save(movement);

        // Return updated inventory
        return mapToInventoryResponse(inventory);
    }

    /**
     * Reduce Stock
     *
     * Reduces stock from inventory (DAMAGE).
     * Decreases total and available.
     *
     * @param productId Product to reduce stock from
     * @param request Stock operation request
     * @return Updated inventory response
     * @throws BadRequestException if reducing more than available
     */
    @Transactional
    public InventoryResponse reduceStock(Long productId, StockOperationRequest request) {
        // Log the reduce stock request
        log.info("Reducing stock for product: {} - quantity: {}, reference: {}",
                productId, request.getQuantity(), request.getReferenceId());

        // Find inventory
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Inventory not found for product: " + productId
                ));

        // Record state before reducing
        int quantityBefore = inventory.getTotalQuantity();

        // Reduce stock (validates not reducing more than available)
        inventory.reduceStock(request.getQuantity());

        // Save updated inventory
        inventory = inventoryRepository.save(inventory);

        // Log successful reduction
        log.info("Reduced {} units from product: {} - total: {}",
                request.getQuantity(), productId, inventory.getTotalQuantity());

        // Record stock movement (DAMAGE)
        StockMovement movement = StockMovement.create(
                productId,
                MovementType.DAMAGE,
                -request.getQuantity(),  // Negative: stock decreased
                quantityBefore,
                inventory.getTotalQuantity(),
                request.getReferenceId(),
                request.getNotes() != null ? request.getNotes() :
                        "Damaged/Lost " + request.getQuantity() + " units - " + request.getReferenceId()
        );

        stockMovementRepository.save(movement);

        // Return updated inventory
        return mapToInventoryResponse(inventory);
    }

    /**
     * Adjust Stock
     *
     * Manually adjusts stock to match physical count.
     * Can increase or decrease.
     *
     * @param productId Product to adjust
     * @param request Stock operation request (quantity is NEW total)
     * @return Updated inventory response
     */
    @Transactional
    public InventoryResponse adjustStock(Long productId, StockOperationRequest request) {
        // Log the adjust stock request
        log.info("Adjusting stock for product: {} - new total: {}, reference: {}",
                productId, request.getQuantity(), request.getReferenceId());

        // Find inventory
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Inventory not found for product: " + productId
                ));

        // Record state before adjustment
        int quantityBefore = inventory.getTotalQuantity();

        // Calculate difference
        int difference = request.getQuantity() - quantityBefore;

        // Adjust stock to new total
        inventory.adjustStock(request.getQuantity());

        // Save updated inventory
        inventory = inventoryRepository.save(inventory);

        // Log successful adjustment
        log.info("Adjusted stock for product: {} - old: {}, new: {}, difference: {}",
                productId, quantityBefore, inventory.getTotalQuantity(), difference);

        // Record stock movement (ADJUSTMENT)
        StockMovement movement = StockMovement.create(
                productId,
                MovementType.ADJUSTMENT,
                difference,  // Can be positive or negative
                quantityBefore,
                inventory.getTotalQuantity(),
                request.getReferenceId(),
                request.getNotes() != null ? request.getNotes() :
                        "Adjusted stock - old: " + quantityBefore + ", new: " +
                        inventory.getTotalQuantity()
        );

        stockMovementRepository.save(movement);

        // Return updated inventory
        return mapToInventoryResponse(inventory);
    }

    // =========================================================================
    // Inventory Creation
    // =========================================================================

    /**
     * Create Inventory
     *
     * Creates initial inventory for a new product.
     * Called by Product Service when new product is added.
     *
     * @param productId Product to create inventory for
     * @param initialQuantity Initial stock quantity
     * @param lowStockThreshold Low stock alert threshold
     * @return Created inventory response
     * @throws DuplicateResourceException if inventory already exists
     */
    @Transactional
    public InventoryResponse createInventory(
            Long productId,
            Integer initialQuantity,
            Integer lowStockThreshold
    ) {
        // Log the creation request
        log.info("Creating inventory for product: {} - initial: {}, threshold: {}",
                productId, initialQuantity, lowStockThreshold);

        // Check if inventory already exists
        if (inventoryRepository.existsByProductId(productId)) {
            log.warn("Inventory already exists for product: {}", productId);
            throw new DuplicateResourceException(
                "Inventory already exists for product: " + productId
            );
        }

        // Create new inventory
        Inventory inventory = Inventory.builder()
                .productId(productId)
                .totalQuantity(initialQuantity)
                .availableQuantity(initialQuantity)
                .reservedQuantity(0)
                .lowStockThreshold(lowStockThreshold != null ? lowStockThreshold : 10)
                .build();

        // Save inventory
        inventory = inventoryRepository.save(inventory);

        // Log successful creation
        log.info("Created inventory for product: {} - total: {}",
                productId, inventory.getTotalQuantity());

        // Record initial stock movement (PURCHASE)
        if (initialQuantity > 0) {
            StockMovement movement = StockMovement.create(
                    productId,
                    MovementType.PURCHASE,
                    initialQuantity,
                    0,
                    initialQuantity,
                    "INITIAL",
                    "Initial stock for new product"
            );

            stockMovementRepository.save(movement);
        }

        // Return created inventory
        return mapToInventoryResponse(inventory);
    }

    // =========================================================================
    // Movement History Methods
    // =========================================================================

    /**
     * Get Movement History
     *
     * Retrieves complete movement history for a product.
     *
     * @param productId Product to get history for
     * @return List of stock movements
     */
    public List<StockMovementResponse> getMovementHistory(Long productId) {
        // Log the request
        log.info("Getting movement history for product: {}", productId);

        // Get movements from repository
        List<StockMovement> movements =
                stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId);

        // Log how many found
        log.info("Found {} movements for product: {}", movements.size(), productId);

        // Convert to DTOs and return
        return movements.stream()
                .map(this::mapToMovementResponse)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Mapping Methods
    // =========================================================================

    /**
     * Map Inventory Entity to InventoryResponse DTO
     */
    private InventoryResponse mapToInventoryResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProductId())
                .availableQuantity(inventory.getAvailableQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .totalQuantity(inventory.getTotalQuantity())
                .lowStockThreshold(inventory.getLowStockThreshold())
                .isLowStock(inventory.isLowStock())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    /**
     * Map StockMovement Entity to StockMovementResponse DTO
     */
    private StockMovementResponse mapToMovementResponse(StockMovement movement) {
        return StockMovementResponse.builder()
                .id(movement.getId())
                .productId(movement.getProductId())
                .movementType(movement.getMovementType())
                .quantityChange(movement.getQuantityChange())
                .quantityBefore(movement.getQuantityBefore())
                .quantityAfter(movement.getQuantityAfter())
                .referenceId(movement.getReferenceId())
                .notes(movement.getNotes())
                .createdAt(movement.getCreatedAt())
                .build();
    }
}
