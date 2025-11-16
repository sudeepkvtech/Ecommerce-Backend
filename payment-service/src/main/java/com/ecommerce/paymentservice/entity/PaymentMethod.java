package com.ecommerce.paymentservice.entity;

/**
 * Payment Method Enum
 *
 * Represents the different payment methods supported by the e-commerce platform.
 * Defines how a customer can pay for their order.
 *
 * Why use enum instead of String?
 * ================================
 * 1. Type Safety: Compiler enforces valid payment methods
 * 2. Code Completion: IDE shows all available payment methods
 * 3. Refactoring: Easy to rename across entire codebase
 * 4. Validation: Cannot use invalid payment methods
 * 5. Documentation: All payment methods defined in one place
 *
 * Database Storage
 * ================
 * Stored as VARCHAR in database using @Enumerated(EnumType.STRING)
 * Example database value: "CREDIT_CARD", "PAYPAL", "BANK_TRANSFER"
 *
 * Alternative (not recommended):
 * @Enumerated(EnumType.ORDINAL) stores as integer (0, 1, 2, ...)
 * Problem: If you reorder enum values, database data becomes invalid
 *
 * Payment Method Selection
 * ========================
 * Each payment method has different:
 * - Processing time
 * - Transaction fees
 * - Refund policies
 * - Geographic availability
 * - Integration requirements
 *
 * Future Enhancements
 * ===================
 * 1. Add payment method metadata:
 *    - Display name
 *    - Icon/logo
 *    - Processing time
 *    - Transaction fees
 *    - Supported currencies
 *    - Supported countries
 *
 * 2. Dynamic payment methods from database:
 *    - Admin can enable/disable methods
 *    - Configure fees per method
 *    - A/B test different methods
 */
public enum PaymentMethod {

    /**
     * CREDIT_CARD
     *
     * Payment via credit card (Visa, Mastercard, Amex, Discover, etc.)
     *
     * Characteristics:
     * ----------------
     * - Processing time: Instant (2-5 seconds)
     * - Refund time: 5-10 business days
     * - Transaction fee: ~2.9% + $0.30 (varies by processor)
     * - Chargeback risk: Yes (customer can dispute)
     * - 3D Secure: Supported (reduces fraud)
     *
     * Integration:
     * ------------
     * - Stripe: stripe.charges.create()
     * - PayPal: Braintree
     * - Square: square.payments.create()
     *
     * Information required:
     * - Card number (16 digits)
     * - Expiration date (MM/YY)
     * - CVV (3-4 digits)
     * - Cardholder name
     * - Billing address (for AVS check)
     *
     * Security considerations:
     * ------------------------
     * - PCI DSS compliance required
     * - Never store full card number
     * - Use tokenization (Stripe tokens)
     * - Encrypt data in transit (HTTPS)
     * - Validate CVV
     * - Address Verification System (AVS)
     * - 3D Secure for high-value transactions
     *
     * User experience:
     * ----------------
     * - Instant payment confirmation
     * - Card saved for future use (with permission)
     * - One-click checkout for returning customers
     *
     * Example flow:
     * 1. Customer enters card details
     * 2. Frontend sends to Stripe (gets token)
     * 3. Token sent to backend (not actual card number)
     * 4. Backend charges token via Stripe API
     * 5. Stripe processes with card network
     * 6. Response returned (success/failure)
     *
     * Common cards:
     * - Visa (most common globally)
     * - Mastercard (widely accepted)
     * - American Express (higher fees, less accepted)
     * - Discover (mainly US)
     * - JCB (Japan)
     * - Diners Club (travel/entertainment)
     */
    CREDIT_CARD,

    /**
     * DEBIT_CARD
     *
     * Payment via debit card (directly from bank account)
     *
     * Characteristics:
     * ----------------
     * - Processing time: Instant (2-5 seconds)
     * - Refund time: 5-10 business days
     * - Transaction fee: Lower than credit cards (~1.5% + $0.30)
     * - Chargeback risk: Lower than credit cards
     * - Requires sufficient funds in account
     *
     * Differences from credit card:
     * -----------------------------
     * - Debit: Money withdrawn immediately from bank account
     * - Credit: Borrowed money, paid back later
     * - Debit: No interest charges
     * - Credit: Interest if not paid in full
     * - Debit: Limited fraud protection
     * - Credit: Better fraud protection
     *
     * Integration:
     * ------------
     * - Same as credit card (Stripe, PayPal, Square)
     * - Card networks: Visa Debit, Mastercard Debit
     * - PIN debit vs signature debit
     *
     * Information required:
     * - Same as credit card
     * - Card number, expiration, CVV
     * - PIN may be required for in-person
     *
     * Security considerations:
     * ------------------------
     * - Same PCI DSS requirements as credit card
     * - Real-time fund availability check
     * - Higher decline rate (insufficient funds)
     *
     * User experience:
     * ----------------
     * - Instant deduction from bank account
     * - May be preferred by budget-conscious customers
     * - No interest charges
     *
     * Example scenario:
     * - Customer has $500 in checking account
     * - Orders product for $50
     * - Payment processed instantly
     * - Account balance now $450
     */
    DEBIT_CARD,

