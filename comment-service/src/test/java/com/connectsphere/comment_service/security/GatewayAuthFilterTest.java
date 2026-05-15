package com.connectsphere.comment_service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

class GatewayAuthFilterTest {

    private GatewayAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GatewayAuthFilter();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Headers present - valid userId sets auth")
    void validHeaders() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(GatewayAuthFilter.HEADER_USER_ID, "100");
        request.addHeader(GatewayAuthFilter.HEADER_USER_ROLE, "USER");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("100:USER");
        assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        assertThat(request.getAttribute(GatewayAuthFilter.HEADER_USER_ID)).isEqualTo(100L);
        
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Invalid userId doesn't set auth")
    void invalidUserId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(GatewayAuthFilter.HEADER_USER_ID, "abc");
        request.addHeader(GatewayAuthFilter.HEADER_USER_ROLE, "USER");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Missing headers proceeds properly without auth")
    void missingHeaders() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        verify(chain).doFilter(request, response);
    }
}
