package com.premiersport.order.controller;

import com.premiersport.common.dto.ApiResponse;
import com.premiersport.order.dto.CreateOrderRequest;
import com.premiersport.order.dto.UpdateOrderStatusRequest;
import com.premiersport.order.entity.OrderEntity;
import com.premiersport.order.service.OrderService;
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

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderEntity>> createOrder(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(orderService.createFromCart(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderEntity>>> getMyOrders(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getByUser(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderEntity>> getOrder(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        return ResponseEntity.ok(ApiResponse.success(orderService.getById(id, userId, isAdmin)));
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
}
