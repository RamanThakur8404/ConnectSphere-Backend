package com.connectsphere.report_service.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.connectsphere.report_service.entity.Report;
import com.connectsphere.report_service.repository.ReportRepository;
import com.connectsphere.report_service.service.AiReportAnalysisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Service that performs AI analysis for reports.
@Service
@Slf4j
@RequiredArgsConstructor
public class AiReportAnalysisServiceImpl implements AiReportAnalysisService {

    private static final String PROVIDER_RULE_BASED = "RULE_BASED";
    private static final String PROVIDER_OPENAI = "OPENAI";
    private static final String PROVIDER_GEMINI = "GEMINI";

    @Value("${ai.provider:${AI_PROVIDER:GEMINI}}")
    private String aiProvider;

    @Value("${ai.api.url:https://api.openai.com/v1/chat/completions}")
    private String aiApiUrl;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.max-output-tokens:${openai.max-tokens:600}}")
    private int maxOutputTokens;

    @Value("${openai.temperature:0.2}")
    private double temperature;

    @Value("${ai.api.key:${openai.api.key:}}")
    private String aiApiKey;

    @Value("${gemini.api.url:${GEMINI_API_URL:https://generativelanguage.googleapis.com/v1beta}}")
    private String geminiApiUrl;

    @Value("${gemini.api.key:${GEMINI_API_KEY:}}")
    private String geminiApiKey;

    @Value("${gemini.model:${GEMINI_MODEL:gemini-2.5-flash}}")
    private String geminiModel;

    private final ReportRepository reportRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Runs AI analysis in background and updates report.
    @Override
    @Async("aiAnalysisExecutor")
    public void analyseReportAsync(Report report) {
        log.info("Starting AI analysis for reportId={}", report.getReportId());
        try {
            if (isRuleBasedProvider()) {
                persistRuleBased(report);
                return;
            }

            if (isGeminiProvider()) {
                if (!hasConfiguredGeminiKey()) {
                    log.warn("Skipping Gemini analysis for reportId={} because gemini.api.key is not configured",
                            report.getReportId());
                    persistFallback(report, "Gemini API key is not configured.");
                    return;
                }
                String rawJson = callGeminiApi(buildPrompt(report));
                parseAndPersist(report, rawJson, "Gemini");
                return;
            }

            if (aiApiKey == null || aiApiKey.isBlank()) {
                log.warn("Skipping AI analysis for reportId={} because ai.api.key is not configured",
                        report.getReportId());
                persistFallback(report, "OpenAI API key is not configured.");
                return;
            }
            String prompt = buildPrompt(report);
            String rawJson = callOpenAiResponsesApi(prompt);
            parseAndPersist(report, rawJson, null);
        } catch (RestClientResponseException ex) {
            String reason = extractApiError(ex);
            log.error("AI analysis failed for reportId={} - {} returned {}: {}",
                    report.getReportId(), activeProviderLabel(), ex.getStatusCode(), reason);
            persistFallback(report, reason);
        } catch (Exception ex) {
            log.error("AI analysis failed for reportId={} - {}", report.getReportId(), ex.getMessage());
            persistFallback(report, activeProviderLabel() + " request failed: " + safeMessage(ex.getMessage()));
        }
    }

    private boolean isRuleBasedProvider() {
        return PROVIDER_RULE_BASED.equalsIgnoreCase(aiProvider);
    }

    private boolean isGeminiProvider() {
        return PROVIDER_GEMINI.equalsIgnoreCase(aiProvider);
    }

    private boolean hasConfiguredGeminiKey() {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return false;
        }
        String normalized = geminiApiKey.trim();
        return !"your_new_gemini_key".equalsIgnoreCase(normalized)
                && !"your_gemini_api_key".equalsIgnoreCase(normalized);
    }

    // Builds prompt for AI request.
    private String buildPrompt(Report report) {
        return """
            Analyse the report and return strictly a JSON object with two fields: 'analysis' (string) and 'severity' (integer 1-10).
            Target: %s
            Reason: %s
            Description: %s
            """.formatted(
                report.getTargetType(),
                report.getReason(),
                report.getDescription() != null ? report.getDescription() : "No details"
        );
    }

    // Calls OpenAI Responses API and returns the structured JSON text.
    private String callOpenAiResponsesApi(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + aiApiKey);

        Map<String, Object> body = Map.of(
                "model", effectiveModel(),
                "max_output_tokens", effectiveMaxOutputTokens(),
                "temperature", effectiveTemperature(),
                "input", List.of(
                        Map.of("role", "system", "content", "You are a content moderation assistant. Return only the requested structured JSON."),
                        Map.of("role", "user", "content", prompt)
                ),
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "report_analysis",
                        "strict", true,
                        "schema", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of(
                                        "analysis", Map.of("type", "string"),
                                        "severity", Map.of("type", "integer", "minimum", 1, "maximum", 10)
                                ),
                                "required", List.of("analysis", "severity")
                        )
                ))
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response =
                restTemplate.postForEntity(resolveResponsesEndpoint(), request, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("API error: " + response.getStatusCode());
        }

        JsonNode root = parseJson(response.getBody());
        JsonNode outputText = root.path("output_text");
        if (outputText.isTextual() && !outputText.asText().isBlank()) {
            return outputText.asText();
        }

        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (!content.isArray()) continue;
                for (JsonNode contentItem : content) {
                    if ("output_text".equals(contentItem.path("type").asText()) && contentItem.path("text").isTextual()) {
                        return contentItem.path("text").asText();
                    }
                }
            }
        }

        throw new RuntimeException("OpenAI response did not include output text");
    }

    // Parses AI response and saves results.
    private String callGeminiApi(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", geminiApiKey);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text",
                                "You are a content moderation assistant. Return only a JSON object with analysis and severity.\n\n" + prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", effectiveTemperature(),
                        "maxOutputTokens", effectiveMaxOutputTokens(),
                        "responseMimeType", "application/json"
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response =
                restTemplate.postForEntity(resolveGeminiEndpoint(), request, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Gemini API error: " + response.getStatusCode());
        }

        JsonNode root = parseJson(response.getBody());
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray()) {
            for (JsonNode candidate : candidates) {
                JsonNode parts = candidate.path("content").path("parts");
                if (!parts.isArray()) continue;
                for (JsonNode part : parts) {
                    if (part.path("text").isTextual() && !part.path("text").asText().isBlank()) {
                        return part.path("text").asText();
                    }
                }
            }
        }

        throw new RuntimeException("Gemini response did not include text");
    }

    private void parseAndPersist(Report report, String rawJson, String providerLabel) {
        JsonNode node = parseJson(rawJson);
        String analysis = node.path("analysis").asText();
        int severity = node.path("severity").asInt(0);

        if (severity < 1 || severity > 10) {
            severity = 0;
        }

        report.setAiAnalysis(providerLabel == null || providerLabel.isBlank()
                ? analysis
                : providerLabel + " analysis: " + analysis);
        report.setAiSeverityScore(severity == 0 ? null : severity);
        reportRepository.save(report);

        log.info("AI analysis saved for reportId={}", report.getReportId());
    }

    private void persistFallback(Report report, String reason) {
        int severity = estimateSeverity(report);
        report.setAiAnalysis(activeProviderLabel() + " analysis unavailable (" + safeMessage(reason)
                + "). Rule-based fallback: " + fallbackSummary(report, severity));
        report.setAiSeverityScore(severity);
        reportRepository.save(report);
    }

    private void persistRuleBased(Report report) {
        int severity = estimateSeverity(report);
        report.setAiAnalysis("Rule-based analysis: " + fallbackSummary(report, severity));
        report.setAiSeverityScore(severity);
        reportRepository.save(report);
        log.info("Rule-based analysis saved for reportId={} severity={}", report.getReportId(), severity);
    }

    private int estimateSeverity(Report report) {
        if (report.getReason() == null) {
            return 4;
        }

        return switch (report.getReason()) {
            case HATE_SPEECH, HARASSMENT -> 8;
            case NSFW -> 7;
            case MISINFORMATION -> 6;
            case SPAM -> 4;
            case OTHER -> report.getDescription() != null && !report.getDescription().isBlank() ? 5 : 3;
        };
    }

    private String fallbackSummary(Report report, int severity) {
        String target = report.getTargetType() + " #" + report.getTargetId();
        String reason = report.getReason() != null ? report.getReason().name().replace('_', ' ') : "UNSPECIFIED";
        String action = severity >= 8
                ? "prioritize moderator review and consider temporary restriction if the target confirms policy risk."
                : severity >= 6
                ? "review soon and compare the report reason against the target content."
                : "low-to-medium priority; review for repeated reports or obvious policy violations.";

        return "Reported " + target + " for " + reason + ". Estimated severity " + severity + "/10; " + action;
    }

    private String resolveResponsesEndpoint() {
        String configured = aiApiUrl == null || aiApiUrl.isBlank()
                ? "https://api.openai.com/v1"
                : aiApiUrl.trim();
        String withoutTrailingSlash = configured.replaceAll("/+$", "");
        if (withoutTrailingSlash.endsWith("/responses")) {
            return withoutTrailingSlash;
        }
        if (withoutTrailingSlash.endsWith("/chat/completions")) {
            return withoutTrailingSlash.substring(0, withoutTrailingSlash.length() - "/chat/completions".length()) + "/responses";
        }
        return withoutTrailingSlash + "/responses";
    }

    private String resolveGeminiEndpoint() {
        String configured = geminiApiUrl == null || geminiApiUrl.isBlank()
                ? "https://generativelanguage.googleapis.com/v1beta"
                : geminiApiUrl.trim();
        String base = configured.replaceAll("/+$", "");
        String modelName = geminiModel == null || geminiModel.isBlank() ? "gemini-2.5-flash" : geminiModel.trim();

        if (base.contains(":generateContent")) {
            return base;
        }

        if (modelName.startsWith("models/")) {
            return base + "/" + modelName + ":generateContent";
        }

        return base + "/models/" + modelName + ":generateContent";
    }

    private String effectiveModel() {
        return model == null || model.isBlank() ? "gpt-4o-mini" : model;
    }

    private int effectiveMaxOutputTokens() {
        return maxOutputTokens > 0 ? maxOutputTokens : 600;
    }

    private double effectiveTemperature() {
        return temperature > 0 ? temperature : 0.2;
    }

    // Converts string to JSON.
    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new RuntimeException("JSON parse failed", ex);
        }
    }

    private String extractApiError(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return ex.getMessage();
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode error = root.path("error");
            String message = error.path("message").asText("");
            String code = error.path("code").asText("");
            if (!message.isBlank() && !code.isBlank()) {
                return code + ": " + message;
            }
            if (!message.isBlank()) {
                return message;
            }
        } catch (Exception ignored) {
            // Use raw body below.
        }
        return body.length() > 300 ? body.substring(0, 300) + "..." : body;
    }

    private String activeProviderLabel() {
        if (isGeminiProvider()) {
            return "Gemini";
        }
        if (PROVIDER_OPENAI.equalsIgnoreCase(aiProvider)) {
            return "OpenAI";
        }
        return "AI";
    }

    private String safeMessage(String value) {
        if (value == null || value.isBlank()) {
            return "unknown error";
        }
        return value.length() > 220 ? value.substring(0, 220) + "..." : value;
    }
}
