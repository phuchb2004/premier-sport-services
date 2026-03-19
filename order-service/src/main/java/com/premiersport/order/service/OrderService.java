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
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;

    private final AtomicLong dailySequence = new AtomicLong(0);
    private volatile String lastDate = "";

    public OrderEntity createFromCart(String userId, CreateOrderRequest request) {
        CartEntity cart = cartService.getOrCreateCart(userId);

        if (cart.getItems().isEmpty()) {
            throw ApiException.badRequest("Cart is empty");
        }

        double total = cart.getItems().stream()
                .mapToDouble(i -> i.getUnitPrice() * i.getQuantity())
                .sum();

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
                .items(List.copyOf(cart.getItems()))
                .shippingAddress(address)
                .status(OrderEntity.OrderStatus.PENDING)
                .paymentStatus(OrderEntity.PaymentStatus.UNPAID)
                .total(total)
                .build();

        order = orderRepository.save(order);
        cartService.clearCart(userId);
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

    private synchronized String generateOrderNumber() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        if (!today.equals(lastDate)) {
            lastDate = today;
            dailySequence.set(0);
        }
        long seq = dailySequence.incrementAndGet();
        return String.format("PS-%s-%04d", today, seq);
    }
}
