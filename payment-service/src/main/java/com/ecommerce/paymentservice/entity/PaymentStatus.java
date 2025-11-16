package com.ecommerce.paymentservice.entity;

/**
 * Payment Status Enum
 *
 * Represents the current status of a payment transaction.
 * Tracks the payment lifecycle from initiation to completion or failure.
 *
 * Why use enum instead of String?
 * ================================
 * 1. Type Safety:
 *    - Compiler ensures only valid values used
 *    - status = "PROCESING" (typo) would compile with String
 *    - status = PaymentStatus.PROCESING (typo) won't compile with enum
 *
 * 2. Code Completion:
 *    - IDE shows all possible values
 *    - Prevents mistakes and reduces bugs
 *
 * 3. Refactoring:
 *    - Easy to rename values across entire codebase
 *    - Find all usages easily
 *
 * 4. Documentation:
 *    - All possible values defined in one place
 *    - Self-documenting code
 *
 * 5. Database Storage:
 *    - JPA stores enum as String in database by default (@Enumerated(EnumType.STRING))
 *    - Can also store as integer ordinal (@Enumerated(EnumType.ORDINAL))
 *    - STRING is preferred (readable in database, safer for changes)
 *
 * Payment Lifecycle
 * =================
 *
 * Normal Flow:
 * ------------
 * PENDING → PROCESSING → COMPLETED
 *
 * Failed Flow:
 * ------------
 * PENDING → PROCESSING → FAILED
 *
 * Refund Flow:
 * ------------
 * COMPLETED → REFUNDED
 *
 * Status Transition Rules
 * =======================
 *
 * From PENDING:
 * - Can transition to: PROCESSING, FAILED
 * - Cannot transition to: COMPLETED, REFUNDED
 * - Reason: Must go through processing before completion
 *
 * From PROCESSING:
 * - Can transition to: COMPLETED, FAILED
 * - Cannot transition to: PENDING, REFUNDED
 * - Reason: Processing is in progress, must complete or fail
 *
 * From COMPLETED:
 * - Can transition to: REFUNDED
 * - Cannot transition to: PENDING, PROCESSING, FAILED
 * - Reason: Completed payments can only be refunded
 *
 * From FAILED:
 * - No transitions allowed (terminal state)
 * - Customer must create new payment
 * - Reason: Failed payments cannot be retried, prevents duplicate charges
 *
 * From REFUNDED:
 * - No transitions allowed (terminal state)
 * - Cannot be re-completed or re-refunded
 * - Reason: Refund is final
 */
public enum PaymentStatus {

    /**
     * PENDING
     *
     * Payment has been initiated but not yet processing.
     *
     * When this status is set:
     * - Order Service creates payment request
     * - Payment record created in database
     * - Customer information validated
     * - Payment amount calculated
     *
     * What happens next:
     * - Payment gateway is called
     * - Status changes to PROCESSING
     *
     * Duration:
     * - Typically < 1 second
     * - Should not stay PENDING long
     *
     * User experience:
     * - "Processing your payment..."
     * - Loading spinner shown
     *
     * Example scenario:
     * - Customer clicks "Place Order" button
     * - Order created with PENDING status
     * - Payment created with PENDING status
     * - Payment gateway API is about to be called
     */
    PENDING,

    /**
     * PROCESSING
     *
     * Payment is being processed by payment gateway.
     *
     * When this status is set:
     * - Payment gateway API called (Stripe, PayPal, etc.)
     * - Card authorization in progress
     * - Fraud checks running
     * - 3D Secure verification if required
     *
     * What happens next:
     * - Gateway returns success → COMPLETED
     * - Gateway returns failure → FAILED
     *
     * Duration:
     * - Typically 2-5 seconds
     * - Can be longer for 3D Secure (30-60 seconds)
     * - Bank transfer: minutes to hours
     *
     * User experience:
     * - "Processing payment with [payment method]..."
     * - Should not refresh or close browser
     * - May show 3D Secure popup
     *
     * Example scenario:
     * - Payment gateway API called: stripe.charges.create()
     * - Waiting for response from Stripe
     * - Customer may be prompted for 3D Secure
     */
    PROCESSING,

    /**
     * COMPLETED
     *
     * Payment successfully processed and funds captured.
     *
     * When this status is set:
     * - Payment gateway confirmed success
     * - Funds captured or authorized
     * - Transaction ID received from gateway
     * - Payment record updated with confirmation
     *
     * What this means:
     * - Money has been charged to customer's payment method
     * - Order can proceed to fulfillment
     * - Invoice can be generated
     *
     * What happens next:
     * - Order Service updates order status to CONFIRMED
     * - Inventory Service reduces stock
     * - Notification Service sends confirmation email
     * - Order proceeds to shipping
     *
     * Can transition to REFUNDED:
     * - Customer requests refund
     * - Admin processes refund
     * - Order cancelled after payment
     *
     * User experience:
     * - "Payment successful!"
     * - Order confirmation page shown
     * - Confirmation email sent
     * - Receipt generated
     *
     * Example scenario:
     * - Stripe returns success response
     * - Transaction ID: ch_1234567890abcdef
     * - Payment marked as COMPLETED
     * - Order status updated to CONFIRMED
     * - Customer sees "Thank you for your order!"
     */
    COMPLETED,

