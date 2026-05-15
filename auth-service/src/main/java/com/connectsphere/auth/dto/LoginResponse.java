package com.connectsphere.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Response payload returned after a successful login.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    // Short-lived JWT access token (24 h). 
    private String accessToken;

    // Long-lived refresh token (7 days). Stored in Redis; invalidated on logout. 
    private String refreshToken;

    // Role of the authenticated user. 
    private String role;

    // Public user ID for the frontend to cache. 
    private Long userId;
}
