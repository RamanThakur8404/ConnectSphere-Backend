package com.connectsphere.notification_service.service.impl;

import com.connectsphere.notification_service.service.NotificationService;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.mail.internet.MimeMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;

@SpringBootTest
@ActiveProfiles("test")
public class NotificationEmailIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("test", "test"))
            .withPerMethodLifecycle(false);

    @Autowired
    private NotificationService notificationService;

    @Test
    @DisplayName("sendEmailAlert_ValidInput_EmailSentSuccessfully")
    void testEmailSendingIntegration() throws Exception {
        String testEmail = "testuser@example.com";
        String testSubject = "Integration Test Alert";
        String testBody = "This is a test body.";

        notificationService.sendEmailAlert(testEmail, testSubject, testBody);

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertEquals(1, receivedMessages.length);

        MimeMessage current = receivedMessages[0];
        assertEquals(testSubject, current.getSubject());
        assertEquals(testEmail, current.getAllRecipients()[0].toString());
    }
}
