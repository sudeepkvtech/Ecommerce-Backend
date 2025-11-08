package com.ecommerce.paymentservice.service;

// Import entity classes
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentMethod;
import com.ecommerce.paymentservice.entity.PaymentStatus;

// Import repository
import com.ecommerce.paymentservice.repository.PaymentRepository;

// Import DTOs
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.dto.ProcessPaymentRequest;
import com.ecommerce.paymentservice.dto.RefundRequest;

// Import exceptions
import com.ecommerce.paymentservice.exception.BadRequestException;
import com.ecommerce.paymentservice.exception.DuplicateResourceException;
import com.ecommerce.paymentservice.exception.ResourceNotFoundException;

// Import Spring annotations
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Import Lombok
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Import BigDecimal for monetary calculations
import java.math.BigDecimal;

// Import List for collections
import java.util.List;
import java.util.stream.Collectors;

/**
 * Payment Service
 *
 * Business logic layer for payment operations.
 * Handles payment processing, refunds, and payment retrieval.
 *
 * What does this service do?
 * ==========================
 * 1. Process Payments:
 *    - Validate payment request
 *    - Check for duplicate payments
 *    - Call payment gateway API (Stripe, PayPal, etc.)
 *    - Save payment record
 *    - Return payment result
 *
 * 2. Retrieve Payments:
 *    - Get payment by ID
 *    - Get payment by order ID
 *    - Get user's payment history
 *    - Get payments by status (admin)
 *
 * 3. Process Refunds:
 *    - Validate refund request
 *    - Call payment gateway refund API
 *    - Update payment status to REFUNDED
 *    - Return updated payment
 *
 * Integration Points:
 * ===================
 * 1. Order Service:
 *    - Receives payment requests from Order Service
 *    - Returns payment results to Order Service
 *    - Order Service updates order status based on payment
 *
 * 2. Payment Gateway (Stripe, PayPal, etc.):
 *    - Sends payment tokens to gateway
 *    - Receives transaction confirmation
 *    - Processes refunds via gateway
 *
 * 3. User Service:
 *    - Validates user exists (optional)
 *    - Could retrieve saved payment methods (future)
 *
 * Current Implementation Notes:
 * =============================
 * This implementation uses PLACEHOLDER payment processing.
 * In production, you need to integrate with actual payment gateways:
 *
 * - Stripe: https://stripe.com/docs/api
 * - PayPal: https://developer.paypal.com/
 * - Square: https://developer.squareup.com/
 * - Braintree: https://developers.braintreepayments.com/
 *
 * Example Stripe integration:
 * ---------------------------
 * // Add dependency:
 * <dependency>
 *     <groupId>com.stripe</groupId>
 *     <artifactId>stripe-java</artifactId>
 *     <version>24.0.0</version>
 * </dependency>
 *
 * // Initialize:
 * Stripe.apiKey = "sk_test_...";
 *
 * // Charge card:
 * Map<String, Object> params = new HashMap<>();
 * params.put("amount", amount.multiply(new BigDecimal("100")).longValue());
 * params.put("currency", "usd");
 * params.put("source", paymentToken);
 * Charge charge = Charge.create(params);
 *
 * // Refund:
 * Map<String, Object> refundParams = new HashMap<>();
 * refundParams.put("charge", transactionId);
 * refundParams.put("amount", refundAmount.multiply(new BigDecimal("100")).longValue());
 * Refund refund = Refund.create(refundParams);
 *
 * Annotations Explained:
 * ======================
 *
 * @Service:
 * - Marks this class as a Spring service component
 * - Spring creates and manages this as a bean
 * - Available for dependency injection
 *
 * @RequiredArgsConstructor (Lombok):
 * - Generates constructor for all final fields
 * - Enables constructor-based dependency injection
 * - Clean way to inject dependencies
 *
 * @Slf4j (Lombok):
 * - Generates logger field: private static final Logger log = ...
 * - Use: log.info(), log.error(), log.debug()
 * - Better than System.out.println()
 *
 * @Transactional:
 * - Applied to methods that modify database
 * - Ensures data consistency
 * - Rolls back changes if error occurs
 * - All database operations succeed or all fail
 */

