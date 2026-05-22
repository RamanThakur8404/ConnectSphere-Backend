package com.connectsphere.auth.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.connectsphere.auth.constant.Role;
import com.connectsphere.auth.entity.User;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // DataJpaTest runs each test in a transaction and rolls back automatically,
        // but it's good practice to ensure we have a fresh dataset for each test.
        userRepository.deleteAll();

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .role(Role.USER)
                .active(true)
                .build();

        // Save the test user into the H2 database before each test runs
        userRepository.save(testUser);
    }

    @Test
    void findByEmail_WhenEmailExists_ReturnsUser() {
        // Act
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");

        // Assert
        assertTrue(foundUser.isPresent());
        assertEquals("testuser", foundUser.get().getUsername());
    }

    @Test
    void findByEmail_WhenEmailDoesNotExist_ReturnsEmpty() {
        // Act
        Optional<User> foundUser = userRepository.findByEmail("nobody@example.com");

        // Assert
        assertFalse(foundUser.isPresent());
    }

    @Test
    void existsByUsername_WhenUsernameExists_ReturnsTrue() {
        // Act
        boolean exists = userRepository.existsByUsername("testuser");

        // Assert
        assertTrue(exists);
    }

    @Test
    void existsByUsername_WhenUsernameDoesNotExist_ReturnsFalse() {
        // Act
        boolean exists = userRepository.existsByUsername("ghostuser");

        // Assert
        assertFalse(exists);
    }

    @Test
    void findAllByRole_ReturnsCorrectUsers() {
        // Arrange: Create and save a second user with an ADMIN role
        User adminUser = User.builder()
                .username("adminuser")
                .email("admin@example.com")
                .passwordHash("hashedpassword")
                .role(Role.ADMIN)
                .active(true)
                .build();
        userRepository.save(adminUser);

        // Act
        List<User> standardUsers = userRepository.findAllByRole(Role.USER);
        List<User> adminUsers = userRepository.findAllByRole(Role.ADMIN);

        // Assert
        assertEquals(1, standardUsers.size());
        assertEquals("testuser", standardUsers.get(0).getUsername());

        assertEquals(1, adminUsers.size());
        assertEquals("adminuser", adminUsers.get(0).getUsername());
    }
}