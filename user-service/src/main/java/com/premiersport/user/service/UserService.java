package com.premiersport.user.service;

import com.premiersport.common.exception.ApiException;
import com.premiersport.common.jwt.JwtUtil;
import com.premiersport.user.dto.*;
import com.premiersport.user.entity.UserEntity;
import com.premiersport.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ApiException.conflict("Email already registered");
        }

        UserEntity user = UserEntity.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .role(UserEntity.Role.USER)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));

        if (!user.isEnabled()) {
            throw ApiException.unauthorized("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw ApiException.unauthorized("Invalid email or password");
        }

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    public UserEntity getById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    public UserEntity updateProfile(String userId, UpdateProfileRequest request) {
        UserEntity user = getById(userId);
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        return userRepository.save(user);
    }

    public void changePassword(String userId, ChangePasswordRequest request) {
        UserEntity user = getById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw ApiException.badRequest("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getEmail());
    }

    public UserEntity addAddress(String userId, UserEntity.Address address) {
        UserEntity user = getById(userId);
        if (address.isDefault()) {
            user.getAddresses().forEach(a -> a.setDefault(false));
        }
        user.getAddresses().add(address);
        return userRepository.save(user);
    }

    public Page<UserEntity> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public UserEntity updateRole(String userId, String role) {
        UserEntity user = getById(userId);
        user.setRole(UserEntity.Role.valueOf(role.toUpperCase()));
        return userRepository.save(user);
    }

    public UserEntity updateStatus(String userId, boolean enabled) {
        UserEntity user = getById(userId);
        user.setEnabled(enabled);
        return userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(UserEntity user) {
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .accessToken(token)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole().name())
                        .addresses(user.getAddresses())
                        .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                        .build())
                .build();
    }
}
