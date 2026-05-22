package com.connectsphere.report_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.connectsphere.report_service.dto.CreateReportRequestDto;
import com.connectsphere.report_service.dto.ReportResponseDto;
import com.connectsphere.report_service.dto.ReportStatsResponseDto;
import com.connectsphere.report_service.dto.ResolveReportRequestDto;
import com.connectsphere.report_service.entity.Report;
import com.connectsphere.report_service.entity.Report.ReportReason;
import com.connectsphere.report_service.entity.Report.ReportStatus;
import com.connectsphere.report_service.entity.Report.TargetType;
import com.connectsphere.report_service.event.NotificationEventPublisher;
import com.connectsphere.report_service.exception.DuplicateReportException;
import com.connectsphere.report_service.exception.ReportNotFoundException;
import com.connectsphere.report_service.repository.ReportRepository;
import com.connectsphere.report_service.service.AiReportAnalysisService;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

	@Mock
	private ReportRepository reportRepository;

	@Mock
	private AiReportAnalysisService aiAnalysisService;

	@Mock
	private NotificationEventPublisher notificationPublisher;

	@InjectMocks
	private ReportServiceImpl reportService;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(reportService, "autoEscalationThreshold", 5);
		ReflectionTestUtils.setField(reportService, "autoEscalationWindowHours", 24L);
	}

	@Test
	void createReportSavesAndStartsAiAnalysis() {
		CreateReportRequestDto request = createRequest();
		Report savedReport = report(101, ReportStatus.PENDING);

		when(reportRepository.existsByReporterIdAndTargetIdAndTargetType(7, 42, TargetType.POST)).thenReturn(false);
		when(reportRepository.save(any(Report.class))).thenReturn(savedReport);
		when(reportRepository.countPendingReportsForTargetSince(eq(42), eq(TargetType.POST), any(LocalDateTime.class)))
				.thenReturn(1L);

		ReportResponseDto response = reportService.createReport(request, 7);

		assertThat(response.getReportId()).isEqualTo(101);
		assertThat(response.getReporterId()).isEqualTo(7);
		assertThat(response.getStatus()).isEqualTo(ReportStatus.PENDING);
		verify(aiAnalysisService).analyseReportAsync(savedReport);
		verify(notificationPublisher, never()).publishAdminEscalation(any(), any(), any(Long.class));
		verify(reportRepository, never()).saveAll(any());
	}

	@Test
	void createReportEscalatesWhenThresholdIsReached() {
		CreateReportRequestDto request = createRequest();
		Report savedReport = report(101, ReportStatus.PENDING);
		Report secondPending = report(102, ReportStatus.PENDING);
		List<Report> pendingReports = List.of(savedReport, secondPending);

		when(reportRepository.existsByReporterIdAndTargetIdAndTargetType(7, 42, TargetType.POST)).thenReturn(false);
		when(reportRepository.save(any(Report.class))).thenReturn(savedReport);
		when(reportRepository.countPendingReportsForTargetSince(eq(42), eq(TargetType.POST), any(LocalDateTime.class)))
				.thenReturn(5L);
		when(reportRepository.findByTargetIdAndTargetTypeAndStatus(42, TargetType.POST, ReportStatus.PENDING))
				.thenReturn(pendingReports);

		reportService.createReport(request, 7);

		assertThat(savedReport.getStatus()).isEqualTo(ReportStatus.UNDER_REVIEW);
		assertThat(secondPending.getStatus()).isEqualTo(ReportStatus.UNDER_REVIEW);
		verify(reportRepository).saveAll(pendingReports);
		verify(notificationPublisher).publishAdminEscalation(TargetType.POST, 42, 5L);
	}

	@Test
	void createReportRejectsDuplicates() {
		CreateReportRequestDto request = createRequest();
		when(reportRepository.existsByReporterIdAndTargetIdAndTargetType(7, 42, TargetType.POST)).thenReturn(true);

		assertThatThrownBy(() -> reportService.createReport(request, 7)).isInstanceOf(DuplicateReportException.class)
				.hasMessageContaining("already reported");

		verify(reportRepository, never()).save(any(Report.class));
	}

	@Test
	void getReportByIdThrowsWhenMissing() {
		when(reportRepository.findById(999)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> reportService.getReportById(999)).isInstanceOf(ReportNotFoundException.class)
				.hasMessageContaining("999");
	}

	@Test
	void getReportQueueUsesRepositoryFindAllWhenStatusIsNull() {
		Report report = report(12, ReportStatus.PENDING);
		PageRequest pageRequest = PageRequest.of(0, 10);
		when(reportRepository.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of(report)));

		var response = reportService.getReportQueue(null, pageRequest);

		assertThat(response.getContent()).hasSize(1);
		assertThat(response.getContent().get(0).getReportId()).isEqualTo(12);
	}

	@Test
	void getReportQueueUsesStatusFilterWhenProvided() {
		Report report = report(12, ReportStatus.UNDER_REVIEW);
		PageRequest pageRequest = PageRequest.of(0, 10);
		when(reportRepository.findByStatus(ReportStatus.UNDER_REVIEW, pageRequest))
				.thenReturn(new PageImpl<>(List.of(report)));

		var response = reportService.getReportQueue(ReportStatus.UNDER_REVIEW, pageRequest);

		assertThat(response.getContent()).hasSize(1);
		assertThat(response.getContent().get(0).getStatus()).isEqualTo(ReportStatus.UNDER_REVIEW);
	}

	@Test
	void getReportsByUserMapsRepositoryResults() {
		Report report = report(55, ReportStatus.PENDING);
		PageRequest pageRequest = PageRequest.of(0, 5);
		when(reportRepository.findByReporterId(7, pageRequest)).thenReturn(new PageImpl<>(List.of(report)));

		var response = reportService.getReportsByUser(7, pageRequest);

		assertThat(response.getContent()).hasSize(1);
		assertThat(response.getContent().get(0).getReporterId()).isEqualTo(7);
	}

	@Test
	void getReportStatsAggregatesCountsByStatus() {
		when(reportRepository.countGroupByStatus()).thenReturn(List.of(new Object[] { ReportStatus.PENDING, 2L },
				new Object[] { ReportStatus.UNDER_REVIEW, 1L }, new Object[] { ReportStatus.RESOLVED, 3L }));

		ReportStatsResponseDto stats = reportService.getReportStats();

		assertThat(stats.getTotalReports()).isEqualTo(6);
		assertThat(stats.getPendingCount()).isEqualTo(2);
		assertThat(stats.getUnderReviewCount()).isEqualTo(1);
		assertThat(stats.getResolvedCount()).isEqualTo(3);
		assertThat(stats.getDismissedCount()).isZero();
	}

	@Test
	void resolveReportUpdatesStateAndPublishesNotification() {
		Report report = report(101, ReportStatus.PENDING);
		ResolveReportRequestDto request = new ResolveReportRequestDto();
		request.setResolutionNote("Content removed");

		when(reportRepository.findById(101)).thenReturn(Optional.of(report));
		when(reportRepository.save(report)).thenReturn(report);

		ReportResponseDto response = reportService.resolveReport(101, request, 9001);

		assertThat(response.getStatus()).isEqualTo(ReportStatus.RESOLVED);
		assertThat(report.getResolvedBy()).isEqualTo(9001);
		assertThat(report.getResolutionNote()).isEqualTo("Content removed");
		assertThat(report.getResolvedAt()).isNotNull();
		verify(notificationPublisher).publishReportAction(7, 101, "RESOLVED", "Content removed");
	}

	@Test
	void markUnderReviewUpdatesPendingReport() {
		Report report = report(101, ReportStatus.PENDING);

		when(reportRepository.findById(101)).thenReturn(Optional.of(report));
		when(reportRepository.save(report)).thenReturn(report);

		ReportResponseDto response = reportService.markUnderReview(101, 9001);

		assertThat(response.getStatus()).isEqualTo(ReportStatus.UNDER_REVIEW);
		assertThat(report.getResolvedBy()).isEqualTo(9001);
		verify(reportRepository).save(report);
	}

	@Test
	void markUnderReviewLeavesClosedReportUnchanged() {
		Report report = report(101, ReportStatus.RESOLVED);

		when(reportRepository.findById(101)).thenReturn(Optional.of(report));

		ReportResponseDto response = reportService.markUnderReview(101, 9001);

		assertThat(response.getStatus()).isEqualTo(ReportStatus.RESOLVED);
		verify(reportRepository, never()).save(report);
	}

	@Test
	void dismissReportUpdatesStateAndPublishesNotification() {
		Report report = report(101, ReportStatus.PENDING);

		when(reportRepository.findById(101)).thenReturn(Optional.of(report));
		when(reportRepository.save(report)).thenReturn(report);

		ReportResponseDto response = reportService.dismissReport(101, 9001);

		assertThat(response.getStatus()).isEqualTo(ReportStatus.DISMISSED);
		assertThat(report.getResolvedBy()).isEqualTo(9001);
		assertThat(report.getResolvedAt()).isNotNull();
		verify(notificationPublisher).publishReportAction(7, 101, "DISMISSED", null);
	}

	private CreateReportRequestDto createRequest() {
		CreateReportRequestDto dto = new CreateReportRequestDto();
		dto.setTargetId(42);
		dto.setTargetType(TargetType.POST);
		dto.setReason(ReportReason.SPAM);
		dto.setDescription("Repeated spam links");
		return dto;
	}

	private Report report(Integer id, ReportStatus status) {
		return Report.builder().reportId(id).reporterId(7).targetId(42).targetType(TargetType.POST)
				.reason(ReportReason.SPAM).description("Repeated spam links").status(status)
				.createdAt(LocalDateTime.now()).build();
	}
}
