package com.ecommerce.paymentservice.dto;

// Import Lombok annotations
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Import payment enums
import com.ecommerce.paymentservice.entity.PaymentMethod;
import com.ecommerce.paymentservice.entity.PaymentStatus;

// Import BigDecimal for monetary values
import java.math.BigDecimal;

// Import time class
import java.time.LocalDateTime;

/**
 * Payment Response DTO
 *
 * Returned when retrieving payment information or after processing a payment.
 * Contains complete payment transaction details.
 *
 * When is this returned?
 * ======================
 * 1. After processing payment:
 *    POST /api/payments → PaymentResponse
 *    - Shows payment result (success/failure)
 *    - Contains transaction ID if successful
 *    - Contains failure reason if failed
 *
 * 2. When retrieving payment:
 *    GET /api/payments/{id} → PaymentResponse
 *    GET /api/payments/order/{orderId} → PaymentResponse
 *    - Shows current payment status
 *    - Shows complete payment history
 *
 * 3. After refund:
 *    POST /api/payments/{id}/refund → PaymentResponse
 *    - Shows updated status (REFUNDED)
 *    - Contains original payment details
 *
 * 4. In payment history:
 *    GET /api/payments/user/{userId} → List<PaymentResponse>
 *    - Shows all user's payments
 *    - For transaction history page
 *
 * Example JSON response (successful payment):
 * ============================================
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
 * Example JSON response (failed payment):
 * ========================================
 * {
 *   "id": 2,
 *   "orderId": 124,
 *   "userId": 456,
 *   "amount": 49.99,
 *   "paymentMethod": "CREDIT_CARD",
 *   "status": "FAILED",
 *   "transactionId": null,
 *   "failureReason": "Your card was declined",
 *   "createdAt": "2024-01-15T11:00:00",
 *   "updatedAt": "2024-01-15T11:00:05"
 * }
 *
 * What Order Service does with this response:
 * ============================================
 * 1. Successful payment (status = COMPLETED):
 *    - Update order status to CONFIRMED
 *    - Store payment ID in order
 *    - Send confirmation email
 *    - Notify Inventory Service to reserve items
 *
 * 2. Failed payment (status = FAILED):
 *    - Keep order status as PENDING
 *    - Show error message to customer
 *    - Allow retry with different payment method
 *    - Log failure for analytics
 *
 * 3. Pending payment (status = PENDING/PROCESSING):
 *    - Show "Processing..." message to customer
 *    - May poll for status update
 *    - Or wait for webhook notification
 */
@Data               // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: no-arg constructor
@AllArgsConstructor // Lombok: all-args constructor
@Builder            // Lombok: builder pattern for clean object creation
public class PaymentResponse {

    /**
     * Payment ID
     *
     * Unique identifier for this payment record.
     * Database-generated value.
     *
     * Usage:
     * - Get payment details: GET /api/payments/{id}
     * - Process refund: POST /api/payments/{id}/refund
     * - Link payment to order
     *
     * Example: 1
     */
    private Long id;

    /**
     * Order ID
     *
     * Reference to the order this payment is for.
     * Links back to Order Service.
     *
     * Order Service uses this to:
     * - Verify payment is for correct order
     * - Update order status
     * - Show payment info on order details page
     *
     * Example: 123 (order #123)
     */
    private Long orderId;

    /**
     * User ID
     *
     * Reference to the user who made this payment.
     * Links back to User Service.
     *
     * User Service uses this to:
     * - Show payment history to user
     * - Verify payment ownership
     * - Generate financial reports for user
     *
     * Example: 456 (user #456)
     */
    private Long userId;

    /**
     * Amount
     *
     * Total payment amount.
     *
     * What this represents:
     * - Amount charged to customer's payment method
     * - In base currency (e.g., USD)
     * - Should match order total
     *
     * Display to customer:
     * - "You were charged $99.99"
     * - On invoice/receipt
     * - In payment history
     *
     * Example: 99.99 (represents $99.99)
     */
    private BigDecimal amount;

    /**
     * Payment Method
     *
     * How the customer paid.
     *
     * Values:
     * - CREDIT_CARD: Paid with credit card
     * - DEBIT_CARD: Paid with debit card
     * - PAYPAL: Paid via PayPal
     * - BANK_TRANSFER: Bank transfer
     * - CASH_ON_DELIVERY: Will pay on delivery
     *
     * Display to customer:
     * - "Payment Method: Credit Card"
     * - On order confirmation
     * - In email receipts
     *
     * Example: CREDIT_CARD
     *
     * JSON representation: "CREDIT_CARD"
     */
    private PaymentMethod paymentMethod;