// @Service marks this as a Spring service component
@Service

// @RequiredArgsConstructor generates constructor for final fields (dependency injection)
@RequiredArgsConstructor

// @Slf4j generates logger for logging
@Slf4j
public class PaymentService {

    /**
     * Payment Repository
     *
     * Injected via constructor (final field + @RequiredArgsConstructor)
     * Provides database access for Payment entity
     */
    private final PaymentRepository paymentRepository;

    /**
     * Process Payment
     *
     * Main method for processing a payment transaction.
     * This is called when a customer needs to pay for an order.
     *
     * Workflow:
     * 1. Validate request
     * 2. Check for duplicate payment
     * 3. Create payment record with PENDING status
     * 4. Process payment via payment gateway
     * 5. Update payment status based on gateway response
     * 6. Return payment result
     *
     * Error handling:
     * - DuplicateResourceException: Payment already exists for order
     * - BadRequestException: Invalid payment request
     * - RuntimeException: Payment gateway error
     *
     * @param request Payment request with order ID, amount, payment method, token
     * @return PaymentResponse with payment status and details
     *
     * @Transactional ensures database consistency:
     * - If payment gateway succeeds but database update fails → rollback
     * - If any error occurs → rollback
     * - All operations succeed or all fail
     */
    @Transactional
    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        // Log the incoming payment request
        // Log level: INFO (important business operation)
        log.info("Processing payment for order: {}", request.getOrderId());

        // =====================================================================
        // Step 1: Validate that payment doesn't already exist for this order
        // =====================================================================

        // Check if payment already exists for this order
        // Why? Prevent duplicate charges if:
        // - User clicks "Pay" button multiple times
        // - Network timeout causes retry
        // - Browser back button confusion
        if (paymentRepository.existsByOrderId(request.getOrderId())) {
            // Log the duplicate payment attempt
            log.warn("Duplicate payment attempt for order: {}", request.getOrderId());

            // Throw exception with helpful message
            // This prevents duplicate charge
            throw new DuplicateResourceException(
                "Payment already exists for order ID: " + request.getOrderId()
            );
        }

        // =====================================================================
        // Step 2: Create payment record with PENDING status
        // =====================================================================

        // Build Payment entity using builder pattern
        // Initially set status to PENDING (not yet processed)
        Payment payment = Payment.builder()
                // Set order ID from request
                .orderId(request.getOrderId())

                // Set user ID from request
                .userId(request.getUserId())

                // Set payment amount from request
                .amount(request.getAmount())

                // Set payment method (CREDIT_CARD, PAYPAL, etc.)
                .paymentMethod(request.getPaymentMethod())

                // Set initial status as PENDING
                // Will update to COMPLETED or FAILED after gateway processing
                .status(PaymentStatus.PENDING)

                // Transaction ID is null initially
                // Will be set after successful gateway processing
                .transactionId(null)

                // No failure reason initially
                .failureReason(null)

                // Build the Payment object
                .build();

        // Save payment to database
        // This generates the ID and sets timestamps (@PrePersist)
        // Now payment is in database with PENDING status
        payment = paymentRepository.save(payment);

        // Log successful creation of payment record
        log.info("Created payment record with ID: {}", payment.getId());

        // =====================================================================
        // Step 3: Update status to PROCESSING
        // =====================================================================

        // Update status to PROCESSING (about to call gateway)
        // This shows payment is actively being processed
        payment.setStatus(PaymentStatus.PROCESSING);

        // Save updated status
        // If gateway call fails, we can see payment was stuck in PROCESSING
        payment = paymentRepository.save(payment);

