package com.connectsphere.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.service.RedisTokenService;

import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

	@Mock
	private JwtUtil jwtUtil;

	@Mock
	private RedisTokenService redisTokenService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private Authentication authentication;

	@Mock
	private CustomOAuth2User customOAuth2User;

	private OAuth2AuthenticationSuccessHandler successHandler;

	@BeforeEach
	void setUp() {
		successHandler = new OAuth2AuthenticationSuccessHandler(jwtUtil, redisTokenService, userRepository,
				"http://localhost:5173/auth/callback", false, "Lax");
	}

	@Test
	void onAuthenticationSuccess_ShouldRedirectWithCookies() throws IOException, ServletException {
		// Arrange
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		User mockUser = new User();
		mockUser.setUserId(1L);

		when(authentication.getPrincipal()).thenReturn(customOAuth2User);
		when(customOAuth2User.getName()).thenReturn("test@google.com");
		when(customOAuth2User.getRoleName()).thenReturn("USER");
		when(customOAuth2User.getUser()).thenReturn(mockUser);

		when(jwtUtil.generateToken("test@google.com", "USER", 1L)).thenReturn("dummy.jwt.token");
		when(jwtUtil.generateRefreshToken("test@google.com", "USER", 1L)).thenReturn("dummy.refresh.token");

		// Act
		successHandler.onAuthenticationSuccess(request, response, authentication);

		// Assert
		String redirectedUrl = response.getRedirectedUrl();
		assertEquals("http://localhost:5173/auth/callback", redirectedUrl);

		verify(redisTokenService, times(1)).storeRefreshToken("test@google.com", "dummy.refresh.token");
		verify(userRepository, times(1)).save(mockUser);
		assertTrue(mockUser.isEmailVerified());

		var cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
		assertTrue(cookies.stream().anyMatch(c -> c.contains("jwt=dummy.jwt.token")));
		assertTrue(cookies.stream().anyMatch(c -> c.contains("refreshToken=dummy.refresh.token")));
	}
}
