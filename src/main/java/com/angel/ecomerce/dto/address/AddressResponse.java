package com.angel.ecomerce.dto.address;

import com.angel.ecomerce.entity.Address;

public record AddressResponse(
        Long id,
        Long userId,
        String street,
        String city,
        String state,
        String zipCode,
        String country
) {
    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getUser() == null ? null : address.getUser().getId(),
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getZipCode(),
                address.getCountry()
        );
    }
}
