package com.ecommerce.paymentservice.dto;

// Import validation annotations
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

// Import Lombok annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Import BigDecimal for monetary values
import java.math.BigDecimal;

/**
 * Refund Request DTO
 *
 * Used when processing a refund for a completed payment.
 * Contains information needed to refund a payment.
 *
 * When is this used?
 * ==================
 * 1. Customer returns product:
 *    - Admin approves return
 *    - Admin processes refund
 *
 * 2. Order cancelled after payment:
 *    - Customer cancels order
 *    - Payment already completed
 *    - Need to refund
 *
 * 3. Duplicate payment:
 *    - Same order charged twice by mistake
 *    - Refund the duplicate
 *
 * 4. Service issue:
 *    - Product damaged/defective
 *    - Service not delivered
 *    - Customer compensation
 *
 * API Endpoint:
 * =============
 * POST /api/payments/{id}/refund
 * Body: RefundRequest
 * Response: PaymentResponse (with status REFUNDED)
 *
 * Example JSON request (full refund):
 * ====================================
 * {
 *   "reason": "Customer returned product - defective"
 * }
 *
 * Example JSON request (partial refund):
 * =======================================
 * {
 *   "amount": 25.00,
 *   "reason": "Partial refund - one item returned out of two"
 * }
 *
 * Refund Types:
 * =============
 * 1. Full Refund:
 *    - Don't specify amount (or amount = original payment amount)
 *    - Entire payment refunded
 *    - Most common case
 *
 * 2. Partial Refund:
 *    - Specify amount < original payment amount
 *    - Some items returned, not all
 *    - Example: Order $100, return $30 worth of items
 *
 * Refund Process:
 * ===============
 * 1. Admin sends RefundRequest with reason
 * 2. Service validates:
 *    - Payment exists
 *    - Payment status is COMPLETED (can only refund completed payments)
 *    - Refund amount <= original payment amount
 *    - Payment has transaction ID (needed for gateway refund)
 * 3. Service calls payment gateway API:
 *    - Stripe: stripe.refunds.create(transactionId, amount)
 *    - PayPal: paypal.refund(transactionId, amount)
 * 4. Gateway processes refund
 * 5. Gateway returns refund confirmation
 * 6. Service updates payment status to REFUNDED
 * 7. Service returns PaymentResponse
 * 8. Order Service updates order status to CANCELLED
 *
 * Refund Timeline:
 * ================
 * - Credit card: 5-10 business days to customer's account
 * - Debit card: 5-10 business days
 * - PayPal: Instant to PayPal balance, 3-5 days to bank
 * - Bank transfer: 3-5 business days
 *
 * Important Notes:
 * ================
 * 1. Transaction fees:
 *    - Payment gateway fees usually NOT refunded
 *    - Example: Stripe charges 2.9% + $0.30
 *    - Customer gets full refund
 *    - Merchant loses fee
 *
 * 2. Refund window:
 *    - Most gateways allow refunds within 90-180 days
 *    - After that, need to process manual refund
 *
 * 3. Partial refunds:
 *    - Can do multiple partial refunds up to original amount
 *    - Example: $100 payment → $30 refund → $40 refund → $30 refund
 *    - Total refunded cannot exceed original amount
 *
 * 4. Already refunded:
 *    - Cannot refund the same payment twice
 *    - Service checks payment status before refunding
 */
@Data               // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: no-arg constructor for Jackson deserialization
@AllArgsConstructor // Lombok: all-args constructor for testing
public class RefundRequest {

    /**
     * Refund Amount
     *
     * Amount to refund (optional).
     * If not specified, full payment amount is refunded.
     *
     * Validations:
     * - @DecimalMin: If specified, must be greater than 0
     * - Not @NotNull: Optional field (null = full refund)
     *
     * Business validations (in service layer):
     * - Must be <= original payment amount
     * - Must be > 0 if specified
     * - Cannot refund more than original payment
     *
     * Full refund example:
     * - Original payment: $99.99
     * - amount = null (or $99.99)
     * - Refund: $99.99
     *
     * Partial refund example:
     * - Original payment: $99.99
     * - amount = $49.99
     * - Refund: $49.99
     * - Customer keeps some items
     *
     * Multiple partial refunds (advanced):
     * - Original payment: $100.00
     * - First refund: $30.00 (one item returned)
     * - Second refund: $20.00 (another item returned)
     * - Total refunded: $50.00
     * - Remaining: $50.00
     * Note: This implementation doesn't track partial refunds
     *       Each payment can only be refunded once (status = REFUNDED)
     *       For multiple partials, need refund history table
     *
     * Gateway considerations:
     * - Stripe: Supports partial refunds
     * - PayPal: Supports partial refunds
     * - Most gateways support partial refunds
     *
     * Example: 25.00 (refund $25.00 out of original payment)
     * Example: null (refund full amount)
     */
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than 0")
    private BigDecimal amount;

