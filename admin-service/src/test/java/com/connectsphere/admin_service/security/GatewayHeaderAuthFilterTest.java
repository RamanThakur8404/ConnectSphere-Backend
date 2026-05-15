package com.connectsphere.admin_service.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class GatewayHeaderAuthFilterTest {

    private final GatewayHeaderAuthFilter filter = new GatewayHeaderAuthFilter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_setsAuthenticationFromGatewayHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "42");
        request.addHeader("X-User-Role", "ADMIN");
        request.addHeader("X-User-Email", "admin@connectsphere.com");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("admin@connectsphere.com");
        assertThat(authentication.getDetails()).isEqualTo("42");
        assertThat(authorityNames(authentication.getAuthorities())).contains("ROLE_ADMIN");
    }

    @Test
    void doFilterInternal_usesUserIdWhenEmailMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "42");
        request.addHeader("X-User-Role", "ADMIN");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("42");
        assertThat(authentication.getDetails()).isEqualTo("42");
    }

    private Collection<String> authorityNames(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).toList();
    }
}
