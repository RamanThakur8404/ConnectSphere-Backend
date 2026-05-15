package com.connectsphere.auth.dto;

import lombok.Data;

@Data
public class RefreshTokenRequest {

    // Optional in browser flows because the controller can fall back to the
    // HttpOnly refreshToken cookie when the JSON body is empty.
    private String refreshToken;
}
