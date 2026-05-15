package com.connectsphere.report_service.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class HeaderAuthenticationFilterTest {

	private final HeaderAuthenticationFilter filter = new HeaderAuthenticationFilter();

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void doFilterInternalSetsRolePrefixedAuthority() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-User-Id", "15");
		request.addHeader("X-User-Role", "admin");

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertThat(authentication).isNotNull();
		assertThat(authentication.getPrincipal()).isEqualTo(15);
		assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
	}

	@Test
	void doFilterInternalIgnoresInvalidUserIds() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-User-Id", "not-a-number");
		request.addHeader("X-User-Role", "MODERATOR");

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}
}
