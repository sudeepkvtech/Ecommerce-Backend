package com.ecommerce.userservice.controller;

// Import DTOs for request and response
import com.ecommerce.userservice.dto.AddressRequest;
import com.ecommerce.userservice.dto.AddressResponse;
import com.ecommerce.userservice.dto.UserResponse;

// Import services
import com.ecommerce.userservice.service.AddressService;
import com.ecommerce.userservice.service.UserService;

// Import Spring Framework annotations
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Import validation
import jakarta.validation.Valid;

// Import Lombok
import lombok.RequiredArgsConstructor;

// Import List
import java.util.List;

/**
 * Address Controller
 *
 * REST controller for address management operations.
 *
 * This controller handles HTTP requests for:
 * - Creating new addresses (shipping/billing)
 * - Retrieving user's addresses
 * - Updating existing addresses
 * - Deleting addresses
 * - Setting default address
 *
 * Base URL: /api/users/addresses
 * All endpoints require authentication (JWT token)
 *
 * Address types:
 * --------------
 * - SHIPPING: For product delivery
 * - BILLING: For payment information
 * - A user can have multiple addresses of each type
 * - One address can be marked as default for quick checkout
 *
 * Why have separate Address Controller?
 * --------------------------------------
 * Addresses are a sub-resource of users, but they have their own CRUD operations.
 * We could put address endpoints in UserController, but separating them:
 * - Keeps controllers focused and maintainable
 * - Makes URL structure clearer
 * - Allows independent testing
 * - Follows RESTful design principles
 *
 * RESTful URL patterns:
 * ---------------------
 * /api/users/addresses → All addresses for current user
 * /api/users/addresses/{id} → Specific address
 * /api/users/addresses/{id}/set-default → Action on specific address
 */

// @RestController = @Controller + @ResponseBody
// All method return values are automatically serialized to JSON
@RestController

// @RequestMapping sets base URL for all endpoints in this controller
// All URLs start with /api/users/addresses
@RequestMapping("/api/users/addresses")

// @RequiredArgsConstructor (Lombok) generates constructor for final fields
// Enables dependency injection
@RequiredArgsConstructor
public class AddressController {

    // AddressService for address operations
    // Injected via constructor
    private final AddressService addressService;

    // UserService to get current user information
    // Needed to extract user ID from authentication
    private final UserService userService;

    /**
     * Create a new address
     *
     * This endpoint creates a new shipping or billing address for the current user.
     *
     * URL: POST /api/users/addresses
     * Authentication: Required (JWT token)
     * Authorization: Any authenticated user
     *
     * Request body:
     * {
     *   "streetAddress": "123 Main St, Apt 4B",
     *   "city": "New York",
     *   "state": "NY",
     *   "postalCode": "10001",
     *   "country": "USA",
     *   "addressType": "SHIPPING",  // or "BILLING"
     *   "isDefault": false
     * }
     *
     * Response: 201 Created
     * {
     *   "id": 1,
     *   "streetAddress": "123 Main St, Apt 4B",
     *   "city": "New York",
     *   "state": "NY",
     *   "postalCode": "10001",
     *   "country": "USA",
     *   "addressType": "SHIPPING",
     *   "isDefault": true,  // Auto-set to true if first address
     *   "createdAt": "2024-01-15T10:30:00",
     *   "updatedAt": "2024-01-15T10:30:00"
     * }
     *
     * Business rules:
     * ---------------
     * - If this is the user's first address, automatically set as default
     * - If isDefault=true, unset all other default addresses first
     * - Address belongs to the authenticated user (cannot create for others)
     *
     * @param request AddressRequest DTO with address details
     * @param authentication Current authenticated user
     * @return ResponseEntity with created AddressResponse and HTTP 201 Created
     */
    @PostMapping  // Maps to POST /api/users/addresses
    public ResponseEntity<AddressResponse> createAddress(
            // @Valid triggers validation on AddressRequest
            // Checks @NotBlank, @Size annotations on all fields
            // If validation fails, throws MethodArgumentNotValidException → HTTP 400
            @Valid @RequestBody AddressRequest request,

            // Current authenticated user (from JWT token)
            // Spring Security automatically injects this
            Authentication authentication) {

        // Extract email from authentication
        // This is the username stored in the JWT token
        String email = authentication.getName();

        // Get current user to retrieve their ID
        // We need the user ID to associate the address with the correct user
        UserResponse currentUser = userService.getCurrentUserProfile(email);

        // Create the address via service
        // Service will:
        // 1. Verify user exists
        // 2. Check if this should be default
        // 3. Unset other defaults if needed
        // 4. Create and save address
        // 5. Return AddressResponse DTO
        AddressResponse createdAddress = addressService.createAddress(
                currentUser.getId(),
                request
        );

        // Return HTTP 201 Created with the created address
        // HTTP 201 indicates successful resource creation
        // ResponseEntity.status(HttpStatus.CREATED).body(createdAddress)
        return new ResponseEntity<>(createdAddress, HttpStatus.CREATED);
    }

