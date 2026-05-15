package com.connectsphere.auth.config;

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

// RabbitMQ configuration for the Auth Service.
@Configuration
public class RabbitMQConfig {

	// -----------------------------------------------------------------------
	// Exchange
	// -----------------------------------------------------------------------

	// Name of the topic exchange through which all auth events are routed. 
	public static final String AUTH_EXCHANGE = "auth.events";

	// -----------------------------------------------------------------------
	// Queue names (also used as routing keys for simplicity)
	// -----------------------------------------------------------------------

	// Queue for new-user registration events. 
	public static final String QUEUE_USER_REGISTERED = "auth.user.registered";

	// Queue for account-deactivation events. 
	public static final String QUEUE_USER_DEACTIVATED = "auth.user.deactivated";

	// -----------------------------------------------------------------------
	// Routing keys
	// -----------------------------------------------------------------------

	public static final String ROUTING_KEY_USER_REGISTERED = "user.registered";
	public static final String ROUTING_KEY_USER_DEACTIVATED = "user.deactivated";
	
	// -----------------------------------------------------------------------
	// Payment Events (consumed from payment-service)
	// -----------------------------------------------------------------------
	public static final String PAYMENT_EXCHANGE = "payment.events";
	public static final String QUEUE_PAYMENT_SUCCESS = "payment.success";
	public static final String ROUTING_KEY_PAYMENT_SUCCESS = "payment.success";

	// -----------------------------------------------------------------------
	// Spring Beans — Exchange, Queues, Bindings
	// -----------------------------------------------------------------------

	// Declares a durable topic exchange. Durable = survives broker restart.
	@Bean
	public TopicExchange authExchange() {
		return new TopicExchange(AUTH_EXCHANGE, true, false);
	}

	// Declares a durable queue for user-registration events.
	@Bean
	public Queue userRegisteredQueue() {
		return new Queue(QUEUE_USER_REGISTERED, true);
	}

	// Declares a durable queue for account-deactivation events.
	@Bean
	public Queue userDeactivatedQueue() {
		return new Queue(QUEUE_USER_DEACTIVATED, true);
	}

	// Binds the registration queue to the exchange using its routing key.
	@Bean
	public Binding bindUserRegistered(Queue userRegisteredQueue, TopicExchange authExchange) {
		return BindingBuilder.bind(userRegisteredQueue).to(authExchange).with(ROUTING_KEY_USER_REGISTERED);
	}

	// Binds the deactivation queue to the exchange using its routing key.
	@Bean
	public Binding bindUserDeactivated(Queue userDeactivatedQueue, TopicExchange authExchange) {
		return BindingBuilder.bind(userDeactivatedQueue).to(authExchange).with(ROUTING_KEY_USER_DEACTIVATED);
	}

	// Declares the payment topic exchange.
	@Bean
	public TopicExchange paymentExchange() {
		return new TopicExchange(PAYMENT_EXCHANGE, true, false);
	}

	// Declares the payment success queue.
	@Bean
	public Queue paymentSuccessQueue() {
		return new Queue(QUEUE_PAYMENT_SUCCESS, true);
	}

	// Binds the payment success queue to the payment exchange.
	@Bean
	public Binding bindPaymentSuccess(Queue paymentSuccessQueue, TopicExchange paymentExchange) {
		return BindingBuilder.bind(paymentSuccessQueue).to(paymentExchange).with(ROUTING_KEY_PAYMENT_SUCCESS);
	}

	// -----------------------------------------------------------------------
	// Message converter — serialize/deserialize as JSON
	// -----------------------------------------------------------------------

	// Uses Jackson to convert message payloads to/from JSON. Without this, Spring
	@Bean
	public MessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	// Configures RabbitTemplate to use the JSON converter.
	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(jsonMessageConverter());
		return template;
	}
}
