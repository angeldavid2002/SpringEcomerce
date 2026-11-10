package com.angel.ecomerce.service.address;

import com.angel.ecomerce.entity.Address;
import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.repository.AddressesRepository;
import com.angel.ecomerce.repository.OrdersRepository;
import com.angel.ecomerce.repository.UsersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
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
        if (address == null) {
            throw new IllegalArgumentException("La dirección es obligatoria");
        }
        if (address.getId() != null) {
            throw new IllegalArgumentException("Para actualizar una dirección use updateById");
        }

        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("No existe el usuario con id " + userId));

        normalize(address);
        validate(address);
        address.setUser(user);
        return addressesRepository.save(address);
    }

    @Override
    public List<Address> getAll() {
        return addressesRepository.findAllByOrderByIdAsc();
    }

    @Override
    public List<Address> getByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }
        return addressesRepository.findByUserIdOrderByIdAsc(userId);
    }

    @Override
    public Optional<Address> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return addressesRepository.findById(id);
    }

    @Override
    public Optional<Address> findByIdAndUserId(Long id, Long userId) {
        if (id == null || userId == null) {
            return Optional.empty();
        }
        return addressesRepository.findByIdAndUserId(id, userId);
    }

    @Override
    @Transactional
    public Address updateById(Long id, Address updatedAddress) {
        if (id == null) {
            throw new IllegalArgumentException("El id de la dirección es obligatorio");
        }
        if (updatedAddress == null) {
            throw new IllegalArgumentException("La dirección es obligatoria");
        }

        return addressesRepository.findById(id)
                .map(existingAddress -> {
                    normalize(updatedAddress);
                    validate(updatedAddress);

                    existingAddress.setStreet(updatedAddress.getStreet());
                    existingAddress.setCity(updatedAddress.getCity());
                    existingAddress.setState(updatedAddress.getState());
                    existingAddress.setZipCode(updatedAddress.getZipCode());
                    existingAddress.setCountry(updatedAddress.getCountry());
                    return addressesRepository.save(existingAddress);
                })
                .orElseThrow(() -> new NoSuchElementException("No existe una dirección con ese id"));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El id de la dirección es obligatorio");
        }
        if (!addressesRepository.existsById(id)) {
            throw new NoSuchElementException("No existe una dirección con ese id");
        }
        if (ordersRepository.existsByAddressId(id)) {
            throw new IllegalStateException("No se puede eliminar la dirección porque está asociada a pedidos");
        }
        addressesRepository.deleteById(id);
    }

    private void normalize(Address address) {
        address.setStreet(trim(address.getStreet()));
        address.setCity(trim(address.getCity()));
        address.setState(trim(address.getState()));
        address.setZipCode(trim(address.getZipCode()));
        address.setCountry(trim(address.getCountry()));
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

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
