package com.connectsphere.admin_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAuditLogRequest {

    @NotBlank(message = "Action is required")
    @Size(max = 100, message = "Action must not exceed 100 characters")
    private String action;

    @Size(max = 2000, message = "Details must not exceed 2000 characters")
    private String details;

    @NotBlank(message = "Target type is required")
    @Size(max = 50, message = "Target type must not exceed 50 characters")
    private String targetType;

    private Long targetId;

    @NotBlank(message = "Status is required")
    @Size(max = 50, message = "Status must not exceed 50 characters")
    private String status;
}
