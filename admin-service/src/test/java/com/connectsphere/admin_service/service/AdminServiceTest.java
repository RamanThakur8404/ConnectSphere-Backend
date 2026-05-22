package com.connectsphere.admin_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.connectsphere.admin_service.dto.AuditLogResponse;
import com.connectsphere.admin_service.entity.AdminAuditLog;
import com.connectsphere.admin_service.repository.AdminAuditLogRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminAuditLogRepository auditLogRepository;

    @InjectMocks
    private AdminService adminService;

    private AdminAuditLog auditLog;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        auditLog = AdminAuditLog.builder()
                .auditId(1L)
                .adminUserId(1L)
                .adminUsername("admin")
                .action("DELETE")
                .details("Deleted post")
                .targetType("POST")
                .targetId(123L)
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .build();

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void logAdminAction_ShouldSaveAuditLog() {
        // Given
        when(auditLogRepository.save(any(AdminAuditLog.class))).thenReturn(auditLog);

        // When
        adminService.logAdminAction(1L, "admin", "DELETE", "Deleted post", "POST", 123L, "SUCCESS");

        // Then
        verify(auditLogRepository, times(1)).save(any(AdminAuditLog.class));
    }

    @Test
    void getAllAuditLogs_ShouldReturnPagedResults() {
        // Given
        List<AdminAuditLog> auditLogs = Arrays.asList(auditLog);
        Page<AdminAuditLog> auditLogPage = new PageImpl<>(auditLogs, pageable, 1);
        when(auditLogRepository.findAll(pageable)).thenReturn(auditLogPage);

        // When
        Page<?> result = adminService.getAllAuditLogs(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isInstanceOf(AuditLogResponse.class);
        verify(auditLogRepository, times(1)).findAll(pageable);
    }

    @Test
    void getAuditLogsByAdminUser_ShouldReturnPagedResults() {
        // Given
        List<AdminAuditLog> auditLogs = Arrays.asList(auditLog);
        Page<AdminAuditLog> auditLogPage = new PageImpl<>(auditLogs, pageable, 1);
        when(auditLogRepository.findByAdminUserId(1L, pageable)).thenReturn(auditLogPage);

        // When
        Page<?> result = adminService.getAuditLogsByAdminUser(1L, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository, times(1)).findByAdminUserId(1L, pageable);
    }

    @Test
    void getAuditLogsByTargetType_ShouldReturnPagedResults() {
        // Given
        List<AdminAuditLog> auditLogs = Arrays.asList(auditLog);
        Page<AdminAuditLog> auditLogPage = new PageImpl<>(auditLogs, pageable, 1);
        when(auditLogRepository.findByTargetType("POST", pageable)).thenReturn(auditLogPage);

        // When
        Page<?> result = adminService.getAuditLogsByTargetType("POST", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository, times(1)).findByTargetType("POST", pageable);
    }

    @Test
    void getAuditLogsByAction_ShouldReturnPagedResults() {
        // Given
        List<AdminAuditLog> auditLogs = Arrays.asList(auditLog);
        Page<AdminAuditLog> auditLogPage = new PageImpl<>(auditLogs, pageable, 1);
        when(auditLogRepository.findByAction("DELETE", pageable)).thenReturn(auditLogPage);

        // When
        Page<?> result = adminService.getAuditLogsByAction("DELETE", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository, times(1)).findByAction("DELETE", pageable);
    }
}
