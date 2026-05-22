package com.connectsphere.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.connectsphere.auth.config.RabbitMQConfig;
import com.connectsphere.auth.constant.Role;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.event.UserRegisteredEvent;
import com.connectsphere.auth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    private OAuth2User mockOAuthUser;

    @BeforeEach
    void setUp() {
        mockOAuthUser = mock(OAuth2User.class);
    }

    @Test
    void processOAuth2User_ShouldRegisterNewUser() {
        // Arrange
        when(mockOAuthUser.getAttribute("email")).thenReturn("newuser@google.com");
        when(mockOAuthUser.getAttribute("name")).thenReturn("New User");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_dummy_password");

        when(userRepository.findByEmail("newuser@google.com")).thenReturn(Optional.empty());
        
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(100L);
            return user;
        });

        // Act
        OAuth2User result = customOAuth2UserService.processOAuth2User("GOOGLE", mockOAuthUser);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof CustomOAuth2User);
        CustomOAuth2User customUser = (CustomOAuth2User) result;
        assertEquals("newuser@google.com", customUser.getName());
        assertEquals("USER", customUser.getRoleName());
        
        verify(userRepository, times(1)).save(any(User.class));
        var convertAndSendInvocation = mockingDetails(rabbitTemplate).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("convertAndSend"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected convertAndSend to be invoked"));
        assertEquals(RabbitMQConfig.AUTH_EXCHANGE, convertAndSendInvocation.getArgument(0));
        assertEquals(RabbitMQConfig.ROUTING_KEY_USER_REGISTERED, convertAndSendInvocation.getArgument(1));
        assertTrue(convertAndSendInvocation.getArgument(2) instanceof UserRegisteredEvent);
    }

    @Test
    void processOAuth2User_ShouldUpdateExistingUserProvider() {
        // Arrange
        when(mockOAuthUser.getAttribute("email")).thenReturn("existing@google.com");
        
        User existingUser = new User();
        existingUser.setEmail("existing@google.com");
        existingUser.setRole(Role.USER);
        existingUser.setProvider(null);

        when(userRepository.findByEmail("existing@google.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Act
        OAuth2User result = customOAuth2UserService.processOAuth2User("GOOGLE", mockOAuthUser);

        // Assert
        assertEquals(com.connectsphere.auth.constant.AuthProvider.GOOGLE, existingUser.getProvider());
        verify(userRepository, times(1)).save(existingUser);
        verifyNoInteractions(rabbitTemplate);
    }
}
