package com.ecommerce.paymentservice.controller;

// Import service layer
import com.ecommerce.paymentservice.service.PaymentService;

// Import DTOs
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.dto.ProcessPaymentRequest;
import com.ecommerce.paymentservice.dto.RefundRequest;

// Import enums
import com.ecommerce.paymentservice.entity.PaymentStatus;

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
 * Payment Controller
 *
 * REST API controller for payment operations.
 * Handles HTTP requests for processing payments, refunds, and retrieving payment information.
 *
 * Base URL: /api/payments
 *
 * Available Endpoints:
 * ====================
 * 1. POST   /api/payments                    - Process a payment
 * 2. GET    /api/payments/{id}               - Get payment by ID
 * 3. GET    /api/payments/order/{orderId}    - Get payment by order ID
 * 4. GET    /api/payments/user/{userId}      - Get user's payment history
 * 5. POST   /api/payments/{id}/refund        - Process refund
 * 6. GET    /api/payments/status/{status}    - Get payments by status (admin)
 *
 * Who calls these endpoints?
 * ==========================
 * 1. Order Service:
 *    - Calls POST /api/payments to process payment when order is created
 *    - Calls GET /api/payments/order/{orderId} to check payment status
 *
 * 2. Frontend (User Interface):
 *    - Shows payment result after checkout
 *    - Displays payment history
 *    - Shows payment details on order page
 *
 * 3. Admin Panel:
 *    - Processes refunds
 *    - Views failed payments
 *    - Monitors payment statistics
 *
 * Security:
 * =========
 * All endpoints require JWT authentication (configured in SecurityConfig).
 * Some endpoints have additional role-based access control:
 * - Regular users: Can only see their own payments
 * - Admins: Can see all payments and process refunds
 *
 * HTTP Status Codes:
 * ==================
 * 200 OK: Request successful
 * 201 CREATED: Payment created successfully
 * 400 BAD REQUEST: Invalid request (validation error, business rule violation)
 * 401 UNAUTHORIZED: Not authenticated (no JWT token or invalid token)
 * 402 PAYMENT REQUIRED: Payment failed (card declined, insufficient funds, etc.)
 * 403 FORBIDDEN: Not authorized (valid token but insufficient permissions)
 * 404 NOT FOUND: Payment not found
 * 409 CONFLICT: Duplicate payment
 * 500 INTERNAL SERVER ERROR: Server error
 *
 * Request/Response Format:
 * ========================
 * All requests and responses use JSON format.
 * Content-Type: application/json
 *
 * Example Request:
 * POST /api/payments
 * {
 *   "orderId": 123,
 *   "userId": 456,
 *   "amount": 99.99,
 *   "paymentMethod": "CREDIT_CARD",
 *   "paymentToken": "tok_1234567890abcdef"
 * }
 *
 * Example Response:
 * {
 *   "id": 1,
 *   "orderId": 123,
 *   "userId": 456,
 *   "amount": 99.99,
 *   "paymentMethod": "CREDIT_CARD",
 *   "status": "COMPLETED",
 *   "transactionId": "ch_1234567890abcdef",
 *   "failureReason": null,
 *   "createdAt": "2024-01-15T10:30:00",
 *   "updatedAt": "2024-01-15T10:30:15"
 * }
 *
 * Annotations Explained:
 * ======================
 *
 * @RestController:
 * - Combines @Controller and @ResponseBody
 * - Indicates this class handles REST API requests
 * - All methods return data (JSON) not views (HTML)
 *
 * @RequestMapping("/api/payments"):
 * - Base path for all endpoints in this controller
 * - All endpoints start with /api/payments
 *
 * @RequiredArgsConstructor (Lombok):
 * - Generates constructor for final fields
 * - Enables dependency injection
 *
 * @Slf4j (Lombok):
 * - Generates logger for logging
 * - Use: log.info(), log.error(), log.debug()
 *
 * @PostMapping, @GetMapping:
 * - Shortcuts for @RequestMapping(method = POST/GET)
 * - Maps HTTP methods to Java methods
 *
 * @PathVariable:
 * - Extracts value from URL path
 * - Example: /api/payments/{id} → @PathVariable Long id
 *
 * @RequestBody:
 * - Binds HTTP request body to Java object
 * - Automatic JSON → Object conversion
 * - Works with @Valid for validation
 *
 * @Valid:
 * - Triggers validation on request DTO
 * - Uses @NotNull, @NotBlank, @Min, etc. from DTO
 * - Returns 400 BAD REQUEST if validation fails
 *
 * @PreAuthorize:
 * - Spring Security annotation
 * - Checks authorization before method execution
 * - Examples:
 *   - @PreAuthorize("hasRole('ADMIN')") - Only admins
 *   - @PreAuthorize("hasRole('USER')") - Any authenticated user
 */

