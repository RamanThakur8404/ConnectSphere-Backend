package com.connectsphere.notification_service.config;

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

// RabbitMQ configuration for the Notification Service.

@Configuration
public class RabbitMQConfig {

    // -----------------------------------------------------------------------
    // Exchanges
    // -----------------------------------------------------------------------

    // Auth-service topic exchange — this service subscribes to user events. 
    public static final String AUTH_EXCHANGE  = "auth.events";

    // Notification-specific exchange for post/like/comment/follow events. 
    public static final String NOTIF_EXCHANGE = "notification.events";

    // -----------------------------------------------------------------------
    // Queue names
    // -----------------------------------------------------------------------

    // Receives user-registered events from auth-service. 
    public static final String QUEUE_USER_REGISTERED  = "auth.user.registered";

    // Receives user-deactivated events from auth-service. 
    public static final String QUEUE_USER_DEACTIVATED = "auth.user.deactivated";

    // Generic queue for in-app notification creation events (post, like, comment, follow). 
    public static final String QUEUE_NOTIF_INAPP      = "notif.inapp";

    // -----------------------------------------------------------------------
    // Routing keys
    // -----------------------------------------------------------------------

    public static final String ROUTING_USER_REGISTERED  = "user.registered";
    public static final String ROUTING_USER_DEACTIVATED = "user.deactivated";
    public static final String ROUTING_NOTIF_INAPP      = "notif.inapp";

    // -----------------------------------------------------------------------
    // Exchanges (Spring beans)
    // -----------------------------------------------------------------------

    // Auth exchange declared here so this service can bind to it. Durable = survives restart. 
    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(AUTH_EXCHANGE, true, false);
    }

    // Notification exchange for post/like/comment/follow events. 
    @Bean
    public TopicExchange notifExchange() {
        return new TopicExchange(NOTIF_EXCHANGE, true, false);
    }

    // -----------------------------------------------------------------------
    // Queues
    // -----------------------------------------------------------------------

    @Bean
    public Queue userRegisteredQueue() {
        return new Queue(QUEUE_USER_REGISTERED, true);
    }

    @Bean
    public Queue userDeactivatedQueue() {
        return new Queue(QUEUE_USER_DEACTIVATED, true);
    }

    @Bean
    public Queue notifInAppQueue() {
        return new Queue(QUEUE_NOTIF_INAPP, true);
    }

    // -----------------------------------------------------------------------
    // Bindings
    // -----------------------------------------------------------------------

    @Bean
    public Binding bindUserRegistered(Queue userRegisteredQueue, TopicExchange authExchange) {
        return BindingBuilder.bind(userRegisteredQueue)
                .to(authExchange)
                .with(ROUTING_USER_REGISTERED);
    }

    @Bean
    public Binding bindUserDeactivated(Queue userDeactivatedQueue, TopicExchange authExchange) {
        return BindingBuilder.bind(userDeactivatedQueue)
                .to(authExchange)
                .with(ROUTING_USER_DEACTIVATED);
    }

    @Bean
    public Binding bindNotifInApp(Queue notifInAppQueue, TopicExchange notifExchange) {
        return BindingBuilder.bind(notifInAppQueue)
                .to(notifExchange)
                .with(ROUTING_NOTIF_INAPP);
    }

    // -----------------------------------------------------------------------
    // Message converter — always use JSON, never Java serialization
    // -----------------------------------------------------------------------

    // Jackson converter for all AMQP messages.
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
