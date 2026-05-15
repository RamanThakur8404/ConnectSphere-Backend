package com.connectsphere.admin_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.connectsphere.admin_service.entity.AdminAuditLog;
import com.connectsphere.admin_service.repository.AdminAuditLogRepository;
import com.connectsphere.admin_service.service.AdminService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminServiceIntegrationTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AdminAuditLogRepository auditLogRepository;

    @Test
    void adminServiceIntegration_ShouldWorkEndToEnd() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When - Log an admin action
        adminService.logAdminAction(1L, "testadmin", "DELETE", "Deleted test post", "POST", 123L, "SUCCESS");

        // Then - Verify it was saved
        Page<?> auditLogs = adminService.getAllAuditLogs(pageable);
        assertThat(auditLogs.getContent()).hasSize(1);

        // Verify the audit log details
        var response = auditLogs.getContent().get(0);
        assertThat(response).isNotNull();
        // Note: Using reflection to check the DTO fields
        assertThat(response).hasFieldOrPropertyWithValue("adminUserId", 1L);
        assertThat(response).hasFieldOrPropertyWithValue("adminUsername", "testadmin");
        assertThat(response).hasFieldOrPropertyWithValue("action", "DELETE");
        assertThat(response).hasFieldOrPropertyWithValue("targetType", "POST");
        assertThat(response).hasFieldOrPropertyWithValue("targetId", 123L);
        assertThat(response).hasFieldOrPropertyWithValue("status", "SUCCESS");
    }

    @Test
    void auditLogRepositoryIntegration_ShouldPersistAndRetrieve() {
        // Given
        AdminAuditLog auditLog = AdminAuditLog.builder()
                .adminUserId(2L)
                .adminUsername("integrationtest")
                .action("CREATE")
                .details("Created test user")
                .targetType("USER")
                .targetId(456L)
                .status("SUCCESS")
                .build();

        // When
        AdminAuditLog saved = auditLogRepository.save(auditLog);

        // Then
        assertThat(saved.getAuditId()).isNotNull();
        assertThat(saved.getAdminUserId()).isEqualTo(2L);
        assertThat(saved.getAdminUsername()).isEqualTo("integrationtest");
        assertThat(saved.getAction()).isEqualTo("CREATE");
        assertThat(saved.getTargetType()).isEqualTo("USER");
        assertThat(saved.getTargetId()).isEqualTo(456L);
        assertThat(saved.getStatus()).isEqualTo("SUCCESS");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void multipleAuditLogs_ShouldBeRetrievable() {
        // Given
        adminService.logAdminAction(1L, "admin1", "DELETE", "Deleted post 1", "POST", 1L, "SUCCESS");
        adminService.logAdminAction(1L, "admin1", "DELETE", "Deleted post 2", "POST", 2L, "SUCCESS");
        adminService.logAdminAction(2L, "admin2", "CREATE", "Created user", "USER", 100L, "SUCCESS");

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<?> allLogs = adminService.getAllAuditLogs(pageable);
        Page<?> admin1Logs = adminService.getAuditLogsByAdminUser(1L, pageable);
        Page<?> postLogs = adminService.getAuditLogsByTargetType("POST", pageable);

        // Then
        assertThat(allLogs.getContent()).hasSize(3);
        assertThat(admin1Logs.getContent()).hasSize(2);
        assertThat(postLogs.getContent()).hasSize(2);
    }
}