    /**
     * Get all addresses for current user
     *
     * This endpoint retrieves all addresses (shipping and billing) for the authenticated user.
     *
     * URL: GET /api/users/addresses
     * Authentication: Required (JWT token)
     * Authorization: Any authenticated user
     *
     * Response: 200 OK
     * [
     *   {
     *     "id": 1,
     *     "streetAddress": "123 Main St",
     *     "city": "New York",
     *     "state": "NY",
     *     "postalCode": "10001",
     *     "country": "USA",
     *     "addressType": "SHIPPING",
     *     "isDefault": true,
     *     "createdAt": "2024-01-15T10:30:00",
     *     "updatedAt": "2024-01-15T10:30:00"
     *   },
     *   {
     *     "id": 2,
     *     "streetAddress": "456 Oak Ave",
     *     "city": "Boston",
     *     "state": "MA",
     *     "postalCode": "02101",
     *     "country": "USA",
     *     "addressType": "BILLING",
     *     "isDefault": false,
     *     "createdAt": "2024-01-16T14:20:00",
     *     "updatedAt": "2024-01-16T14:20:00"
     *   }
     * ]
     *
     * Returns empty array [] if user has no addresses
     *
     * @param authentication Current authenticated user
     * @return ResponseEntity with List of AddressResponse and HTTP 200 OK
     */
    @GetMapping  // Maps to GET /api/users/addresses
    public ResponseEntity<List<AddressResponse>> getUserAddresses(
            Authentication authentication) {

        // Extract email from authentication
        String email = authentication.getName();

        // Get current user ID
        UserResponse currentUser = userService.getCurrentUserProfile(email);

        // Get all addresses for this user
        // Service will:
        // 1. Verify user exists
        // 2. Query all addresses for user_id
        // 3. Convert each Address entity to AddressResponse DTO
        // 4. Return List<AddressResponse>
        List<AddressResponse> addresses = addressService.getUserAddresses(currentUser.getId());

        // Return HTTP 200 OK with list of addresses
        // List is automatically serialized to JSON array
        return ResponseEntity.ok(addresses);
    }

