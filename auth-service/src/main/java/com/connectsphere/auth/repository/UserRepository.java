package com.connectsphere.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.connectsphere.auth.constant.Role;
import com.connectsphere.auth.entity.User;

// Repository interface for User entity. Handles all database operations using
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	// Find user by email.
	Optional<User> findByEmail(String email);

	// Find user by username.
	Optional<User> findByUsername(String username);

	// Check if email already exists.
	boolean existsByEmail(String email);

	// Check if username already exists.
	boolean existsByUsername(String username);

	// Get all users with a specific role.
	List<User> findAllByRole(Role role);

	// Search users by username (case-insensitive partial match).
	List<User> findByUsernameContainingIgnoreCase(String username);

	// Combined search across username AND fullName (used by searchUsers()).
    List<User> findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            String username, String fullName);
    
	// Delete user by ID.
	void deleteByUserId(Long userId);
}