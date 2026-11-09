package com.angel.ecomerce.service.address;

import com.angel.ecomerce.entity.Address;
import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.repository.AddressesRepository;
import com.angel.ecomerce.repository.OrdersRepository;
import com.angel.ecomerce.repository.UsersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AddressService implements IAddress {
    private final AddressesRepository addressesRepository;
    private final UsersRepository usersRepository;
    private final OrdersRepository ordersRepository;

    public AddressService(AddressesRepository addressesRepository,
                          UsersRepository usersRepository,
                          OrdersRepository ordersRepository) {
        this.addressesRepository = addressesRepository;
        this.usersRepository = usersRepository;
        this.ordersRepository = ordersRepository;
    }

    @Override
    @Transactional
    public Address save(Long userId, Address address) {
        if (userId == null) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el usuario con id " + userId));

        validate(address);
        address.setId(null);
        address.setUser(user);
        return addressesRepository.save(address);
    }

    @Override
    public List<Address> getAll() {
        return addressesRepository.findAllByOrderByIdAsc();
    }

    @Override
    public List<Address> getByUserId(Long userId) {
        return addressesRepository.findByUserIdOrderByIdAsc(userId);
    }

    @Override
    public Optional<Address> findById(Long id) {
        return addressesRepository.findById(id);
    }

    @Override
    public Optional<Address> findByIdAndUserId(Long id, Long userId) {
        return addressesRepository.findByIdAndUserId(id, userId);
    }

    @Override
    @Transactional
    public Optional<Address> updateById(Long id, Address updatedAddress) {
        validate(updatedAddress);

        return addressesRepository.findById(id)
                .map(existingAddress -> {
                    existingAddress.setStreet(updatedAddress.getStreet());
                    existingAddress.setCity(updatedAddress.getCity());
                    existingAddress.setState(updatedAddress.getState());
                    existingAddress.setZipCode(updatedAddress.getZipCode());
                    existingAddress.setCountry(updatedAddress.getCountry());
                    return addressesRepository.save(existingAddress);
                });
    }

    @Override
    @Transactional
    public String deleteById(Long id) {
        if (!addressesRepository.existsById(id)) {
            return "No existe una dirección con ese id";
        }
        if (ordersRepository.existsByAddressId(id)) {
            return "No se puede eliminar la dirección porque está asociada a pedidos";
        }
        addressesRepository.deleteById(id);
        return "Se eliminó exitosamente";
    }

    private void validate(Address address) {
        if (address.getStreet() == null || address.getStreet().isBlank()) {
            throw new IllegalArgumentException("La calle es obligatoria");
        }
        if (address.getCity() == null || address.getCity().isBlank()) {
            throw new IllegalArgumentException("La ciudad es obligatoria");
        }
        if (address.getCountry() == null || address.getCountry().isBlank()) {
            throw new IllegalArgumentException("El país es obligatorio");
        }
    }
}
