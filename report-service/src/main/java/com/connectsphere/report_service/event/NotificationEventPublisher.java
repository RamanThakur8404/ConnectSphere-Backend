package com.connectsphere.report_service.event;

import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.connectsphere.report_service.entity.Report.TargetType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Publishes events to RabbitMQ for notifications.
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventPublisher {

	@Value("${rabbitmq.exchange.notification:connectsphere.notification}")
	private String notificationExchange;

	@Value("${rabbitmq.routing-key.report-action:report.action}")
	private String reportActionRoutingKey;

	@Value("${rabbitmq.routing-key.admin-escalation:report.escalated}")
	private String adminEscalationRoutingKey;

	private final RabbitTemplate rabbitTemplate;

	// Sends event when a report is resolved or dismissed.
	public void publishReportAction(Integer reporterId, Integer reportId, String action, String note) {
		Map<String, Object> event = Map.of("type", "REPORT_ACTION", "recipientId", reporterId, "reportId", reportId,
				"action", action, "note", note != null ? note : "");
		try {
			rabbitTemplate.convertAndSend(notificationExchange, reportActionRoutingKey, event);
			log.info("Published report action: reportId={} action={}", reportId, action);
		} catch (Exception ex) {
			log.error("Failed to publish report action for reportId={}: {}", reportId, ex.getMessage());
		}
	}

	// Sends event when a target is escalated.
	public void publishAdminEscalation(TargetType targetType, Integer targetId, long reportCount) {
		Map<String, Object> event = Map.of("type", "ADMIN_ESCALATION", "targetType", targetType.name(), "targetId",
				targetId, "reportCount", reportCount);
		try {
			rabbitTemplate.convertAndSend(notificationExchange, adminEscalationRoutingKey, event);
			log.warn("Published escalation: targetType={} targetId={} count={}", targetType, targetId, reportCount);
		} catch (Exception ex) {
			log.error("Failed to publish escalation: {}", ex.getMessage());
		}
	}
}
