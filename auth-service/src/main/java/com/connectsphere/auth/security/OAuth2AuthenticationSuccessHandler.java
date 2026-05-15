package com.connectsphere.auth.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.service.RedisTokenService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final RedisTokenService redisTokenService;
    private final UserRepository userRepository;
    private final String redirectUri;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);
    private static final int ACCESS_COOKIE_MAX_AGE  = 24 * 60 * 60;
    private static final int REFRESH_COOKIE_MAX_AGE = 7 * 24 * 60 * 60;


    public OAuth2AuthenticationSuccessHandler(
            JwtUtil jwtUtil,
            RedisTokenService redisTokenService,
            UserRepository userRepository,
            @Value("${app.frontend.oauth2-redirect-uri}") String redirectUri,
            @Value("${app.auth.cookie.secure:false}") boolean cookieSecure,
            @Value("${app.auth.cookie.same-site:Lax}") String cookieSameSite) {
        this.jwtUtil           = jwtUtil;
        this.redisTokenService = redisTokenService;
        this.userRepository    = userRepository;
        this.redirectUri       = redirectUri;
        this.cookieSecure      = cookieSecure;
        this.cookieSameSite    = cookieSameSite;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        CustomOAuth2User oauthUser = (CustomOAuth2User) authentication.getPrincipal();
        String email  = oauthUser.getName();
        String role   = oauthUser.getRoleName();
        User user = oauthUser.getUser();
        Long   userId = user.getUserId();

        user.setLastLoginAt(LocalDateTime.now());
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
        }
        userRepository.save(user);

        // Issue tokens — same flow as password login
        String accessToken  = jwtUtil.generateToken(email, role, userId);
        String refreshToken = jwtUtil.generateRefreshToken(email, role, userId);

        // Store refresh token in Redis
        redisTokenService.storeRefreshToken(email, refreshToken);

        // Set both tokens as HttpOnly cookies
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("jwt",          accessToken,  ACCESS_COOKIE_MAX_AGE));
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("refreshToken", refreshToken, REFRESH_COOKIE_MAX_AGE));

        log.info("OAuth2 login successful — email: {}, redirecting to frontend", email);
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }

    private String buildCookie(String name, String value, int maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAge)
                .sameSite(cookieSameSite)
                .build()
                .toString();
    }
}
