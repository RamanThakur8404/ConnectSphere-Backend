package com.connectsphere.report_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.connectsphere.report_service.entity.Report;
import com.connectsphere.report_service.entity.Report.ReportReason;
import com.connectsphere.report_service.entity.Report.ReportStatus;
import com.connectsphere.report_service.entity.Report.TargetType;
import com.connectsphere.report_service.repository.ReportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AiReportAnalysisServiceImplTest {

	@Mock
	private ReportRepository reportRepository;

	@Mock
	private RestTemplate restTemplate;

	private AiReportAnalysisServiceImpl aiReportAnalysisService;

	@BeforeEach
	void setUp() {
		aiReportAnalysisService = new AiReportAnalysisServiceImpl(reportRepository, restTemplate, new ObjectMapper());
	}

	@Test
	void analyseReportAsyncUsesRuleBasedProviderByDefault() {
		ReflectionTestUtils.setField(aiReportAnalysisService, "aiProvider", "RULE_BASED");
		ReflectionTestUtils.setField(aiReportAnalysisService, "aiApiKey", "");

		Report report = report();
		aiReportAnalysisService.analyseReportAsync(report);

		verify(restTemplate, never()).postForEntity(any(String.class), any(), eq(String.class));
		assertThat(report.getAiAnalysis()).contains("Rule-based analysis");
		assertThat(report.getAiSeverityScore()).isEqualTo(8);
		verify(reportRepository).save(report);
	}

	@Test
	void analyseReportAsyncFallsBackWhenOpenAiKeyIsMissing() {
		ReflectionTestUtils.setField(aiReportAnalysisService, "aiProvider", "OPENAI");
		ReflectionTestUtils.setField(aiReportAnalysisService, "aiApiKey", "");

		Report report = report();
		aiReportAnalysisService.analyseReportAsync(report);

		verify(restTemplate, never()).postForEntity(any(String.class), any(), eq(String.class));
		assertThat(report.getAiAnalysis()).contains("OpenAI API key is not configured");
		assertThat(report.getAiAnalysis()).contains("Rule-based fallback");
		assertThat(report.getAiSeverityScore()).isEqualTo(8);
		verify(reportRepository).save(report);
	}

	@Test
	void analyseReportAsyncParsesAndPersistsAnalysis() {
		ReflectionTestUtils.setField(aiReportAnalysisService, "aiProvider", "OPENAI");
		ReflectionTestUtils.setField(aiReportAnalysisService, "aiApiKey", "test-key");
        ReflectionTestUtils.setField(aiReportAnalysisService, "aiApiUrl", "https://api.openai.com/v1");
		Report report = report();
		String apiResponse = """
				{"output":[{"content":[{"type":"output_text","text":"{\\"analysis\\":\\"Escalate immediately\\",\\"severity\\":8}"}]}]}
				""";
		when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
				.thenReturn(ResponseEntity.ok(apiResponse));

		aiReportAnalysisService.analyseReportAsync(report);

		assertThat(report.getAiAnalysis()).isEqualTo("Escalate immediately");
		assertThat(report.getAiSeverityScore()).isEqualTo(8);
		verify(reportRepository).save(report);
	}

	@Test
	void analyseReportAsyncParsesGeminiAnalysis() {
		ReflectionTestUtils.setField(aiReportAnalysisService, "aiProvider", "GEMINI");
		ReflectionTestUtils.setField(aiReportAnalysisService, "geminiApiKey", "test-gemini-key");
		ReflectionTestUtils.setField(aiReportAnalysisService, "geminiApiUrl", "https://generativelanguage.googleapis.com/v1beta");
		ReflectionTestUtils.setField(aiReportAnalysisService, "geminiModel", "gemini-2.5-flash");
		Report report = report();
		String apiResponse = """
				{"candidates":[{"content":{"parts":[{"text":"{\\"analysis\\":\\"Likely policy violation\\",\\"severity\\":7}"}]}}]}
				""";
		when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
				.thenReturn(ResponseEntity.ok(apiResponse));

		aiReportAnalysisService.analyseReportAsync(report);

		assertThat(report.getAiAnalysis()).isEqualTo("Gemini analysis: Likely policy violation");
		assertThat(report.getAiSeverityScore()).isEqualTo(7);
		verify(reportRepository).save(report);
	}

	@Test
	void analyseReportAsyncClearsOutOfRangeSeverity() {
		ReflectionTestUtils.setField(aiReportAnalysisService, "aiProvider", "OPENAI");
		ReflectionTestUtils.setField(aiReportAnalysisService, "aiApiKey", "test-key");
        ReflectionTestUtils.setField(aiReportAnalysisService, "aiApiUrl", "https://api.openai.com/v1/responses");
		Report report = report();
		String apiResponse = """
				{"output_text":"{\\"analysis\\":\\"Needs review\\",\\"severity\\":42}"}
				""";
		when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
				.thenReturn(ResponseEntity.ok(apiResponse));

		aiReportAnalysisService.analyseReportAsync(report);

		assertThat(report.getAiAnalysis()).isEqualTo("Needs review");
		assertThat(report.getAiSeverityScore()).isNull();
		verify(reportRepository).save(report);
	}

	private Report report() {
		return Report.builder().reportId(88).reporterId(7).targetId(42).targetType(TargetType.POST)
				.reason(ReportReason.HARASSMENT).description("Threatening language").status(ReportStatus.PENDING)
				.createdAt(LocalDateTime.now()).build();
	}
}
