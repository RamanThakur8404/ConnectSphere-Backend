package com.connectsphere.report_service.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.connectsphere.report_service.entity.Report.TargetType;

@ExtendWith(MockitoExtension.class)
class NotificationEventPublisherTest {

	@Mock
	private RabbitTemplate rabbitTemplate;

	private NotificationEventPublisher publisher;

	@BeforeEach
	void setUp() {
		publisher = new NotificationEventPublisher(rabbitTemplate);
		ReflectionTestUtils.setField(publisher, "notificationExchange", "connectsphere.notification");
		ReflectionTestUtils.setField(publisher, "reportActionRoutingKey", "report.action");
		ReflectionTestUtils.setField(publisher, "adminEscalationRoutingKey", "report.escalated");
	}

	@Test
	void publishReportActionSendsExpectedPayload() {
		ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

		publisher.publishReportAction(7, 101, "RESOLVED", "Removed");

		verify(rabbitTemplate).convertAndSend(eq("connectsphere.notification"), eq("report.action"),
				eventCaptor.capture());
		assertThatCode(() -> eventCaptor.getValue()).doesNotThrowAnyException();
		Map<String, Object> event = eventCaptor.getValue();
		org.assertj.core.api.Assertions.assertThat(event).containsEntry("type", "REPORT_ACTION")
				.containsEntry("recipientId", 7).containsEntry("reportId", 101).containsEntry("action", "RESOLVED")
				.containsEntry("note", "Removed");
	}

	@Test
	void publishReportActionSwallowsBrokerErrors() {
		doThrow(new RuntimeException("broker unavailable")).when(rabbitTemplate).convertAndSend(
				eq("connectsphere.notification"), eq("report.action"), anyMap());

		assertThatCode(() -> publisher.publishReportAction(7, 101, "DISMISSED", null)).doesNotThrowAnyException();

		verify(rabbitTemplate).convertAndSend(eq("connectsphere.notification"), eq("report.action"), anyMap());
	}

	@Test
	void publishAdminEscalationSendsExpectedPayload() {
		ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

		publisher.publishAdminEscalation(TargetType.COMMENT, 42, 6L);

		verify(rabbitTemplate).convertAndSend(eq("connectsphere.notification"), eq("report.escalated"),
				eventCaptor.capture());
		org.assertj.core.api.Assertions.assertThat(eventCaptor.getValue())
				.containsEntry("type", "ADMIN_ESCALATION")
				.containsEntry("targetType", "COMMENT")
				.containsEntry("targetId", 42)
				.containsEntry("reportCount", 6L);
	}

	@Test
	void publishAdminEscalationSwallowsBrokerErrors() {
		doThrow(new RuntimeException("broker unavailable")).when(rabbitTemplate).convertAndSend(
				eq("connectsphere.notification"), eq("report.escalated"), anyMap());

		assertThatCode(() -> publisher.publishAdminEscalation(TargetType.POST, 42, 5L)).doesNotThrowAnyException();

		verify(rabbitTemplate).convertAndSend(eq("connectsphere.notification"), eq("report.escalated"), anyMap());
	}
}