// @RestController marks this as a REST API controller
@RestController

// @RequestMapping sets base path for all endpoints
@RequestMapping("/api/payments")

// @RequiredArgsConstructor generates constructor for dependency injection
@RequiredArgsConstructor

// @Slf4j generates logger
@Slf4j
public class PaymentController {

    /**
     * Payment Service
     *
     * Injected via constructor (final field + @RequiredArgsConstructor)
     * Handles all payment business logic
     */
    private final PaymentService paymentService;

    /**
     * Process Payment
     *
     * Endpoint: POST /api/payments
     * Description: Process a payment transaction for an order
     *
     * Who calls this?
     * ---------------
     * - Order Service: When customer creates an order and needs to pay
     * - Could also be called directly from frontend in some architectures
     *
     * Request Body:
     * {
     *   "orderId": 123,
     *   "userId": 456,
     *   "amount": 99.99,
     *   "paymentMethod": "CREDIT_CARD",
     *   "paymentToken": "tok_1234567890abcdef"
     * }
     *
     * Success Response (201 CREATED):
     * {
     *   "id": 1,
     *   "orderId": 123,
     *   "status": "COMPLETED",
     *   "transactionId": "ch_1234567890abcdef",
     *   ...
     * }
     *
     * Failure Response (402 PAYMENT REQUIRED):
     * {
     *   "id": 1,
     *   "orderId": 123,
     *   "status": "FAILED",
     *   "failureReason": "Your card was declined",
     *   ...
     * }
     *
     * Error Responses:
     * - 400 BAD REQUEST: Invalid request (validation errors)
     * - 409 CONFLICT: Payment already exists for this order
     *
     * @param request ProcessPaymentRequest with payment details
     * @return ResponseEntity<PaymentResponse> with payment result
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
            // @Valid triggers validation on request
            // @RequestBody binds JSON to Java object
            @Valid @RequestBody ProcessPaymentRequest request
    ) {
        // Log incoming payment request
        // Don't log sensitive data (payment token, card details)
        log.info("Received payment request for order: {}", request.getOrderId());

        // Call service to process payment
        // Service handles all business logic and gateway communication
        PaymentResponse response = paymentService.processPayment(request);

        // Log payment result
        log.info("Payment processed for order: {}. Status: {}",
            request.getOrderId(), response.getStatus());

        // Return response with appropriate HTTP status code
        // If payment completed successfully: 201 CREATED
        // If payment failed: 402 PAYMENT REQUIRED
        // If payment still processing: 202 ACCEPTED
        if (response.getStatus() == PaymentStatus.COMPLETED) {
            // Payment successful!
            // Return 201 CREATED with payment details
            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } else if (response.getStatus() == PaymentStatus.FAILED) {
            // Payment failed (card declined, etc.)
            // Return 402 PAYMENT REQUIRED with failure reason
            // Client can retry with different payment method
            return new ResponseEntity<>(response, HttpStatus.PAYMENT_REQUIRED);

        } else {
            // Payment still processing (PENDING or PROCESSING)
            // Return 202 ACCEPTED (request accepted but not completed yet)
            // Client may poll for status or wait for webhook
            return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
        }
    }

    /**
     * Get Payment by ID
     *
     * Endpoint: GET /api/payments/{id}
     * Description: Retrieve payment details by payment ID
     *
     * Who calls this?
     * ---------------
     * - Frontend: To show payment details to user
     * - Admin panel: To view payment information
     * - Customer support: To lookup payment details
     *
     * URL Example:
     * GET /api/payments/123
     *
     * Success Response (200 OK):
     * {
     *   "id": 123,
     *   "orderId": 456,
     *   "userId": 789,
     *   "amount": 99.99,
     *   "status": "COMPLETED",
     *   ...
     * }
     *
     * Error Response:
     * - 404 NOT FOUND: Payment with this ID doesn't exist
     *
     * @param id Payment ID from URL path
     * @return ResponseEntity<PaymentResponse> with payment details
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(
            // @PathVariable extracts {id} from URL path
            @PathVariable Long id
    ) {
        // Log the request
        log.info("Fetching payment with ID: {}", id);

        // Get payment from service
        // Service will throw ResourceNotFoundException if not found
        PaymentResponse response = paymentService.getPaymentById(id);

        // Return payment with 200 OK
        return ResponseEntity.ok(response);
    }

    /**
     * Get Payment by Order ID
     *
     * Endpoint: GET /api/payments/order/{orderId}
     * Description: Retrieve payment for a specific order
     *
     * Who calls this?
     * ---------------
     * - Order Service: To check if payment completed before shipping
     * - Frontend: To show payment status on order details page
     *
     * URL Example:
     * GET /api/payments/order/456
     *
     * Success Response (200 OK):
     * {
     *   "id": 123,
     *   "orderId": 456,
     *   "status": "COMPLETED",
     *   ...
     * }
     *
     * Error Response:
     * - 404 NOT FOUND: No payment found for this order
     *
     * @param orderId Order ID from URL path
     * @return ResponseEntity<PaymentResponse> with payment details
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(
            // @PathVariable extracts {orderId} from URL
            @PathVariable Long orderId
    ) {
        // Log the request
        log.info("Fetching payment for order ID: {}", orderId);

        // Get payment from service
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);

        // Return payment with 200 OK
        return ResponseEntity.ok(response);
    }

    /**
     * Get User Payment History
     *
     * Endpoint: GET /api/payments/user/{userId}
     * Description: Retrieve all payments made by a user
     *
     * Who calls this?
     * ---------------
     * - Frontend: For "My Payments" or "Transaction History" page
     * - Admin panel: To view user's payment history
     * - Customer support: To help with payment inquiries
     *
     * URL Example:
     * GET /api/payments/user/789
     *
     * Success Response (200 OK):
     * [
     *   {
     *     "id": 123,
     *     "orderId": 456,
     *     "amount": 99.99,
     *     "status": "COMPLETED",
     *     "createdAt": "2024-01-15T10:30:00",
     *     ...
     *   },
     *   {
     *     "id": 124,
     *     "orderId": 457,
     *     "amount": 49.99,
     *     "status": "REFUNDED",
     *     "createdAt": "2024-01-10T14:20:00",
     *     ...
     *   }
     * ]
     *
     * Returns empty array if user has no payments.
     *
     * Security Note:
     * --------------
     * In production, should verify that authenticated user can only
     * view their own payment history (unless admin).
     *
     * Example check:
     * String email = authentication.getName();
     * Long authenticatedUserId = userService.getUserIdByEmail(email);
     * if (!authenticatedUserId.equals(userId) && !hasRole("ADMIN")) {
     *     throw new ForbiddenException("Cannot view other users' payments");
     * }
     *
     * @param userId User ID from URL path
     * @return ResponseEntity<List<PaymentResponse>> with payment history
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentResponse>> getUserPaymentHistory(
            // @PathVariable extracts {userId} from URL
            @PathVariable Long userId
    ) {
        // Log the request
        log.info("Fetching payment history for user ID: {}", userId);

        // TODO: In production, add security check here
        // Verify authenticated user can only see their own payments

        // Get payment history from service
        List<PaymentResponse> payments = paymentService.getUserPaymentHistory(userId);

        // Log how many payments found
        log.info("Found {} payments for user ID: {}", payments.size(), userId);

        // Return payments with 200 OK
        // Returns empty list if no payments (not 404)
        return ResponseEntity.ok(payments);
    }

    /**
     * Process Refund
     *
     * Endpoint: POST /api/payments/{id}/refund
     * Description: Process a refund for a completed payment
     *
     * Who calls this?
     * ---------------
     * - Admin panel: When admin approves a return/refund
     * - Order Service: When order is cancelled after payment
     *
     * Security:
     * ---------
     * Only admins can process refunds (@PreAuthorize("hasRole('ADMIN')"))
     * Regular users cannot refund their own payments.
     *
     * URL Example:
     * POST /api/payments/123/refund
     *
     * Request Body (Full Refund):
     * {
     *   "reason": "Customer returned product - defective"
     * }
     *
     * Request Body (Partial Refund):
     * {
     *   "amount": 49.99,
     *   "reason": "Partial refund - one item returned"
     * }
     *
     * Success Response (200 OK):
     * {
     *   "id": 123,
     *   "orderId": 456,
     *   "amount": 99.99,
     *   "status": "REFUNDED",
     *   ...
     * }
     *
     * Error Responses:
     * - 400 BAD REQUEST: Cannot refund (wrong status, invalid amount, etc.)
     * - 403 FORBIDDEN: Not an admin
     * - 404 NOT FOUND: Payment doesn't exist
     *
     * @param id Payment ID from URL path
     * @param request RefundRequest with optional amount and reason
     * @return ResponseEntity<PaymentResponse> with updated payment
     */
    @PostMapping("/{id}/refund")
    // Only admins can process refunds
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> processRefund(
            // Payment ID from URL
            @PathVariable Long id,

            // Refund request with amount and reason
            @Valid @RequestBody RefundRequest request
    ) {
        // Log refund request (admin action)
        log.info("Admin processing refund for payment ID: {}", id);
        log.info("Refund reason: {}", request.getReason());

        // Process refund via service
        // Service handles validation, gateway call, and database update
        PaymentResponse response = paymentService.processRefund(id, request);

        // Log successful refund
        log.info("Refund processed successfully for payment ID: {}", id);

        // Return updated payment with 200 OK
        return ResponseEntity.ok(response);
    }

