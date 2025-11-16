// Package declaration - this interface belongs to the repository package
package com.ecommerce.userservice.repository;

// Import Spring Data JPA repository interface
import org.springframework.data.jpa.repository.JpaRepository;
// Import Query annotation for custom queries
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
// Import parameter annotation
import org.springframework.data.repository.query.Param;
// Import stereotype annotation
import org.springframework.stereotype.Repository;
// Import the entity class
import com.ecommerce.userservice.entity.Address;
import com.ecommerce.userservice.entity.Address.AddressType;

// Import Optional and List for return types
import java.util.Optional;
import java.util.List;

/**
 * Address Repository Interface
 *
 * This interface provides database operations for the Address entity.
 * Manages user shipping and billing addresses.
 *
 * What is this repository used for?
 * ----------------------------------
 * - Find all addresses for a user
 * - Find default address for quick checkout
 * - Find shipping or billing addresses
 * - Update default address
 * - Delete user addresses
 *
 * Use Cases:
 * ----------
 * - Checkout: Get default shipping address
 * - Address Book: Show all user addresses
 * - Order Processing: Get billing address for invoice
 * - Delivery: Get shipping address for package
 */

// @Repository marks this as a Data Access Object (DAO)
@Repository

// JpaRepository<Address, Long>
// - Address: The entity type this repository manages
// - Long: Type of Address's primary key (id field)
public interface AddressRepository extends JpaRepository<Address, Long> {

    /**
     * Find all addresses for a specific user
     *
     * Method Naming Convention:
     * -------------------------
     * "findByUserId" generates: SELECT * FROM addresses WHERE user_id = ?
     *
     * Why user_id?
     * ------------
     * - Address entity has @JoinColumn(name = "user_id")
     * - This is the foreign key column in addresses table
     * - Links address to user
     *
     * @param userId The user ID to find addresses for
     * @return List<Address> - All addresses belonging to this user
     *
     * Usage Examples:
     * ---------------
     *
     * Example 1: Show user's address book
     * @GetMapping("/addresses")
     * public List<AddressResponse> getUserAddresses() {
     *     Long userId = getCurrentUserId();
     *     List<Address> addresses = addressRepository.findByUserId(userId);
     *     return addresses.stream()
     *         .map(this::toResponse)
     *         .collect(Collectors.toList());
     * }
     *
     * Example 2: Count user's addresses
     * List<Address> addresses = addressRepository.findByUserId(userId);
     * int count = addresses.size();
     *
     * Example 3: Check if user has addresses
     * List<Address> addresses = addressRepository.findByUserId(userId);
     * if (addresses.isEmpty()) {
     *     // Prompt user to add address
     * }
     *
     * Database Query:
     * ---------------
     * SELECT * FROM addresses WHERE user_id = ?
     * ORDER BY is_default DESC, created_at DESC
     *
     * Returns addresses ordered by:
     * 1. Default addresses first
     * 2. Most recently created
     */
    List<Address> findByUserId(Long userId);

