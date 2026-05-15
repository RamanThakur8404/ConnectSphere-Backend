package com.connectsphere.message_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.connectsphere.message_service.dto.ApiResponse;
import com.connectsphere.message_service.dto.ConversationResponseDTO;
import com.connectsphere.message_service.dto.MessageRequestDTO;
import com.connectsphere.message_service.dto.MessageResponseDTO;
import com.connectsphere.message_service.exception.UnauthorizedMessageAccessException;
import com.connectsphere.message_service.service.MessageService;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private MessageController messageController;

    @Test
    void getConversationsResolvesUserFromGatewayAttribute() {
        ConversationResponseDTO conversation = ConversationResponseDTO.builder()
                .conversationId(1L)
                .otherUserId(2L)
                .lastMessageContent("hello")
                .lastMessageAt(LocalDateTime.of(2026, 5, 10, 12, 0))
                .unreadCount(1L)
                .build();

        when(request.getAttribute("X-User-Id")).thenReturn(1L);
        when(messageService.getConversations(1L)).thenReturn(List.of(conversation));

        ResponseEntity<ApiResponse<List<ConversationResponseDTO>>> response = messageController.getConversations(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Conversations retrieved successfully", response.getBody().getMessage());
        assertEquals(List.of(conversation), response.getBody().getData());
        verify(messageService).getConversations(1L);
    }

    @Test
    void getOrCreateConversationDelegatesWithCurrentAndOtherUserIds() {
        ConversationResponseDTO conversation = ConversationResponseDTO.builder()
                .conversationId(2L)
                .otherUserId(8L)
                .build();

        when(request.getAttribute("X-User-Id")).thenReturn(4L);
        when(messageService.getOrCreateConversation(4L, 8L)).thenReturn(conversation);

        ResponseEntity<ApiResponse<ConversationResponseDTO>> response =
                messageController.getOrCreateConversation(8L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(conversation, response.getBody().getData());
        verify(messageService).getOrCreateConversation(4L, 8L);
    }

    @Test
    void getMessagesPassesPaginationToService() {
        MessageResponseDTO message = MessageResponseDTO.builder()
                .messageId(3L)
                .conversationId(9L)
                .senderId(4L)
                .content("payload")
                .isRead(false)
                .build();

        when(request.getAttribute("X-User-Id")).thenReturn(4L);
        when(messageService.getMessages(9L, 4L, 1, 25)).thenReturn(List.of(message));

        ResponseEntity<ApiResponse<List<MessageResponseDTO>>> response =
                messageController.getMessages(9L, 1, 25, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(List.of(message), response.getBody().getData());
        verify(messageService).getMessages(9L, 4L, 1, 25);
    }

    @Test
    void sendMessageReturnsCreatedResponse() {
        MessageRequestDTO requestDTO = MessageRequestDTO.builder().content("hello").build();
        MessageResponseDTO message = MessageResponseDTO.builder()
                .messageId(10L)
                .conversationId(5L)
                .senderId(7L)
                .content("hello")
                .isRead(false)
                .build();

        when(request.getAttribute("X-User-Id")).thenReturn(7L);
        when(messageService.sendMessage(5L, 7L, requestDTO)).thenReturn(message);

        ResponseEntity<ApiResponse<MessageResponseDTO>> response =
                messageController.sendMessage(5L, requestDTO, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Message sent successfully", response.getBody().getMessage());
        assertEquals(message, response.getBody().getData());
        verify(messageService).sendMessage(5L, 7L, requestDTO);
    }

    @Test
    void updateMessageDelegatesToService() {
        MessageRequestDTO requestDTO = MessageRequestDTO.builder().content("updated").build();
        MessageResponseDTO message = MessageResponseDTO.builder()
                .messageId(15L)
                .conversationId(5L)
                .senderId(7L)
                .content("updated")
                .isRead(false)
                .build();

        when(request.getAttribute("X-User-Id")).thenReturn(7L);
        when(messageService.updateMessage(15L, 7L, requestDTO)).thenReturn(message);

        ResponseEntity<ApiResponse<MessageResponseDTO>> response =
                messageController.updateMessage(15L, requestDTO, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Message updated successfully", response.getBody().getMessage());
        assertEquals(message, response.getBody().getData());
        verify(messageService).updateMessage(15L, 7L, requestDTO);
    }

    @Test
    void deleteMessageDelegatesToService() {
        MessageResponseDTO message = MessageResponseDTO.builder()
                .messageId(16L)
                .conversationId(5L)
                .senderId(7L)
                .content("This message was deleted")
                .isRead(false)
                .isDeleted(true)
                .build();

        when(request.getAttribute("X-User-Id")).thenReturn(7L);
        when(messageService.deleteMessage(16L, 7L)).thenReturn(message);

        ResponseEntity<ApiResponse<MessageResponseDTO>> response =
                messageController.deleteMessage(16L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Message deleted successfully", response.getBody().getMessage());
        assertEquals(message, response.getBody().getData());
        verify(messageService).deleteMessage(16L, 7L);
    }

    @Test
    void markAsReadDelegatesToService() {
        when(request.getAttribute("X-User-Id")).thenReturn(2L);
        doNothing().when(messageService).markAsRead(12L, 2L);

        ResponseEntity<ApiResponse<Void>> response = messageController.markAsRead(12L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Conversation marked as read", response.getBody().getMessage());
        verify(messageService).markAsRead(12L, 2L);
    }

    @Test
    void getUnreadCountDelegatesToService() {
        when(request.getAttribute("X-User-Id")).thenReturn(3L);
        when(messageService.getUnreadCount(3L)).thenReturn(6L);

        ResponseEntity<ApiResponse<Long>> response = messageController.getUnreadCount(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(6L, response.getBody().getData());
        verify(messageService).getUnreadCount(3L);
    }

    @Test
    void missingGatewayUserAttributeThrowsUnauthorizedException() {
        when(request.getAttribute("X-User-Id")).thenReturn(null);

        assertThrows(UnauthorizedMessageAccessException.class, () -> messageController.getUnreadCount(request));
    }
}
