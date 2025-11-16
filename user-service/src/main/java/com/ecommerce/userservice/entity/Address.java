// Package declaration - this class belongs to the entity package
package com.ecommerce.userservice.entity;

// Import JPA annotations for database mapping
import jakarta.persistence.*;
// Import Lombok annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Import Java time class for timestamps
import java.time.LocalDateTime;

/**
 * Address Entity Class
 *
 * Represents a shipping or billing address for a user
 * This class is mapped to a database table called "addresses"
 *
 * Use Cases:
 * ----------
 * - Shipping Address: Where to deliver orders
 * - Billing Address: For invoice and payment
 * - Multiple Addresses: Users can have home, work, etc.
 * - Default Address: Quick checkout with saved address
 *
 * Database Table Structure:
 * --------------------------
 * | id | user_id | street_address | city | state | postal_code | country | is_default | address_type | created_at | updated_at |
 * --------------------------
 *
 * Relationships:
 * - Many-to-One with User (one user can have many addresses)
 */

// @Entity marks this class as a JPA entity
@Entity

// @Table specifies the table name and indexes
@Table(
    name = "addresses",
    indexes = {
        // Index on user_id for fast lookup of user's addresses
        // SELECT * FROM addresses WHERE user_id = ?
        @Index(name = "idx_user_id", columnList = "user_id"),

        // Index on postal_code for delivery zone queries
        @Index(name = "idx_postal_code", columnList = "postal_code")
    }
)

// @Data generates getters, setters, toString, equals, hashCode
@Data

// @NoArgsConstructor generates a no-argument constructor
@NoArgsConstructor

// @AllArgsConstructor generates a constructor with all fields
@AllArgsConstructor
public class Address {

    /**
     * Primary Key - Unique identifier for each address
     *
     * @Id marks this field as the primary key
     * @GeneratedValue with IDENTITY strategy means database auto-increments
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User Relationship
     *
     * @ManyToOne defines a many-to-one relationship:
     * - Many addresses can belong to one user
     * - One user can have many addresses
     *
     * @JoinColumn specifies the foreign key column:
     * - name = "user_id": Name of the foreign key column
     * - nullable = false: Address must belong to a user
     * - referencedColumnName = "id": Points to id column in users table
     *
     * fetch = FetchType.LAZY:
     * - Don't load user when loading address
     * - User is only loaded when explicitly accessed
     * - Improves performance (avoid unnecessary queries)
     *
     * Example in database:
     * addresses table:
     * | id | user_id | street_address | city | state |
     * | 1  | 5       | 123 Main St    | NYC  | NY    | → belongs to user 5
     * | 2  | 5       | 456 Work Ave   | NYC  | NY    | → also belongs to user 5
     * | 3  | 7       | 789 Home Rd    | LA   | CA    | → belongs to user 7
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "id")
    private User user;

    /**
     * Street Address
     *
     * Full street address including house/apartment number
     *
     * @Column constraints:
     * - nullable = false: Street address is required
     * - length = 500: Accommodates long addresses
     *
     * Examples:
     * ---------
     * - "123 Main Street"
     * - "456 Oak Avenue, Apartment 3B"
     * - "789 Corporate Blvd, Suite 200"
     *
     * Should include:
     * - House/building number
     * - Street name
     * - Apartment/unit number (if applicable)
     *
     * Used for:
     * ---------
     * - Shipping labels
     * - Delivery driver navigation
     * - Address verification
     */
    @Column(nullable = false, length = 500)
    private String streetAddress;

    /**
     * City
     *
     * City or town name
     *
     * @Column constraints:
     * - nullable = false: City is required
     * - length = 100: Standard city name length
     *
     * Examples:
     * ---------
     * - "New York"
     * - "Los Angeles"
     * - "San Francisco"
     *
     * Used for:
     * ---------
     * - Shipping calculations
     * - Delivery zone determination
     * - Tax calculations (city tax)
     * - Filtering orders by city
     */
    @Column(nullable = false, length = 100)
    private String city;

    /**
     * State/Province
     *
     * State, province, or region
     *
     * @Column constraints:
     * - nullable = false: State is required
     * - length = 100: Accommodates long state names
     *
     * Format:
     * -------
     * Can be:
     * - Full name: "California", "New York"
     * - Abbreviation: "CA", "NY"
     * - Province: "Ontario", "Quebec" (Canada)
     * - Region: "Bavaria", "Tuscany" (Europe)
     *
     * Used for:
     * ---------
     * - State tax calculations
     * - Shipping restrictions (some states banned)
     * - Delivery time estimates
     * - Regional analytics
     *
     * Validation:
     * -----------
     * In production, validate against list of valid states
     * Use dropdown in frontend for consistency
     */
    @Column(nullable = false, length = 100)
    private String state;

    /**
     * Postal Code / ZIP Code
     *
     * Postal code, ZIP code, or PIN code
     *
     * @Column constraints:
     * - nullable = false: Postal code is required
     * - length = 20: Accommodates international formats
     *
     * Format Examples:
     * ----------------
     * - US ZIP: "90210", "12345-6789"
     * - UK Postcode: "SW1A 1AA"
     * - Canada: "K1A 0B1"
     * - India PIN: "110001"
     *
     * Used for:
     * ---------
     * - Shipping cost calculation
     * - Delivery time estimation
     * - Service availability checks
     * - Address verification
     * - Tax calculations (local taxes)
     *
     * Why indexed?
     * ------------
     * Frequently used for:
     * - "Do we deliver to this ZIP code?"
     * - "What's the shipping cost to 90210?"
     * - Grouping orders by postal code for delivery optimization
     */
    @Column(nullable = false, length = 20)
    private String postalCode;