    /**
     * Get a specific address by ID
     *
     * This endpoint retrieves a single address.
     * Users can only access their own addresses (ownership verified by service).
     *
     * URL: GET /api/users/addresses/{id}
     * Authentication: Required (JWT token)
     * Authorization: Must be address owner
     *
     * Request: GET /api/users/addresses/123
     *
     * Response: 200 OK
     * {
     *   "id": 123,
     *   "streetAddress": "123 Main St",
     *   "city": "New York",
     *   "state": "NY",
     *   "postalCode": "10001",
     *   "country": "USA",
     *   "addressType": "SHIPPING",
     *   "isDefault": true,
     *   "createdAt": "2024-01-15T10:30:00",
     *   "updatedAt": "2024-01-15T10:30:00"
     * }
     *
     * Error responses:
     * - 404 Not Found: Address doesn't exist
     * - 400 Bad Request: Address doesn't belong to current user
     *
     * @param id The ID of the address to retrieve
     * @param authentication Current authenticated user
     * @return ResponseEntity with AddressResponse and HTTP 200 OK
     */
    @GetMapping("/{id}")  // Maps to GET /api/users/addresses/{id}
    public ResponseEntity<AddressResponse> getAddressById(
            // @PathVariable extracts {id} from URL
            // Example: GET /api/users/addresses/123 → id = 123
            @PathVariable Long id,

            Authentication authentication) {

        // Extract email and get user ID
        String email = authentication.getName();
        UserResponse currentUser = userService.getCurrentUserProfile(email);

        // Get address by ID
        // Service will:
        // 1. Find address by ID
        // 2. Verify it belongs to current user
        // 3. If not, throw BadRequestException → HTTP 400
        // 4. Return AddressResponse DTO
        AddressResponse address = addressService.getAddressById(
                currentUser.getId(),
                id
        );

        // Return HTTP 200 OK with address data
        return ResponseEntity.ok(address);
    }

    /**
     * Update an existing address
     *
     * This endpoint updates address details.
     * Users can only update their own addresses.
     *
     * URL: PUT /api/users/addresses/{id}
     * Authentication: Required (JWT token)
     * Authorization: Must be address owner
     *
     * Request: PUT /api/users/addresses/123
     * {
     *   "streetAddress": "456 New St",
     *   "city": "Boston",
     *   "state": "MA",
     *   "postalCode": "02101",
     *   "country": "USA",
     *   "addressType": "SHIPPING",
     *   "isDefault": true
     * }
     *
     * Response: 200 OK
     * {
     *   "id": 123,
     *   "streetAddress": "456 New St",
     *   "city": "Boston",
     *   "state": "MA",
     *   "postalCode": "02101",
     *   "country": "USA",
     *   "addressType": "SHIPPING",
     *   "isDefault": true,
     *   "createdAt": "2024-01-15T10:30:00",
     *   "updatedAt": "2024-01-15T11:00:00"  // Updated timestamp
     * }
     *
     * Business rules:
     * ---------------
     * - If setting isDefault=true, unset all other default addresses
     * - All fields are updated (even if unchanged)
     * - Cannot change which user owns the address
     *
     * @param id The ID of the address to update
     * @param request AddressRequest DTO with updated data
     * @param authentication Current authenticated user
     * @return ResponseEntity with updated AddressResponse and HTTP 200 OK
     */
    @PutMapping("/{id}")  // Maps to PUT /api/users/addresses/{id}
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long id,

            // @Valid triggers validation
            @Valid @RequestBody AddressRequest request,

