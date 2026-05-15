package com.connectsphere.report_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// DTO for PUT /api/v1/reports/{id}/resolve — submitted by admin/moderator.
@Data
public class ResolveReportRequestDto {

	@NotBlank(message = "resolutionNote is required")
	@Size(max = 1000, message = "resolutionNote must not exceed 1000 characters")
	private String resolutionNote;
}
