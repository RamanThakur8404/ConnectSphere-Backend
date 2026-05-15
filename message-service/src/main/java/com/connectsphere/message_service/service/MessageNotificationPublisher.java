package com.connectsphere.message_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.connectsphere.message_service.config.RabbitMQConfig;
import com.connectsphere.message_service.dto.NotificationCreateEvent;
import com.connectsphere.message_service.entity.Message;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MessageNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(MessageNotificationPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public void publishNewMessageNotification(Message message, Long recipientId) {
        if (message == null || recipientId == null || message.getSenderId().equals(recipientId)) {
            return;
        }

        try {
            NotificationCreateEvent event = NotificationCreateEvent.builder()
                    .recipientId(recipientId.intValue())
                    .actorId(message.getSenderId().intValue())
                    .type("MESSAGE")
                    .message("User " + message.getSenderId() + " sent you a message")
                    .targetId(message.getConversationId().intValue())
                    .targetType("MESSAGE")
                    .deepLinkUrl("/messages")
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIF_EXCHANGE,
                    RabbitMQConfig.ROUTING_NOTIF_INAPP,
                    event);
        } catch (Exception ex) {
            log.warn("Failed to publish message notification for messageId={} recipientId={} - {}",
                    message.getMessageId(), recipientId, ex.getMessage());
        }
    }
}