    /**
     * Status
     *
     * Current status of the payment.
     *
     * Values and meanings:
     * --------------------
     * PENDING:
     * - Payment initiated but not processed yet
     * - Customer: "Your payment is being processed..."
     * - Action: Wait for processing to complete
     *
     * PROCESSING:
     * - Payment is being processed by payment gateway
     * - Customer: "Processing your payment..."
     * - Action: Wait for gateway response
     *
     * COMPLETED:
     * - Payment successful, funds captured
     * - Customer: "Payment successful! ✓"
     * - Action: Proceed with order fulfillment
     * - Order status: Update to CONFIRMED
     *
     * FAILED:
     * - Payment failed (card declined, insufficient funds, etc.)
     * - Customer: "Payment failed: [reason]"
     * - Action: Allow retry with different payment method
     * - Order status: Keep as PENDING
     *
     * REFUNDED:
     * - Payment was refunded to customer
     * - Customer: "Refund processed"
     * - Action: None (terminal state)
     * - Order status: Usually CANCELLED
     *
     * Frontend handling:
     * ------------------
     * if (payment.status === 'COMPLETED') {
     *     showSuccessMessage("Payment successful!");
     *     redirectToOrderConfirmation();
     * } else if (payment.status === 'FAILED') {
     *     showErrorMessage(payment.failureReason);
     *     showRetryButton();
     * } else if (payment.status === 'PROCESSING') {
     *     showLoadingSpinner();
     *     pollForUpdates();
     * }
     *
     * Example: COMPLETED
     */
    private PaymentStatus status;

    /**
     * Transaction ID
     *
     * Unique identifier from payment gateway.
     * Proof of transaction.
     *
     * Format by gateway:
     * ------------------
     * Stripe: "ch_1234567890abcdefghijklmn"
     * PayPal: "PAY-1234567890ABCDEFGHIJKLMN"
     * Square: "sq_1234567890abcdef"
     *
     * When is this null?
     * ------------------
     * - Payment is PENDING (not processed yet)
     * - Payment is PROCESSING (gateway hasn't responded)
     * - Payment FAILED (no transaction created)
     * - CASH_ON_DELIVERY (no online transaction)
     *
     * When is this not null?
     * ----------------------
     * - Payment COMPLETED (successful transaction)
     * - Payment REFUNDED (original transaction ID preserved)
     *
     * What can you do with transaction ID?
     * ------------------------------------
     * 1. Process refund:
     *    - Need transaction ID to refund via gateway
     *    - stripe.refunds.create(transactionId)
     *
     * 2. Dispute resolution:
     *    - Customer disputes charge
     *    - Use transaction ID to look up in gateway
     *    - Provide evidence to gateway
     *
     * 3. Reconciliation:
     *    - Match against bank deposits
     *    - Match against gateway reports
     *    - Accounting and bookkeeping
     *
     * 4. Customer support:
     *    - Customer calls about charge
     *    - Look up transaction in gateway
     *    - Verify charge details
     *
     * Display to customer:
     * - "Transaction ID: ch_1234567890abcdef"
     * - On receipt/invoice
     * - For their records
     * - For disputes
     *
     * Example: "ch_1234567890abcdef"
     */
    private String transactionId;

    /**
     * Failure Reason
     *
     * Explanation of why payment failed (if status is FAILED).
     * Null if payment didn't fail.
     *
     * Common failure reasons:
     * -----------------------
     * Card declined:
     * - "Your card was declined"
     * - "Card declined by issuing bank"
     * - Customer action: Try different card
     *
     * Insufficient funds:
     * - "Insufficient funds"
     * - "Your card has insufficient funds"
     * - Customer action: Add money or use different card
     *
     * Invalid card:
     * - "Your card number is incorrect"
     * - "Your card has expired"
     * - Customer action: Check card details
     *
     * Incorrect CVV:
     * - "Your card's security code is incorrect"
     * - Customer action: Re-enter CVV
     *
     * Fraud/Risk:
     * - "This transaction cannot be processed"
     * - "Card flagged for fraud prevention"
     * - Customer action: Contact bank or use different card
     *
     * Gateway error:
     * - "Payment processing error, please try again"
     * - "Connection timeout"
     * - Customer action: Retry
     *
     * Display to customer:
     * --------------------
     * Don't show raw technical errors:
     * - Bad: "card_declined_insufficient_funds_INSUFFICIENT_BALANCE"
     * - Good: "Your card has insufficient funds. Please try a different card."
     *
     * Frontend should have user-friendly message mapping:
     * const friendlyMessages = {
     *     'card_declined': 'Your card was declined. Please try another card.',
     *     'insufficient_funds': 'Insufficient funds. Please use a different card.',
     *     'incorrect_cvc': 'Incorrect security code. Please check and try again.'
     * };
     *
     * Usage in UI:
     * ------------
     * if (payment.status === 'FAILED') {
     *     showErrorAlert({
     *         title: "Payment Failed",
     *         message: payment.failureReason,
     *         action: "Try Again"
     *     });
     * }
     *
     * Analytics:
     * ----------
     * Track failure reasons to:
     * - Identify common issues
     * - Improve checkout process
     * - Reduce payment failures
     * - Optimize payment flow
     *
     * Example: "Your card was declined"
     */
    private String failureReason;