        // Log status update
        log.info("Payment {} status updated to PROCESSING", payment.getId());

        // =====================================================================
        // Step 4: Process payment via payment gateway
        // =====================================================================

        // IMPORTANT: This is a PLACEHOLDER implementation
        // In production, replace with actual payment gateway integration

        try {
            // TODO: Call actual payment gateway API here
            // Example with Stripe:
            // ------------------
            // Map<String, Object> params = new HashMap<>();
            // params.put("amount", request.getAmount().multiply(new BigDecimal("100")).longValue());
            // params.put("currency", "usd");
            // params.put("source", request.getPaymentToken());
            // params.put("description", "Payment for order " + request.getOrderId());
            //
            // Charge charge = Charge.create(params);
            //
            // if (charge.getStatus().equals("succeeded")) {
            //     payment.setStatus(PaymentStatus.COMPLETED);
            //     payment.setTransactionId(charge.getId());
            // } else {
            //     payment.setStatus(PaymentStatus.FAILED);
            //     payment.setFailureReason("Payment gateway returned: " + charge.getStatus());
            // }

            // PLACEHOLDER: Simulate successful payment processing
            // In reality, this would be a call to Stripe, PayPal, etc.
            String transactionId = simulatePaymentGateway(request);

            // Payment successful!
            // Update payment record with success status and transaction ID
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setTransactionId(transactionId);
            payment.setFailureReason(null);  // Clear any previous failure reason

            // Log successful payment
            log.info("Payment {} completed successfully. Transaction ID: {}",
                payment.getId(), transactionId);

        } catch (Exception e) {
            // Payment gateway error (card declined, network error, etc.)

            // Update payment status to FAILED
            payment.setStatus(PaymentStatus.FAILED);

            // Store the failure reason from exception
            payment.setFailureReason(e.getMessage());

            // Transaction ID remains null (no successful transaction)
            payment.setTransactionId(null);

            // Log the payment failure
            log.error("Payment {} failed: {}", payment.getId(), e.getMessage());
        }

        // =====================================================================
        // Step 5: Save final payment status
        // =====================================================================

        // Save the payment with final status (COMPLETED or FAILED)
        // This also updates the updatedAt timestamp
        payment = paymentRepository.save(payment);

        // Log final status
        log.info("Payment {} final status: {}", payment.getId(), payment.getStatus());

        // =====================================================================
        // Step 6: Convert to DTO and return
        // =====================================================================

