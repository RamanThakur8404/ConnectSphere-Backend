package com.connectsphere.report_service.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.connectsphere.report_service.dto.CreateReportRequestDto;
import com.connectsphere.report_service.dto.ReportResponseDto;
import com.connectsphere.report_service.dto.ReportStatsResponseDto;
import com.connectsphere.report_service.dto.ResolveReportRequestDto;
import com.connectsphere.report_service.dto.ApiResponse;
import com.connectsphere.report_service.entity.Report.ReportStatus;
import com.connectsphere.report_service.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Controller for handling report-related API requests.
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Report API", description = "Content moderation report endpoints")
public class ReportController {

    private final ReportService reportService;

    // Creates a new report.
    @PostMapping
    @Operation(summary = "Submit a content report")
    public ResponseEntity<ApiResponse<ReportResponseDto>> createReport(
            @Valid @RequestBody CreateReportRequestDto dto,
            @RequestHeader("X-User-Id") Integer userId) {

        ReportResponseDto response = reportService.createReport(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Report submitted successfully", response));
    }

    // Gets a report by ID.
    @GetMapping("/{reportId}")
    @Operation(summary = "Get report by ID")
    public ResponseEntity<ApiResponse<ReportResponseDto>> getReport(
            @PathVariable Integer reportId) {

        return ResponseEntity.ok(ApiResponse.success("Report retrieved", reportService.getReportById(reportId)));
    }

    // Re-runs AI analysis for a report.
    @PostMapping("/{reportId}/ai-analysis/retry")
    @Operation(summary = "Retry AI analysis")
    public ResponseEntity<ApiResponse<ReportResponseDto>> retryAiAnalysis(
            @PathVariable Integer reportId,
            @RequestHeader("X-User-Id") Integer adminId) {

        return ResponseEntity.ok(ApiResponse.success("AI analysis queued",
                reportService.retryAiAnalysis(reportId, adminId)));
    }

    // Gets report queue with optional status filter.
    @GetMapping("/queue")
    @Operation(summary = "Get moderation queue")
    public ResponseEntity<ApiResponse<Page<ReportResponseDto>>> getQueue(
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success("Moderation queue retrieved", reportService.getReportQueue(status, pageable)));
    }

    // Resolves a report.
    @PutMapping("/{reportId}/resolve")
    @Operation(summary = "Resolve a report")
    public ResponseEntity<ApiResponse<ReportResponseDto>> resolveReport(
            @PathVariable Integer reportId,
            @Valid @RequestBody ResolveReportRequestDto dto,
            @RequestHeader("X-User-Id") Integer adminId) {

        return ResponseEntity.ok(ApiResponse.success("Report resolved", reportService.resolveReport(reportId, dto, adminId)));
    }

    // Marks a report as under review.
    @PutMapping("/{reportId}/review")
    @Operation(summary = "Mark report as under review")
    public ResponseEntity<ApiResponse<ReportResponseDto>> markUnderReview(
            @PathVariable Integer reportId,
            @RequestHeader("X-User-Id") Integer adminId) {

        return ResponseEntity.ok(ApiResponse.success("Report marked under review", reportService.markUnderReview(reportId, adminId)));
    }

    // Dismisses a report.
    @PutMapping("/{reportId}/dismiss")
    @Operation(summary = "Dismiss a report")
    public ResponseEntity<ApiResponse<ReportResponseDto>> dismissReport(
            @PathVariable Integer reportId,
            @RequestHeader("X-User-Id") Integer adminId) {

        return ResponseEntity.ok(ApiResponse.success("Report dismissed", reportService.dismissReport(reportId, adminId)));
    }

    // Gets all reports created by a user.
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get reports by user")
    public ResponseEntity<ApiResponse<Page<ReportResponseDto>>> getReportsByUser(
            @PathVariable Integer userId,
            @RequestHeader("X-User-Id") Integer requesterId,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String requesterRole,
            @PageableDefault(size = 20) Pageable pageable) {

        boolean privileged = "ADMIN".equalsIgnoreCase(requesterRole) || "MODERATOR".equalsIgnoreCase(requesterRole);
        if (!privileged && !userId.equals(requesterId)) {
            throw new AccessDeniedException("Users can only view their own reports");
        }

        return ResponseEntity.ok(ApiResponse.success("User reports retrieved", reportService.getReportsByUser(userId, pageable)));
    }

    // Gets report statistics.
    @GetMapping("/stats")
    @Operation(summary = "Get report stats")
    public ResponseEntity<ApiResponse<ReportStatsResponseDto>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Report stats retrieved", reportService.getReportStats()));
    }
}
