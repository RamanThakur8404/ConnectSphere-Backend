package com.connectsphere.notification_service.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.connectsphere.notification_service.dto.CreateRequest;
import com.connectsphere.notification_service.event.UserRegisteredEvent;
import com.connectsphere.notification_service.service.NotificationService;
import com.rabbitmq.client.Channel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    private static final long DELIVERY_TAG = 42L;

    @Mock
    private NotificationService notificationService;

    @Mock
    private Channel channel;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    void onUserRegistered_SendsWelcomeNotificationAndAcknowledgesMessage() throws Exception {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(11L)
                .email("newuser@example.com")
                .username("new_user")
                .fullName("New User")
                .build();

        listener.onUserRegistered(event, channel, DELIVERY_TAG);

        verify(notificationService).createNotification(argThat(request ->
                request.getRecipientId() == 11
                        && request.getActorId() == 0
                        && request.getMessage().contains("new_user")));
        verify(notificationService).sendEmailAlert(
                eq("newuser@example.com"),
                eq("Welcome to ConnectSphere!"),
                contains("@new_user"));
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void onUserRegistered_WhenNotificationFails_StillAcknowledgesMessage() throws Exception {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(12L)
                .email("broken@example.com")
                .username("broken_user")
                .fullName("Broken User")
                .build();
        doThrow(new RuntimeException("DB down")).when(notificationService).createNotification(any(CreateRequest.class));

        listener.onUserRegistered(event, channel, DELIVERY_TAG);

        verify(notificationService).createNotification(any(CreateRequest.class));
        verify(notificationService, never()).sendEmailAlert(any(), any(), any());
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void onInAppNotification_CreatesNotificationAndAcknowledgesMessage() throws Exception {
        CreateRequest request = new CreateRequest();
        request.setRecipientId(21);
        request.setActorId(5);
        request.setMessage("A new follower event");

        listener.onInAppNotification(request, channel, DELIVERY_TAG);

        verify(notificationService).createNotification(request);
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void onUserDeactivated_AcknowledgesMessage() throws Exception {
        listener.onUserDeactivated("user-deactivated", channel, DELIVERY_TAG);

        verify(channel).basicAck(DELIVERY_TAG, false);
    }
}