    /**
     * Find default address for a user
     *
     * Method Naming Convention:
     * -------------------------
     * "findByUserIdAndIsDefaultTrue"
     * - "UserId": Filter by user
     * - "And": Combine conditions
     * - "IsDefaultTrue": Filter by isDefault = true
     *
     * Generated SQL:
     * --------------
     * SELECT * FROM addresses WHERE user_id = ? AND is_default = true
     *
     * @param userId The user ID
     * @return Optional<Address> - Default address if exists, empty otherwise
     *
     * Why Optional?
     * -------------
     * - User might not have set a default address yet
     * - New users have no addresses
     * - Prevents NullPointerException
     *
     * Usage Examples:
     * ---------------
     *
     * Example 1: Quick checkout
     * @PostMapping("/checkout")
     * public OrderResponse checkout(@RequestBody CheckoutRequest request) {
     *     Long userId = getCurrentUserId();
     *
     *     // Get default address or throw error
     *     Address shippingAddress = addressRepository
     *         .findByUserIdAndIsDefaultTrue(userId)
     *         .orElseThrow(() -> new IllegalStateException(
     *             "Please add a default shipping address"
     *         ));
     *
     *     // Process order with default address
     *     return orderService.createOrder(request, shippingAddress);
     * }
     *
     * Example 2: Pre-fill checkout form
     * Optional<Address> defaultAddress = addressRepository
     *     .findByUserIdAndIsDefaultTrue(userId);
     *
     * if (defaultAddress.isPresent()) {
     *     // Pre-fill form with default address
     *     return CheckoutForm.withAddress(defaultAddress.get());
     * } else {
     *     // Show empty form
     *     return CheckoutForm.empty();
     * }
     *
     * Example 3: Show default indicator in address list
     * List<Address> addresses = addressRepository.findByUserId(userId);
     * Optional<Address> defaultAddress = addressRepository
     *     .findByUserIdAndIsDefaultTrue(userId);
     *
     * addresses.forEach(address -> {
     *     if (defaultAddress.isPresent() && address.getId().equals(defaultAddress.get().getId())) {
     *         System.out.println(address + " [DEFAULT]");
     *     } else {
     *         System.out.println(address);
     *     }
     * });
     *
     * Business Rule:
     * --------------
     * - Only ONE address per user should have isDefault = true
     * - When setting new default, unset previous default
     * - Enforced in Service layer or database trigger
     */
    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);

    /**
     * Find addresses by user and type
     *
     * Method Naming Convention:
     * -------------------------
     * "findByUserIdAndAddressType"
     * - "UserId": Filter by user
     * - "And": Combine conditions
     * - "AddressType": Filter by type (SHIPPING or BILLING)
     *
     * Generated SQL:
     * --------------
     * SELECT * FROM addresses WHERE user_id = ? AND address_type = ?
     *
     * @param userId The user ID
     * @param addressType The address type (SHIPPING or BILLING)
     * @return List<Address> - Addresses of specified type
     *
     * Usage Examples:
     * ---------------
     *
     * Example 1: Get shipping addresses only
     * List<Address> shippingAddresses = addressRepository
     *     .findByUserIdAndAddressType(userId, AddressType.SHIPPING);
     *
     * Example 2: Get billing addresses only
     * List<Address> billingAddresses = addressRepository
     *     .findByUserIdAndAddressType(userId, AddressType.BILLING);
     *
     * Example 3: Show separate address lists in checkout
     * @GetMapping("/checkout")
     * public CheckoutForm getCheckoutForm() {
     *     Long userId = getCurrentUserId();
     *
     *     List<Address> shippingAddresses = addressRepository
     *         .findByUserIdAndAddressType(userId, AddressType.SHIPPING);
     *
     *     List<Address> billingAddresses = addressRepository
     *         .findByUserIdAndAddressType(userId, AddressType.BILLING);
     *
     *     return new CheckoutForm(shippingAddresses, billingAddresses);
     * }
     *
     * Why separate shipping and billing?
     * ----------------------------------
     * - Ship to office, bill to home
     * - Gift orders (ship to recipient, bill to buyer)
     * - Corporate orders (ship to office, bill to company)
     */
    List<Address> findByUserIdAndAddressType(Long userId, AddressType addressType);

    /**
     * Unset all default addresses for a user
     *
     * Custom update query using @Query and @Modifying
     *
     * @Query annotation:
     * -----------------
     * Allows writing custom UPDATE queries
     *
     * @Modifying annotation:
     * --------------------
     * Indicates this query modifies data (UPDATE, DELETE)
     * Without this, Spring expects SELECT query
     *
     * Query Explanation:
     * ------------------
     * UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId
     *
     * - "Address a": Update Address entity (alias 'a')
     * - "SET a.isDefault = false": Set isDefault field to false
     * - "WHERE a.user.id = :userId": Filter by user
     * - ":userId": Named parameter
     *
     * @param userId The user ID
     * @return int - Number of addresses updated
     *
     * Why is this needed?
     * -------------------
     * Business rule: Only ONE default address per user
     *
     * When setting new default address:
     * 1. Unset all current defaults (this method)
     * 2. Set new address as default
     *
     * Usage Example:
     * --------------
     * @Transactional
     * public void setDefaultAddress(Long userId, Long addressId) {
     *     // Step 1: Unset all current defaults
     *     addressRepository.updateAllToNonDefault(userId);
     *
     *     // Step 2: Set new default
     *     Address address = addressRepository.findById(addressId)
     *         .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
     *
     *     // Verify address belongs to user (security)
     *     if (!address.getUser().getId().equals(userId)) {
     *         throw new UnauthorizedException("This address does not belong to you");
     *     }
     *
     *     address.setIsDefault(true);
     *     addressRepository.save(address);
     * }
     *
     * Why @Transactional?
     * -------------------
     * - Ensures both steps succeed or both fail
     * - Prevents state where no default or multiple defaults
     * - Atomic operation (all or nothing)
     *
     * Return value:
     * -------------
     * Returns number of addresses updated
     * - Usually 0 (no previous default) or 1 (previous default unset)
     * - Could be more if data integrity issue (shouldn't happen)
     */
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId")
    int updateAllToNonDefault(@Param("userId") Long userId);

    /**
     * Delete all addresses for a user
     *
     * Method Naming Convention:
     * -------------------------
     * "deleteByUserId" generates: DELETE FROM addresses WHERE user_id = ?
     *
     * @param userId The user ID
     * @return int - Number of addresses deleted
     *
     * Usage Examples:
     * ---------------
     *
     * Example 1: Delete user account (cascade)
     * @Transactional
     * public void deleteUserAccount(Long userId) {
     *     // Delete addresses first
     *     addressRepository.deleteByUserId(userId);
     *
     *     // Then delete user
     *     userRepository.deleteById(userId);
     * }
     *
     * Example 2: Clean up orphaned addresses
     * int deleted = addressRepository.deleteByUserId(userId);
     * System.out.println("Deleted " + deleted + " addresses");
     *
     * Warning:
     * --------
     * - This permanently deletes addresses
     * - Cannot be undone
     * - Use with caution
     * - Consider soft delete instead (set enabled = false)
     */
    int deleteByUserId(Long userId);

    /**
     * Count addresses for a user
     *
     * Method Naming Convention:
     * -------------------------
     * "countByUserId" generates: SELECT COUNT(*) FROM addresses WHERE user_id = ?
     *
     * @param userId The user ID
     * @return long - Number of addresses
     *
     * Usage Examples:
     * ---------------
     *
     * Example 1: Check address limit
     * long addressCount = addressRepository.countByUserId(userId);
     * if (addressCount >= 10) {
     *     throw new LimitExceededException("Maximum 10 addresses allowed");
     * }
     *
     * Example 2: Show count in UI
     * long count = addressRepository.countByUserId(userId);
     * System.out.println("You have " + count + " saved addresses");
     *
     * Example 3: Prompt to add address
     * if (addressRepository.countByUserId(userId) == 0) {
     *     return "Please add a shipping address to continue";
     * }
     *
     * Performance:
     * ------------
     * More efficient than loading all addresses and calling .size()
     * Database only counts, doesn't return data
     */
    long countByUserId(Long userId);

    // =========================================================================
    // Additional Custom Queries (for future use)
    // =========================================================================
    //
    // Example 1: Find addresses by postal code (for delivery zones)
    // List<Address> findByPostalCode(String postalCode);
    //
    // Example 2: Find addresses by city
    // List<Address> findByCity(String city);
    //
    // Example 3: Find addresses by country
    // List<Address> findByCountry(String country);
    //
    // Example 4: Find addresses created after a date
    // List<Address> findByCreatedAtAfter(LocalDateTime date);
    //
    // Example 5: Find addresses in a specific region
    // @Query("SELECT a FROM Address a WHERE a.state IN :states")
    // List<Address> findByStates(@Param("states") List<String> states);
    //
    // =========================================================================
    // Best Practices
    // =========================================================================
    //
    // 1. Default Address:
    //    - Enforce ONE default per user
    //    - Use transactions when updating default
    //    - Validate before setting default
    //
    // 2. Address Validation:
    //    - Validate postal code format
    //    - Verify state is valid for country
    //    - Check delivery availability
    //
    // 3. Security:
    //    - Always verify address belongs to user
    //    - Don't allow modifying other users' addresses
    //    - Check authorization before operations
    //
    // 4. Performance:
    //    - Use countByUserId() instead of findByUserId().size()
    //    - Index on user_id for fast queries
    //    - Limit number of addresses per user
    //
    // 5. Data Integrity:
    //    - Don't allow deleting address if used in orders
    //    - Keep historical addresses for order reference
    //    - Consider soft delete instead of hard delete
}
