package com.connectsphere.message_service.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.connectsphere.message_service.config.RabbitMQConfig;
import com.connectsphere.message_service.dto.NotificationCreateEvent;
import com.connectsphere.message_service.entity.Message;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MessageNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(MessageNotificationPublisher.class);
    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${AUTH_SERVICE_URI:http://localhost:8081}")
    private String authServiceUri;

    public void publishNewMessageNotification(Message message, Long recipientId) {
        if (message == null || recipientId == null || message.getSenderId().equals(recipientId)) {
            return;
        }

        try {
            String senderName = resolveSenderName(message.getSenderId());
            NotificationCreateEvent event = NotificationCreateEvent.builder()
                    .recipientId(recipientId.intValue())
                    .actorId(message.getSenderId().intValue())
                    .type("MESSAGE")
                    .message(senderName + " sent you a message")
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

    private String resolveSenderName(Long senderId) {
        try {
            String baseUrl = authServiceUri.endsWith("/")
                    ? authServiceUri.substring(0, authServiceUri.length() - 1)
                    : authServiceUri;
            Map<?, ?> response = restTemplate.getForObject(
                    baseUrl + "/api/v1/auth/users/public/" + senderId,
                    Map.class);
            Object data = response != null && response.containsKey("data") ? response.get("data") : response;
            if (data instanceof Map<?, ?> profile) {
                String fullName = asText(profile.get("fullName"));
                if (!fullName.isBlank()) {
                    return fullName;
                }
                String username = asText(profile.get("username"));
                if (!username.isBlank()) {
                    return "@" + username;
                }
            }
        } catch (RestClientException ex) {
            log.debug("Could not resolve sender profile for userId={} - {}", senderId, ex.getMessage());
        }
        return "User " + senderId;
    }

    private String asText(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
