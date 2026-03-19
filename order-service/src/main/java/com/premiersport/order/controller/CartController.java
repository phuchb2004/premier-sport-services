package com.premiersport.order.controller;

import com.premiersport.common.dto.ApiResponse;
import com.premiersport.order.config.UserPrincipal;
import com.premiersport.order.dto.AddCartItemRequest;
import com.premiersport.order.dto.UpdateCartItemRequest;
import com.premiersport.order.entity.CartEntity;
import com.premiersport.order.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartEntity>> getCart(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(cartService.getOrCreateCart(principal.userId())));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartEntity>> addItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cartService.addItem(principal.userId(), request)));
    }

    @PutMapping("/items/{itemIndex}")
    public ResponseEntity<ApiResponse<CartEntity>> updateItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable int itemIndex,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                cartService.updateItem(principal.userId(), itemIndex, request.getQuantity())));
    }

    @DeleteMapping("/items/{itemIndex}")
    public ResponseEntity<ApiResponse<CartEntity>> removeItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable int itemIndex) {
        return ResponseEntity.ok(ApiResponse.success(cartService.removeItem(principal.userId(), itemIndex)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal UserPrincipal principal) {
        cartService.clearCart(principal.userId());
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }
}
