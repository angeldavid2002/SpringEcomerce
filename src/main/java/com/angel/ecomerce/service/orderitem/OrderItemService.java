package com.angel.ecomerce.service.orderitem;

import com.angel.ecomerce.entity.Order;
import com.angel.ecomerce.entity.OrderItem;
import com.angel.ecomerce.repository.OrderItemsRepository;
import com.angel.ecomerce.service.order.IOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class OrderItemService implements IOrderItem {
    private final OrderItemsRepository orderItemsRepository;
    private final IOrder orderService;

    public OrderItemService(OrderItemsRepository orderItemsRepository, IOrder orderService) {
        this.orderItemsRepository = orderItemsRepository;
        this.orderService = orderService;
    }

    @Override
    public List<OrderItem> getByOrderId(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("El pedido es obligatorio");
        }
        return orderItemsRepository.findByOrderIdOrderByIdAsc(orderId);
    }

    @Override
    public Optional<OrderItem> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return orderItemsRepository.findById(id);
    }

    @Override
    @Transactional
    public Order addItem(Long orderId, Long productId, int quantity) {
        return orderService.addItem(orderId, productId, quantity);
    }

    @Override
    @Transactional
    public Order updateQuantity(Long orderId, Long orderItemId, int quantity) {
        return orderService.updateItemQuantity(orderId, orderItemId, quantity);
    }

    @Override
    @Transactional
    public Order removeItem(Long orderId, Long orderItemId) {
        return orderService.removeItem(orderId, orderItemId);
    }

    @Override
    @Transactional
    public void deleteById(Long orderItemId) {
        if (orderItemId == null) {
            throw new IllegalArgumentException("El id del item de pedido es obligatorio");
        }

        OrderItem item = orderItemsRepository.findById(orderItemId)
                .orElseThrow(() -> new NoSuchElementException("No existe un item de pedido con ese id"));
        orderService.removeItem(item.getOrder().getId(), orderItemId);
    }
}
