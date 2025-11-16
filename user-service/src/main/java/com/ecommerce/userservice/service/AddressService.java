package com.ecommerce.userservice.service;

// Import entities
import com.ecommerce.userservice.entity.Address;
import com.ecommerce.userservice.entity.User;

// Import DTOs
import com.ecommerce.userservice.dto.AddressRequest;
import com.ecommerce.userservice.dto.AddressResponse;

// Import repositories
import com.ecommerce.userservice.repository.AddressRepository;
import com.ecommerce.userservice.repository.UserRepository;

// Import exceptions
import com.ecommerce.userservice.exception.ResourceNotFoundException;
import com.ecommerce.userservice.exception.BadRequestException;

// Import Spring annotations
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Import Lombok
import lombok.RequiredArgsConstructor;

// Import Java utilities
import java.util.List;
import java.util.stream.Collectors;

/**
 * Address Service
 *
 * Business logic layer for address management operations.
 *
 * This service handles:
 * - Creating new addresses for users
 * - Retrieving user addresses
 * - Updating existing addresses
 * - Deleting addresses
 * - Managing default address (one default per user)
 *
 * Address types:
 * - SHIPPING: For product delivery
 * - BILLING: For payment information
 * - A user can have multiple addresses of each type
 * - One address can be marked as default for quick checkout
 */
@Service  // Spring component for business logic
@RequiredArgsConstructor  // Lombok generates constructor for final fields
@Transactional  // All methods run in database transactions
public class AddressService {

    // AddressRepository for address database operations
    private final AddressRepository addressRepository;

    // UserRepository to verify user exists and fetch user data
    private final UserRepository userRepository;

