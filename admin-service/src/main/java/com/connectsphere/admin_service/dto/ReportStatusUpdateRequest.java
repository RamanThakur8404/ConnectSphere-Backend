package com.connectsphere.admin_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportStatusUpdateRequest {
    
    @NotBlank(message = "Status is required")
    private String status;
    
    private String remarks;
}
