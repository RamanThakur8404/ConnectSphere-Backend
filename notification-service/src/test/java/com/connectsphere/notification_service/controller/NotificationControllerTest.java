package com.connectsphere.notification_service.controller;

import com.connectsphere.notification_service.constants.NotificationType;
import com.connectsphere.notification_service.dto.*;
import com.connectsphere.notification_service.exception.NotificationNotFoundException;
import com.connectsphere.notification_service.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// REST layer tests for {@link NotificationController}.
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController REST Layer Tests")
class NotificationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // for LocalDateTime serialization
    }

    // ===================================================================
    // POST /api/v1/notifications — create
    // ===================================================================

    @Nested
    @DisplayName("POST /api/v1/notifications")
    class CreateTests {

        @Test
        @DisplayName("create_ValidRequest_Returns201WithSuccessTrue")
        void create_ValidRequest_Returns201WithSuccessTrue() throws Exception {
            CreateRequest request = new CreateRequest();
            request.setRecipientId(101);
            request.setActorId(202);
            request.setType(NotificationType.LIKE);
            request.setMessage("Alice liked your post.");

            when(notificationService.createNotification(any(CreateRequest.class))).thenReturn(null);

            mockMvc.perform(post("/api/v1/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Notification created"));

            verify(notificationService, times(1)).createNotification(any(CreateRequest.class));
        }

        @Test
        @DisplayName("create_MissingRequiredFields_Returns400")
        void create_MissingRequiredFields_Returns400() throws Exception {
            // Empty request — all @NotNull / @NotBlank fields missing
            CreateRequest request = new CreateRequest();

            mockMvc.perform(post("/api/v1/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ===================================================================
    // POST /api/v1/notifications/bulk
    // ===================================================================

    @Nested
    @DisplayName("POST /api/v1/notifications/bulk")
    class BulkTests {

        @Test
        @DisplayName("sendBulk_ValidRequest_Returns200WithSuccessTrue")
        void sendBulk_ValidRequest_Returns200WithSuccessTrue() throws Exception {
            BulkRequest request = new BulkRequest();
            request.setRecipientIds(Arrays.asList(1, 2, 3));
            request.setActorId(303);
            request.setType(NotificationType.BROADCAST);
            request.setMessage("Bulk update");

            when(notificationService.sendBulkNotification(anyList(), anyInt(), any(NotificationType.class), anyString()))
                    .thenReturn(3);

            mockMvc.perform(post("/api/v1/notifications/bulk")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Bulk notifications sent"))
                    .andExpect(jsonPath("$.data").value(3));

            verify(notificationService, times(1))
                    .sendBulkNotification(anyList(), anyInt(), any(NotificationType.class), anyString());
        }
    }

    // ===================================================================
    // POST /api/v1/notifications/email
    // ===================================================================

    @Nested
    @DisplayName("POST /api/v1/notifications/email")
    class EmailTests {

        @Test
        @DisplayName("sendEmail_ValidRequest_Returns200WithSuccessTrue")
        void sendEmail_ValidRequest_Returns200WithSuccessTrue() throws Exception {
            EmailAlertRequest request = new EmailAlertRequest();
            request.setToEmail("user@test.com");
            request.setSubject("Welcome");
            request.setBody("Welcome to ConnectSphere!");

            doNothing().when(notificationService).sendEmailAlert(anyString(), anyString(), anyString());

            mockMvc.perform(post("/api/v1/notifications/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(notificationService, times(1)).sendEmailAlert(anyString(), anyString(), anyString());
        }
    }

    // ===================================================================
    // GET /api/v1/notifications/recipient/{recipientId}
    // ===================================================================

    @Nested
    @DisplayName("GET /api/v1/notifications/recipient/{recipientId}")
    class GetByRecipientTests {

        @Test
        @DisplayName("getByRecipient_ValidRecipient_Returns200WithData")
        void getByRecipient_ValidRecipient_Returns200WithData() throws Exception {
            ResponseDTO dto = ResponseDTO.builder()
                    .notificationId(1)
                    .recipientId(101)
                    .message("Alice liked your post.")
                    .build();

            when(notificationService.getByRecipient(101)).thenReturn(List.of(dto));

            mockMvc.perform(get("/api/v1/notifications/recipient/101")
                    .header("X-User-Id", "101"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].notificationId").value(1))
                    .andExpect(jsonPath("$.data[0].message").value("Alice liked your post."));

            verify(notificationService).getByRecipient(101);
        }

        @Test
        @DisplayName("getByRecipient_EmptyResult_Returns200WithEmptyArray")
        void getByRecipient_EmptyResult_Returns200WithEmptyArray() throws Exception {
            when(notificationService.getByRecipient(999)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/notifications/recipient/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // ===================================================================
    // GET /api/v1/notifications/recipient/{recipientId}/paged
    // ===================================================================

    @Nested
    @DisplayName("GET /api/v1/notifications/recipient/{recipientId}/paged")
    class GetPagedTests {

        @Test
        @DisplayName("getByRecipientPaged_DefaultParams_Returns200")
        void getByRecipientPaged_DefaultParams_Returns200() throws Exception {
            SummaryDTO dto = new SummaryDTO();
            dto.setNotificationId(1);

            when(notificationService.getByRecipientPaged(anyInt(), anyInt(), anyInt()))
                    .thenReturn(List.of(dto));

            mockMvc.perform(get("/api/v1/notifications/recipient/101/paged")
                    .param("page", "0")
                    .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].notificationId").value(1));
        }
    }

    // ===================================================================
    // GET /api/v1/notifications/recipient/{recipientId}/unread
    // ===================================================================

    @Nested
    @DisplayName("GET /api/v1/notifications/recipient/{recipientId}/unread")
    class GetUnreadTests {

        @Test
        @DisplayName("getUnread_HasUnread_Returns200WithList")
        void getUnread_HasUnread_Returns200WithList() throws Exception {
            ResponseDTO dto = ResponseDTO.builder().notificationId(2).build();
            when(notificationService.getUnreadByRecipient(101)).thenReturn(List.of(dto));

            mockMvc.perform(get("/api/v1/notifications/recipient/101?unreadOnly=true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].notificationId").value(2));
        }
    }

    // ===================================================================
    // GET /api/v1/notifications/recipient/{recipientId}/unread-count
    // ===================================================================

    @Nested
    @DisplayName("GET /api/v1/notifications/recipient/{recipientId}/unread-count")
    class GetUnreadCountTests {

        @Test
        @DisplayName("getUnreadCount_ValidRecipient_Returns200WithCount")
        void getUnreadCount_ValidRecipient_Returns200WithCount() throws Exception {
            when(notificationService.getUnreadCount(101)).thenReturn(5);

            mockMvc.perform(get("/api/v1/notifications/recipient/101/unread-count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(5));
        }
    }

    // ===================================================================
    // GET /api/v1/notifications/all
    // ===================================================================

    @Nested
    @DisplayName("GET /api/v1/notifications/all")
    class GetAllTests {

        @Test
        @DisplayName("getAll_AdminRequest_Returns200WithAllNotifications")
        void getAll_AdminRequest_Returns200WithAllNotifications() throws Exception {
            when(notificationService.getAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/notifications/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // ===================================================================
    // PUT /api/v1/notifications/{notificationId}/read
    // ===================================================================

    @Nested
    @DisplayName("PUT /api/v1/notifications/{notificationId}/read")
    class MarkAsReadTests {

        @Test
        @DisplayName("markAsRead_ExistingNotification_Returns200")
        void markAsRead_ExistingNotification_Returns200() throws Exception {
            doNothing().when(notificationService).markAsRead(1);

            mockMvc.perform(put("/api/v1/notifications/1/read"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Marked as read"));

            verify(notificationService).markAsRead(1);
        }
    }

    // ===================================================================
    // PUT /api/v1/notifications/recipient/{recipientId}/read-all
    // ===================================================================

    @Nested
    @DisplayName("PUT /api/v1/notifications/recipient/{recipientId}/read-all")
    class MarkAllReadTests {

        @Test
        @DisplayName("markAllRead_ValidRecipient_Returns200WithCount")
        void markAllRead_ValidRecipient_Returns200WithCount() throws Exception {
            when(notificationService.markAllRead(101)).thenReturn(3);

            mockMvc.perform(put("/api/v1/notifications/recipient/101/read-all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(3));
        }
    }

    // ===================================================================
    // DELETE /api/v1/notifications/{notificationId}
    // ===================================================================

    @Nested
    @DisplayName("DELETE /api/v1/notifications/{notificationId}")
    class DeleteTests {

        @Test
        @DisplayName("delete_ExistingNotification_Returns200")
        void delete_ExistingNotification_Returns200() throws Exception {
            doNothing().when(notificationService).deleteNotification(1);

            mockMvc.perform(delete("/api/v1/notifications/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Deleted successfully"));

            verify(notificationService).deleteNotification(1);
        }

        @Test
        @DisplayName("delete_NotFound_ServiceThrowsNotificationNotFoundException")
        void delete_NotFound_ServiceThrowsNotificationNotFoundException() throws Exception {
            doThrow(new NotificationNotFoundException(999))
                    .when(notificationService).deleteNotification(999);

            // Exception propagates — standalone MockMvc returns 404 because of @ResponseStatus on exception
            mockMvc.perform(delete("/api/v1/notifications/999"))
                    .andExpect(status().isNotFound());
        }
    }
}