    /**
     * Create a new address for a user
     *
     * This method creates a new shipping or billing address for a user.
     *
     * Business rules:
     * 1. User must exist in database
     * 2. If isDefault is true, unset any existing default address
     * 3. If this is the user's first address, automatically make it default
     *
     * @param userId The ID of the user who owns this address
     * @param request AddressRequest DTO with address details
     * @return AddressResponse DTO with created address data
     * @throws ResourceNotFoundException if user doesn't exist
     */
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        // First, verify that the user exists
        // We need the User entity to establish the relationship
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId
                ));

        // Check if user wants this as default address
        // If yes, we need to unset any existing default addresses
        if (request.getIsDefault() != null && request.getIsDefault()) {
            // Find all current default addresses for this user
            // We use a custom query method from AddressRepository
            List<Address> currentDefaults = addressRepository.findByUserIdAndIsDefaultTrue(userId);

            // Loop through and unset the default flag
            // Why loop? User might have data inconsistency (multiple defaults)
            // This ensures only ONE address is default after this operation
            for (Address currentDefault : currentDefaults) {
                currentDefault.setIsDefault(false);  // Unset default flag
                addressRepository.save(currentDefault);  // Persist change
            }
        }

        // Check if this is the user's first address
        // If yes, automatically make it the default for convenience
        List<Address> existingAddresses = addressRepository.findByUserId(userId);
        boolean isFirstAddress = existingAddresses.isEmpty();

        // Create new Address entity from request DTO
        Address address = new Address();

        // Set the user relationship
        // This establishes the foreign key: address.user_id → user.id
        address.setUser(user);

        // Set address details from request
        address.setStreetAddress(request.getStreetAddress());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());

        // Set address type (SHIPPING or BILLING)
        // If not provided, defaults to SHIPPING (set in AddressRequest)
        address.setAddressType(request.getAddressType());

        // Set default flag
        // Logic: Use requested value, OR true if this is first address
        address.setIsDefault(isFirstAddress || (request.getIsDefault() != null && request.getIsDefault()));

        // Save to database
        // JPA will:
        // 1. Generate an ID
        // 2. Set created_at and updated_at timestamps (via @PrePersist)
        // 3. Insert into addresses table
        Address savedAddress = addressRepository.save(address);

        // Convert entity to DTO and return
        return mapToAddressResponse(savedAddress);
    }

    /**
     * Get all addresses for a user
     *
     * This method retrieves all addresses (both shipping and billing)
     * for a specific user.
     *
     * @param userId The ID of the user whose addresses to retrieve
     * @return List of AddressResponse DTOs
     */
    @Transactional(readOnly = true)  // Read-only optimization
    public List<AddressResponse> getUserAddresses(Long userId) {
        // Verify user exists
        // Even though we're just getting addresses, we validate the user ID
        // This prevents someone from guessing user IDs to see if they exist
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }

        // Get all addresses for this user
        // This uses a custom query: SELECT * FROM addresses WHERE user_id = ?
        List<Address> addresses = addressRepository.findByUserId(userId);

        // Convert each Address entity to AddressResponse DTO
        // Stream API:
        // 1. addresses.stream() - convert List to Stream
        // 2. .map(this::mapToAddressResponse) - transform Address → AddressResponse
        // 3. .collect(Collectors.toList()) - collect back to List
        return addresses.stream()
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific address by ID
     *
     * This method retrieves a single address.
     * It verifies that the address belongs to the specified user
     * to prevent unauthorized access.
     *
     * @param userId The ID of the user who owns the address
     * @param addressId The ID of the address to retrieve
     * @return AddressResponse DTO
     * @throws ResourceNotFoundException if address doesn't exist
     * @throws BadRequestException if address doesn't belong to user
     */
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(Long userId, Long addressId) {
        // Find the address by ID
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with ID: " + addressId
                ));

        // Security check: Verify this address belongs to the requesting user
        // This prevents users from accessing other users' addresses
        if (!address.getUser().getId().equals(userId)) {
            throw new BadRequestException(
                    "Address " + addressId + " does not belong to user " + userId
            );
        }

        // Return address data
        return mapToAddressResponse(address);
    }

    /**
     * Update an existing address
     *
     * This method updates address details.
     * Users can update all fields except the user relationship.
     *
     * @param userId The ID of the user who owns the address
     * @param addressId The ID of the address to update
     * @param request AddressRequest DTO with updated data
     * @return Updated AddressResponse DTO
     * @throws ResourceNotFoundException if address doesn't exist
     * @throws BadRequestException if address doesn't belong to user
     */
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        // Find the existing address
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with ID: " + addressId
                ));

        // Security check: Verify ownership
        if (!address.getUser().getId().equals(userId)) {
            throw new BadRequestException(
                    "Address " + addressId + " does not belong to user " + userId
            );
        }

        // If setting this address as default, unset other defaults
        if (request.getIsDefault() != null && request.getIsDefault() && !address.getIsDefault()) {
            // Find current default addresses
            List<Address> currentDefaults = addressRepository.findByUserIdAndIsDefaultTrue(userId);

            // Unset all current defaults
            for (Address currentDefault : currentDefaults) {
                // Skip the address we're updating (we'll set it to default below)
                if (!currentDefault.getId().equals(addressId)) {
                    currentDefault.setIsDefault(false);
                    addressRepository.save(currentDefault);
                }
            }
        }

        // Update address fields
        // We update all fields even if they haven't changed
        // Alternative approach: Only update changed fields (more complex)
        address.setStreetAddress(request.getStreetAddress());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        address.setAddressType(request.getAddressType());

        // Update default flag
        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }

        // Save updated address
        // JPA will:
        // 1. Detect changes (dirty checking)
        // 2. Update updated_at timestamp (via @PreUpdate)
        // 3. Generate UPDATE SQL
        Address updatedAddress = addressRepository.save(address);

        // Return updated data
        return mapToAddressResponse(updatedAddress);
    }

    /**
     * Delete an address
     *
     * This method permanently deletes an address from the database.
     *
     * Business rule: If deleting the default address and other addresses exist,
     * automatically make the first remaining address the new default.
     *
     * @param userId The ID of the user who owns the address
     * @param addressId The ID of the address to delete
     * @throws ResourceNotFoundException if address doesn't exist
     * @throws BadRequestException if address doesn't belong to user
     */
    public void deleteAddress(Long userId, Long addressId) {
        // Find the address
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with ID: " + addressId
                ));

        // Security check: Verify ownership
        if (!address.getUser().getId().equals(userId)) {
            throw new BadRequestException(
                    "Address " + addressId + " does not belong to user " + userId
            );
        }

        // Check if we're deleting the default address
        boolean wasDefault = address.getIsDefault();

        // Delete the address
        // This will cascade delete if there are any related records
        addressRepository.delete(address);

        // If we deleted the default address, make another address default
        if (wasDefault) {
            // Get remaining addresses for this user
            List<Address> remainingAddresses = addressRepository.findByUserId(userId);

            // If user still has addresses, make the first one default
            if (!remainingAddresses.isEmpty()) {
                Address newDefault = remainingAddresses.get(0);  // Get first address
                newDefault.setIsDefault(true);
                addressRepository.save(newDefault);
            }
            // If no addresses remain, nothing to do (user has no addresses)
        }
    }

    /**
     * Set an address as the default
     *
     * This method marks one address as default and unsets all others.
     * This is a convenience method - same can be achieved with updateAddress.
     *
     * @param userId The ID of the user who owns the address
     * @param addressId The ID of the address to set as default
     * @return Updated AddressResponse DTO
     * @throws ResourceNotFoundException if address doesn't exist
     * @throws BadRequestException if address doesn't belong to user
     */
    public AddressResponse setDefaultAddress(Long userId, Long addressId) {
        // Find the address
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with ID: " + addressId
                ));

        // Security check: Verify ownership
        if (!address.getUser().getId().equals(userId)) {
            throw new BadRequestException(
                    "Address " + addressId + " does not belong to user " + userId
            );
        }

        // If already default, nothing to do
        if (address.getIsDefault()) {
            return mapToAddressResponse(address);
        }

        // Unset all current default addresses for this user
        List<Address> currentDefaults = addressRepository.findByUserIdAndIsDefaultTrue(userId);
        for (Address currentDefault : currentDefaults) {
            currentDefault.setIsDefault(false);
            addressRepository.save(currentDefault);
        }

        // Set this address as default
        address.setIsDefault(true);
        Address updatedAddress = addressRepository.save(address);

        // Return updated address
        return mapToAddressResponse(updatedAddress);
    }

    /**
     * Get user's default address
     *
     * This method retrieves the default address for quick checkout.
     *
     * @param userId The ID of the user
     * @return AddressResponse DTO of default address
     * @throws ResourceNotFoundException if user has no default address
     */
    @Transactional(readOnly = true)
    public AddressResponse getDefaultAddress(Long userId) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }

        // Get all default addresses (should be only one, but using List for safety)
        List<Address> defaultAddresses = addressRepository.findByUserIdAndIsDefaultTrue(userId);

        // Check if default address exists
        if (defaultAddresses.isEmpty()) {
            throw new ResourceNotFoundException("No default address found for user " + userId);
        }

        // Return the first default address
        // If there are multiple (data inconsistency), we just return the first
        return mapToAddressResponse(defaultAddresses.get(0));
    }

    /**
     * Helper method: Convert Address entity to AddressResponse DTO
     *
     * This method transforms database entities to API response objects.
     *
     * Why DTOs?
     * - Hide user relationship details (prevent infinite recursion in JSON)
     * - Include only necessary data in API response
     * - Flexibility to add computed fields
     *
     * @param address The Address entity from database
     * @return AddressResponse DTO for API response
     */
    private AddressResponse mapToAddressResponse(Address address) {
        // Use builder pattern for clean object construction
        return AddressResponse.builder()
                // Copy ID
                .id(address.getId())

                // Copy address details
                .streetAddress(address.getStreetAddress())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())

                // Copy flags
                .isDefault(address.getIsDefault())
                .addressType(address.getAddressType())

                // Copy timestamps
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())

                // Note: We don't include user details here
                // The client already knows which user this belongs to
                // Including user would cause circular reference in JSON

                // Build and return
                .build();
    }
}
