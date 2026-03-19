package com.premiersport.order.controller;

import com.premiersport.common.dto.ApiResponse;
import com.premiersport.order.config.UserPrincipal;
import com.premiersport.order.dto.CreateOrderRequest;
import com.premiersport.order.dto.UpdateOrderStatusRequest;
import com.premiersport.order.entity.OrderEntity;
import com.premiersport.order.service.OrderService;
import com.premiersport.order.service.StripeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final StripeService stripeService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderEntity>> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.createFromCart(principal.userId(), principal.email(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderEntity>>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getByUser(principal.userId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderEntity>> getOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        return ResponseEntity.ok(ApiResponse.success(orderService.getById(id, principal.userId(), isAdmin)));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderEntity>>> getAllOrders(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllAdmin(pageable)));
    }

    @PutMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderEntity>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(orderService.updateStatus(id, request.getStatus())));
    }

    // --- Payment endpoints (merged from PaymentController, fix #16) ---

    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<Map<String, String>>> createPaymentIntent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        Map<String, String> result = stripeService.createPaymentIntent(id, principal.userId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/webhook/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        stripeService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok("ok");
    }
}