    /**
     * PAYPAL
     *
     * Payment via PayPal account
     *
     * Characteristics:
     * ----------------
     * - Processing time: Instant
     * - Refund time: Instant to PayPal balance, 3-5 days to bank
     * - Transaction fee: ~2.9% + $0.30 (similar to cards)
     * - Buyer protection: Strong fraud protection
     * - No card details shared with merchant
     *
     * Benefits:
     * ---------
     * - Customer doesn't enter card on merchant site
     * - One-click checkout (if already logged into PayPal)
     * - Widely trusted brand
     * - International payments easier
     * - Multiple funding sources (card, bank, balance)
     *
     * Integration:
     * ------------
     * - PayPal SDK
     * - PayPal REST API
     * - PayPal Checkout (button integration)
     * - Express Checkout
     *
     * Payment flow:
     * -------------
     * 1. Customer clicks "PayPal" button
     * 2. Redirected to PayPal login
     * 3. Customer authorizes payment
     * 4. Redirected back to merchant
     * 5. Merchant captures payment
     * 6. Funds transferred to merchant account
     *
     * Information required:
     * - PayPal email or phone
     * - PayPal password
     * - (Customer enters on PayPal, not merchant site)
     *
     * Security considerations:
     * ------------------------
     * - OAuth for authentication
     * - No PCI compliance needed (PayPal handles cards)
     * - Webhook verification for payment notifications
     * - IPN (Instant Payment Notification) validation
     *
     * User experience:
     * ----------------
     * - Faster checkout (no card entry)
     * - Trust factor (PayPal brand)
     * - Mobile-friendly
     * - Saved payment methods
     *
     * Example scenario:
     * - Customer selects PayPal
     * - Popup window opens (or redirect on mobile)
     * - Customer logs in: john@example.com
     * - Reviews order: "Pay $49.99 to Example Store"
     * - Clicks "Pay Now"
     * - Returns to merchant site
     * - Order confirmed
     */
    PAYPAL,

    /**
     * BANK_TRANSFER
     *
     * Direct bank transfer / wire transfer
     *
     * Characteristics:
     * ----------------
     * - Processing time: 1-3 business days
     * - Refund time: 3-5 business days
     * - Transaction fee: Low or none (depends on bank)
     * - Chargeback risk: Very low (irreversible)
     * - Manual verification often required
     *
     * Use cases:
     * ----------
     * - Large orders (lower fees for high amounts)
     * - B2B transactions
     * - International payments
     * - Customers without cards
     *
     * Types:
     * ------
     * 1. Wire Transfer:
     *    - Fast (same day possible)
     *    - Higher fees ($15-$50)
     *    - International: SWIFT
     *    - Domestic: Fedwire, CHIPS
     *
     * 2. ACH Transfer (US):
     *    - Slower (2-3 days)
     *    - Low/no fees
     *    - Automated Clearing House
     *
     * 3. SEPA Transfer (Europe):
     *    - 1-2 business days
     *    - Low fees within EU
     *    - Single Euro Payments Area
     *
     * Integration:
     * ------------
     * - Stripe Bank Transfers
     * - Plaid (bank account verification)
     * - Manual bank transfers (provide bank details to customer)
     *
     * Payment flow:
     * -------------
     * 1. Customer selects bank transfer
     * 2. System provides bank account details:
     *    - Account name
     *    - Account number
     *    - Routing number (US) or SWIFT (international)
     *    - Reference number (to match payment)
     * 3. Customer initiates transfer from their bank
     * 4. Payment received in 1-3 days
     * 5. Admin verifies payment received
     * 6. Order status updated
     *
     * Information required:
     * - Customer's bank account details
     * - Reference/invoice number
     * - Payment amount
     *
     * Challenges:
     * -----------
     * - Delayed confirmation
     * - Manual verification needed
     * - Order held until payment confirmed
     * - Customer may forget reference number
     *
     * Security considerations:
     * ------------------------
     * - Verify payment amount matches order
     * - Validate reference number
     * - Check sender name matches customer
     * - Prevent duplicate payments
     *
     * User experience:
     * ----------------
     * - Order placed but not confirmed
     * - Email sent with bank details
     * - "Awaiting payment" status
     * - Manual confirmation when received
     * - Slower fulfillment
     *
     * Example scenario:
     * - Customer orders $5,000 equipment
     * - Selects bank transfer (save on fees)
     * - Receives email: "Transfer to Account #12345, Ref: ORDER-001"
     * - Logs into online banking
     * - Initiates transfer
     * - 2 days later, payment received
     * - Admin confirms, order ships
     */
    BANK_TRANSFER,

