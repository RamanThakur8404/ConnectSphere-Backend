package com.connectsphere.admin_service.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import com.connectsphere.admin_service.entity.AdminAuditLog;

@DataJpaTest
@ActiveProfiles("test")
class AdminAuditLogRepositoryTest {

    @Autowired
    private AdminAuditLogRepository auditLogRepository;

    private AdminAuditLog auditLog1;
    private AdminAuditLog auditLog2;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll(); // Clean up before each test

        auditLog1 = AdminAuditLog.builder()
                .adminUserId(1L)
                .adminUsername("admin1")
                .action("DELETE")
                .details("Deleted post")
                .targetType("POST")
                .targetId(123L)
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .build();

        auditLog2 = AdminAuditLog.builder()
                .adminUserId(2L)
                .adminUsername("admin2")
                .action("CREATE")
                .details("Created user")
                .targetType("USER")
                .targetId(456L)
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .build();

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void save_ShouldPersistAuditLog() {
        // When
        AdminAuditLog saved = auditLogRepository.save(auditLog1);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getAuditId()).isNotNull();
        assertThat(saved.getAdminUsername()).isEqualTo("admin1");
        assertThat(saved.getAction()).isEqualTo("DELETE");
        assertThat(saved.getTargetType()).isEqualTo("POST");
    }

    @Test
    void findAll_ShouldReturnAllAuditLogs() {
        // Given
        auditLogRepository.save(auditLog1);
        auditLogRepository.save(auditLog2);

        // When
        Page<AdminAuditLog> result = auditLogRepository.findAll(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findByAdminUserId_ShouldReturnFilteredResults() {
        // Given
        auditLogRepository.save(auditLog1);
        auditLogRepository.save(auditLog2);

        // When
        Page<AdminAuditLog> result = auditLogRepository.findByAdminUserId(1L, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAdminUserId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getAdminUsername()).isEqualTo("admin1");
    }

    @Test
    void findByTargetType_ShouldReturnFilteredResults() {
        // Given
        auditLogRepository.save(auditLog1);
        auditLogRepository.save(auditLog2);

        // When
        Page<AdminAuditLog> result = auditLogRepository.findByTargetType("POST", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTargetType()).isEqualTo("POST");
    }

    @Test
    void findByAction_ShouldReturnFilteredResults() {
        // Given
        auditLogRepository.save(auditLog1);
        auditLogRepository.save(auditLog2);

        // When
        Page<AdminAuditLog> result = auditLogRepository.findByAction("CREATE", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAction()).isEqualTo("CREATE");
    }

    @Test
    void findByAdminUserId_WithNoResults_ShouldReturnEmptyPage() {
        // Given
        auditLogRepository.save(auditLog1);

        // When
        Page<AdminAuditLog> result = auditLogRepository.findByAdminUserId(999L, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void save_WithNullValues_ShouldHandleGracefully() {
        // Given
        AdminAuditLog auditLogWithNulls = AdminAuditLog.builder()
                .adminUserId(1L)
                .adminUsername("admin")
                .action("TEST")
                .targetType("TEST")
                .status("SUCCESS")
                .build();

        // When
        AdminAuditLog saved = auditLogRepository.save(auditLogWithNulls);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getAuditId()).isNotNull();
        assertThat(saved.getDetails()).isNull();
        assertThat(saved.getTargetId()).isNull();
    }
}
