package com.connectsphere.media_service.config;

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

	// -----------------------------------------------------------------------
	// Exchange
	// -----------------------------------------------------------------------

	// Media events exchange — media-service publishes here. 
	public static final String MEDIA_EXCHANGE = "media.events";

	// -----------------------------------------------------------------------
	// Queue names
	// -----------------------------------------------------------------------

	// Consumed by search-service for media indexing. 
	public static final String QUEUE_MEDIA_UPLOADED = "media.uploaded";

	// Consumed by notification-service to alert followers of a new story. 
	public static final String QUEUE_STORY_CREATED = "story.created";

	// -----------------------------------------------------------------------
	// Routing keys
	// -----------------------------------------------------------------------

	public static final String ROUTING_MEDIA_UPLOADED = "media.uploaded";
	public static final String ROUTING_STORY_CREATED = "story.created";

	// -----------------------------------------------------------------------
	// Exchange bean
	// -----------------------------------------------------------------------

	@Bean
	public TopicExchange mediaExchange() {
		return new TopicExchange(MEDIA_EXCHANGE, true, false);
	}

	// -----------------------------------------------------------------------
	// Queue beans
	// -----------------------------------------------------------------------

	@Bean
	public Queue mediaUploadedQueue() {
		return new Queue(QUEUE_MEDIA_UPLOADED, true);
	}

	@Bean
	public Queue storyCreatedQueue() {
		return new Queue(QUEUE_STORY_CREATED, true);
	}

	// -----------------------------------------------------------------------
	// Bindings
	// -----------------------------------------------------------------------

	@Bean
	public Binding bindMediaUploaded(Queue mediaUploadedQueue, TopicExchange mediaExchange) {
		return BindingBuilder.bind(mediaUploadedQueue).to(mediaExchange).with(ROUTING_MEDIA_UPLOADED);
	}

	@Bean
	public Binding bindStoryCreated(Queue storyCreatedQueue, TopicExchange mediaExchange) {
		return BindingBuilder.bind(storyCreatedQueue).to(mediaExchange).with(ROUTING_STORY_CREATED);
	}

	// -----------------------------------------------------------------------
	// Message converter — always use JSON, never Java serialization
	// -----------------------------------------------------------------------

	// Jackson converter for all AMQP messages. Mirrors notification-service
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