package com.connectsphere.notification_service.listener;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.connectsphere.notification_service.config.RabbitMQConfig;
import com.connectsphere.notification_service.constants.NotificationType;
import com.connectsphere.notification_service.dto.CreateRequest;
import com.connectsphere.notification_service.event.UserRegisteredEvent;
import com.connectsphere.notification_service.service.NotificationService;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;

// RabbitMQ event listener for the Notification Service.
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    // Handles user-registered events from auth-service.
    @RabbitListener(queues = RabbitMQConfig.QUEUE_USER_REGISTERED)
    public void onUserRegistered(UserRegisteredEvent event, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("Received UserRegisteredEvent - userId: {}, email: {}",
                event.getUserId(), event.getEmail());

        try {
            CreateRequest request = new CreateRequest();
            request.setRecipientId(event.getUserId().intValue());
            request.setActorId(0);
            request.setType(NotificationType.MENTION);
            request.setMessage("Welcome to ConnectSphere, " + event.getUsername() + "!");

            notificationService.createNotification(request);
            notificationService.sendEmailAlert(event.getEmail(), "Welcome to ConnectSphere!",
                    buildWelcomeEmailBody(event.getFullName(), event.getUsername()));

            log.info("Welcome notification + email sent for userId: {}", event.getUserId());
        } catch (Exception ex) {
            // Non-critical: log and acknowledge so old messages do not replay forever.
            log.error("Failed to handle UserRegisteredEvent for userId: {} - {}",
                    event.getUserId(), ex.getMessage(), ex);
        } finally {
            acknowledgeMessage(channel, deliveryTag, "UserRegisteredEvent");
        }
    }

    // Handles user-deactivated events from auth-service.
    @RabbitListener(queues = RabbitMQConfig.QUEUE_USER_DEACTIVATED)
    public void onUserDeactivated(Object rawEvent, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("Received UserDeactivatedEvent: {}", rawEvent);
            // Future: send account-closure email if email is present in event.
        } finally {
            acknowledgeMessage(channel, deliveryTag, "UserDeactivatedEvent");
        }
    }

    // Handles generic in-app notification creation events from other services.
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIF_INAPP)
    public void onInAppNotification(CreateRequest request, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("Received in-app notification event - recipientId: {}, type: {}",
                request.getRecipientId(), request.getType());

        try {
            notificationService.createNotification(request);
        } catch (Exception ex) {
            log.error("Failed to create in-app notification from event - {}",
                    ex.getMessage(), ex);
        } finally {
            acknowledgeMessage(channel, deliveryTag, "InAppNotification");
        }
    }

    private String buildWelcomeEmailBody(String fullName, String username) {
        String name = (fullName != null && !fullName.isBlank()) ? fullName : username;
        String safeName = escapeHtml(name);
        String safeUsername = escapeHtml(username);
        String safeLoginUrl = escapeHtml(frontendBaseUrl + "/login");

        return """
                <!DOCTYPE html>
                <html>
                  <body style="margin:0;background:#eef3f8;font-family:Arial,Helvetica,sans-serif;color:#172033;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#eef3f8;padding:34px 12px;">
                      <tr>
                        <td align="center">
                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:640px;background:#ffffff;border:1px solid #dbe5f1;border-radius:22px;overflow:hidden;box-shadow:0 18px 50px rgba(15,23,42,.12);">
                            <tr>
                              <td style="background:#0f172a;padding:34px 34px 28px;color:#ffffff;">
                                <div style="display:inline-block;padding:7px 12px;border:1px solid rgba(255,255,255,.22);border-radius:999px;color:#bfdbfe;font-size:12px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;">ConnectSphere</div>
                                <h1 style="margin:22px 0 10px;font-size:30px;line-height:1.2;">Welcome, %s</h1>
                                <p style="margin:0;color:#cbd5e1;font-size:16px;line-height:1.6;">Your account is ready. Start sharing moments, finding people, and building your circle.</p>
                              </td>
                            </tr>
                            <tr>
                              <td style="padding:32px 34px;">
                                <p style="margin:0 0 18px;font-size:16px;line-height:1.65;color:#334155;">Hi %s, your ConnectSphere profile has been created successfully.</p>
                                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin:22px 0;background:#f8fafc;border:1px solid #e2e8f0;border-radius:14px;">
                                  <tr>
                                    <td style="padding:18px 20px;color:#64748b;font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:.05em;">Username</td>
                                    <td align="right" style="padding:18px 20px;color:#0f172a;font-size:18px;font-weight:700;">@%s</td>
                                  </tr>
                                </table>
                                <a href="%s" style="display:inline-block;background:#2563eb;color:#ffffff;text-decoration:none;font-weight:700;font-size:15px;padding:14px 22px;border-radius:12px;">Log in to ConnectSphere</a>
                                <p style="margin:22px 0 0;font-size:14px;line-height:1.6;color:#64748b;">If the button does not work, open ConnectSphere and log in with the email address you used during registration.</p>
                              </td>
                            </tr>
                            <tr>
                              <td style="padding:20px 34px;background:#f8fafc;color:#64748b;font-size:13px;line-height:1.5;">
                                Thanks for joining us,<br>The ConnectSphere Team
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </body>
                </html>
                """.formatted(safeName, safeName, safeUsername, safeLoginUrl);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void acknowledgeMessage(Channel channel, long deliveryTag, String eventName) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException ex) {
            log.error("Failed to acknowledge {} message - {}", eventName, ex.getMessage(), ex);
        }
    }
}