            Authentication authentication) {

        // Extract email and get user ID
        String email = authentication.getName();
        UserResponse currentUser = userService.getCurrentUserProfile(email);

        // Update address via service
        // Service will:
        // 1. Find address by ID
        // 2. Verify ownership
        // 3. Handle default address logic
        // 4. Update all fields
        // 5. Save to database
        // 6. Return updated AddressResponse
        AddressResponse updatedAddress = addressService.updateAddress(
                currentUser.getId(),
                id,
                request
        );

        // Return HTTP 200 OK with updated address
        return ResponseEntity.ok(updatedAddress);
    }

    /**
     * Delete an address
     *
     * This endpoint permanently removes an address.
     * Users can only delete their own addresses.
     *
     * URL: DELETE /api/users/addresses/{id}
     * Authentication: Required (JWT token)
     * Authorization: Must be address owner
     *
     * Request: DELETE /api/users/addresses/123
     *
     * Response: 200 OK
     * {
     *   "message": "Address deleted successfully"
     * }
     *
     * Business rules:
     * ---------------
     * - Address is permanently deleted (not soft delete)
     * - If deleting default address, another address becomes default automatically
     * - If no addresses remain, user has no default (OK)
     *
     * Error responses:
     * - 404 Not Found: Address doesn't exist
     * - 400 Bad Request: Address doesn't belong to current user
     *
     * @param id The ID of the address to delete
     * @param authentication Current authenticated user
     * @return ResponseEntity with success message and HTTP 200 OK
     */
    @DeleteMapping("/{id}")  // Maps to DELETE /api/users/addresses/{id}
    public ResponseEntity<String> deleteAddress(
            @PathVariable Long id,
            Authentication authentication) {

        // Extract email and get user ID
        String email = authentication.getName();
        UserResponse currentUser = userService.getCurrentUserProfile(email);

        // Delete address via service
        // Service will:
        // 1. Find address by ID
        // 2. Verify ownership
        // 3. Check if it was default
        // 4. Delete from database
        // 5. If was default, make another address default
        addressService.deleteAddress(currentUser.getId(), id);

        // Return HTTP 200 OK with success message
        return ResponseEntity.ok("Address deleted successfully");
    }

    /**
     * Set an address as default
     *
     * This endpoint marks one address as the default for quick checkout.
     * All other addresses are automatically unset as default.
     *
     * URL: PUT /api/users/addresses/{id}/set-default
     * Authentication: Required (JWT token)
     * Authorization: Must be address owner
     *
     * Request: PUT /api/users/addresses/123/set-default
     *
     * Response: 200 OK
     * {
     *   "id": 123,
     *   "streetAddress": "123 Main St",
     *   "city": "New York",
     *   "state": "NY",
     *   "postalCode": "10001",
     *   "country": "USA",
     *   "addressType": "SHIPPING",
     *   "isDefault": true,  // Now set to true
     *   "createdAt": "2024-01-15T10:30:00",
     *   "updatedAt": "2024-01-15T11:00:00"
     * }
     *
     * This is a convenience endpoint - same can be achieved with PUT /addresses/{id}
     * but this makes the intent clearer and requires less data in request.
     *
     * @param id The ID of the address to set as default
     * @param authentication Current authenticated user
     * @return ResponseEntity with updated AddressResponse and HTTP 200 OK
     */
    @PutMapping("/{id}/set-default")  // Maps to PUT /api/users/addresses/{id}/set-default
    public ResponseEntity<AddressResponse> setDefaultAddress(
            @PathVariable Long id,
            Authentication authentication) {

        // Extract email and get user ID
        String email = authentication.getName();
        UserResponse currentUser = userService.getCurrentUserProfile(email);

        // Set as default via service
        // Service will:
        // 1. Find address by ID
        // 2. Verify ownership
        // 3. If already default, do nothing
        // 4. Unset all other default addresses
        // 5. Set this address as default
        // 6. Return updated AddressResponse
        AddressResponse updatedAddress = addressService.setDefaultAddress(
                currentUser.getId(),
                id
        );

        // Return HTTP 200 OK with updated address
        return ResponseEntity.ok(updatedAddress);
    }

    /**
     * Get user's default address
     *
     * This endpoint retrieves the default address for quick checkout.
     *
     * URL: GET /api/users/addresses/default
     * Authentication: Required (JWT token)
     * Authorization: Any authenticated user
     *
     * Response: 200 OK
     * {
     *   "id": 1,
     *   "streetAddress": "123 Main St",
     *   "city": "New York",
     *   "state": "NY",
     *   "postalCode": "10001",
     *   "country": "USA",
     *   "addressType": "SHIPPING",
     *   "isDefault": true,
     *   "createdAt": "2024-01-15T10:30:00",
     *   "updatedAt": "2024-01-15T10:30:00"
     * }
     *
     * Error responses:
     * - 404 Not Found: User has no default address
     *
     * @param authentication Current authenticated user
     * @return ResponseEntity with AddressResponse and HTTP 200 OK
     */
    @GetMapping("/default")  // Maps to GET /api/users/addresses/default
    public ResponseEntity<AddressResponse> getDefaultAddress(
            Authentication authentication) {

        // Extract email and get user ID
        String email = authentication.getName();
        UserResponse currentUser = userService.getCurrentUserProfile(email);

        // Get default address via service
        // Service will:
        // 1. Verify user exists
        // 2. Query for addresses with isDefault=true
        // 3. If none found, throw ResourceNotFoundException → HTTP 404
        // 4. Return first default address
        AddressResponse defaultAddress = addressService.getDefaultAddress(currentUser.getId());

        // Return HTTP 200 OK with default address
        return ResponseEntity.ok(defaultAddress);
    }

    // =========================================================================
    // RESTful API Design Principles Used
    // =========================================================================
    //
    // 1. Resource-oriented URLs:
    //    - /api/users/addresses (collection)
    //    - /api/users/addresses/{id} (specific resource)
    //    - /api/users/addresses/{id}/set-default (action on resource)
    //
    // 2. HTTP methods match operations:
    //    - GET: Retrieve (read-only)
    //    - POST: Create new resource
    //    - PUT: Update existing resource
    //    - DELETE: Remove resource
    //
    // 3. Consistent response formats:
    //    - Success: HTTP 200/201 with JSON data
    //    - Error: HTTP 4xx/5xx with ErrorResponse
    //
    // 4. Stateless:
    //    - Each request contains JWT token
    //    - Server doesn't maintain session state
    //    - Can scale horizontally
    //
    // 5. Standard status codes:
    //    - 200 OK: Successful GET, PUT, DELETE
    //    - 201 Created: Successful POST
    //    - 400 Bad Request: Validation errors
    //    - 404 Not Found: Resource doesn't exist
    //
    // =========================================================================
    // Security Considerations
    // =========================================================================
    //
    // 1. Authentication:
    //    - All endpoints require JWT token
    //    - JwtAuthenticationFilter validates token
    //    - Unauthenticated requests → HTTP 401
    //
    // 2. Authorization:
    //    - Users can only access their own addresses
    //    - Service layer verifies ownership
    //    - Unauthorized access → HTTP 400
    //
    // 3. Input validation:
    //    - @Valid triggers DTO validation
    //    - Prevents malformed data
    //    - Returns clear error messages
    //
    // 4. No sensitive data in responses:
    //    - Addresses are safe to return
    //    - No passwords or tokens
    //    - User data limited to owner
    //
    // =========================================================================
    // Testing Examples
    // =========================================================================
    //
    // 1. Create address:
    //    curl -X POST http://localhost:8082/user-service/api/users/addresses \
    //      -H "Authorization: Bearer YOUR_JWT_TOKEN" \
    //      -H "Content-Type: application/json" \
    //      -d '{
    //        "streetAddress": "123 Main St",
    //        "city": "New York",
    //        "state": "NY",
    //        "postalCode": "10001",
    //        "country": "USA",
    //        "addressType": "SHIPPING",
    //        "isDefault": true
    //      }'
    //
    // 2. Get all addresses:
    //    curl http://localhost:8082/user-service/api/users/addresses \
    //      -H "Authorization: Bearer YOUR_JWT_TOKEN"
    //
    // 3. Update address:
    //    curl -X PUT http://localhost:8082/user-service/api/users/addresses/1 \
    //      -H "Authorization: Bearer YOUR_JWT_TOKEN" \
    //      -H "Content-Type: application/json" \
    //      -d '{
    //        "streetAddress": "456 Oak Ave",
    //        "city": "Boston",
    //        "state": "MA",
    //        "postalCode": "02101",
    //        "country": "USA",
    //        "addressType": "SHIPPING",
    //        "isDefault": false
    //      }'
    //
    // 4. Delete address:
    //    curl -X DELETE http://localhost:8082/user-service/api/users/addresses/1 \
    //      -H "Authorization: Bearer YOUR_JWT_TOKEN"
    //
    // 5. Set default:
    //    curl -X PUT http://localhost:8082/user-service/api/users/addresses/2/set-default \
    //      -H "Authorization: Bearer YOUR_JWT_TOKEN"
}
