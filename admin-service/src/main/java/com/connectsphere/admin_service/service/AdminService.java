package com.connectsphere.admin_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connectsphere.admin_service.dto.AuditLogResponse;
import com.connectsphere.admin_service.entity.AdminAuditLog;
import com.connectsphere.admin_service.repository.AdminAuditLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    
    private final AdminAuditLogRepository auditLogRepository;

    @Transactional
    public void logAdminAction(Long adminUserId, String adminUsername, String action, 
                               String details, String targetType, Long targetId, String status) {
        AdminAuditLog auditLog = AdminAuditLog.builder()
                .adminUserId(adminUserId)
                .adminUsername(adminUsername)
                .action(action)
                .details(details)
                .targetType(targetType)
                .targetId(targetId)
                .status(status)
                .build();
        
        auditLogRepository.save(auditLog);
        log.info("Admin action logged: {} - {} ({})", action, targetType, adminUsername);
    }

    public Page<AuditLogResponse> getAllAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    public Page<AuditLogResponse> getAuditLogsByAdminUser(Long adminUserId, Pageable pageable) {
        return auditLogRepository.findByAdminUserId(adminUserId, pageable)
                .map(this::mapToResponse);
    }

    public Page<AuditLogResponse> getAuditLogsByTargetType(String targetType, Pageable pageable) {
        return auditLogRepository.findByTargetType(targetType, pageable)
                .map(this::mapToResponse);
    }

    public Page<AuditLogResponse> getAuditLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable)
                .map(this::mapToResponse);
    }

    private AuditLogResponse mapToResponse(AdminAuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getAuditId(),
                auditLog.getAdminUserId(),
                auditLog.getAdminUsername(),
                auditLog.getAction(),
                auditLog.getDetails(),
                auditLog.getTargetType(),
                auditLog.getTargetId(),
                auditLog.getStatus(),
                auditLog.getCreatedAt()
        );
    }
}
