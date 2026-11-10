package com.angel.ecomerce.dto.address;

import com.angel.ecomerce.entity.Address;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressCreateRequest(
        @NotBlank(message = "La calle es obligatoria")
        @Size(max = 255, message = "La calle no puede superar los 255 caracteres")
        String street,
        @NotBlank(message = "La ciudad es obligatoria")
        @Size(max = 255, message = "La ciudad no puede superar los 255 caracteres")
        String city,
        @Size(max = 255, message = "El estado no puede superar los 255 caracteres")
        String state,
        @Size(max = 255, message = "El código postal no puede superar los 255 caracteres")
        String zipCode,
        @NotBlank(message = "El país es obligatorio")
        @Size(max = 255, message = "El país no puede superar los 255 caracteres")
        String country
) {
    public Address toEntity() {
        Address address = new Address();
        address.setStreet(street);
        address.setCity(city);
        address.setState(state);
        address.setZipCode(zipCode);
        address.setCountry(country);
        return address;
    }
}
