package com.connectsphere.auth.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.connectsphere.auth.constant.AuthProvider;
import com.connectsphere.auth.constant.Role;
import com.connectsphere.auth.dto.RegisterRequest;
import com.connectsphere.auth.dto.UserResponse;
import com.connectsphere.auth.entity.User;

class UserMapperTest {

    @Test
    void toEntity_ShouldMapAllFields() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");
        request.setBio("A short bio");
        request.setProfilePicUrl("http://example.com/pic.jpg");

        User user = UserMapper.toEntity(request);

        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("password123", user.getPasswordHash()); // raw, encoded later
        assertEquals("Test User", user.getFullName());
        assertEquals("A short bio", user.getBio());
        assertEquals("http://example.com/pic.jpg", user.getProfilePicUrl());
    }

    @Test
    void toEntity_WithNullOptionalFields() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("user");
        request.setEmail("user@example.com");
        request.setPassword("pass");

        User user = UserMapper.toEntity(request);

        assertEquals("user", user.getUsername());
        assertNull(user.getFullName());
        assertNull(user.getBio());
        assertNull(user.getProfilePicUrl());
    }

    @Test
    void toResponse_ShouldMapAllFields() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .bio("Bio text")
                .profilePicUrl("http://example.com/pic.jpg")
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .active(true)
                .createdAt(now)
                .build();

        UserResponse response = UserMapper.toResponse(user);

        assertEquals(1L, response.getUserId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
        assertEquals("Bio text", response.getBio());
        assertEquals("http://example.com/pic.jpg", response.getProfilePicUrl());
        assertEquals("USER", response.getRole());
        assertEquals("LOCAL", response.getProvider());
        assertTrue(response.isActive());
        assertEquals(now, response.getCreatedAt());
    }

    @Test
    void toResponse_WithNullRoleAndProvider() {
        User user = User.builder()
                .userId(2L)
                .username("user2")
                .email("user2@example.com")
                .role(null)
                .provider(null)
                .active(false)
                .build();

        UserResponse response = UserMapper.toResponse(user);

        assertNull(response.getRole());
        assertNull(response.getProvider());
        assertFalse(response.isActive());
    }

    @Test
    void toResponse_WithNullActive_ShouldDefaultFalse() {
        User user = User.builder()
                .userId(3L)
                .username("user3")
                .email("user3@example.com")
                .active(null)
                .build();

        UserResponse response = UserMapper.toResponse(user);

        assertFalse(response.isActive());
    }
}