    /**
     * Reason
     *
     * Explanation for the refund.
     * Required for record-keeping and analytics.
     *
     * Validations:
     * - @NotBlank: Cannot be empty or just whitespace
     * - Must provide reason for audit trail
     *
     * Common refund reasons:
     * ----------------------
     * Customer-initiated:
     * - "Customer returned product - changed mind"
     * - "Customer returned product - wrong size"
     * - "Customer cancelled order"
     *
     * Quality issues:
     * - "Product defective"
     * - "Product damaged in shipping"
     * - "Wrong item shipped"
     * - "Product not as described"
     *
     * Service issues:
     * - "Service not delivered"
     * - "Delayed delivery - customer compensation"
     * - "Poor service - customer complaint"
     *
     * Merchant errors:
     * - "Duplicate charge"
     * - "Incorrect price charged"
     * - "Item out of stock after payment"
     *
     * Fraud/Risk:
     * - "Fraudulent transaction"
     * - "Chargeback prevention"
     *
     * Why reason is important:
     * ------------------------
     * 1. Audit trail:
     *    - Track why money was refunded
     *    - Accounting and bookkeeping
     *    - Financial audits
     *
     * 2. Analytics:
     *    - Identify common refund reasons
     *    - Improve product quality
     *    - Improve product descriptions
     *    - Reduce returns
     *
     * 3. Dispute resolution:
     *    - If customer disputes refund
     *    - Have record of why refund was issued
     *    - Prove refund was legitimate
     *
     * 4. Customer support:
     *    - Customer calls about refund
     *    - Look up reason in system
     *    - Explain to customer
     *
     * Display in admin panel:
     * -----------------------
     * Payment ID: 123
     * Order ID: 456
     * Original Amount: $99.99
     * Refund Amount: $99.99
     * Refund Reason: "Customer returned product - defective"
     * Refunded By: admin@example.com
     * Refund Date: 2024-01-15 14:30:00
     *
     * Example: "Customer returned product - defective"
     */
    @NotBlank(message = "Refund reason is required")
    private String reason;

    /**
     * Additional Fields (Not Implemented)
     * ====================================
     *
     * For a more advanced refund system, could add:
     *
     * 1. Refund type:
     *    private RefundType type;  // FULL, PARTIAL
     *
     * 2. Return tracking:
     *    private String returnTrackingNumber;
     *    // Track physical return of product
     *
     * 3. Refund method:
     *    private RefundMethod method;  // ORIGINAL_PAYMENT, STORE_CREDIT, BANK_TRANSFER
     *    // Where to send refund (usually original payment method)
     *
     * 4. Notes:
     *    private String notes;
     *    // Internal notes (not shown to customer)
     *
     * 5. Attachments:
     *    private List<String> attachmentUrls;
     *    // Photos of defective product, etc.
     */

    /**
     * Example Usage
     * =============
     *
     * Full refund:
     * ------------
     * RefundRequest request = new RefundRequest();
     * request.setReason("Customer returned product - defective");
     * // amount is null → full refund
     *
     * POST /api/payments/123/refund
     * Body: {
     *   "reason": "Customer returned product - defective"
     * }
     *
     * Partial refund:
     * ---------------
     * RefundRequest request = new RefundRequest();
     * request.setAmount(new BigDecimal("49.99"));
     * request.setReason("Partial refund - one item returned out of two");
     *
     * POST /api/payments/123/refund
     * Body: {
     *   "amount": 49.99,
     *   "reason": "Partial refund - one item returned out of two"
     * }
     *
     * Service validation example:
     * ---------------------------
     * public PaymentResponse processRefund(Long paymentId, RefundRequest request) {
     *     // Get payment
     *     Payment payment = paymentRepository.findById(paymentId)
     *         .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
     *
     *     // Validate status
     *     if (payment.getStatus() != PaymentStatus.COMPLETED) {
     *         throw new BadRequestException("Can only refund completed payments");
     *     }
     *
     *     // Validate amount
     *     BigDecimal refundAmount = request.getAmount();
     *     if (refundAmount == null) {
     *         refundAmount = payment.getAmount();  // Full refund
     *     }
     *     if (refundAmount.compareTo(payment.getAmount()) > 0) {
     *         throw new BadRequestException("Refund amount cannot exceed payment amount");
     *     }
     *
     *     // Process refund via gateway
     *     String transactionId = payment.getTransactionId();
     *     Refund refund = stripe.refunds.create(transactionId, refundAmount);
     *
     *     // Update payment
     *     payment.setStatus(PaymentStatus.REFUNDED);
     *     payment = paymentRepository.save(payment);
     *
     *     return mapToResponse(payment);
     * }
     */
}
