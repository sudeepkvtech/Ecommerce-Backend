package com.ecommerce.orderservice.service;

// Import entities
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;

// Import DTOs
import com.ecommerce.orderservice.dto.*;

// Import repositories
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.repository.OrderItemRepository;

// Import exceptions
import com.ecommerce.orderservice.exception.ResourceNotFoundException;
import com.ecommerce.orderservice.exception.BadRequestException;

// Import Spring annotations
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Import Lombok
import lombok.RequiredArgsConstructor;

// Import SLF4J for logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Import BigDecimal for monetary calculations
import java.math.BigDecimal;

// Import time classes
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Import List and stream API
import java.util.List;
import java.util.stream.Collectors;

/**
 * Order Service
 *
 * Business logic layer for order management.
 *
 * This service handles:
 * - Order creation and validation
 * - Order retrieval and filtering
 * - Order status updates
 * - Order cancellation
 * - Integration with User Service and Product Service (future)
 *
 * Order Creation Flow:
 * 1. Validate request (items not empty, address provided)
 * 2. Extract user ID from authentication
 * 3. Validate products exist (future: call Product Service)
 * 4. Fetch product prices (future: call Product Service)
 * 5. Calculate subtotals and total amount
 * 6. Generate unique order number
 * 7. Create Order entity with PENDING status
 * 8. Create OrderItem entities
 * 9. Save to database
 * 10. Return OrderResponse
 *
 * Note: In a complete microservices architecture, this service would:
 * - Call User Service to validate user and address
 * - Call Product Service to validate products and get prices
 * - Call Inventory Service to reserve stock
 * - Call Payment Service to process payment
 *
 * For this implementation, we'll simulate these calls with placeholder logic.
 */
@Service  // Marks this as a Spring service component
@RequiredArgsConstructor  // Lombok generates constructor for final fields
@Transactional  // All methods run in database transactions
public class OrderService {

    // Logger for debugging and monitoring
    // Used to log order creation, status changes, errors, etc.
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    // Repository dependencies injected via constructor
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * Create a new order
     *
     * This method handles the complete order creation process.
     *
     * Steps:
     * 1. Validate request data
     * 2. Get user ID from email (from JWT token)
     * 3. Validate products and fetch prices
     * 4. Create order entity
     * 5. Create order items
     * 6. Calculate total
     * 7. Save to database
     * 8. Return response
     *
     * @param userEmail Email of authenticated user (from JWT token)
     * @param request OrderRequest with items and shipping details
     * @return OrderResponse with created order details
     * @throws BadRequestException if validation fails
     */
    public OrderResponse createOrder(String userEmail, OrderRequest request) {
        // Log order creation attempt
        logger.info("Creating order for user: {}", userEmail);

        // Step 1: Extract user ID
        // In real implementation, would call User Service to get user by email
        // For now, we'll simulate with a placeholder
        // TODO: Implement User Service integration
        Long userId = getUserIdByEmail(userEmail);

        // Step 2: Validate shipping address belongs to user
        // In real implementation, would call User Service
        // TODO: Implement address validation
        validateShippingAddress(userId, request.getShippingAddressId());

        // Step 3: Generate unique order number
        // Format: ORD-YYYYMMDD-NNN
        String orderNumber = generateOrderNumber();
        logger.debug("Generated order number: {}", orderNumber);

        // Step 4: Create Order entity
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNumber(orderNumber);
        order.setStatus(OrderStatus.PENDING);  // New orders start as PENDING
        order.setShippingAddressId(request.getShippingAddressId());
        order.setPaymentMethod(request.getPaymentMethod());
        // Total amount will be calculated after creating items

        // Step 5: Process order items
        // Calculate subtotals and create OrderItem entities
        List<OrderItem> orderItems = createOrderItems(order, request.getItems());

        // Step 6: Calculate total amount
        // Sum of all item subtotals
        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        // Step 7: Save order (cascade saves items automatically)
        Order savedOrder = orderRepository.save(order);
        logger.info("Order created successfully. Order number: {}, Total: {}",
                savedOrder.getOrderNumber(), savedOrder.getTotalAmount());

        // Step 8: Convert to DTO and return
        return mapToOrderResponse(savedOrder);
    }

