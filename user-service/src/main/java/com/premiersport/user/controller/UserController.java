package com.premiersport.user.controller;

import com.premiersport.common.dto.ApiResponse;
import com.premiersport.user.dto.ChangePasswordRequest;
import com.premiersport.user.dto.UpdateProfileRequest;
import com.premiersport.user.dto.UpdateRoleRequest;
import com.premiersport.user.dto.UpdateStatusRequest;
import com.premiersport.user.entity.UserEntity;
import com.premiersport.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserEntity>> getMe(Authentication auth) {
        UserEntity user = userService.getById(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserEntity>> updateProfile(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserEntity user = userService.updateProfile(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", user));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication auth,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @PostMapping("/me/addresses")
    public ResponseEntity<ApiResponse<UserEntity>> addAddress(
            Authentication auth,
            @Valid @RequestBody UserEntity.Address address) {
        UserEntity user = userService.addAddress(auth.getName(), address);
        return ResponseEntity.ok(ApiResponse.success("Address added", user));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserEntity>>> getAllUsers(
            @RequestParam(required = false) String email,
            Pageable pageable) {
        Page<UserEntity> users = userService.getAllUsers(email, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserEntity>> getUserById(@PathVariable String id) {
        UserEntity user = userService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserEntity>> updateRole(
            @PathVariable String id,
            @Valid @RequestBody UpdateRoleRequest request) {
        UserEntity user = userService.updateRole(id, request.getRole());
        return ResponseEntity.ok(ApiResponse.success("Role updated", user));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserEntity>> updateStatus(
            Authentication auth,
            @PathVariable String id,
            @Valid @RequestBody UpdateStatusRequest request) {
        UserEntity user = userService.updateStatus(id, request.getEnabled(), auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Status updated", user));
    }
}
