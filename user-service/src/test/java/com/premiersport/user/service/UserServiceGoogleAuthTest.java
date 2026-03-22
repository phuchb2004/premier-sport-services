package com.premiersport.user.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.premiersport.common.exception.ApiException;
import com.premiersport.common.jwt.JwtUtil;
import com.premiersport.user.dto.ChangePasswordRequest;
import com.premiersport.user.dto.GoogleAuthRequest;
import com.premiersport.user.entity.UserEntity;
import com.premiersport.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceGoogleAuthTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private GoogleIdTokenVerifier googleIdTokenVerifier;
    @Mock private GoogleIdToken mockGoogleIdToken;
    @Mock private GoogleIdToken.Payload mockPayload;

    @InjectMocks
    private UserService userService;

    private UserEntity googleUser;

    @BeforeEach
    void setUp() {
        googleUser = UserEntity.builder()
                .id("user-123")
                .email("john@gmail.com")
                .googleId("google-sub-123")
                .authProvider(UserEntity.AuthProvider.GOOGLE)
                .role(UserEntity.Role.USER)
                .enabled(true)
                .build();
    }

    // --- changePassword null guard ---

    @Test
    void changePassword_nullPassword_throwsBadRequest() {
        UserEntity userWithNoPassword = UserEntity.builder()
                .id("user-google")
                .email("g@gmail.com")
                .password(null)
                .authProvider(UserEntity.AuthProvider.GOOGLE)
                .role(UserEntity.Role.USER)
                .enabled(true)
                .build();

        when(userRepository.findById("user-google")).thenReturn(Optional.of(userWithNoPassword));

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("anything");
        req.setNewPassword("newPassword123");

        assertThatThrownBy(() -> userService.changePassword("user-google", req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Password change not available for Google-linked accounts");
    }

    // --- helpers ---

    private void setupValidGoogleToken(String email, String sub) throws Exception {
        when(googleIdTokenVerifier.verify(anyString())).thenReturn(mockGoogleIdToken);
        when(mockGoogleIdToken.getPayload()).thenReturn(mockPayload);
        when(mockPayload.getEmailVerified()).thenReturn(Boolean.TRUE);
        when(mockPayload.getEmail()).thenReturn(email);
        when(mockPayload.getSubject()).thenReturn(sub);
        when(mockPayload.get("given_name")).thenReturn("John");
        when(mockPayload.get("family_name")).thenReturn("Doe");
    }

    private void setupJwtMock() {
        when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(anyString())).thenReturn("refresh-token");
    }

    // --- invalid token ---

    @Test
    void googleAuth_invalidToken_throws401() throws Exception {
        when(googleIdTokenVerifier.verify(anyString())).thenReturn(null);

        GoogleAuthRequest req = new GoogleAuthRequest();
        req.setCredential("bad-token");

        assertThatThrownBy(() -> userService.googleAuth(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid Google token");
    }

    @Test
    void googleAuth_verifierThrowsException_throws401() throws Exception {
        when(googleIdTokenVerifier.verify(anyString())).thenThrow(new RuntimeException("network error"));

        GoogleAuthRequest req = new GoogleAuthRequest();
        req.setCredential("bad-token");

        assertThatThrownBy(() -> userService.googleAuth(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid Google token");
    }

    @Test
    void googleAuth_emailNotVerified_throws401() throws Exception {
        when(googleIdTokenVerifier.verify(anyString())).thenReturn(mockGoogleIdToken);
        when(mockGoogleIdToken.getPayload()).thenReturn(mockPayload);
        when(mockPayload.getEmailVerified()).thenReturn(Boolean.FALSE);

        GoogleAuthRequest req = new GoogleAuthRequest();
        req.setCredential("token");

        assertThatThrownBy(() -> userService.googleAuth(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Google email not verified");
    }

    // --- new user (case c) ---

    @Test
    void googleAuth_newUser_createsUserWithRoleUser() throws Exception {
        setupValidGoogleToken("new@gmail.com", "new-sub-456");
        setupJwtMock();
        when(userRepository.findByEmail("new@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            u.setId("generated-id");
            return u;
        });

        GoogleAuthRequest req = new GoogleAuthRequest();
        req.setCredential("valid-token");

        var response = userService.googleAuth(req);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        verify(userRepository).save(argThat(u ->
                u.getEmail().equals("new@gmail.com") &&
                u.getGoogleId().equals("new-sub-456") &&
                u.getRole() == UserEntity.Role.USER &&
                u.isEnabled() &&
                u.getAuthProvider() == UserEntity.AuthProvider.GOOGLE
        ));
    }

    // --- existing user, matching googleId (case a) ---

    @Test
    void googleAuth_existingGoogleUser_returnsToken() throws Exception {
        setupValidGoogleToken("john@gmail.com", "google-sub-123");
        setupJwtMock();
        when(userRepository.findByEmail("john@gmail.com")).thenReturn(Optional.of(googleUser));

        GoogleAuthRequest req = new GoogleAuthRequest();
        req.setCredential("valid-token");

        var response = userService.googleAuth(req);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        // No save call — already linked
        verify(userRepository, never()).save(any());
    }

    // --- existing local user, no googleId (case b) ---

    @Test
    void googleAuth_existingLocalUser_linksGoogleId() throws Exception {
        setupValidGoogleToken("local@example.com", "new-sub-789");
        setupJwtMock();

        UserEntity localUser = UserEntity.builder()
                .id("local-user")
                .email("local@example.com")
                .password("hashed")
                .authProvider(UserEntity.AuthProvider.LOCAL)
                .role(UserEntity.Role.USER)
                .enabled(true)
                .build();

        when(userRepository.findByEmail("local@example.com")).thenReturn(Optional.of(localUser));
        when(userRepository.save(any())).thenReturn(localUser);

        GoogleAuthRequest req = new GoogleAuthRequest();
        req.setCredential("valid-token");

        userService.googleAuth(req);

        verify(userRepository).save(argThat(u ->
                "new-sub-789".equals(u.getGoogleId()) &&
                u.getAuthProvider() == UserEntity.AuthProvider.LOCAL  // authProvider kept as LOCAL
        ));
    }

    // --- existing user, different googleId (case a') ---

    @Test
    void googleAuth_differentGoogleId_throws409() throws Exception {
        setupValidGoogleToken("john@gmail.com", "different-sub-999");
        when(userRepository.findByEmail("john@gmail.com")).thenReturn(Optional.of(googleUser));

        GoogleAuthRequest req = new GoogleAuthRequest();
        req.setCredential("valid-token");

        assertThatThrownBy(() -> userService.googleAuth(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Email already linked to another Google account");
    }

    // --- disabled user ---

    @Test
    void googleAuth_disabledUser_throws401() throws Exception {
        setupValidGoogleToken("john@gmail.com", "google-sub-123");
        UserEntity disabled = UserEntity.builder()
                .id("user-123")
                .email("john@gmail.com")
                .googleId("google-sub-123")
                .authProvider(UserEntity.AuthProvider.GOOGLE)
                .role(UserEntity.Role.USER)
                .enabled(false)
                .build();
        when(userRepository.findByEmail("john@gmail.com")).thenReturn(Optional.of(disabled));

        GoogleAuthRequest req = new GoogleAuthRequest();
        req.setCredential("valid-token");

        assertThatThrownBy(() -> userService.googleAuth(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Account is disabled");
    }
}