        // Convert Payment entity to PaymentResponse DTO
        // DTO is what we return to the caller (Order Service, Controller)
        // Don't return entity directly (separation of concerns)
        return mapToResponse(payment);
    }

    /**
     * Get Payment by ID
     *
     * Retrieves a payment by its unique ID.
     * Used for looking up specific payment details.
     *
     * @param id Payment ID
     * @return PaymentResponse
     * @throws ResourceNotFoundException if payment not found
     */
    public PaymentResponse getPaymentById(Long id) {
        // Log the request
        log.info("Fetching payment with ID: {}", id);

        // Find payment by ID
        // orElseThrow() throws exception if not found
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Payment not found with ID: " + id
                ));

        // Convert to DTO and return
        return mapToResponse(payment);
    }

    /**
     * Get Payment by Order ID
     *
     * Retrieves the payment for a specific order.
     * Used by Order Service to check payment status.
     *
     * Why is this important?
     * ----------------------
     * Order Service needs to know payment status to update order status:
     * - Payment COMPLETED → Order CONFIRMED
     * - Payment FAILED → Order remains PENDING
     * - Payment PENDING → Order waits for payment
     *
     * @param orderId Order ID
     * @return PaymentResponse
     * @throws ResourceNotFoundException if payment not found
     */
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        // Log the request
        log.info("Fetching payment for order ID: {}", orderId);

        // Find payment by order ID
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Payment not found for order ID: " + orderId
                ));

        // Convert to DTO and return
        return mapToResponse(payment);
    }

    /**
     * Get User Payment History
     *
     * Retrieves all payments made by a user.
     * Used for "My Payments" page in user interface.
     *
     * Results are sorted by creation date (newest first).
     *
     * @param userId User ID
     * @return List of PaymentResponse (empty if user has no payments)
     */
    public List<PaymentResponse> getUserPaymentHistory(Long userId) {
        // Log the request
        log.info("Fetching payment history for user ID: {}", userId);

        // Get all payments by user, sorted by date (newest first)
        List<Payment> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // Log how many payments found
        log.info("Found {} payments for user ID: {}", payments.size(), userId);

        // Convert list of Payment entities to list of PaymentResponse DTOs
        // Using Java Stream API for transformation
        return payments.stream()
                // Map each Payment to PaymentResponse
                .map(this::mapToResponse)
                // Collect results into a List
                .collect(Collectors.toList());
    }

    /**
     * Get Payments by Status (Admin)
     *
     * Retrieves all payments with a specific status.
     * Used by admins for monitoring and reporting.
     *
     * Examples:
     * - Get all failed payments for investigation
     * - Get all completed payments for reconciliation
     * - Get all pending payments that might be stuck
     *
     * @param status Payment status to filter by
     * @return List of PaymentResponse (empty if no payments with this status)
     */
    public List<PaymentResponse> getPaymentsByStatus(PaymentStatus status) {
        // Log the request
        log.info("Fetching payments with status: {}", status);

        // Get all payments with this status
        List<Payment> payments = paymentRepository.findByStatus(status);

        // Log how many payments found
        log.info("Found {} payments with status: {}", payments.size(), status);

        // Convert to DTOs and return
        return payments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Process Refund
     *
     * Refunds a completed payment.
     * This is typically done when:
     * - Customer returns a product
     * - Order is cancelled after payment
     * - Service issue or complaint resolution
     *
     * Workflow:
     * 1. Find payment by ID
     * 2. Validate payment can be refunded
     * 3. Determine refund amount (full or partial)
     * 4. Call payment gateway refund API
     * 5. Update payment status to REFUNDED
     * 6. Return updated payment
     *
     * @param paymentId ID of payment to refund
     * @param request Refund request with optional amount and reason
     * @return PaymentResponse with status REFUNDED
     * @throws ResourceNotFoundException if payment not found
     * @throws BadRequestException if payment cannot be refunded
     *
     * @Transactional ensures refund is atomic:
     * - Gateway refund succeeds + database update succeeds = complete
     * - Either fails → rollback
     */
    @Transactional
    public PaymentResponse processRefund(Long paymentId, RefundRequest request) {
        // Log the refund request
        log.info("Processing refund for payment ID: {}", paymentId);
        log.info("Refund reason: {}", request.getReason());

        // =====================================================================
        // Step 1: Find payment
        // =====================================================================

        // Find payment by ID
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Payment not found with ID: " + paymentId
                ));

        // =====================================================================
        // Step 2: Validate payment can be refunded
        // =====================================================================

        // Check if payment status is COMPLETED
        // Can only refund completed payments
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            // Log invalid refund attempt
            log.warn("Cannot refund payment {} with status: {}",
                paymentId, payment.getStatus());

            // Throw exception
            throw new BadRequestException(
                "Can only refund completed payments. Current status: " + payment.getStatus()
            );
        }

        // Check if payment has transaction ID
        // Need transaction ID to process refund via gateway
        if (payment.getTransactionId() == null) {
            // Log missing transaction ID
            log.error("Payment {} is missing transaction ID", paymentId);

            // Throw exception
            throw new BadRequestException(
                "Payment is missing transaction ID. Cannot process refund."
            );
        }

        // =====================================================================
        // Step 3: Determine refund amount
        // =====================================================================

        // Get refund amount from request
        // If null, refund full payment amount (full refund)
        BigDecimal refundAmount = request.getAmount();

        // If amount not specified, do full refund
        if (refundAmount == null) {
            refundAmount = payment.getAmount();
            log.info("Refund amount not specified. Refunding full amount: {}", refundAmount);
        } else {
            log.info("Refunding partial amount: {}", refundAmount);
        }

        // Validate refund amount doesn't exceed payment amount
        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            // Log invalid refund amount
            log.warn("Refund amount {} exceeds payment amount {}",
                refundAmount, payment.getAmount());

            // Throw exception
            throw new BadRequestException(
                "Refund amount (" + refundAmount + ") cannot exceed payment amount ("
                + payment.getAmount() + ")"
            );
        }

        // =====================================================================
        // Step 4: Process refund via payment gateway
        // =====================================================================

        try {
            // TODO: Call actual payment gateway refund API here
            // Example with Stripe:
            // ------------------
            // Map<String, Object> params = new HashMap<>();
            // params.put("charge", payment.getTransactionId());
            // params.put("amount", refundAmount.multiply(new BigDecimal("100")).longValue());
            // params.put("reason", "requested_by_customer");
            // params.put("metadata", Map.of("refund_reason", request.getReason()));
            //
            // Refund refund = Refund.create(params);
            //
            // if (!refund.getStatus().equals("succeeded")) {
            //     throw new RuntimeException("Refund failed: " + refund.getStatus());
            // }

            // PLACEHOLDER: Simulate refund processing
            simulateRefundGateway(payment.getTransactionId(), refundAmount);

            // Log successful refund
            log.info("Refund processed successfully via payment gateway");

        } catch (Exception e) {
            // Refund failed
            log.error("Refund failed for payment {}: {}", paymentId, e.getMessage());

            // Throw exception (transaction will be rolled back)
            throw new RuntimeException("Refund processing failed: " + e.getMessage());
        }

        // =====================================================================
        // Step 5: Update payment status
        // =====================================================================

        // Update payment status to REFUNDED
        payment.setStatus(PaymentStatus.REFUNDED);

        // Note: Keep original transaction ID
        // This is the transaction that was refunded
        // Some gateways provide separate refund transaction ID
        // Could add refundTransactionId field if needed

        // Save updated payment
        payment = paymentRepository.save(payment);

        // Log successful refund
        log.info("Payment {} status updated to REFUNDED", paymentId);

        // =====================================================================
        // Step 6: Convert to DTO and return
        // =====================================================================

        // Convert to DTO and return
        return mapToResponse(payment);
    }

    /**
     * Map Payment Entity to PaymentResponse DTO
     *
     * Converts internal Payment entity to external PaymentResponse DTO.
     * This separates database layer from API layer.
     *
     * Why use DTOs?
     * -------------
     * 1. Separation of Concerns:
     *    - Entity: Database representation
     *    - DTO: API representation
     *    - Can change one without changing the other
     *
     * 2. Security:
     *    - Don't expose internal entity structure
     *    - Only expose what API consumers need
     *    - Can hide sensitive fields
     *
     * 3. Versioning:
     *    - API version 1 uses PaymentResponseV1
     *    - API version 2 uses PaymentResponseV2
     *    - Same entity, different DTOs
     *
     * 4. Flexibility:
     *    - DTO can combine data from multiple entities
     *    - DTO can calculate derived fields
     *    - DTO can format data for frontend
     *
     * @param payment Payment entity
     * @return PaymentResponse DTO
     */
    private PaymentResponse mapToResponse(Payment payment) {
        // Use builder pattern for clean object creation
        return PaymentResponse.builder()
                // Map ID
                .id(payment.getId())

                // Map order ID
                .orderId(payment.getOrderId())

                // Map user ID
                .userId(payment.getUserId())

                // Map amount
                .amount(payment.getAmount())

                // Map payment method
                .paymentMethod(payment.getPaymentMethod())

                // Map status
                .status(payment.getStatus())

                // Map transaction ID (may be null)
                .transactionId(payment.getTransactionId())

                // Map failure reason (may be null)
                .failureReason(payment.getFailureReason())

                // Map timestamps
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())

                // Build the DTO
                .build();
    }

    /**
     * Simulate Payment Gateway
     *
     * PLACEHOLDER method that simulates calling a payment gateway.
     * In production, replace this with actual payment gateway integration.
     *
     * This simulation:
     * - Always succeeds for demonstration purposes
     * - Generates fake transaction ID
     * - Returns immediately (real gateways take 2-5 seconds)
     *
     * Real implementation would:
     * - Call Stripe API, PayPal API, etc.
     * - Handle card declined, insufficient funds, etc.
     * - Return actual transaction ID from gateway
     * - Handle network errors, timeouts
     *
     * @param request Payment request
     * @return Simulated transaction ID
     * @throws RuntimeException to simulate payment failure
     */
    private String simulatePaymentGateway(ProcessPaymentRequest request) {
        // Log simulation
        log.debug("SIMULATION: Processing payment via gateway");
        log.debug("Amount: {}, Method: {}, Token: {}",
            request.getAmount(), request.getPaymentMethod(), request.getPaymentToken());

        // TODO: In production, replace with actual gateway call
        // See comments in processPayment() method for Stripe example

        // Simulate different outcomes based on payment method
        // This is just for demonstration - remove in production
        switch (request.getPaymentMethod()) {
            case CREDIT_CARD:
            case DEBIT_CARD:
                // Simulate successful card payment
                // Generate fake transaction ID (Stripe format)
                String transactionId = "ch_" + System.currentTimeMillis();
                log.debug("SIMULATION: Payment successful. Transaction ID: {}", transactionId);
                return transactionId;

            case PAYPAL:
                // Simulate successful PayPal payment
                String paypalTxId = "PAY-" + System.currentTimeMillis();
                log.debug("SIMULATION: PayPal payment successful. Transaction ID: {}", paypalTxId);
                return paypalTxId;

            case BANK_TRANSFER:
                // Simulate bank transfer (usually takes days, but we'll simulate immediate)
                String bankTxId = "TRF-" + System.currentTimeMillis();
                log.debug("SIMULATION: Bank transfer initiated. Transaction ID: {}", bankTxId);
                return bankTxId;

            case CASH_ON_DELIVERY:
                // COD doesn't have transaction ID yet (payment at delivery)
                // But we'll simulate one for demonstration
                String codTxId = "COD-" + System.currentTimeMillis();
                log.debug("SIMULATION: COD order confirmed. Reference: {}", codTxId);
                return codTxId;

            default:
                // Unknown payment method
                throw new RuntimeException("Unsupported payment method: " + request.getPaymentMethod());
        }

        // To simulate payment failure for testing, uncomment this:
        // throw new RuntimeException("Card declined - insufficient funds");
    }

    /**
     * Simulate Refund Gateway
     *
     * PLACEHOLDER method that simulates calling payment gateway refund API.
     * In production, replace with actual gateway refund call.
     *
     * @param transactionId Original transaction ID
     * @param refundAmount Amount to refund
     * @throws RuntimeException to simulate refund failure
     */
    private void simulateRefundGateway(String transactionId, BigDecimal refundAmount) {
        // Log simulation
        log.debug("SIMULATION: Processing refund via gateway");
        log.debug("Transaction ID: {}, Refund Amount: {}", transactionId, refundAmount);

        // TODO: In production, replace with actual gateway refund call
        // Example with Stripe: see comments in processRefund() method

        // Simulate successful refund
        log.debug("SIMULATION: Refund successful");

        // To simulate refund failure for testing, uncomment this:
        // throw new RuntimeException("Refund failed - transaction too old");
    }
}