    /**
     * Country
     *
     * Country name or code
     *
     * @Column constraints:
     * - nullable = false: Country is required
     * - length = 100: Standard country name length
     *
     * Format:
     * -------
     * Can be:
     * - Full name: "United States", "Canada", "United Kingdom"
     * - ISO code: "US", "CA", "GB" (2-letter)
     * - ISO code: "USA", "CAN", "GBR" (3-letter)
     *
     * Used for:
     * ---------
     * - International shipping
     * - Currency conversion
     * - Tax/customs calculations
     * - Shipping restrictions
     * - Service availability
     *
     * Best Practice:
     * --------------
     * Store ISO country codes for consistency
     * Display full names in UI using a mapping
     * Example: Store "US", display "United States"
     */
    @Column(nullable = false, length = 100)
    private String country;

    /**
     * Default Address Flag
     *
     * Indicates if this is the user's default address
     * - true: This is the default address (used for quick checkout)
     * - false: This is an alternate address
     *
     * Default value: false (not default by default)
     *
     * Use Cases:
     * ----------
     * 1. Quick Checkout:
     *    - Pre-fill shipping form with default address
     *    - One-click checkout
     *
     * 2. Multiple Addresses:
     *    - User has home, work, parents' address
     *    - Default is most commonly used
     *
     * 3. Address Selection:
     *    - Show default address first in list
     *    - Mark with "Default" badge in UI
     *
     * Business Rule:
     * --------------
     * - Only ONE address per user should be default
     * - When setting new default, unset previous default
     *
     * Example Logic:
     * --------------
     * public void setAsDefault(Long userId, Long addressId) {
     *     // Unset all defaults for this user
     *     addressRepository.updateAllToNonDefault(userId);
     *     // Set this address as default
     *     Address address = addressRepository.findById(addressId).get();
     *     address.setIsDefault(true);
     *     addressRepository.save(address);
     * }
     */
    @Column(nullable = false)
    private Boolean isDefault = false;

    /**
     * Address Type
     *
     * Type of address: SHIPPING or BILLING
     *
     * @Enumerated(EnumType.STRING):
     * - Stores enum as string in database ("SHIPPING", "BILLING")
     * - Alternative: EnumType.ORDINAL stores as integer (0, 1)
     * - STRING is preferred (more readable, safer if enum order changes)
     *
     * Why separate types?
     * -------------------
     * - Shipping Address: Where to deliver the package
     * - Billing Address: Where credit card statement goes
     * - Often the same, but can be different
     * - Example: Ship to office, bill to home
     *
     * Use Cases:
     * ----------
     * - Filter addresses: "Show only shipping addresses"
     * - Validation: "Billing address required for payment"
     * - Checkout: Separate shipping and billing address selection
     *
     * Default: SHIPPING (most common use case)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AddressType addressType = AddressType.SHIPPING;

    /**
     * Created Timestamp
     *
     * When this address was added
     *
     * Use Cases:
     * ----------
     * - Display "Added on: January 15, 2024"
     * - Sort addresses by creation date
     * - Identify old/unused addresses
     *
     * Automatically set by @PrePersist callback
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Updated Timestamp
     *
     * When this address was last modified
     *
     * Use Cases:
     * ----------
     * - Track when address was last updated
     * - Identify recently changed addresses
     * - Audit trail
     *
     * Automatically updated by @PreUpdate callback
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Lifecycle Callback - Before Insert
     *
     * @PrePersist is called automatically before saving a NEW address
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    /**
     * Lifecycle Callback - Before Update
     *
     * @PreUpdate is called automatically before updating an EXISTING address
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Address Type Enum
     *
     * Defines the type of address
     */
    public enum AddressType {
        /**
         * SHIPPING - Address for package delivery
         */
        SHIPPING,

        /**
         * BILLING - Address for billing/invoicing
         */
        BILLING
    }

    // =========================================================================
    // Address Validation
    // =========================================================================
    //
    // Addresses should be validated before saving:
    //
    // 1. Format Validation (done in DTO):
    //    - Postal code format for specific country
    //    - State/province valid for country
    //    - Phone number format (if included)
    //
    // 2. Address Verification (optional):
    //    - Use service like Google Maps API, USPS API
    //    - Verify address exists and is deliverable
    //    - Suggest corrections for typos
    //
    // 3. Delivery Availability:
    //    - Check if we deliver to this postal code
    //    - Check if address is in serviceable area
    //    - International shipping restrictions
    //
    // =========================================================================
    // Helper Methods (could be added)
    // =========================================================================
    //
    // Get full address as string:
    // public String getFullAddress() {
    //     return streetAddress + ", " + city + ", " + state + " " + postalCode + ", " + country;
    // }
    //
    // Example output:
    // "123 Main Street, New York, NY 10001, United States"
}
