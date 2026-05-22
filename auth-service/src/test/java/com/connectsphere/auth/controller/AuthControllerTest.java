package com.connectsphere.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import com.connectsphere.auth.constant.Role;
import com.connectsphere.auth.dto.*;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.service.AuthService;
import com.connectsphere.auth.security.JwtUtil;
import com.connectsphere.auth.security.CustomOAuth2UserService;
import com.connectsphere.auth.security.OAuth2AuthenticationSuccessHandler;
import com.connectsphere.auth.security.JwtFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private AuthService authService;

	@MockBean
	private JwtUtil jwtUtil;

	@MockBean
	private CustomOAuth2UserService customOAuth2UserService;

	@MockBean
	private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

	@MockBean
	private JwtFilter jwtFilter;

	private static final String BASE_URL = "/api/v1/auth";

	// ================= PUBLIC =================

	@Test
	void register_ShouldReturnCreatedUser() throws Exception {

		RegisterRequest request = new RegisterRequest();
		request.setUsername("testuser");
		request.setEmail("test@example.com");
		request.setPassword("Valid@123");

		UserResponse response = UserResponse.builder().userId(1L).username("testuser").email("test@example.com")
				.role("USER").build();

		when(authService.register(any())).thenReturn(response);

		mockMvc.perform(post(BASE_URL + "/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value("testuser"));

		verify(authService).register(any());
	}

	@Test
	void login_ShouldReturnToken() throws Exception {

		LoginRequest request = new LoginRequest();
		request.setEmail("test@example.com");
		request.setPassword("Valid@123");

		when(authService.login(anyString(), anyString())).thenReturn(LoginResponse.builder().accessToken("mock.token").build());

		mockMvc.perform(post(BASE_URL + "/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("mock.token"));

		verify(authService).login(anyString(), anyString());
	}

	@Test
	void searchUsers_ShouldReturnList() throws Exception {

		when(authService.searchUsers(anyString())).thenReturn(List.of(UserResponse.builder().username("abc").build()));

		mockMvc.perform(get(BASE_URL + "/search").param("username", "abc")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].username").value("abc"));
	}

	@Test
	void getPublicProfile_ShouldReturnSafeData() throws Exception {

	    // Arrange
	    User user = new User();
	    user.setUserId(1L);
	    user.setUsername("testuser");
	    user.setEmail("test@example.com");   // ✅ IMPORTANT
	    user.setRole(Role.USER);             // ✅ IMPORTANT

	    when(authService.getUserById(1L)).thenReturn(user);

	    // Act & Assert
	    mockMvc.perform(get(BASE_URL + "/users/public/1"))
//	            .andDo(print()) // 🔥 MUST ADD (see actual response)
	            .andExpect(status().isOk())
	            .andExpect(jsonPath("$.username").value("testuser"));
	}
	// ================= AUTHENTICATED =================

	@Test
	void logout_ShouldReturnSuccess() throws Exception {

		mockMvc.perform(post(BASE_URL + "/logout").header("Authorization", "Bearer token")).andExpect(status().isOk())
				.andExpect(content().string("Logged out successfully"));

		verify(authService).logout(eq("token"), isNull());
	}

	@Test
	void refresh_ShouldReturnNewToken() throws Exception {

		when(authService.refreshToken(anyString())).thenReturn(LoginResponse.builder().accessToken("new.token").build());

		RefreshTokenRequest request = new RefreshTokenRequest();
		request.setRefreshToken("old.token");

		mockMvc.perform(post(BASE_URL + "/refresh").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").value("new.token"));
	}

	@Test
	void refresh_ShouldUseCookieWhenBodyIsBlank() throws Exception {

		when(authService.refreshToken("cookie.refresh.token"))
				.thenReturn(LoginResponse.builder().accessToken("new.token").refreshToken("rolled.refresh.token").build());

		mockMvc.perform(post(BASE_URL + "/refresh").contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.cookie(new Cookie("refreshToken", "cookie.refresh.token")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("new.token"));

		verify(authService).refreshToken("cookie.refresh.token");
	}

	@Test
	void getProfile_ShouldReturnUser() throws Exception {

		Authentication auth = org.mockito.Mockito.mock(Authentication.class);
		when(auth.getName()).thenReturn("test@example.com");

		User user = new User();
		user.setEmail("test@example.com");

		when(authService.getUserByEmail(anyString())).thenReturn(user);

		mockMvc.perform(get(BASE_URL + "/users/profile").principal(auth)).andExpect(status().isOk());
	}

	@Test
	void updateProfile_ShouldReturnUpdatedUser() throws Exception {

		Authentication auth = org.mockito.Mockito.mock(Authentication.class);
		when(auth.getName()).thenReturn("test@example.com");

		UpdateUserRequest request = new UpdateUserRequest();

		when(authService.updateUserProfile(anyString(), any()))
				.thenReturn(UserResponse.builder().username("updated").build());

		mockMvc.perform(put(BASE_URL + "/users/update").principal(auth).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("updated"));
	}

	@Test
	void changePassword_ShouldReturnSuccess() throws Exception {

		Authentication auth = org.mockito.Mockito.mock(Authentication.class);
		when(auth.getName()).thenReturn("test@example.com");

		ChangePasswordRequest request = new ChangePasswordRequest();
		request.setCurrentPassword("oldpass");
		request.setNewPassword("newpass");

		mockMvc.perform(patch(BASE_URL + "/users/password").principal(auth).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(content().string("Password changed successfully"));

		verify(authService).changeUserPassword(anyString(), anyString(), anyString());
	}

	// ================= ADMIN =================

	@Test
	void getAllUsers_ShouldReturnUsers() throws Exception {
		when(authService.getAllUsers()).thenReturn(List.of(
				UserResponse.builder().userId(1L).username("abc").email("abc@example.com").role("USER").build()));

		mockMvc.perform(get(BASE_URL + "/admin/users"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].username").value("abc"));

		verify(authService).getAllUsers();
	}

	@Test
	void createPrivilegedUser_ShouldReturnCreatedUser() throws Exception {

		RegisterRequest request = new RegisterRequest();
		request.setUsername("admin");
		request.setEmail("admin@example.com"); // ✅ ADD THIS
		request.setPassword("Valid@123"); // ✅ ADD THIS

		User user = new User();
		user.setUsername("admin");

		when(authService.createPrivilegedUser(any(), any())).thenReturn(user);

		mockMvc.perform(post(BASE_URL + "/admin/users/create").param("role", "ADMIN")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated()); // ✅ now it will pass
	}

	@Test
	void deactivateAccount_ShouldReturnSuccess() throws Exception {

		Authentication auth = org.mockito.Mockito.mock(Authentication.class);
		when(auth.getName()).thenReturn("admin@example.com");

		mockMvc.perform(patch(BASE_URL + "/admin/users/deactivate").principal(auth).param("userId", "1"))
				.andExpect(status().isOk()).andExpect(content().string("Account deactivated successfully"));

		verify(authService).deactivateUserAccount(anyString(), anyLong());
	}

	@Test
	void activateAccount_ShouldReturnSuccess() throws Exception {

		Authentication auth = org.mockito.Mockito.mock(Authentication.class);
		when(auth.getName()).thenReturn("admin@example.com");

		mockMvc.perform(patch(BASE_URL + "/admin/users/activate").principal(auth).param("userId", "1"))
				.andExpect(status().isOk()).andExpect(content().string("Account activated successfully"));

		verify(authService).activateUserAccount(anyString(), anyLong());
	}
}
