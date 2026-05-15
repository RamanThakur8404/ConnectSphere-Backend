package com.connectsphere.report_service.dto;

import java.time.LocalDateTime;

import com.connectsphere.report_service.entity.Report.ReportReason;
import com.connectsphere.report_service.entity.Report.ReportStatus;
import com.connectsphere.report_service.entity.Report.TargetType;

import lombok.Builder;
import lombok.Data;

// Full report view returned to admin/moderator consumers. Includes AI-generated
@Data
@Builder
public class ReportResponseDto {

	private Integer reportId;
	private Integer reporterId;
	private Integer targetId;
	private TargetType targetType;
	private ReportReason reason;
	private String description;
	private ReportStatus status;
	private Integer resolvedBy;
	private String resolutionNote;

	// AI-generated narrative analysis (null until async job completes). 
	private String aiAnalysis;

	// AI severity score 1–10 (null until async job completes). 
	private Integer aiSeverityScore;

	private LocalDateTime createdAt;
	private LocalDateTime resolvedAt;
}