    /**
     * Get Payments by Status (Admin)
     *
     * Endpoint: GET /api/payments/status/{status}
     * Description: Retrieve all payments with a specific status
     *
     * Who calls this?
     * ---------------
     * - Admin panel: For monitoring and reporting
     * - Analytics: For dashboard statistics
     *
     * Security:
     * ---------
     * Only admins can view payments by status (@PreAuthorize("hasRole('ADMIN')"))
     *
     * URL Examples:
     * GET /api/payments/status/COMPLETED  - All successful payments
     * GET /api/payments/status/FAILED     - All failed payments
     * GET /api/payments/status/PENDING    - All pending payments
     * GET /api/payments/status/REFUNDED   - All refunded payments
     *
     * Success Response (200 OK):
     * [
     *   {
     *     "id": 123,
     *     "orderId": 456,
     *     "status": "FAILED",
     *     "failureReason": "Card declined",
     *     ...
     *   },
     *   {
     *     "id": 124,
     *     "orderId": 457,
     *     "status": "FAILED",
     *     "failureReason": "Insufficient funds",
     *     ...
     *   }
     * ]
     *
     * Returns empty array if no payments with this status.
     *
     * Valid status values:
     * - PENDING: Payment initiated but not processed
     * - PROCESSING: Payment being processed by gateway
     * - COMPLETED: Payment successful
     * - FAILED: Payment failed
     * - REFUNDED: Payment refunded
     *
     * @param status Payment status from URL path
     * @return ResponseEntity<List<PaymentResponse>> with matching payments
     */
    @GetMapping("/status/{status}")
    // Only admins can view payments by status
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByStatus(
            // @PathVariable extracts {status} from URL
            // Automatically converted from String to PaymentStatus enum
            @PathVariable PaymentStatus status
    ) {
        // Log the request
        log.info("Admin fetching payments with status: {}", status);

        // Get payments from service
        List<PaymentResponse> payments = paymentService.getPaymentsByStatus(status);

        // Log how many payments found
        log.info("Found {} payments with status: {}", payments.size(), status);

        // Return payments with 200 OK
        return ResponseEntity.ok(payments);
    }

