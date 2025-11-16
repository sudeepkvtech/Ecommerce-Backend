package com.ecommerce.userservice.dto;

import com.ecommerce.userservice.entity.Address.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Address Request DTO
 * Used when creating or updating addresses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    // Street address - required
    @NotBlank(message = "Street address is required")
    @Size(max = 500, message = "Street address cannot exceed 500 characters")
    private String streetAddress;

    // City - required
    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    // State/Province - required
    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    // Postal code - required
    @NotBlank(message = "Postal code is required")
    @Size(max = 20, message = "Postal code cannot exceed 20 characters")
    private String postalCode;

    // Country - required
    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country cannot exceed 100 characters")
    private String country;

    // Is this the default address?
    private Boolean isDefault = false;

    // Address type (SHIPPING or BILLING)
    private AddressType addressType = AddressType.SHIPPING;
}
