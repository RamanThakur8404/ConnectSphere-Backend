package com.connectsphere.report_service.dto;

import lombok.Builder;
import lombok.Data;

// Aggregated report statistics for the admin dashboard.
@Data
@Builder
public class ReportStatsResponseDto {

    private long totalReports;
    private long pendingCount;
    private long underReviewCount;
    private long resolvedCount;
    private long dismissedCount;
}