    /**
     * CASH_ON_DELIVERY (COD)
     *
     * Payment in cash when order is delivered
     *
     * Characteristics:
     * ----------------
     * - Processing time: At delivery (days to weeks)
     * - Refund: Return cash if product returned
     * - Transaction fee: May have COD fee ($1-$5)
     * - Chargeback risk: Customer may refuse delivery
     * - Popular in: India, Middle East, Southeast Asia
     *
     * Benefits:
     * ---------
     * - No online payment needed
     * - Good for customers without cards/bank accounts
     * - Inspect product before paying
     * - Builds trust (see before pay)
     *
     * Challenges:
     * -----------
     * - Higher return rate (easy to refuse)
     * - Cash handling for delivery personnel
     * - Fake orders (address validation needed)
     * - Limited to physical products
     * - Cannot be used for digital products
     *
     * Integration:
     * ------------
     * - Usually integrated with shipping partner
     * - Delivery person collects cash
     * - Remits to merchant later
     * - COD fee may apply
     *
     * Payment flow:
     * -------------
     * 1. Customer selects COD at checkout
     * 2. Order confirmed immediately
     * 3. Product shipped
     * 4. Delivery person attempts delivery
     * 5. Customer inspects product
     * 6. Customer pays cash to delivery person
     * 7. Delivery person gives receipt
     * 8. Cash remitted to merchant (minus COD fee)
     *
     * Information required:
     * - Delivery address
     * - Phone number (for delivery coordination)
     * - Alternate phone number (recommended)
     *
     * Risk mitigation:
     * ----------------
     * - Phone verification before shipping
     * - Address verification
     * - Limit COD for first-time customers
     * - COD fee to reduce fake orders
     * - Blacklist addresses with high refusal rate
     *
     * Security considerations:
     * ------------------------
     * - Verify phone number
     * - Confirm order via call/SMS
     * - Track refusal rate by customer
     * - Insurance for delivery personnel
     * - Proper receipt documentation
     *
     * User experience:
     * ----------------
     * - Convenient for cash-preferring customers
     * - Peace of mind (pay after inspection)
     * - May have COD fee
     * - Order confirmed, but not paid
     * - Phone call for delivery confirmation
     *
     * Example scenario:
     * - Customer in India orders shoes
     * - Selects "Cash on Delivery"
     * - COD fee: ₹50 ($0.60)
     * - Order total: ₹3,050 (₹3,000 + ₹50 COD fee)
     * - Receives SMS: "Order confirmed, delivery in 3 days"
     * - Delivery person arrives
     * - Customer inspects shoes
     * - Pays ₹3,050 in cash
     * - Receives receipt
     *
     * Regional availability:
     * ----------------------
     * - India: Very popular (~50% of e-commerce)
     * - Middle East: Common in UAE, Saudi Arabia
     * - Southeast Asia: Popular in Indonesia, Philippines
     * - Latin America: Used in some countries
     * - Western countries: Rare (card/digital payment preferred)
     */
    CASH_ON_DELIVERY;

    /**
     * Helper Methods
     * ==============
     *
     * These could be added to provide metadata and business logic:
     */

    /**
     * Get display name for user interface
     *
     * @return Human-readable payment method name
     *
     * Usage:
     * String displayName = paymentMethod.getDisplayName();
     * // "Credit Card" instead of "CREDIT_CARD"
     */
    // public String getDisplayName() {
    //     return switch (this) {
    //         case CREDIT_CARD -> "Credit Card";
    //         case DEBIT_CARD -> "Debit Card";
    //         case PAYPAL -> "PayPal";
    //         case BANK_TRANSFER -> "Bank Transfer";
    //         case CASH_ON_DELIVERY -> "Cash on Delivery";
    //     };
    // }

    /**
     * Check if payment method requires immediate processing
     *
     * @return true if instant, false if delayed
     *
     * Usage:
     * if (paymentMethod.isInstant()) {
     *     processPaymentNow();
     * } else {
     *     markAsAwaitingPayment();
     * }
     */
    // public boolean isInstant() {
    //     return this == CREDIT_CARD || this == DEBIT_CARD || this == PAYPAL;
    // }

    /**
     * Check if payment method requires online payment
     *
     * @return true if online, false if offline
     *
     * Usage:
     * if (paymentMethod.isOnlinePayment()) {
     *     processViaGateway();
     * } else {
     *     createManualPaymentRecord();
     * }
     */
    // public boolean isOnlinePayment() {
    //     return this != CASH_ON_DELIVERY && this != BANK_TRANSFER;
    // }

    /**
     * Get transaction fee percentage (example)
     *
     * @return Fee as decimal (e.g., 0.029 for 2.9%)
     *
     * Note: In production, fees should come from database configuration
     */
    // public BigDecimal getTransactionFeePercentage() {
    //     return switch (this) {
    //         case CREDIT_CARD -> new BigDecimal("0.029");  // 2.9%
    //         case DEBIT_CARD -> new BigDecimal("0.015");   // 1.5%
    //         case PAYPAL -> new BigDecimal("0.029");       // 2.9%
    //         case BANK_TRANSFER -> new BigDecimal("0.00"); // 0%
    //         case CASH_ON_DELIVERY -> new BigDecimal("0.00"); // 0% (fixed fee instead)
    //     };
    // }
}
