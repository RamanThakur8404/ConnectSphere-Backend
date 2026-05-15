package com.connectsphere.report_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

// Configuration for RabbitMQ messaging. Defines exchange, queues, routing, and
@Configuration
public class RabbitMqConfig {

	// Exchange shared with notification-service
	public static final String NOTIFICATION_EXCHANGE = "connectsphere.notification";

	// Queues
	public static final String REPORT_ACTION_QUEUE = "report.action.queue";
	public static final String REPORT_ESCALATION_QUEUE = "report.escalation.queue";

	// Routing keys
	public static final String REPORT_ACTION_RK = "report.action";
	public static final String REPORT_ESCALATION_RK = "report.escalated";

	// Creates topic exchange for notifications.
	@Bean
	public TopicExchange notificationExchange() {
		return new TopicExchange(NOTIFICATION_EXCHANGE, true, false);
	}

	// Queue for normal report actions.
	@Bean
	public Queue reportActionQueue() {
		return QueueBuilder.durable(REPORT_ACTION_QUEUE).build();
	}

	// Queue for escalated reports.
	@Bean
	public Queue reportEscalationQueue() {
		return QueueBuilder.durable(REPORT_ESCALATION_QUEUE).build();
	}

	// Binds report action queue to exchange.
	@Bean
	public Binding reportActionBinding() {
		return BindingBuilder.bind(reportActionQueue()).to(notificationExchange()).with(REPORT_ACTION_RK);
	}

	// Binds escalation queue to exchange.
	@Bean
	public Binding reportEscalationBinding() {
		return BindingBuilder.bind(reportEscalationQueue()).to(notificationExchange()).with(REPORT_ESCALATION_RK);
	}

	// Converts messages to JSON format.
	@Bean
	public Jackson2JsonMessageConverter messageConverter(ObjectMapper objectMapper) {
		return new Jackson2JsonMessageConverter(objectMapper);
	}

	// RabbitTemplate for sending messages.
	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(converter);
		return template;
	}
}
