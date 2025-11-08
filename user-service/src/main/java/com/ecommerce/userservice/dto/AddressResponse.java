package com.ecommerce.userservice.dto;

import com.ecommerce.userservice.entity.Address.AddressType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Address Response DTO
 * Used in API responses for address data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse {

    // Address ID
    private Long id;

    // Address details
    private String streetAddress;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    // Default and type flags
    private Boolean isDefault;
    private AddressType addressType;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
