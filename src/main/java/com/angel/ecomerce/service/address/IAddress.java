package com.angel.ecomerce.service.address;

import com.angel.ecomerce.entity.Address;

import java.util.List;
import java.util.Optional;

public interface IAddress {
    Address save(Long userId, Address address);

    List<Address> getAll();

    List<Address> getByUserId(Long userId);

    Optional<Address> findById(Long id);

    Optional<Address> findByIdAndUserId(Long id, Long userId);

    Optional<Address> updateById(Long id, Address updatedAddress);

    String deleteById(Long id);
}
