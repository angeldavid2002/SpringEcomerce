package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressesRepository extends JpaRepository<Address, Long> {
    Optional<Address> findByIdAndUserId(Long id, Long userId);

    List<Address> findAllByOrderByIdAsc();

    List<Address> findByUserIdOrderByIdAsc(Long userId);
}
