package com.premiersport.order.service;

import com.premiersport.common.exception.ApiException;
import com.premiersport.order.client.ProductServiceClient;
import com.premiersport.order.dto.AddCartItemRequest;
import com.premiersport.order.entity.CartEntity;
import com.premiersport.order.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductServiceClient productServiceClient;

    public CartEntity getOrCreateCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(
                        CartEntity.builder()
                                .userId(userId)
                                .items(new ArrayList<>())
                                .build()
                ));
    }

    public CartEntity addItem(String userId, AddCartItemRequest request) {
        // Fetch authoritative price from product-service to prevent client-side tampering (#2)
        double serverPrice = productServiceClient.getProductPrice(request.getProductId());

        CartEntity cart = getOrCreateCart(userId);

        // Merge quantities if same product+size already in cart
        boolean merged = false;
        for (CartEntity.CartItem existing : cart.getItems()) {
            if (existing.getProductId().equals(request.getProductId())
                    && existing.getSize().equals(request.getSize())) {
                existing.setQuantity(existing.getQuantity() + request.getQuantity());
                // Also refresh the price in case it changed since last add
                existing.setUnitPrice(serverPrice);
                merged = true;
                break;
            }
        }

        if (!merged) {
            CartEntity.CartItem item = CartEntity.CartItem.builder()
                    .productId(request.getProductId())
                    .productName(request.getProductName())
                    .productImage(request.getProductImage())
                    .size(request.getSize())
                    .color(request.getColor())
                    .quantity(request.getQuantity())
                    .unitPrice(serverPrice)
                    .build();
            cart.getItems().add(item);
        }

        return cartRepository.save(cart);
    }

    public CartEntity updateItem(String userId, int itemIndex, int quantity) {
        CartEntity cart = getOrCreateCart(userId);
        validateIndex(cart, itemIndex);
        cart.getItems().get(itemIndex).setQuantity(quantity);
        return cartRepository.save(cart);
    }

    public CartEntity removeItem(String userId, int itemIndex) {
        CartEntity cart = getOrCreateCart(userId);
        validateIndex(cart, itemIndex);
        cart.getItems().remove(itemIndex);
        return cartRepository.save(cart);
    }

    public void clearCart(String userId) {
        CartEntity cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private void validateIndex(CartEntity cart, int index) {
        if (index < 0 || index >= cart.getItems().size()) {
            throw ApiException.badRequest("Item index " + index + " is out of range");
        }
    }
}
