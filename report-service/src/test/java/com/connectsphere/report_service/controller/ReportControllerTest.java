package com.connectsphere.report_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.connectsphere.report_service.dto.ReportResponseDto;
import com.connectsphere.report_service.dto.ReportStatsResponseDto;
import com.connectsphere.report_service.entity.Report.ReportReason;
import com.connectsphere.report_service.entity.Report.ReportStatus;
import com.connectsphere.report_service.entity.Report.TargetType;
import com.connectsphere.report_service.exception.GlobalExceptionHandler;
import com.connectsphere.report_service.service.ReportService;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

	@Mock
	private ReportService reportService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new ReportController(reportService))
				.setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
				.setControllerAdvice(new GlobalExceptionHandler()).build();
	}

	@Test
	void createReportReturnsCreatedResponse() throws Exception {
		when(reportService.createReport(any(), eq(7))).thenReturn(responseDto());

		mockMvc.perform(
				post("/api/v1/reports").contentType(MediaType.APPLICATION_JSON).header("X-User-Id", "7").content("""
						{"targetId":42,"targetType":"POST","reason":"SPAM","description":"Repeated spam links"}
						""")).andExpect(status().isCreated()).andExpect(jsonPath("$.data.reportId").value(101))
				.andExpect(jsonPath("$.data.status").value("PENDING"));

		verify(reportService).createReport(any(), eq(7));
	}

	@Test
	void resolveReportRejectsInvalidRequestBody() throws Exception {
		mockMvc.perform(put("/api/v1/reports/101/resolve").contentType(MediaType.APPLICATION_JSON)
				.header("X-User-Id", "99").content("""
						{"resolutionNote":""}
						""")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fields.resolutionNote").exists());
	}

	@Test
	void getQueueReturnsPagedResult() throws Exception {
		when(reportService.getReportQueue(eq(ReportStatus.PENDING), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(java.util.List.of(responseDto()), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/api/v1/reports/queue").param("status", "PENDING")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content[0].reportId").value(101));
	}

	@Test
	void getReportReturnsServicePayload() throws Exception {
		when(reportService.getReportById(101)).thenReturn(responseDto());

		mockMvc.perform(get("/api/v1/reports/101")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.reportId").value(101));

		verify(reportService).getReportById(101);
	}

	@Test
	void retryAiAnalysisReturnsUpdatedReport() throws Exception {
		when(reportService.retryAiAnalysis(101, 99)).thenReturn(responseDto());

		mockMvc.perform(post("/api/v1/reports/101/ai-analysis/retry").header("X-User-Id", "99"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("AI analysis queued"));

		verify(reportService).retryAiAnalysis(101, 99);
	}

	@Test
	void resolveReportReturnsUpdatedReport() throws Exception {
		ReportResponseDto resolved = ReportResponseDto.builder().reportId(101).reporterId(7).targetId(42)
				.targetType(TargetType.POST).reason(ReportReason.SPAM).description("Repeated spam links")
				.status(ReportStatus.RESOLVED).createdAt(LocalDateTime.of(2026, 4, 19, 12, 0)).build();
		when(reportService.resolveReport(eq(101), any(), eq(99))).thenReturn(resolved);

		mockMvc.perform(put("/api/v1/reports/101/resolve").contentType(MediaType.APPLICATION_JSON)
				.header("X-User-Id", "99").content("""
						{"resolutionNote":"Handled"}
						""")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("RESOLVED"));

		verify(reportService).resolveReport(eq(101), any(), eq(99));
	}

	@Test
	void getStatsReturnsServicePayload() throws Exception {
		when(reportService.getReportStats())
				.thenReturn(ReportStatsResponseDto.builder().totalReports(6).pendingCount(2).resolvedCount(3).build());

		mockMvc.perform(get("/api/v1/reports/stats")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalReports").value(6)).andExpect(jsonPath("$.data.pendingCount").value(2))
				.andExpect(jsonPath("$.data.resolvedCount").value(3));
	}

	@Test
	void markUnderReviewReturnsUpdatedReport() throws Exception {
		ReportResponseDto underReview = ReportResponseDto.builder().reportId(101).reporterId(7).targetId(42)
				.targetType(TargetType.POST).reason(ReportReason.SPAM).description("Repeated spam links")
				.status(ReportStatus.UNDER_REVIEW).createdAt(LocalDateTime.of(2026, 4, 19, 12, 0)).build();
		when(reportService.markUnderReview(101, 99)).thenReturn(underReview);

		mockMvc.perform(put("/api/v1/reports/101/review").header("X-User-Id", "99"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"));

		verify(reportService).markUnderReview(101, 99);
	}

	@Test
	void dismissReportReturnsUpdatedReport() throws Exception {
		ReportResponseDto dismissed = ReportResponseDto.builder().reportId(101).reporterId(7).targetId(42)
				.targetType(TargetType.POST).reason(ReportReason.SPAM).description("Repeated spam links")
				.status(ReportStatus.DISMISSED).createdAt(LocalDateTime.of(2026, 4, 19, 12, 0)).build();
		when(reportService.dismissReport(101, 99)).thenReturn(dismissed);

		mockMvc.perform(put("/api/v1/reports/101/dismiss").header("X-User-Id", "99"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("DISMISSED"));

		verify(reportService).dismissReport(101, 99);
	}

	@Test
	void getReportsByUserReturnsPagedResult() throws Exception {
		when(reportService.getReportsByUser(eq(7), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(java.util.List.of(responseDto()), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/api/v1/reports/user/7")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content[0].reporterId").value(7));

		verify(reportService).getReportsByUser(eq(7), any(PageRequest.class));
	}

	private ReportResponseDto responseDto() {
		return ReportResponseDto.builder().reportId(101).reporterId(7).targetId(42).targetType(TargetType.POST)
				.reason(ReportReason.SPAM).description("Repeated spam links").status(ReportStatus.PENDING)
				.createdAt(LocalDateTime.of(2026, 4, 19, 12, 0)).build();
	}
}
