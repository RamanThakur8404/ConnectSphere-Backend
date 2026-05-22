package com.connectsphere.report_service.service;

import com.connectsphere.report_service.entity.Report;

// Service for AI-based report analysis.
public interface AiReportAnalysisService {

	// Performs async analysis on a report and updates AI results.
	void analyseReportAsync(Report report);
}