    /**
     * Additional Endpoints (Could be added)
     * ======================================
     *
     * 1. Get Payment Statistics (Admin):
     * @GetMapping("/stats")
     * @PreAuthorize("hasRole('ADMIN')")
     * public ResponseEntity<PaymentStatsDTO> getPaymentStatistics() {
     *     // Total revenue, success rate, average transaction, etc.
     * }
     *
     * 2. Retry Failed Payment:
     * @PostMapping("/{id}/retry")
     * public ResponseEntity<PaymentResponse> retryPayment(@PathVariable Long id) {
     *     // Attempt to process failed payment again
     * }
     *
     * 3. Cancel Pending Payment:
     * @PostMapping("/{id}/cancel")
     * public ResponseEntity<Void> cancelPayment(@PathVariable Long id) {
     *     // Cancel a pending payment
     * }
     *
     * 4. Webhook from Payment Gateway:
     * @PostMapping("/webhook")
     * public ResponseEntity<Void> handleWebhook(@RequestBody StripeEvent event) {
     *     // Handle payment gateway webhooks (Stripe, PayPal, etc.)
     *     // Update payment status based on gateway notification
     * }
     *
     * 5. Get Payment by Transaction ID:
     * @GetMapping("/transaction/{transactionId}")
     * public ResponseEntity<PaymentResponse> getByTransactionId(
     *     @PathVariable String transactionId
     * ) {
     *     // Lookup payment by gateway transaction ID
     *     // Useful for reconciliation and webhook processing
     * }
     */
}
