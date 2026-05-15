package com.connectsphere.report_service.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connectsphere.report_service.dto.CreateReportRequestDto;
import com.connectsphere.report_service.dto.ReportResponseDto;
import com.connectsphere.report_service.dto.ReportStatsResponseDto;
import com.connectsphere.report_service.dto.ResolveReportRequestDto;
import com.connectsphere.report_service.entity.Report;
import com.connectsphere.report_service.entity.Report.ReportStatus;
import com.connectsphere.report_service.event.NotificationEventPublisher;
import com.connectsphere.report_service.exception.DuplicateReportException;
import com.connectsphere.report_service.exception.ReportNotFoundException;
import com.connectsphere.report_service.repository.ReportRepository;
import com.connectsphere.report_service.service.AiReportAnalysisService;
import com.connectsphere.report_service.service.ReportService;

import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Implementation of report service with business logic.
@Service
@Slf4j
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

	private final ReportRepository reportRepository;
	private final AiReportAnalysisService aiAnalysisService;
	private final NotificationEventPublisher notificationPublisher;

	@Value("${report.auto-escalation.threshold:5}")
	private int autoEscalationThreshold;

	@Value("${report.auto-escalation.window-hours:24}")
	private long autoEscalationWindowHours;

	// Creates a new report and triggers AI analysis.
	@Override
	@Transactional
	public ReportResponseDto createReport(CreateReportRequestDto dto, Integer reporterId) {
		validateNoDuplicate(reporterId, dto.getTargetId(), dto.getTargetType());

		Report report = Report.builder().reporterId(reporterId).targetId(dto.getTargetId())
				.targetType(dto.getTargetType()).reason(dto.getReason()).description(dto.getDescription())
				.status(ReportStatus.PENDING).build();

		report = reportRepository.save(report);

		aiAnalysisService.analyseReportAsync(report);
		checkAndEscalate(report);

		return toDto(report);
	}

	// Gets report by ID.
	@Override
	@Transactional(readOnly = true)
	public ReportResponseDto getReportById(Integer reportId) {
		return toDto(findOrThrow(reportId));
	}

	@Override
	@Transactional
	public ReportResponseDto retryAiAnalysis(Integer reportId, Integer adminId) {
		Report report = findOrThrow(reportId);
		report.setAiAnalysis("AI analysis queued by admin #" + adminId + ".");
		report.setAiSeverityScore(null);
		reportRepository.save(report);
		aiAnalysisService.analyseReportAsync(report);
		return toDto(report);
	}

	// Gets report queue with optional filter.
	@Override
	@Transactional(readOnly = true)
	public Page<ReportResponseDto> getReportQueue(ReportStatus status, Pageable pageable) {
		Page<Report> page = (status == null) ? reportRepository.findAll(pageable)
				: reportRepository.findByStatus(status, pageable);
		return page.map(this::toDto);
	}

	// Gets reports by user.
	@Override
	@Transactional(readOnly = true)
	public Page<ReportResponseDto> getReportsByUser(Integer userId, Pageable pageable) {
		return reportRepository.findByReporterId(userId, pageable).map(this::toDto);
	}

	// Gets report statistics.
	@Override
	@Transactional(readOnly = true)
	public ReportStatsResponseDto getReportStats() {
		List<Object[]> rows = reportRepository.countGroupByStatus();
		Map<String, Long> counts = new java.util.HashMap<>();

		for (Object[] row : rows) {
			counts.put(row[0].toString(), (Long) row[1]);
		}

		long total = counts.values().stream().mapToLong(Long::longValue).sum();

		return ReportStatsResponseDto.builder().totalReports(total).pendingCount(counts.getOrDefault("PENDING", 0L))
				.underReviewCount(counts.getOrDefault("UNDER_REVIEW", 0L))
				.resolvedCount(counts.getOrDefault("RESOLVED", 0L)).dismissedCount(counts.getOrDefault("DISMISSED", 0L))
				.build();
	}

	// Resolves a report and sends notification.
	@Override
	@Transactional
	public ReportResponseDto resolveReport(Integer reportId, ResolveReportRequestDto dto, Integer adminId) {
		Report report = findOrThrow(reportId);

		report.setStatus(ReportStatus.RESOLVED);
		report.setResolvedBy(adminId);
		report.setResolutionNote(dto.getResolutionNote());
		report.setResolvedAt(LocalDateTime.now());

		reportRepository.save(report);

		notificationPublisher.publishReportAction(report.getReporterId(), report.getReportId(), "RESOLVED",
				dto.getResolutionNote());

		return toDto(report);
	}

	// Marks a report as actively being reviewed.
	@Override
	@Transactional
	public ReportResponseDto markUnderReview(Integer reportId, Integer adminId) {
		Report report = findOrThrow(reportId);

		if (report.getStatus() == ReportStatus.RESOLVED || report.getStatus() == ReportStatus.DISMISSED) {
			return toDto(report);
		}

		report.setStatus(ReportStatus.UNDER_REVIEW);
		report.setResolvedBy(adminId);
		reportRepository.save(report);

		return toDto(report);
	}

	// Dismisses a report and sends notification.
	@Override
	@Transactional
	public ReportResponseDto dismissReport(Integer reportId, Integer adminId) {
		Report report = findOrThrow(reportId);

		report.setStatus(ReportStatus.DISMISSED);
		report.setResolvedBy(adminId);
		report.setResolvedAt(LocalDateTime.now());

		reportRepository.save(report);

		notificationPublisher.publishReportAction(report.getReporterId(), report.getReportId(), "DISMISSED", null);

		return toDto(report);
	}

	// Checks and escalates reports if threshold is reached.
	private void checkAndEscalate(Report trigger) {
		LocalDateTime since = LocalDateTime.now().minusHours(autoEscalationWindowHours);

		long count = reportRepository.countPendingReportsForTargetSince(trigger.getTargetId(), trigger.getTargetType(),
				since);

		if (count >= autoEscalationThreshold) {
			List<Report> pending = reportRepository.findByTargetIdAndTargetTypeAndStatus(trigger.getTargetId(),
					trigger.getTargetType(), ReportStatus.PENDING);

			pending.forEach(r -> r.setStatus(ReportStatus.UNDER_REVIEW));
			reportRepository.saveAll(pending);

			notificationPublisher.publishAdminEscalation(trigger.getTargetType(), trigger.getTargetId(), count);
		}
	}

	// Validates duplicate report.
	private void validateNoDuplicate(Integer reporterId, Integer targetId, Report.TargetType targetType) {
		if (reportRepository.existsByReporterIdAndTargetIdAndTargetType(reporterId, targetId, targetType)) {
			throw new DuplicateReportException("You have already reported this target");
		}
	}

	// Finds report or throws exception.
	private Report findOrThrow(Integer reportId) {
		return reportRepository.findById(reportId)
				.orElseThrow(() -> new ReportNotFoundException("Report not found for id " + reportId));
	}

	// Converts entity to DTO.
	private ReportResponseDto toDto(Report r) {
		return ReportResponseDto.builder().reportId(r.getReportId()).reporterId(r.getReporterId())
				.targetId(r.getTargetId()).targetType(r.getTargetType()).reason(r.getReason())
				.description(r.getDescription()).status(r.getStatus()).resolvedBy(r.getResolvedBy())
				.resolutionNote(r.getResolutionNote()).aiAnalysis(r.getAiAnalysis())
				.aiSeverityScore(r.getAiSeverityScore()).createdAt(r.getCreatedAt()).resolvedAt(r.getResolvedAt())
				.build();
	}
}
