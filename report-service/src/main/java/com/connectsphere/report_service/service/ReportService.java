package com.connectsphere.report_service.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.connectsphere.report_service.dto.CreateReportRequestDto;
import com.connectsphere.report_service.dto.ReportResponseDto;
import com.connectsphere.report_service.dto.ReportStatsResponseDto;
import com.connectsphere.report_service.dto.ResolveReportRequestDto;
import com.connectsphere.report_service.entity.Report.ReportStatus;

// Service interface for report operations.
public interface ReportService {

    // Creates a new report.
    ReportResponseDto createReport(CreateReportRequestDto dto, Integer reporterId);

    // Gets a report by ID.
    ReportResponseDto getReportById(Integer reportId);

    // Re-runs AI analysis for a report.
    ReportResponseDto retryAiAnalysis(Integer reportId, Integer adminId);

    // Gets report queue with optional status filter.
    Page<ReportResponseDto> getReportQueue(ReportStatus status, Pageable pageable);

    // Resolves a report.
    ReportResponseDto resolveReport(Integer reportId, ResolveReportRequestDto dto, Integer adminId);

    // Marks a report as under review.
    ReportResponseDto markUnderReview(Integer reportId, Integer adminId);

    // Dismisses a report.
    ReportResponseDto dismissReport(Integer reportId, Integer adminId);

    // Gets all reports by a user.
    Page<ReportResponseDto> getReportsByUser(Integer userId, Pageable pageable);

    // Gets report statistics.
    ReportStatsResponseDto getReportStats();
}
