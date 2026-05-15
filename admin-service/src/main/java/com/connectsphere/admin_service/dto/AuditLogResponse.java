package com.connectsphere.admin_service.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long auditId;
    private Long adminUserId;
    private String adminUsername;
    private String action;
    private String details;
    private String targetType;
    private Long targetId;
    private String status;
    private LocalDateTime createdAt;
}
