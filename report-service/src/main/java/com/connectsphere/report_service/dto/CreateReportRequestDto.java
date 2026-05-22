package com.connectsphere.report_service.dto;

import com.connectsphere.report_service.entity.Report.ReportReason;
import com.connectsphere.report_service.entity.Report.TargetType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

// DTO for POST /api/v1/reports — submitted by an authenticated user.
@Data
public class CreateReportRequestDto {

    @NotNull(message = "targetId is required")
    private Integer targetId;

    @NotNull(message = "targetType is required (POST, COMMENT, USER)")
    private TargetType targetType;

    @NotNull(message = "reason is required")
    private ReportReason reason;

    @Size(max = 500, message = "description must not exceed 500 characters")
    private String description;
}
