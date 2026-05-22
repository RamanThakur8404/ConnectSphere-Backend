package com.connectsphere.payment_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

	// Exchange for all payment-related events. 
	public static final String PAYMENT_EXCHANGE = "payment.events";

	// Queue for successful payments (used by auth-service). 
	public static final String QUEUE_PAYMENT_SUCCESS = "payment.success";

	// Queue for refunded payments (used by notification-service). 
	public static final String QUEUE_PAYMENT_REFUNDED = "payment.refunded";

	// Routing keys 
	public static final String ROUTING_KEY_PAYMENT_SUCCESS = "payment.success";
	public static final String ROUTING_KEY_PAYMENT_REFUNDED = "payment.refunded";

	// Topic exchange (durable). 
	@Bean
	public TopicExchange paymentExchange() {
		return new TopicExchange(PAYMENT_EXCHANGE, true, false);
	}

	// Queues (durable). 
	@Bean
	public Queue paymentSuccessQueue() {
		return new Queue(QUEUE_PAYMENT_SUCCESS, true);
	}

	@Bean
	public Queue paymentRefundedQueue() {
		return new Queue(QUEUE_PAYMENT_REFUNDED, true);
	}

	// Bind queues to exchange using routing keys. 
	@Bean
	public Binding bindPaymentSuccess(Queue paymentSuccessQueue, TopicExchange paymentExchange) {
		return BindingBuilder.bind(paymentSuccessQueue).to(paymentExchange).with(ROUTING_KEY_PAYMENT_SUCCESS);
	}

	@Bean
	public Binding bindPaymentRefunded(Queue paymentRefundedQueue, TopicExchange paymentExchange) {
		return BindingBuilder.bind(paymentRefundedQueue).to(paymentExchange).with(ROUTING_KEY_PAYMENT_REFUNDED);
	}

	// Use JSON instead of Java serialization. 
	@Bean
	public MessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	// RabbitTemplate with JSON converter. 
	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(jsonMessageConverter());
		return template;
	}
}