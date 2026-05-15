package com.connectsphere.search_service.config;

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

// RabbitMQ configuration for the Search / Hashtag Service.
@Configuration
public class RabbitMQConfig {

	// Topic exchange published by post-service for all post lifecycle events. 
	public static final String POST_EXCHANGE = "post.events";

	// Queue for post-created and post-updated events — triggers hashtag indexing.
	public static final String QUEUE_POST_INDEXED = "search.post.indexed";

	// Queue for post-deleted events — triggers hashtag index removal. 
	public static final String QUEUE_POST_REMOVED = "search.post.removed";

	// Routing key for new post creation events. 
	public static final String ROUTING_KEY_POST_CREATED = "post.created";

	// Routing key for post update events (re-index hashtags). 
	public static final String ROUTING_KEY_POST_UPDATED = "post.updated";

	// Routing key for post deletion events. 
	public static final String ROUTING_KEY_POST_DELETED = "post.deleted";

	// Declares the post events exchange. Durable = survives broker restart. Must
	@Bean
	public TopicExchange postExchange() {
		return new TopicExchange(POST_EXCHANGE, true, false);
	}

	// Queue for indexing events (post.created and post.updated).
	@Bean
	public Queue postIndexedQueue() {
		return new Queue(QUEUE_POST_INDEXED, true);
	}

	// Queue for index-removal events (post.deleted).
	@Bean
	public Queue postRemovedQueue() {
		return new Queue(QUEUE_POST_REMOVED, true);
	}

	// Binds search.post.indexed queue to post.created routing key.
	@Bean
	public Binding bindPostCreated(Queue postIndexedQueue, TopicExchange postExchange) {
		return BindingBuilder.bind(postIndexedQueue).to(postExchange).with(ROUTING_KEY_POST_CREATED);
	}

	// Binds search.post.indexed queue to post.updated routing key. Re-indexing on
	@Bean
	public Binding bindPostUpdated(Queue postIndexedQueue, TopicExchange postExchange) {
		return BindingBuilder.bind(postIndexedQueue).to(postExchange).with(ROUTING_KEY_POST_UPDATED);
	}

	// Binds search.post.removed queue to post.deleted routing key.
	@Bean
	public Binding bindPostDeleted(Queue postRemovedQueue, TopicExchange postExchange) {
		return BindingBuilder.bind(postRemovedQueue).to(postExchange).with(ROUTING_KEY_POST_DELETED);
	}

	// Uses Jackson to convert message payloads to/from JSON. Must match the
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
