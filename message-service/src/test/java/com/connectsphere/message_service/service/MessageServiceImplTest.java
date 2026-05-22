package com.connectsphere.message_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.connectsphere.message_service.dto.ConversationResponseDTO;
import com.connectsphere.message_service.dto.MessageRequestDTO;
import com.connectsphere.message_service.dto.MessageResponseDTO;
import com.connectsphere.message_service.entity.Conversation;
import com.connectsphere.message_service.entity.Message;
import com.connectsphere.message_service.exception.ConversationNotFoundException;
import com.connectsphere.message_service.exception.UnauthorizedMessageAccessException;
import com.connectsphere.message_service.repository.ConversationRepository;
import com.connectsphere.message_service.repository.MessageRepository;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private ConversationRepository conversationRepo;

    @Mock
    private MessageRepository messageRepo;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MessageNotificationPublisher notificationPublisher;

    @InjectMocks
    private MessageServiceImpl messageService;

    @Test
    void getConversationsMapsLastMessageAndUnreadCount() {
        LocalDateTime lastMessageAt = LocalDateTime.of(2026, 5, 10, 14, 30);
        Conversation conversation = conversation(10L, 1L, 2L, lastMessageAt);
        Message lastMessage = message(100L, 10L, 2L, "hello", false, lastMessageAt);

        when(conversationRepo.findByUserId(1L)).thenReturn(List.of(conversation));
        when(messageRepo.findTopByConversationIdOrderByCreatedAtDesc(10L)).thenReturn(lastMessage);
        when(messageRepo.countUnread(10L, 1L)).thenReturn(3L);

        List<ConversationResponseDTO> result = messageService.getConversations(1L);

        assertEquals(1, result.size());
        ConversationResponseDTO dto = result.get(0);
        assertEquals(10L, dto.getConversationId());
        assertEquals(2L, dto.getOtherUserId());
        assertEquals("hello", dto.getLastMessageContent());
        assertEquals(lastMessageAt, dto.getLastMessageAt());
        assertEquals(3L, dto.getUnreadCount());
    }

    @Test
    void getOrCreateConversationReturnsExistingConversationWithNormalizedUserIds() {
        Conversation existing = conversation(20L, 3L, 9L, LocalDateTime.now());

        when(conversationRepo.findByUser1IdAndUser2Id(3L, 9L)).thenReturn(Optional.of(existing));
        when(messageRepo.countUnread(20L, 9L)).thenReturn(0L);

        ConversationResponseDTO result = messageService.getOrCreateConversation(9L, 3L);

        assertEquals(20L, result.getConversationId());
        assertEquals(3L, result.getOtherUserId());
        verify(conversationRepo, never()).save(any(Conversation.class));
    }

    @Test
    void getOrCreateConversationCreatesConversationWhenMissing() {
        when(conversationRepo.findByUser1IdAndUser2Id(4L, 8L)).thenReturn(Optional.empty());
        when(conversationRepo.save(any(Conversation.class)))
                .thenAnswer(invocation -> {
                    Conversation saved = invocation.getArgument(0);
                    saved.setConversationId(30L);
                    saved.setLastMessageAt(LocalDateTime.of(2026, 5, 10, 15, 0));
                    return saved;
                });
        when(messageRepo.countUnread(30L, 4L)).thenReturn(0L);

        ConversationResponseDTO result = messageService.getOrCreateConversation(4L, 8L);

        assertEquals(30L, result.getConversationId());
        assertEquals(8L, result.getOtherUserId());

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepo).save(captor.capture());
        assertEquals(4L, captor.getValue().getUser1Id());
        assertEquals(8L, captor.getValue().getUser2Id());
    }

    @Test
    void getOrCreateConversationRejectsSelfConversation() {
        assertThrows(IllegalArgumentException.class, () -> messageService.getOrCreateConversation(5L, 5L));
        verifyNoInteractions(conversationRepo, messageRepo, messagingTemplate);
    }

    @Test
    void getMessagesReturnsChronologicalOrderAfterRepositoryFetchesDescending() {
        Conversation conversation = conversation(40L, 1L, 2L, LocalDateTime.now());
        Message newest = message(2L, 40L, 2L, "newest", false, LocalDateTime.of(2026, 5, 10, 16, 0));
        Message oldest = message(1L, 40L, 1L, "oldest", true, LocalDateTime.of(2026, 5, 10, 15, 0));

        when(conversationRepo.findById(40L)).thenReturn(Optional.of(conversation));
        when(messageRepo.findByConversationIdOrderByCreatedAtDesc(40L, PageRequest.of(0, 2)))
                .thenReturn(new PageImpl<>(List.of(newest, oldest)));

        List<MessageResponseDTO> result = messageService.getMessages(40L, 1L, 0, 2);

        assertEquals(List.of(1L, 2L), result.stream().map(MessageResponseDTO::getMessageId).toList());
        assertEquals("oldest", result.get(0).getContent());
        assertEquals("newest", result.get(1).getContent());
    }

    @Test
    void getMessagesThrowsWhenConversationMissing() {
        when(conversationRepo.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ConversationNotFoundException.class, () -> messageService.getMessages(404L, 1L, 0, 10));
        verify(messageRepo, never()).findByConversationIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void getMessagesRejectsNonParticipant() {
        when(conversationRepo.findById(50L)).thenReturn(Optional.of(conversation(50L, 1L, 2L, LocalDateTime.now())));

        assertThrows(UnauthorizedMessageAccessException.class, () -> messageService.getMessages(50L, 3L, 0, 10));
        verify(messageRepo, never()).findByConversationIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void sendMessageSavesMessageUpdatesConversationAndBroadcastsToBothUsers() {
        Conversation conversation = conversation(60L, 1L, 2L, LocalDateTime.of(2026, 5, 10, 12, 0));
        Message saved = message(600L, 60L, 1L, "ping", false, LocalDateTime.of(2026, 5, 10, 17, 0));

        when(conversationRepo.findById(60L)).thenReturn(Optional.of(conversation));
        when(messageRepo.save(any(Message.class))).thenReturn(saved);

        MessageResponseDTO result = messageService.sendMessage(60L, 1L, MessageRequestDTO.builder().content("ping").build());

        assertEquals(600L, result.getMessageId());
        assertEquals("ping", result.getContent());
        assertFalse(result.getIsRead());
        verify(conversationRepo).save(conversation);
        verify(messagingTemplate).convertAndSend("/topic/user/2/messages", result);
        verify(messagingTemplate).convertAndSend("/topic/user/1/messages", result);
        verify(notificationPublisher).publishNewMessageNotification(saved, 2L);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepo).save(captor.capture());
        assertEquals(60L, captor.getValue().getConversationId());
        assertEquals(1L, captor.getValue().getSenderId());
        assertEquals("ping", captor.getValue().getContent());
    }

    @Test
    void updateMessageUpdatesOwnedMessageAndBroadcastsToParticipants() {
        Conversation conversation = conversation(61L, 1L, 2L, LocalDateTime.now());
        Message existing = message(601L, 61L, 1L, "old", false, LocalDateTime.of(2026, 5, 10, 18, 0));

        when(messageRepo.findById(601L)).thenReturn(Optional.of(existing));
        when(conversationRepo.findById(61L)).thenReturn(Optional.of(conversation));
        when(messageRepo.save(existing)).thenReturn(existing);

        MessageResponseDTO result = messageService.updateMessage(601L, 1L,
                MessageRequestDTO.builder().content("updated").build());

        assertEquals("updated", result.getContent());
        assertNotNull(result.getEditedAt());
        verify(messagingTemplate).convertAndSend("/topic/user/1/messages/updates", result);
        verify(messagingTemplate).convertAndSend("/topic/user/2/messages/updates", result);
    }

    @Test
    void updateMessageRejectsDeletedMessages() {
        Conversation conversation = conversation(62L, 1L, 2L, LocalDateTime.now());
        Message deleted = message(602L, 62L, 1L, "old", false, LocalDateTime.of(2026, 5, 10, 18, 0));
        deleted.setIsDeleted(true);

        when(messageRepo.findById(602L)).thenReturn(Optional.of(deleted));
        when(conversationRepo.findById(62L)).thenReturn(Optional.of(conversation));

        assertThrows(IllegalArgumentException.class, () -> messageService.updateMessage(602L, 1L,
                MessageRequestDTO.builder().content("updated").build()));
        verify(messageRepo, never()).save(any(Message.class));
    }

    @Test
    void updateMessageRejectsNonOwner() {
        Conversation conversation = conversation(63L, 1L, 2L, LocalDateTime.now());
        Message existing = message(603L, 63L, 1L, "old", false, LocalDateTime.of(2026, 5, 10, 18, 0));

        when(messageRepo.findById(603L)).thenReturn(Optional.of(existing));
        when(conversationRepo.findById(63L)).thenReturn(Optional.of(conversation));

        assertThrows(UnauthorizedMessageAccessException.class, () -> messageService.updateMessage(603L, 2L,
                MessageRequestDTO.builder().content("updated").build()));
        verify(messageRepo, never()).save(any(Message.class));
    }

    @Test
    void updateMessageThrowsWhenMessageMissing() {
        when(messageRepo.findById(604L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> messageService.updateMessage(604L, 1L,
                MessageRequestDTO.builder().content("updated").build()));
        verifyNoInteractions(conversationRepo);
    }

    @Test
    void deleteMessageSoftDeletesOwnedMessageAndBroadcastsToParticipants() {
        Conversation conversation = conversation(64L, 1L, 2L, LocalDateTime.now());
        Message existing = message(605L, 64L, 1L, "remove me", false, LocalDateTime.of(2026, 5, 10, 18, 0));

        when(messageRepo.findById(605L)).thenReturn(Optional.of(existing));
        when(conversationRepo.findById(64L)).thenReturn(Optional.of(conversation));
        when(messageRepo.save(existing)).thenReturn(existing);

        MessageResponseDTO result = messageService.deleteMessage(605L, 1L);

        assertEquals("This message was deleted", result.getContent());
        assertTrue(result.getIsDeleted());
        assertNotNull(result.getDeletedAt());
        verify(messagingTemplate).convertAndSend("/topic/user/1/messages/updates", result);
        verify(messagingTemplate).convertAndSend("/topic/user/2/messages/updates", result);
    }

    @Test
    void deleteMessageThrowsWhenMessageMissing() {
        when(messageRepo.findById(606L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> messageService.deleteMessage(606L, 1L));
        verifyNoInteractions(conversationRepo);
    }

    @Test
    void markAsReadMarksUnreadMessagesAndNotifiesOtherParticipant() {
        Conversation conversation = conversation(70L, 1L, 2L, LocalDateTime.now());

        when(conversationRepo.findById(70L)).thenReturn(Optional.of(conversation));
        when(messageRepo.markAsRead(70L, 2L)).thenReturn(4);

        messageService.markAsRead(70L, 2L);

        verify(messageRepo).markAsRead(70L, 2L);
        verify(messagingTemplate).convertAndSend("/topic/user/1/read", 70L);
    }

    @Test
    void markAsReadThrowsWhenConversationMissing() {
        when(conversationRepo.findById(71L)).thenReturn(Optional.empty());

        assertThrows(ConversationNotFoundException.class, () -> messageService.markAsRead(71L, 1L));
        verify(messageRepo, never()).markAsRead(any(), any());
    }

    @Test
    void getUnreadCountReturnsZeroWhenUserHasNoConversations() {
        when(conversationRepo.findByUserId(1L)).thenReturn(List.of());

        long result = messageService.getUnreadCount(1L);

        assertEquals(0L, result);
        verify(messageRepo, never()).countTotalUnread(any(), any());
    }

    @Test
    void getUnreadCountAggregatesAcrossConversationIds() {
        when(conversationRepo.findByUserId(1L)).thenReturn(List.of(
                conversation(80L, 1L, 2L, LocalDateTime.now()),
                conversation(81L, 1L, 3L, LocalDateTime.now())));
        when(messageRepo.countTotalUnread(List.of(80L, 81L), 1L)).thenReturn(7L);

        long result = messageService.getUnreadCount(1L);

        assertEquals(7L, result);
    }

    private static Conversation conversation(Long id, Long user1Id, Long user2Id, LocalDateTime lastMessageAt) {
        return Conversation.builder()
                .conversationId(id)
                .user1Id(user1Id)
                .user2Id(user2Id)
                .lastMessageAt(lastMessageAt)
                .build();
    }

    private static Message message(Long id, Long conversationId, Long senderId, String content, Boolean isRead,
            LocalDateTime createdAt) {
        return Message.builder()
                .messageId(id)
                .conversationId(conversationId)
                .senderId(senderId)
                .content(content)
                .isRead(isRead)
                .createdAt(createdAt)
                .build();
    }
}
