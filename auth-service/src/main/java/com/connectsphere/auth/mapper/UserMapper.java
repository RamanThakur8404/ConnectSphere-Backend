package com.connectsphere.auth.mapper;

import com.connectsphere.auth.dto.RegisterRequest;
import com.connectsphere.auth.dto.UserResponse;
import com.connectsphere.auth.entity.User;

public class UserMapper {

    public static User toEntity(RegisterRequest request) {
        return User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(request.getPassword()) // will be encoded later
                .fullName(request.getFullName())
                .bio(request.getBio())
                .profilePicUrl(request.getProfilePicUrl())
                .build();
    }

    public static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .bio(user.getBio())
                .profilePicUrl(user.getProfilePicUrl())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .provider(user.getProvider() != null ? user.getProvider().name() : null)
                .active(Boolean.TRUE.equals(user.getActive()))
                .isPremium(user.isPremium())
                .twoFactorEnabled(user.isTwoFactorEnabled())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
