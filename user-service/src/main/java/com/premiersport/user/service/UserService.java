package com.premiersport.user.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.premiersport.common.exception.ApiException;
import com.premiersport.common.jwt.JwtUtil;
import com.premiersport.user.dto.*;
import com.premiersport.user.entity.UserEntity;
import com.premiersport.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       @Autowired(required = false) GoogleIdTokenVerifier googleIdTokenVerifier) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
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

        if (user.getPassword() == null) {
            throw ApiException.badRequest("Password change not available for Google-linked accounts");
        }

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
        // Ensure address has an id
        if (address.getId() == null || address.getId().isBlank()) {
            address.setId(UUID.randomUUID().toString());
        }
        user.getAddresses().add(address);
        return userRepository.save(user);
    }

    public UserEntity removeAddress(String userId, String addressId) {
        UserEntity user = getById(userId);
        boolean removed = user.getAddresses().removeIf(a -> addressId.equals(a.getId()));
        if (!removed) {
            throw ApiException.notFound("Address not found");
        }
        return userRepository.save(user);
    }

    public AuthResponse googleAuth(GoogleAuthRequest request) {
        if (googleIdTokenVerifier == null) {
            throw ApiException.badRequest("Google OAuth is not configured");
        }
        GoogleIdToken idToken;
        try {
            idToken = googleIdTokenVerifier.verify(request.getCredential());
        } catch (Exception e) {
            throw ApiException.unauthorized("Invalid Google token");
        }
        if (idToken == null) {
            throw ApiException.unauthorized("Invalid Google token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw ApiException.unauthorized("Google email not verified");
        }

        String email     = payload.getEmail().toLowerCase().trim();
        String googleId  = payload.getSubject();
        String firstName = (String) payload.get("given_name");
        String lastName  = (String) payload.get("family_name");

        Optional<UserEntity> existing = userRepository.findByEmail(email);

        UserEntity user;
        if (existing.isPresent()) {
            user = existing.get();

            if (user.getGoogleId() != null && !user.getGoogleId().equals(googleId)) {
                throw ApiException.conflict("Email already linked to another Google account");
            }

            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                userRepository.save(user);
                log.info("Linked Google account to existing user: {}", email);
            } else {
                log.info("Google login for existing user: {}", email);
            }
        } else {
            user = UserEntity.builder()
                    .email(email)
                    .firstName(firstName != null ? firstName.trim() : "")
                    .lastName(lastName != null ? lastName.trim() : "")
                    .googleId(googleId)
                    .authProvider(UserEntity.AuthProvider.GOOGLE)
                    .role(UserEntity.Role.USER)
                    .enabled(true)
                    .build();
            userRepository.save(user);
            log.info("Created new user via Google OAuth: {}", email);
        }

        if (!user.isEnabled()) {
            throw ApiException.unauthorized("Account is disabled");
        }

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw ApiException.unauthorized("Invalid or expired refresh token");
        }
        String userId = jwtUtil.extractUserId(refreshToken);
        UserEntity user = getById(userId);
        if (!user.isEnabled()) {
            throw ApiException.unauthorized("Account is disabled");
        }
        return buildAuthResponse(user);
    }

    public Page<UserEntity> getAllUsers(String email, Pageable pageable) {
        if (email != null && !email.isBlank()) {
            return userRepository.findByEmailContainingIgnoreCase(email.trim(), pageable);
        }
        return userRepository.findAll(pageable);
    }

    public UserEntity updateRole(String userId, String role) {
        UserEntity user = getById(userId);
        try {
            user.setRole(UserEntity.Role.valueOf(role.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Invalid role: must be USER or ADMIN");
        }
        return userRepository.save(user);
    }

    public UserEntity updateStatus(String userId, boolean enabled, String adminId) {
        if (userId.equals(adminId)) {
            throw ApiException.forbidden("Admin cannot disable their own account");
        }
        UserEntity user = getById(userId);
        user.setEnabled(enabled);
        return userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(UserEntity user) {
        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole().name())
                        .addresses(user.getAddresses())
                        .createdAt(user.getCreatedAt())
                        .build())
                .build();
    }
}
