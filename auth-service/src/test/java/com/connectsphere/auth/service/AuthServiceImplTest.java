package com.connectsphere.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.connectsphere.auth.constant.Role;
import com.connectsphere.auth.dto.LoginResponse;
import com.connectsphere.auth.dto.RegisterRequest;
import com.connectsphere.auth.dto.UpdateUserRequest;
import com.connectsphere.auth.dto.UserResponse;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.exception.AuthenticationFailedException;
import com.connectsphere.auth.exception.DuplicateResourceException;
import com.connectsphere.auth.exception.ResourceNotFoundException;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisTokenService redisTokenService;

    @Mock
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private User testAdmin;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .role(Role.USER)
                .active(true)
                .build();

        testAdmin = User.builder()
                .userId(2L)
                .username("adminuser")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .active(true)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("password123");

        lenient().when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    // =========================================================================
    // Registration
    // =========================================================================

    @Test
    void register_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        User savedUser = User.builder()
                .userId(3L)
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .role(Role.USER)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("newuser", response.getUsername());
        assertEquals("new@example.com", response.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_EmailAlreadyExists_ThrowsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        RuntimeException exception = assertThrows(DuplicateResourceException.class, () -> {
            authService.register(registerRequest);
        });
        assertEquals("Email is already registered", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_UsernameAlreadyTaken_ThrowsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> {
            authService.register(registerRequest);
        });
        assertEquals("Username is already taken", exception.getMessage());
    }

    @Test
    void register_WhenRabbitFails_ShouldStillSucceed() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        User savedUser = User.builder()
                .userId(4L).username("newuser").email("new@example.com").role(Role.USER).build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        doThrow(new RuntimeException("RabbitMQ down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        UserResponse response = authService.register(registerRequest);
        assertNotNull(response);
    }

    // =========================================================================
    // Login
    // =========================================================================

    @Test
    void login_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedpassword")).thenReturn(true);
        when(jwtUtil.generateToken("test@example.com", "USER", 1L)).thenReturn("dummy.jwt.token");
        when(jwtUtil.generateRefreshToken("test@example.com", "USER", 1L)).thenReturn("dummy.refresh.token");

        LoginResponse response = authService.login("test@example.com", "password123");

        assertEquals("dummy.jwt.token", response.getAccessToken());
        assertEquals("dummy.refresh.token", response.getRefreshToken());
        assertEquals("USER", response.getRole());
        assertEquals(1L, response.getUserId());
        verify(redisTokenService).storeRefreshToken("test@example.com", "dummy.refresh.token");
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "hashedpassword")).thenReturn(false);

        AuthenticationFailedException exception = assertThrows(AuthenticationFailedException.class, () -> {
            authService.login("test@example.com", "wrongpassword");
        });
        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(AuthenticationFailedException.class, () -> {
            authService.login("nonexistent@example.com", "password");
        });
    }

    @Test
    void login_DeactivatedAccount_ThrowsException() {
        testUser.setActive(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        AuthenticationFailedException ex = assertThrows(AuthenticationFailedException.class, () -> {
            authService.login("test@example.com", "password123");
        });
        assertEquals("This account has been deactivated", ex.getMessage());
    }

    // =========================================================================
    // OTP Login
    // =========================================================================

    @Test
    void requestLoginOtp_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        authService.requestLoginOtp("test@example.com");

        verify(redisTokenService).storeOtp(eq("test@example.com"), anyString());
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void requestLoginOtp_UserNotFound_DoesNotThrowOrSendMail() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.requestLoginOtp("nonexistent@example.com"));
        verify(redisTokenService, never()).storeOtp(anyString(), anyString());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void requestLoginOtp_DeactivatedAccount_ThrowsException() {
        testUser.setActive(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(AuthenticationFailedException.class, () -> {
            authService.requestLoginOtp("test@example.com");
        });
    }

    @Test
    void requestLoginOtp_EmailFails_ShouldNotThrow() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> authService.requestLoginOtp("test@example.com"));
    }

    @Test
    void verifyLoginOtp_Success() {
        when(redisTokenService.getOtp("test@example.com")).thenReturn("123456");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken("test@example.com", "USER", 1L)).thenReturn("access.token");
        when(jwtUtil.generateRefreshToken("test@example.com", "USER", 1L)).thenReturn("refresh.token");

        LoginResponse response = authService.verifyLoginOtp("test@example.com", "123456");

        assertNotNull(response);
        assertEquals("access.token", response.getAccessToken());
        verify(redisTokenService).deleteOtp("test@example.com");
    }

    @Test
    void verifyLoginOtp_InvalidOtp_ThrowsException() {
        when(redisTokenService.getOtp("test@example.com")).thenReturn("123456");

        assertThrows(AuthenticationFailedException.class, () -> {
            authService.verifyLoginOtp("test@example.com", "999999");
        });
    }

    @Test
    void verifyLoginOtp_ExpiredOtp_ThrowsException() {
        when(redisTokenService.getOtp("test@example.com")).thenReturn(null);

        assertThrows(AuthenticationFailedException.class, () -> {
            authService.verifyLoginOtp("test@example.com", "123456");
        });
    }

    @Test
    void verifyLoginOtp_DeactivatedAccount_ThrowsException() {
        testUser.setActive(false);
        when(redisTokenService.getOtp("test@example.com")).thenReturn("123456");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(AuthenticationFailedException.class, () -> {
            authService.verifyLoginOtp("test@example.com", "123456");
        });
    }

    // =========================================================================
    // Logout
    // =========================================================================

    @Test
    void logout_WithBothTokens_Success() {
        when(jwtUtil.extractEmail("refresh.token")).thenReturn("test@example.com");

        authService.logout("access.token", "refresh.token");

        verify(redisTokenService).blacklistAccessToken("access.token");
        verify(redisTokenService).deleteRefreshToken("test@example.com");
    }

    @Test
    void logout_WithNullTokens() {
        authService.logout(null, null);
        verify(redisTokenService, never()).blacklistAccessToken(anyString());
        verify(redisTokenService, never()).deleteRefreshToken(anyString());
    }

    @Test
    void logout_WithBlankTokens() {
        authService.logout("  ", "  ");
        verify(redisTokenService, never()).blacklistAccessToken(anyString());
        verify(redisTokenService, never()).deleteRefreshToken(anyString());
    }

    @Test
    void logout_RefreshTokenExtractFails_FallsBackToAccessToken() {
        when(jwtUtil.extractEmail("refresh.token")).thenThrow(new RuntimeException("bad token"));
        when(jwtUtil.extractEmail("access.token")).thenReturn("test@example.com");

        authService.logout("access.token", "refresh.token");

        verify(redisTokenService).blacklistAccessToken("access.token");
        verify(redisTokenService).deleteRefreshToken("test@example.com");
    }

    @Test
    void logout_BothExtractFail_NoDeleteCalled() {
        when(jwtUtil.extractEmail("refresh.token")).thenThrow(new RuntimeException("bad"));
        when(jwtUtil.extractEmail("access.token")).thenThrow(new RuntimeException("bad"));

        authService.logout("access.token", "refresh.token");

        verify(redisTokenService).blacklistAccessToken("access.token");
        verify(redisTokenService, never()).deleteRefreshToken(anyString());
    }

    @Test
    void logout_AccessTokenOnlyNull_RefreshPresent() {
        when(jwtUtil.extractEmail("refresh.token")).thenReturn("test@example.com");

        authService.logout(null, "refresh.token");

        verify(redisTokenService, never()).blacklistAccessToken(anyString());
        verify(redisTokenService).deleteRefreshToken("test@example.com");
    }

    // =========================================================================
    // Refresh Token
    // =========================================================================

    @Test
    void refreshToken_Success() {
        when(jwtUtil.validateToken("old.refresh")).thenReturn(true);
        when(redisTokenService.isBlacklisted("old.refresh")).thenReturn(false);
        when(jwtUtil.extractEmail("old.refresh")).thenReturn("test@example.com");
        when(redisTokenService.getRefreshToken("test@example.com")).thenReturn("old.refresh");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken("test@example.com", "USER", 1L)).thenReturn("new.access");
        when(jwtUtil.generateRefreshToken("test@example.com", "USER", 1L)).thenReturn("new.refresh");

        LoginResponse response = authService.refreshToken("old.refresh");

        assertEquals("new.access", response.getAccessToken());
        assertEquals("new.refresh", response.getRefreshToken());
        verify(redisTokenService).storeRefreshToken("test@example.com", "new.refresh");
    }

    @Test
    void refreshToken_BlankToken_ThrowsException() {
        assertThrows(AuthenticationFailedException.class, () -> {
            authService.refreshToken("");
        });
    }

    @Test
    void refreshToken_NullToken_ThrowsException() {
        assertThrows(AuthenticationFailedException.class, () -> {
            authService.refreshToken(null);
        });
    }

    @Test
    void refreshToken_InvalidToken_ThrowsException() {
        when(jwtUtil.validateToken("invalid.token")).thenReturn(false);

        assertThrows(AuthenticationFailedException.class, () -> {
            authService.refreshToken("invalid.token");
        });
    }

    @Test
    void refreshToken_BlacklistedToken_ThrowsException() {
        when(jwtUtil.validateToken("blacklisted.token")).thenReturn(true);
        when(redisTokenService.isBlacklisted("blacklisted.token")).thenReturn(true);

        assertThrows(AuthenticationFailedException.class, () -> {
            authService.refreshToken("blacklisted.token");
        });
    }

    @Test
    void refreshToken_RotatedToken_ThrowsException() {
        when(jwtUtil.validateToken("rotated.token")).thenReturn(true);
        when(redisTokenService.isBlacklisted("rotated.token")).thenReturn(false);
        when(jwtUtil.extractEmail("rotated.token")).thenReturn("test@example.com");
        when(redisTokenService.getRefreshToken("test@example.com")).thenReturn("different.token");

        assertThrows(AuthenticationFailedException.class, () -> {
            authService.refreshToken("rotated.token");
        });
    }

    @Test
    void refreshToken_DeactivatedUser_ThrowsException() {
        testUser.setActive(false);
        when(jwtUtil.validateToken("valid.refresh")).thenReturn(true);
        when(redisTokenService.isBlacklisted("valid.refresh")).thenReturn(false);
        when(jwtUtil.extractEmail("valid.refresh")).thenReturn("test@example.com");
        when(redisTokenService.getRefreshToken("test@example.com")).thenReturn("valid.refresh");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(AuthenticationFailedException.class, () -> {
            authService.refreshToken("valid.refresh");
        });
    }

    // =========================================================================
    // User retrieval
    // =========================================================================

    @Test
    void getUserByEmail_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        User user = authService.getUserByEmail("test@example.com");
        assertEquals("test@example.com", user.getEmail());
    }

    @Test
    void getUserByEmail_NotFound_ThrowsException() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            authService.getUserByEmail("nonexistent@example.com");
        });
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        User user = authService.getUserById(1L);
        assertEquals(1L, user.getUserId());
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            authService.getUserById(999L);
        });
    }

    @Test
    void getUserProfile_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        UserResponse response = authService.getUserProfile("test@example.com");
        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
    }

    // =========================================================================
    // Profile update
    // =========================================================================

    @Test
    void updateUserProfile_AllFields() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("newname")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("newname");
        request.setFullName("New Full Name");
        request.setBio("New bio");
        request.setProfilePicUrl("http://example.com/pic.jpg");

        UserResponse response = authService.updateUserProfile("test@example.com", request);
        assertNotNull(response);
    }

    @Test
    void updateUserProfile_UsernameTaken_ThrowsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("takenname")).thenReturn(true);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("takenname");

        assertThrows(DuplicateResourceException.class, () -> {
            authService.updateUserProfile("test@example.com", request);
        });
    }

    @Test
    void updateUserProfile_SameUsername_NoConflict() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("testuser")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("testuser"); // same as current

        UserResponse response = authService.updateUserProfile("test@example.com", request);
        assertNotNull(response);
    }

    @Test
    void updateUserProfile_NullFields_NoUpdates() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UpdateUserRequest request = new UpdateUserRequest(); // all null

        UserResponse response = authService.updateUserProfile("test@example.com", request);
        assertNotNull(response);
    }

    // =========================================================================
    // Change Password
    // =========================================================================

    @Test
    void changeUserPassword_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldpass", "hashedpassword")).thenReturn(true);
        when(passwordEncoder.encode("newpass")).thenReturn("newhashed");

        authService.changeUserPassword("test@example.com", "oldpass", "newpass");

        verify(userRepository).save(testUser);
        assertEquals("newhashed", testUser.getPasswordHash());
    }

    @Test
    void changeUserPassword_WrongCurrentPassword_ThrowsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "hashedpassword")).thenReturn(false);

        assertThrows(AuthenticationFailedException.class, () -> {
            authService.changeUserPassword("test@example.com", "wrong", "newpass");
        });
    }

    // =========================================================================
    // Forget / Reset Password
    // =========================================================================

    @Test
    void forgetPassword_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        authService.forgetPassword("test@example.com");

        verify(redisTokenService).storeResetToken(anyString(), eq("test@example.com"));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void forgetPassword_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            authService.forgetPassword("nonexistent@example.com");
        });
    }

    @Test
    void forgetPassword_DeactivatedAccount_ThrowsException() {
        testUser.setActive(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(AuthenticationFailedException.class, () -> {
            authService.forgetPassword("test@example.com");
        });
    }

    @Test
    void forgetPassword_MailFails_ShouldNotThrow() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> authService.forgetPassword("test@example.com"));
    }

    @Test
    void resetPassword_Success() {
        when(redisTokenService.getResetToken("reset-token")).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncoded");

        authService.resetPassword("reset-token", "newPassword");

        verify(userRepository).save(testUser);
        verify(redisTokenService).deleteResetToken("reset-token");
        assertEquals("newEncoded", testUser.getPasswordHash());
    }

    @Test
    void resetPassword_InvalidToken_ThrowsException() {
        when(redisTokenService.getResetToken("bad-token")).thenReturn(null);

        assertThrows(AuthenticationFailedException.class, () -> {
            authService.resetPassword("bad-token", "newPassword");
        });
    }

    // =========================================================================
    // Deactivation
    // =========================================================================

    @Test
    void deactivateUserAccount_Success() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(testAdmin));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        authService.deactivateUserAccount("admin@example.com", 1L);

        assertFalse(testUser.getActive());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void deactivateUserAccount_NotAdmin_ThrowsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        RuntimeException exception = assertThrows(AuthenticationFailedException.class, () -> {
            authService.deactivateUserAccount("test@example.com", 2L);
        });
        assertEquals("Only ADMIN can deactivate accounts", exception.getMessage());
    }

    @Test
    void deactivateUserAccount_SelfDeactivate_ThrowsException() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(testAdmin));

        assertThrows(IllegalArgumentException.class, () -> {
            authService.deactivateUserAccount("admin@example.com", 2L); // admin's own ID
        });
    }

    @Test
    void deactivateUserAccount_AdminNotFound_ThrowsException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            authService.deactivateUserAccount("unknown@example.com", 1L);
        });
    }

    @Test
    void deactivateUserAccount_TargetNotFound_ThrowsException() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(testAdmin));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            authService.deactivateUserAccount("admin@example.com", 999L);
        });
    }

    @Test
    void deactivateUserAccount_RabbitFails_ShouldStillSucceed() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(testAdmin));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doThrow(new RuntimeException("RabbitMQ down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        assertDoesNotThrow(() -> authService.deactivateUserAccount("admin@example.com", 1L));
        assertFalse(testUser.getActive());
    }

    @Test
    void activateUserAccount_Success() {
        testUser.setActive(false);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(testAdmin));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        authService.activateUserAccount("admin@example.com", 1L);

        assertTrue(testUser.getActive());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void activateUserAccount_NotAdmin_ThrowsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        RuntimeException exception = assertThrows(AuthenticationFailedException.class, () -> {
            authService.activateUserAccount("test@example.com", 2L);
        });
        assertEquals("Only ADMIN can deactivate accounts", exception.getMessage());
    }

    // =========================================================================
    // Search
    // =========================================================================

    @Test
    void searchUsers_ReturnsResults() {
        when(userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase("test", "test"))
                .thenReturn(List.of(testUser));

        List<UserResponse> results = authService.searchUsers("test");
        assertEquals(1, results.size());
        assertEquals("testuser", results.get(0).getUsername());
    }

    @Test
    void searchUsers_EmptyResults() {
        when(userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase("xyz", "xyz"))
                .thenReturn(List.of());

        List<UserResponse> results = authService.searchUsers("xyz");
        assertTrue(results.isEmpty());
    }

    // =========================================================================
    // Create Privileged User
    // =========================================================================

    @Test
    void createPrivilegedUser_Admin_Success() {
        User newAdmin = User.builder()
                .username("newadmin")
                .email("newadmin@example.com")
                .passwordHash("plainpass")
                .build();

        when(userRepository.existsByEmail("newadmin@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newadmin")).thenReturn(false);
        when(passwordEncoder.encode("plainpass")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(newAdmin);

        User created = authService.createPrivilegedUser(newAdmin, Role.ADMIN);
        assertNotNull(created);
        verify(userRepository).save(newAdmin);
    }

    @Test
    void createPrivilegedUser_UserRole_ThrowsException() {
        User u = User.builder().username("u").email("u@u.com").passwordHash("p").build();

        assertThrows(IllegalArgumentException.class, () -> {
            authService.createPrivilegedUser(u, Role.USER);
        });
    }

    @Test
    void createPrivilegedUser_EmailExists_ThrowsException() {
        User u = User.builder().username("u").email("existing@example.com").passwordHash("p").build();
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> {
            authService.createPrivilegedUser(u, Role.ADMIN);
        });
    }

    @Test
    void createPrivilegedUser_UsernameExists_ThrowsException() {
        User u = User.builder().username("existinguser").email("new@example.com").passwordHash("p").build();
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> {
            authService.createPrivilegedUser(u, Role.ADMIN);
        });
    }
}
