package com.connectsphere.admin_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.connectsphere.admin_service.dto.AuditLogResponse;
import com.connectsphere.admin_service.dto.CreateAuditLogRequest;
import com.connectsphere.admin_service.entity.AdminAuditLog;
import com.connectsphere.admin_service.repository.AdminAuditLogRepository;
import com.connectsphere.admin_service.security.JwtUtil;
import com.connectsphere.admin_service.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AdminController adminController;

	@MockBean
	private AdminService adminService;

	@MockBean
	private AdminAuditLogRepository auditLogRepository;

	@MockBean
	private JwtUtil jwtUtil;

	private AdminAuditLog auditLog;
	private Pageable pageable;

	@BeforeEach
	void setUp() {
		auditLog = AdminAuditLog.builder().auditId(1L).adminUserId(1L).adminUsername("admin").action("DELETE")
				.details("Deleted post").targetType("POST").targetId(123L).status("SUCCESS")
				.createdAt(LocalDateTime.now()).build();

		pageable = PageRequest.of(0, 10);
	}

	@Test
	void health_ShouldReturnOk() throws Exception {
		mockMvc.perform(get("/api/v1/admin/health")).andExpect(status().isOk())
				.andExpect(content().string("Admin Service is running"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void getAllAuditLogs_ShouldReturnPagedResults() throws Exception {
		// Given
		List<AuditLogResponse> auditLogs = Arrays.asList(toResponse(auditLog));
		Page<AuditLogResponse> auditLogPage = new PageImpl<>(auditLogs, pageable, 1);
		when(adminService.getAllAuditLogs(any(Pageable.class))).thenReturn(auditLogPage);

		// When & Then
		mockMvc.perform(get("/api/v1/admin/logs").with(authentication(adminAuthentication()))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray()).andExpect(jsonPath("$.content[0].auditId").value(1))
				.andExpect(jsonPath("$.content[0].adminUsername").value("admin"))
				.andExpect(jsonPath("$.content[0].action").value("DELETE"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void createAuditLogUsesAuthenticationDetailsForAdminUserId() {
		CreateAuditLogRequest request = createAuditLogRequest();
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("admin", "password",
				AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
		auth.setDetails("42");

		var response = adminController.createAuditLog(auth, request);

		assertThat(response.getStatusCode().value()).isEqualTo(204);
		verify(adminService).logAdminAction(42L, "admin", "SUSPEND", "Suspended spam account", "USER", 99L,
				"SUCCESS");
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void createAuditLogFallsBackToZeroWhenAuthenticationDetailsAreInvalid() {
		CreateAuditLogRequest request = createAuditLogRequest();
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("admin", "password",
				AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
		auth.setDetails("not-a-number");

		var response = adminController.createAuditLog(auth, request);

		assertThat(response.getStatusCode().value()).isEqualTo(204);
		verify(adminService).logAdminAction(0L, "admin", "SUSPEND", "Suspended spam account", "USER", 99L,
				"SUCCESS");
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void getAuditLogsByAdminUser_ShouldReturnPagedResults() throws Exception {
		// Given
		List<AuditLogResponse> auditLogs = Arrays.asList(toResponse(auditLog));
		Page<AuditLogResponse> auditLogPage = new PageImpl<>(auditLogs, pageable, 1);
		when(adminService.getAuditLogsByAdminUser(eq(1L), any(Pageable.class))).thenReturn(auditLogPage);

		// When & Then
		mockMvc.perform(get("/api/v1/admin/logs/admin/1").with(authentication(adminAuthentication()))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray()).andExpect(jsonPath("$.content[0].adminUserId").value(1));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void getAuditLogsByTargetType_ShouldReturnPagedResults() throws Exception {
		// Given
		List<AuditLogResponse> auditLogs = Arrays.asList(toResponse(auditLog));
		Page<AuditLogResponse> auditLogPage = new PageImpl<>(auditLogs, pageable, 1);
		when(adminService.getAuditLogsByTargetType(eq("POST"), any(Pageable.class))).thenReturn(auditLogPage);

		// When & Then
		mockMvc.perform(get("/api/v1/admin/logs/target/POST").with(authentication(adminAuthentication()))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.content[0].targetType").value("POST"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void getAuditLogsByAction_ShouldReturnPagedResults() throws Exception {
		// Given
		List<AuditLogResponse> auditLogs = Arrays.asList(toResponse(auditLog));
		Page<AuditLogResponse> auditLogPage = new PageImpl<>(auditLogs, pageable, 1);
		when(adminService.getAuditLogsByAction(eq("DELETE"), any(Pageable.class))).thenReturn(auditLogPage);

		// When & Then
		mockMvc.perform(get("/api/v1/admin/logs/action/DELETE").with(authentication(adminAuthentication()))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray()).andExpect(jsonPath("$.content[0].action").value("DELETE"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void getDashboard_ShouldReturnDashboardData() {
		var response = adminController.getDashboard(adminAuthentication());
		Map<String, Object> body = objectMapper.convertValue(response.getBody(), Map.class);

		assertThat(body.get("message")).isEqualTo("Welcome to Admin Dashboard");
		assertThat(body.get("features")).isInstanceOf(List.class);
	}

	@Test
	@WithMockUser(roles = "USER")
	void getAllAuditLogs_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
		assertThatThrownBy(() -> adminController.getAllAuditLogs(pageable)).isInstanceOf(AccessDeniedException.class);
	}

	private AuditLogResponse toResponse(AdminAuditLog log) {
		return new AuditLogResponse(log.getAuditId(), log.getAdminUserId(), log.getAdminUsername(), log.getAction(),
				log.getDetails(), log.getTargetType(), log.getTargetId(), log.getStatus(), log.getCreatedAt());
	}

	private CreateAuditLogRequest createAuditLogRequest() {
		CreateAuditLogRequest request = new CreateAuditLogRequest();
		request.setAction("SUSPEND");
		request.setDetails("Suspended spam account");
		request.setTargetType("USER");
		request.setTargetId(99L);
		request.setStatus("SUCCESS");
		return request;
	}

	private Authentication adminAuthentication() {
		return new UsernamePasswordAuthenticationToken("admin", "password",
				AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
	}

	private Authentication userAuthentication() {
		return new UsernamePasswordAuthenticationToken("user", "password",
				AuthorityUtils.createAuthorityList("ROLE_USER"));
	}
}