    /**
     * Created At
     *
     * When this payment was created.
     * Timestamp of payment initiation.
     *
     * Format: ISO 8601 date-time
     * Example: "2024-01-15T10:30:00"
     *
     * Display to customer:
     * - "Payment Date: January 15, 2024 at 10:30 AM"
     * - On receipt/invoice
     * - In payment history
     *
     * Usage:
     * - Sort payment history (newest first)
     * - Filter payments by date range
     * - Calculate payment age
     * - Audit trail
     */
    private LocalDateTime createdAt;

    /**
     * Updated At
     *
     * When this payment was last modified.
     * Timestamp of last status change.
     *
     * Format: ISO 8601 date-time
     * Example: "2024-01-15T10:30:15"
     *
     * Uses:
     * - Track when status changed
     * - Debugging (how long was payment processing?)
     * - Audit trail
     *
     * Example scenario:
     * - createdAt: 2024-01-15T10:30:00 (payment initiated)
     * - updatedAt: 2024-01-15T10:30:15 (payment completed)
     * - Processing time: 15 seconds
     */
    private LocalDateTime updatedAt;

    /**
     * Additional Information (Not Included)
     * ======================================
     *
     * What's NOT in this response and why:
     *
     * 1. Payment token:
     *    - Security: Never return payment tokens to client
     *    - Single-use: Token already used for processing
     *    - Privacy: Tokens represent sensitive payment data
     *
     * 2. Full card number:
     *    - PCI compliance: Never store or return full card numbers
     *    - Security: Risk of theft
     *    - Could add last 4 digits: "****1234"
     *
     * 3. CVV:
     *    - PCI compliance: NEVER store or return CVV
     *    - Security: CVV must never be stored anywhere
     *
     * 4. Raw gateway response:
     *    - Too technical for client
     *    - May contain sensitive data
     *    - Store separately for debugging
     *
     * Future enhancements:
     * --------------------
     * 1. Last 4 digits of card:
     *    private String cardLast4;  // "****1234"
     *
     * 2. Card brand:
     *    private String cardBrand;  // "Visa", "Mastercard"
     *
     * 3. Currency:
     *    private String currency;  // "USD", "EUR"
     *
     * 4. Gateway name:
     *    private String gateway;  // "Stripe", "PayPal"
     *
     * 5. Receipt URL:
     *    private String receiptUrl;  // Link to detailed receipt
     */

    /**
     * Example Usage in Service
     * =========================
     *
     * Converting Payment entity to PaymentResponse:
     *
     * public PaymentResponse mapToResponse(Payment payment) {
     *     return PaymentResponse.builder()
     *         .id(payment.getId())
     *         .orderId(payment.getOrderId())
     *         .userId(payment.getUserId())
     *         .amount(payment.getAmount())
     *         .paymentMethod(payment.getPaymentMethod())
     *         .status(payment.getStatus())
     *         .transactionId(payment.getTransactionId())
     *         .failureReason(payment.getFailureReason())
     *         .createdAt(payment.getCreatedAt())
     *         .updatedAt(payment.getUpdatedAt())
     *         .build();
     * }
     *
     * Example Usage in Controller
     * ============================
     *
     * @PostMapping
     * public ResponseEntity<PaymentResponse> processPayment(
     *     @Valid @RequestBody ProcessPaymentRequest request
     * ) {
     *     PaymentResponse response = paymentService.processPayment(request);
     *
     *     // Return 201 CREATED for successful payment
     *     if (response.getStatus() == PaymentStatus.COMPLETED) {
     *         return ResponseEntity.status(HttpStatus.CREATED).body(response);
     *     }
     *
     *     // Return 402 PAYMENT_REQUIRED for failed payment
     *     else if (response.getStatus() == PaymentStatus.FAILED) {
     *         return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
     *     }
     *
     *     // Return 202 ACCEPTED for processing payment
     *     else {
     *         return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
     *     }
     * }
     */
}