    /**
     * FAILED
     *
     * Payment processing failed.
     *
     * When this status is set:
     * - Payment gateway returned error
     * - Card declined
     * - Insufficient funds
     * - Invalid card information
     * - Fraud check failed
     * - Gateway timeout
     *
     * Common failure reasons:
     * - Card declined: Issuing bank rejected transaction
     * - Insufficient funds: Not enough money in account
     * - Invalid card: Wrong number, expired, or invalid CVV
     * - Fraud: Transaction flagged as suspicious
     * - Gateway error: Payment gateway down or timeout
     * - 3D Secure failed: Customer didn't complete authentication
     *
     * What happens next:
     * - Order remains in PENDING status
     * - Customer notified of failure
     * - Customer can retry with different payment method
     * - New payment record created for retry
     *
     * This is a terminal state:
     * - No status transitions allowed
     * - Prevents accidental re-processing
     * - Keeps audit trail of failed attempts
     *
     * User experience:
     * - "Payment failed: [reason]"
     * - "Please try a different payment method"
     * - Option to retry with same or different card
     * - Contact support if issue persists
     *
     * Example scenarios:
     * 1. Card Declined:
     *    - Stripe error: card_declined
     *    - Message: "Your card was declined"
     *    - Customer tries different card
     *
     * 2. Insufficient Funds:
     *    - Error: insufficient_funds
     *    - Message: "Insufficient funds in account"
     *    - Customer adds money or uses different card
     *
     * 3. Invalid CVV:
     *    - Error: incorrect_cvc
     *    - Message: "Incorrect security code"
     *    - Customer re-enters correct CVV
     *
     * Analytics:
     * - Track failed payment rate
     * - Identify common failure reasons
     * - Optimize checkout process
     * - Reduce cart abandonment
     */
    FAILED,

    /**
     * REFUNDED
     *
     * Payment has been refunded to customer.
     *
     * When this status is set:
     * - Admin processed refund request
     * - Refund API called on payment gateway
     * - Gateway confirmed refund success
     * - Funds returned to customer's payment method
     *
     * Refund types:
     * 1. Full Refund:
     *    - Entire payment amount returned
     *    - Order fully cancelled
     *
     * 2. Partial Refund:
     *    - Some items returned, not all
     *    - Portion of payment returned
     *    - May need additional tracking (not in this simple implementation)
     *
     * When refunds occur:
     * - Customer returns product
     * - Order cancelled after payment
     * - Duplicate payment made
     * - Product out of stock after payment
     * - Service issue or complaint resolution
     *
     * Refund timeline:
     * - Credit card: 5-10 business days
     * - Debit card: 5-10 business days
     * - PayPal: Instant to account, 3-5 days to bank
     * - Bank transfer: 3-5 business days
     *
     * What happens in system:
     * - Payment marked as REFUNDED
     * - Order status may change to CANCELLED
     * - Inventory updated (items back in stock)
     * - Customer notified of refund
     * - Refund transaction ID stored
     *
     * This is a terminal state:
     * - No further status changes allowed
     * - Cannot re-complete refunded payment
     * - Cannot refund twice (prevents errors)
     *
     * User experience:
     * - "Refund processed successfully"
     * - "Refund of $XX.XX will appear in 5-10 business days"
     * - Email confirmation sent
     * - Receipt updated to show refund
     *
     * Example scenario:
     * - Customer returns defective product
     * - Admin approves return
     * - Admin processes refund in system
     * - Stripe refund API called
     * - Payment marked as REFUNDED
     * - Customer receives email: "Your refund of $49.99 has been processed"
     * - Money appears in customer's account in 5-10 days
     *
     * Accounting:
     * - Original payment: +$49.99 revenue
     * - Refund: -$49.99 revenue
     * - Net: $0.00
     * - Transaction fees usually not refunded by gateway
     *
     * Analytics:
     * - Track refund rate (should be low)
     * - Identify products with high return rate
     * - Monitor refund reasons
     * - Improve product quality and descriptions
     */
    REFUNDED;

    /**
     * Helper Methods
     * ==============
     *
     * These could be added to provide business logic around status transitions:
     */

    /**
     * Check if status is a terminal state (no further transitions allowed)
     *
     * @return true if FAILED or REFUNDED, false otherwise
     *
     * Usage:
     * if (payment.getStatus().isTerminal()) {
     *     throw new BadRequestException("Cannot modify payment in terminal state");
     * }
     */
    // public boolean isTerminal() {
    //     return this == FAILED || this == REFUNDED;
    // }

    /**
     * Check if status can transition to another status
     *
     * @param newStatus The status to transition to
     * @return true if transition is allowed, false otherwise
     *
     * Usage:
     * if (!currentStatus.canTransitionTo(newStatus)) {
     *     throw new BadRequestException("Cannot transition from " + currentStatus + " to " + newStatus);
     * }
     */
    // public boolean canTransitionTo(PaymentStatus newStatus) {
    //     return switch (this) {
    //         case PENDING -> newStatus == PROCESSING || newStatus == FAILED;
    //         case PROCESSING -> newStatus == COMPLETED || newStatus == FAILED;
    //         case COMPLETED -> newStatus == REFUNDED;
    //         case FAILED, REFUNDED -> false;
    //     };
    // }

    /**
     * Get user-friendly display name
     *
     * @return Human-readable status name
     *
     * Usage:
     * String message = "Payment status: " + status.getDisplayName();
     * // "Payment status: Processing"
     */
    // public String getDisplayName() {
    //     return switch (this) {
    //         case PENDING -> "Pending";
    //         case PROCESSING -> "Processing";
    //         case COMPLETED -> "Completed";
    //         case FAILED -> "Failed";
    //         case REFUNDED -> "Refunded";
    //     };
    // }
}
