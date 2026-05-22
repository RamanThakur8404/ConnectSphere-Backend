package com.connectsphere.message_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.connectsphere.message_service.config.RabbitMQConfig;
import com.connectsphere.message_service.dto.NotificationCreateEvent;
import com.connectsphere.message_service.entity.Message;

@ExtendWith(MockitoExtension.class)
class MessageNotificationPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private MessageNotificationPublisher publisher;

    @Test
    void publishNewMessageNotificationSendsMessageEventWithMessageTargetType() {
        Message message = Message.builder()
                .messageId(10L)
                .conversationId(2L)
                .senderId(19L)
                .content("hello")
                .build();

        publisher.publishNewMessageNotification(message, 8L);

        ArgumentCaptor<NotificationCreateEvent> eventCaptor = ArgumentCaptor.forClass(NotificationCreateEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.NOTIF_EXCHANGE),
                eq(RabbitMQConfig.ROUTING_NOTIF_INAPP),
                eventCaptor.capture());

        NotificationCreateEvent event = eventCaptor.getValue();
        assertEquals(8, event.getRecipientId());
        assertEquals(19, event.getActorId());
        assertEquals("MESSAGE", event.getType());
        assertEquals(2, event.getTargetId());
        assertEquals("MESSAGE", event.getTargetType());
        assertEquals("/messages", event.getDeepLinkUrl());
    }

    @Test
    void publishNewMessageNotificationSkipsSelfNotifications() {
        Message message = Message.builder()
                .messageId(11L)
                .conversationId(2L)
                .senderId(19L)
                .content("hello")
                .build();

        publisher.publishNewMessageNotification(message, 19L);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void publishNewMessageNotificationSkipsMissingMessageOrRecipient() {
        Message message = Message.builder()
                .messageId(12L)
                .conversationId(2L)
                .senderId(19L)
                .content("hello")
                .build();

        publisher.publishNewMessageNotification(null, 8L);
        publisher.publishNewMessageNotification(message, null);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void publishNewMessageNotificationSwallowsBrokerFailures() {
        Message message = Message.builder()
                .messageId(13L)
                .conversationId(2L)
                .senderId(19L)
                .content("hello")
                .build();
        doThrow(new RuntimeException("broker down"))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        publisher.publishNewMessageNotification(message, 8L);

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
