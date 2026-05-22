package com.connectsphere.auth.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.connectsphere.auth.constant.AuthProvider;
import com.connectsphere.auth.constant.Role;

class UserTest {

    @Test
    void beforeSave_ShouldSetCreatedAtAndActive() {
        User user = new User();
        user.setUsername("test");
        user.setEmail("test@example.com");
        user.setPasswordHash("hash");

        user.beforeSave();

        assertNotNull(user.getCreatedAt());
        assertTrue(user.getActive());
    }

    @Test
    void beforeSave_ShouldNotOverrideExistingValues() {
        User user = new User();
        user.setActive(false);
        java.time.LocalDateTime fixed = java.time.LocalDateTime.of(2025, 1, 1, 0, 0);
        user.setCreatedAt(fixed);

        user.beforeSave();

        assertEquals(fixed, user.getCreatedAt());
        assertFalse(user.getActive()); // should NOT be overridden
    }

    @Test
    void builder_DefaultValues() {
        User user = User.builder()
                .username("test")
                .email("test@example.com")
                .passwordHash("hash")
                .build();

        assertEquals(Role.USER, user.getRole());
        assertEquals(AuthProvider.LOCAL, user.getProvider());
        assertTrue(user.getActive());
        assertFalse(user.isTwoFactorEnabled());
        assertFalse(user.isEmailVerified());
    }

    @Test
    void settersAndGetters() {
        User user = new User();
        user.setUserId(1L);
        user.setUsername("user1");
        user.setEmail("user1@example.com");
        user.setPasswordHash("hashed");
        user.setFullName("User One");
        user.setBio("My bio");
        user.setProfilePicUrl("http://pic.com/1.jpg");
        user.setRole(Role.ADMIN);
        user.setProvider(AuthProvider.GOOGLE);
        user.setActive(false);
        user.setTwoFactorEnabled(true);
        user.setEmailVerified(true);

        assertEquals(1L, user.getUserId());
        assertEquals("user1", user.getUsername());
        assertEquals("user1@example.com", user.getEmail());
        assertEquals("hashed", user.getPasswordHash());
        assertEquals("User One", user.getFullName());
        assertEquals("My bio", user.getBio());
        assertEquals("http://pic.com/1.jpg", user.getProfilePicUrl());
        assertEquals(Role.ADMIN, user.getRole());
        assertEquals(AuthProvider.GOOGLE, user.getProvider());
        assertFalse(user.getActive());
        assertTrue(user.isTwoFactorEnabled());
        assertTrue(user.isEmailVerified());
    }
}
