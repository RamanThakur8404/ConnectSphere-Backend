package com.connectsphere.auth.security;

import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.connectsphere.auth.service.RedisTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisTokenService redisTokenService;

    @Mock
    private FilterChain filterChain;

    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtFilter(jwtUtil, redisTokenService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_WithValidCookieToken_ShouldSetAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setCookies(new Cookie("jwt", "valid.token"));

        when(redisTokenService.isBlacklisted("valid.token")).thenReturn(false);
        when(jwtUtil.validateToken("valid.token")).thenReturn(true);
        when(jwtUtil.extractEmail("valid.token")).thenReturn("test@example.com");
        when(jwtUtil.extractRole("valid.token")).thenReturn("USER");
        when(jwtUtil.extractUserId("valid.token")).thenReturn(1L);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("test@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        assertEquals(1L, request.getAttribute("X-User-Id"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_WithBearerHeader_ShouldSetAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer header.token");

        when(redisTokenService.isBlacklisted("header.token")).thenReturn(false);
        when(jwtUtil.validateToken("header.token")).thenReturn(true);
        when(jwtUtil.extractEmail("header.token")).thenReturn("admin@example.com");
        when(jwtUtil.extractRole("header.token")).thenReturn("ADMIN");
        when(jwtUtil.extractUserId("header.token")).thenReturn(2L);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("admin@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_WithNoToken_ShouldPassThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_WithBlacklistedToken_ShouldNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setCookies(new Cookie("jwt", "blacklisted.token"));

        when(redisTokenService.isBlacklisted("blacklisted.token")).thenReturn(true);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_WithInvalidToken_ShouldNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer invalid.token");

        when(redisTokenService.isBlacklisted("invalid.token")).thenReturn(false);
        when(jwtUtil.validateToken("invalid.token")).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_CookiePreferredOverHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setCookies(new Cookie("jwt", "cookie.token"));
        request.addHeader("Authorization", "Bearer header.token");

        when(redisTokenService.isBlacklisted("cookie.token")).thenReturn(false);
        when(jwtUtil.validateToken("cookie.token")).thenReturn(true);
        when(jwtUtil.extractEmail("cookie.token")).thenReturn("cookie@example.com");
        when(jwtUtil.extractRole("cookie.token")).thenReturn("USER");
        when(jwtUtil.extractUserId("cookie.token")).thenReturn(5L);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertEquals("cookie@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void doFilter_WithNullUserId_ShouldNotSetAttribute() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setCookies(new Cookie("jwt", "valid.token"));

        when(redisTokenService.isBlacklisted("valid.token")).thenReturn(false);
        when(jwtUtil.validateToken("valid.token")).thenReturn(true);
        when(jwtUtil.extractEmail("valid.token")).thenReturn("test@example.com");
        when(jwtUtil.extractRole("valid.token")).thenReturn("USER");
        when(jwtUtil.extractUserId("valid.token")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(request.getAttribute("X-User-Id"));
    }

    @Test
    void doFilter_WithNonJwtCookie_ShouldFallbackToHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setCookies(new Cookie("session", "some-value")); // not a jwt cookie

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
