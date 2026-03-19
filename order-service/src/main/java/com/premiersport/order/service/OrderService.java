package com.premiersport.order.service;

import com.premiersport.common.exception.ApiException;
import com.premiersport.order.dto.CreateOrderRequest;
import com.premiersport.order.entity.CartEntity;
import com.premiersport.order.entity.OrderEntity;
import com.premiersport.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    static final double DELIVERY_COST = 4.99;

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final CounterService counterService;

    public OrderEntity createFromCart(String userId, String customerEmail, CreateOrderRequest request) {
        CartEntity cart = cartService.getOrCreateCart(userId);

        if (cart.getItems().isEmpty()) {
            throw ApiException.badRequest("Cart is empty");
        }

        double itemsTotal = cart.getItems().stream()
                .mapToDouble(i -> i.getUnitPrice() * i.getQuantity())
                .sum();
        double total = itemsTotal + DELIVERY_COST;

        CreateOrderRequest.ShippingAddressDto addrDto = request.getShippingAddress();
        OrderEntity.ShippingAddress address = OrderEntity.ShippingAddress.builder()
                .street(addrDto.getStreet())
                .city(addrDto.getCity())
                .state(addrDto.getState())
                .postalCode(addrDto.getPostalCode())
                .country(addrDto.getCountry())
                .build();

        OrderEntity order = OrderEntity.builder()
                .orderNumber(generateOrderNumber())
                .userId(userId)
                .customerEmail(customerEmail)
                .items(List.copyOf(cart.getItems()))
                .shippingAddress(address)
                .status(OrderEntity.OrderStatus.PENDING)
                .paymentStatus(OrderEntity.PaymentStatus.UNPAID)
                .total(total)
                .build();

        order = orderRepository.save(order);

        // Best-effort cart clear: order is already persisted, so cart sync failure is not fatal
        try {
            cartService.clearCart(userId);
        } catch (Exception e) {
            log.error("Failed to clear cart for user {} after order {} creation — cart may be stale",
                    userId, order.getOrderNumber());
        }

        log.info("Order created: {} for user {}", order.getOrderNumber(), userId);
        return order;
    }

    public OrderEntity getById(String orderId, String userId, boolean isAdmin) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));

        if (!isAdmin && !order.getUserId().equals(userId)) {
            throw ApiException.forbidden("Access denied");
        }

        return order;
    }

    public List<OrderEntity> getByUser(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Page<OrderEntity> getAllAdmin(Pageable pageable) {
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public OrderEntity updateStatus(String orderId, String newStatusStr) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));

        OrderEntity.OrderStatus newStatus;
        try {
            newStatus = OrderEntity.OrderStatus.valueOf(newStatusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Invalid status: " + newStatusStr);
        }

        validateStatusTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    private void validateStatusTransition(OrderEntity.OrderStatus current, OrderEntity.OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == OrderEntity.OrderStatus.CONFIRMED || next == OrderEntity.OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderEntity.OrderStatus.SHIPPED || next == OrderEntity.OrderStatus.CANCELLED;
            case SHIPPED -> next == OrderEntity.OrderStatus.DELIVERED;
            default -> false;
        };

        if (!valid) {
            throw ApiException.badRequest("Invalid status transition: " + current + " → " + next);
        }
    }

    private String generateOrderNumber() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = counterService.getNextSequence("order_" + today);
        return String.format("PS-%s-%04d", today, seq);
    }
}