    /**
     * Get all orders for a user
     *
     * Retrieves order history for the authenticated user.
     * Orders are sorted by creation date (newest first).
     *
     * @param userEmail Email of authenticated user
     * @return List of OrderResponse (order history)
     */
    @Transactional(readOnly = true)  // Read-only optimization
    public List<OrderResponse> getUserOrders(String userEmail) {
        logger.info("Fetching orders for user: {}", userEmail);

        // Get user ID from email
        Long userId = getUserIdByEmail(userEmail);

        // Fetch orders sorted by creation date (newest first)
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);

        logger.debug("Found {} orders for user {}", orders.size(), userEmail);

        // Convert to DTOs
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get order by ID
     *
     * Retrieves a specific order by its ID.
     * Validates that the order belongs to the requesting user.
     *
     * @param orderId The ID of the order to retrieve
     * @param userEmail Email of authenticated user
     * @return OrderResponse with order details
     * @throws ResourceNotFoundException if order not found
     * @throws BadRequestException if order doesn't belong to user
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, String userEmail) {
        logger.info("Fetching order {} for user: {}", orderId, userEmail);

        // Get user ID
        Long userId = getUserIdByEmail(userEmail);

        // Find order by ID
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with ID: " + orderId
                ));

        // Security check: Verify order belongs to user
        if (!order.getUserId().equals(userId)) {
            logger.warn("User {} attempted to access order {} belonging to user {}",
                    userId, orderId, order.getUserId());
            throw new BadRequestException(
                    "Order " + orderId + " does not belong to user"
            );
        }

        // Return order details
        return mapToOrderResponse(order);
    }

    /**
     * Get order by order number
     *
     * Retrieves order using customer-facing order number.
     * Useful for order tracking.
     *
     * @param orderNumber The order number (e.g., ORD-20240115-001)
     * @param userEmail Email of authenticated user
     * @return OrderResponse with order details
     * @throws ResourceNotFoundException if order not found
     * @throws BadRequestException if order doesn't belong to user
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber, String userEmail) {
        logger.info("Fetching order {} for user: {}", orderNumber, userEmail);

        // Get user ID
        Long userId = getUserIdByEmail(userEmail);

        // Find order by order number
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with order number: " + orderNumber
                ));

        // Security check
        if (!order.getUserId().equals(userId)) {
            throw new BadRequestException(
                    "Order " + orderNumber + " does not belong to user"
            );
        }

        return mapToOrderResponse(order);
    }

    /**
     * Update order status (Admin only)
     *
     * Allows administrators to change order status.
     * Validates state transitions are allowed.
     *
     * @param orderId The ID of the order to update
     * @param newStatus The new status to set
     * @return Updated OrderResponse
     * @throws ResourceNotFoundException if order not found
     * @throws BadRequestException if transition invalid
     */
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        logger.info("Updating order {} to status: {}", orderId, newStatus);

        // Find order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with ID: " + orderId
                ));

        OrderStatus currentStatus = order.getStatus();

        // Validate status transition is allowed
        validateStatusTransition(currentStatus, newStatus);

        // Update status
        order.setStatus(newStatus);

        // Save changes
        Order updatedOrder = orderRepository.save(order);

        logger.info("Order {} status updated: {} → {}",
                orderId, currentStatus, newStatus);

        return mapToOrderResponse(updatedOrder);
    }

    /**
     * Cancel order
     *
     * Allows users to cancel their own orders.
     * Only orders in PENDING or CONFIRMED status can be cancelled.
     *
     * @param orderId The ID of the order to cancel
     * @param userEmail Email of authenticated user
     * @return Updated OrderResponse
     * @throws ResourceNotFoundException if order not found
     * @throws BadRequestException if order cannot be cancelled
     */
    public OrderResponse cancelOrder(Long orderId, String userEmail) {
        logger.info("Cancelling order {} for user: {}", orderId, userEmail);

        // Get user ID
        Long userId = getUserIdByEmail(userEmail);

        // Find order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with ID: " + orderId
                ));

        // Security check: Verify ownership
        if (!order.getUserId().equals(userId)) {
            throw new BadRequestException(
                    "Cannot cancel order that doesn't belong to you"
            );
        }

        // Business rule: Can only cancel PENDING or CONFIRMED orders
        if (order.getStatus() != OrderStatus.PENDING &&
                order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException(
                    "Cannot cancel order with status: " + order.getStatus() +
                            ". Only PENDING or CONFIRMED orders can be cancelled."
            );
        }

        // Update status to CANCELLED
        order.setStatus(OrderStatus.CANCELLED);

        // Save changes
        Order cancelledOrder = orderRepository.save(order);

        logger.info("Order {} cancelled successfully", orderId);

        // TODO: In real implementation, would trigger:
        // - Payment refund (if payment was processed)
        // - Inventory release (release reserved items)
        // - Notification to user (email confirmation)

        return mapToOrderResponse(cancelledOrder);
    }

    /**
     * Get all orders by status (Admin only)
     *
     * Retrieves all orders with a specific status.
     * Useful for administrative dashboards.
     *
     * @param status The order status to filter by
     * @return List of orders with specified status
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        logger.info("Fetching all orders with status: {}", status);

        List<Order> orders = orderRepository.findByStatus(status);

        logger.debug("Found {} orders with status {}", orders.size(), status);

        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Get user ID by email
     *
     * In real implementation, would call User Service API.
     * For now, returns a simulated user ID.
     *
     * TODO: Implement User Service integration
     * - Make HTTP call to User Service: GET /api/users/by-email/{email}
     * - Use RestTemplate or WebClient
     * - Handle errors (user not found, service unavailable)
     *
     * @param email User's email address
     * @return User ID
     */
    private Long getUserIdByEmail(String email) {
        // Placeholder implementation
        // In production, would call User Service
        logger.debug("Getting user ID for email: {} (placeholder)", email);

        // For demo purposes, return a simulated user ID
        // Real implementation:
        // UserResponse user = userServiceClient.getUserByEmail(email);
        // return user.getId();

        return 1L;  // Placeholder user ID
    }

    /**
     * Validate shipping address belongs to user
     *
     * In real implementation, would call User Service to validate.
     *
     * TODO: Implement User Service integration
     * - Call User Service: GET /api/users/addresses/{addressId}
     * - Verify address exists
     * - Verify address belongs to user
     * - Verify address type is SHIPPING
     *
     * @param userId The user's ID
     * @param addressId The address ID to validate
     * @throws BadRequestException if address invalid
     */
    private void validateShippingAddress(Long userId, Long addressId) {
        logger.debug("Validating address {} for user {} (placeholder)", addressId, userId);

        // Placeholder implementation
        // In production, would call User Service
        // AddressResponse address = userServiceClient.getAddress(userId, addressId);
        // if (address.getAddressType() != AddressType.SHIPPING) {
        //     throw new BadRequestException("Address is not a shipping address");
        // }

        // For demo, assume address is valid
    }

    /**
     * Create order items from request
     *
     * For each item in the request:
     * 1. Validate product exists
     * 2. Fetch product details (name, price)
     * 3. Calculate subtotal
     * 4. Create OrderItem entity
     *
     * TODO: Implement Product Service integration
     * - Call Product Service to validate products
     * - Fetch current prices
     * - Validate product availability
     * - Check inventory
     *
     * @param order The parent order
     * @param itemRequests List of items to add
     * @return List of created OrderItem entities
     */
    private List<OrderItem> createOrderItems(Order order, List<OrderItemRequest> itemRequests) {
        logger.debug("Creating {} order items", itemRequests.size());

        return itemRequests.stream()
                .map(itemRequest -> {
                    // In real implementation, would call Product Service
                    // ProductResponse product = productServiceClient.getProduct(itemRequest.getProductId());

                    // For demo, use placeholder product details
                    String productName = "Product #" + itemRequest.getProductId();
                    BigDecimal productPrice = new BigDecimal("99.99");  // Placeholder price

                    // Calculate subtotal
                    BigDecimal quantity = new BigDecimal(itemRequest.getQuantity());
                    BigDecimal subtotal = productPrice.multiply(quantity);

                    // Create OrderItem
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setProductId(itemRequest.getProductId());
                    orderItem.setProductName(productName);
                    orderItem.setProductPrice(productPrice);
                    orderItem.setQuantity(itemRequest.getQuantity());
                    orderItem.setSubtotal(subtotal);

                    logger.debug("Created order item: {} x {} = {}",
                            productName, itemRequest.getQuantity(), subtotal);

                    return orderItem;
                })
                .collect(Collectors.toList());
    }

    /**
     * Generate unique order number
     *
     * Format: ORD-YYYYMMDD-NNN
     * Example: ORD-20240115-001
     *
     * Algorithm:
     * 1. Get current date
     * 2. Format as YYYYMMDD
     * 3. Find existing orders for today
     * 4. Generate next sequence number
     * 5. Check if order number exists (handle race conditions)
     * 6. Return unique order number
     *
     * @return Unique order number
     */
    private String generateOrderNumber() {
        // Get current date
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Find orders created today
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);
        List<Order> todayOrders = orderRepository.findByCreatedAtBetween(startOfDay, endOfDay);

        // Calculate next sequence number
        int sequenceNumber = todayOrders.size() + 1;

        // Generate order number
        String orderNumber;
        do {
            orderNumber = String.format("ORD-%s-%03d", dateStr, sequenceNumber);
            sequenceNumber++;
        } while (orderRepository.existsByOrderNumber(orderNumber));
        // Loop ensures uniqueness in case of race conditions

        return orderNumber;
    }

    /**
     * Validate status transition is allowed
     *
     * Business rules for valid transitions:
     * - PENDING → CONFIRMED, CANCELLED
     * - CONFIRMED → PROCESSING, CANCELLED
     * - PROCESSING → SHIPPED, CANCELLED
     * - SHIPPED → DELIVERED
     * - DELIVERED → (none, final state)
     * - CANCELLED → (none, final state)
     *
     * @param currentStatus Current order status
     * @param newStatus Desired new status
     * @throws BadRequestException if transition invalid
     */
    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // Same status is allowed (no-op)
        if (currentStatus == newStatus) {
            return;
        }

        // Check if transition is valid
        boolean isValid = switch (currentStatus) {
            case PENDING -> newStatus == OrderStatus.CONFIRMED ||
                    newStatus == OrderStatus.CANCELLED;
            case CONFIRMED -> newStatus == OrderStatus.PROCESSING ||
                    newStatus == OrderStatus.CANCELLED;
            case PROCESSING -> newStatus == OrderStatus.SHIPPED ||
                    newStatus == OrderStatus.CANCELLED;
            case SHIPPED -> newStatus == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;  // Final states, no transitions
        };

        if (!isValid) {
            throw new BadRequestException(
                    "Cannot transition order from " + currentStatus +
                            " to " + newStatus
            );
        }
    }

    /**
     * Map Order entity to OrderResponse DTO
     *
     * Converts database entity to API response object.
     *
     * @param order Order entity
     * @return OrderResponse DTO
     */
    private OrderResponse mapToOrderResponse(Order order) {
        // Map order items
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());

        // Build response using builder pattern
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddressId(order.getShippingAddressId())
                .paymentMethod(order.getPaymentMethod())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * Map OrderItem entity to OrderItemResponse DTO
     *
     * @param item OrderItem entity
     * @return OrderItemResponse DTO
     */
    private OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productPrice(item.getProductPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
