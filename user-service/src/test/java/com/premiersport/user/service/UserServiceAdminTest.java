package com.premiersport.user.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.premiersport.common.exception.ApiException;
import com.premiersport.common.jwt.JwtUtil;
import com.premiersport.user.entity.UserEntity;
import com.premiersport.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceAdminTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private GoogleIdTokenVerifier googleIdTokenVerifier;

    @InjectMocks
    private UserService userService;

    private UserEntity adminUser;
    private UserEntity targetUser;

    @BeforeEach
    void setUp() {
        adminUser = UserEntity.builder()
                .id("admin-id")
                .email("admin@test.com")
                .role(UserEntity.Role.ADMIN)
                .enabled(true)
                .build();

        targetUser = UserEntity.builder()
                .id("user-id")
                .email("user@test.com")
                .role(UserEntity.Role.USER)
                .enabled(true)
                .build();
    }

    // --- getAllUsers ---

    @Test
    void getAllUsers_noSearch_returnsAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserEntity> expected = new PageImpl<>(List.of(adminUser, targetUser));
        when(userRepository.findAll(pageable)).thenReturn(expected);

        Page<UserEntity> result = userService.getAllUsers(null, pageable);

        assertThat(result).isEqualTo(expected);
        verify(userRepository).findAll(pageable);
        verify(userRepository, never()).findByEmailContainingIgnoreCase(any(), any());
    }

    @Test
    void getAllUsers_withEmail_searchesByEmail() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserEntity> expected = new PageImpl<>(List.of(targetUser));
        when(userRepository.findByEmailContainingIgnoreCase("user@", pageable)).thenReturn(expected);

        Page<UserEntity> result = userService.getAllUsers("user@", pageable);

        assertThat(result).isEqualTo(expected);
        verify(userRepository).findByEmailContainingIgnoreCase("user@", pageable);
    }

    @Test
    void getAllUsers_blankEmail_returnsAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserEntity> expected = new PageImpl<>(List.of(adminUser, targetUser));
        when(userRepository.findAll(pageable)).thenReturn(expected);

        Page<UserEntity> result = userService.getAllUsers("   ", pageable);

        assertThat(result).isEqualTo(expected);
        verify(userRepository).findAll(pageable);
    }

    // --- updateRole ---

    @Test
    void updateRole_validRole_updatesSuccessfully() {
        when(userRepository.findById("user-id")).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserEntity result = userService.updateRole("user-id", "ADMIN");

        assertThat(result.getRole()).isEqualTo(UserEntity.Role.ADMIN);
    }

    @Test
    void updateRole_lowercaseRole_updatesSuccessfully() {
        when(userRepository.findById("user-id")).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserEntity result = userService.updateRole("user-id", "admin");

        assertThat(result.getRole()).isEqualTo(UserEntity.Role.ADMIN);
    }

    @Test
    void updateRole_invalidRole_throws400() {
        when(userRepository.findById("user-id")).thenReturn(Optional.of(targetUser));

        assertThatThrownBy(() -> userService.updateRole("user-id", "SUPERADMIN"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid role");
    }

    @Test
    void updateRole_unknownUser_throws404() {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateRole("unknown", "ADMIN"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("User not found");
    }

    // --- updateStatus ---

    @Test
    void updateStatus_disableTarget_succeeds() {
        when(userRepository.findById("user-id")).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserEntity result = userService.updateStatus("user-id", false, "admin-id");

        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    void updateStatus_disableSelf_throws403() {
        assertThatThrownBy(() -> userService.updateStatus("admin-id", false, "admin-id"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot disable their own account");
    }

    @Test
    void updateStatus_enableAccount_succeeds() {
        targetUser.setEnabled(false);
        when(userRepository.findById("user-id")).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserEntity result = userService.updateStatus("user-id", true, "admin-id");

        assertThat(result.isEnabled()).isTrue();
    }

    @Test
    void updateStatus_unknownUser_throws404() {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateStatus("unknown", false, "admin-id"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("User not found");
    }
}
