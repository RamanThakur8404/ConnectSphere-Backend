package com.connectsphere.post_service.config;

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

// RabbitMQ configuration for post-service.
@Configuration
public class RabbitMQConfig {

    // Exchange
    public static final String POST_EXCHANGE = "post.exchange";

    // Routing keys
    public static final String POST_CREATED_ROUTING_KEY = "post.created";
    public static final String POST_UPDATED_ROUTING_KEY = "post.updated";
    public static final String POST_DELETED_ROUTING_KEY = "post.deleted";

    // Queue names
    public static final String POST_CREATED_QUEUE = "post.created.queue";
    public static final String POST_UPDATED_QUEUE = "post.updated.queue";
    public static final String POST_DELETED_QUEUE = "post.deleted.queue";

    @Bean
    public TopicExchange postExchange() {
        return new TopicExchange(POST_EXCHANGE, true, false);
    }

    @Bean
    public Queue postCreatedQueue() {
        return new Queue(POST_CREATED_QUEUE, true);
    }

    @Bean
    public Queue postUpdatedQueue() {
        return new Queue(POST_UPDATED_QUEUE, true);
    }

    @Bean
    public Queue postDeletedQueue() {
        return new Queue(POST_DELETED_QUEUE, true);
    }

    @Bean
    public Binding postCreatedBinding(Queue postCreatedQueue, TopicExchange postExchange) {
        return BindingBuilder.bind(postCreatedQueue).to(postExchange).with(POST_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding postUpdatedBinding(Queue postUpdatedQueue, TopicExchange postExchange) {
        return BindingBuilder.bind(postUpdatedQueue).to(postExchange).with(POST_UPDATED_ROUTING_KEY);
    }

    @Bean
    public Binding postDeletedBinding(Queue postDeletedQueue, TopicExchange postExchange) {
        return BindingBuilder.bind(postDeletedQueue).to(postExchange).with(POST_DELETED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
