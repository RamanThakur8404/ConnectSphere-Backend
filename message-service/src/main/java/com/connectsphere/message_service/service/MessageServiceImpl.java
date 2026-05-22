package com.connectsphere.message_service.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);

    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageNotificationPublisher notificationPublisher;

    @Override
    public List<ConversationResponseDTO> getConversations(Long userId) {
        log.info("Fetching conversations for userId={}", userId);
        List<Conversation> conversations = conversationRepo.findByUserId(userId);
        return conversations.stream()
                .map(c -> toConversationDTO(c, userId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ConversationResponseDTO getOrCreateConversation(Long userId, Long otherUserId) {
        if (userId.equals(otherUserId)) {
            throw new IllegalArgumentException("Cannot create a conversation with yourself");
        }

        // Always store lower ID as user1 for uniqueness
        Long user1 = Math.min(userId, otherUserId);
        Long user2 = Math.max(userId, otherUserId);

        Conversation conversation = conversationRepo.findByUser1IdAndUser2Id(user1, user2)
                .orElseGet(() -> {
                    log.info("Creating new conversation between {} and {}", user1, user2);
                    return conversationRepo.save(Conversation.builder()
                            .user1Id(user1)
                            .user2Id(user2)
                            .build());
                });

        return toConversationDTO(conversation, userId);
    }

    @Override
    public List<MessageResponseDTO> getMessages(Long conversationId, Long userId, int page, int size) {
        Conversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        verifyParticipant(conversation, userId);

        Page<Message> messages = messageRepo.findByConversationIdOrderByCreatedAtDesc(
                conversationId, PageRequest.of(page, size));

        // Return in chronological order (oldest first)
        List<MessageResponseDTO> result = messages.getContent().stream()
                .map(this::toMessageDTO)
                .collect(Collectors.toList());
        java.util.Collections.reverse(result);
        return result;
    }

    @Override
    @Transactional
    public MessageResponseDTO sendMessage(Long conversationId, Long senderId, MessageRequestDTO request) {
        Conversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        verifyParticipant(conversation, senderId);

        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .content(request.getContent())
                .build();

        Message saved = messageRepo.save(message);

        // Update conversation's last message timestamp
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepo.save(conversation);

        log.info("Message sent in conversation={} by sender={}", conversationId, senderId);
        MessageResponseDTO responseDto = toMessageDTO(saved);
        
        // Broadcast to the recipient (and also sender for sync if they have multiple devices)
        Long recipientId = conversation.getUser1Id().equals(senderId) ? conversation.getUser2Id() : conversation.getUser1Id();
        messagingTemplate.convertAndSend("/topic/user/" + recipientId + "/messages", responseDto);
        messagingTemplate.convertAndSend("/topic/user/" + senderId + "/messages", responseDto);
        notificationPublisher.publishNewMessageNotification(saved, recipientId);

        return responseDto;
    }

    @Override
    @Transactional
    public MessageResponseDTO updateMessage(Long messageId, Long userId, MessageRequestDTO request) {
        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));
        Conversation conversation = conversationRepo.findById(message.getConversationId())
                .orElseThrow(() -> new ConversationNotFoundException(message.getConversationId()));

        verifyParticipant(conversation, userId);
        verifyMessageOwner(message, userId);
        if (Boolean.TRUE.equals(message.getIsDeleted())) {
            throw new IllegalArgumentException("Deleted messages cannot be edited");
        }

        message.setContent(request.getContent());
        message.setEditedAt(LocalDateTime.now());

        Message saved = messageRepo.save(message);
        MessageResponseDTO responseDto = toMessageDTO(saved);
        broadcastMessageUpdate(conversation, responseDto);
        return responseDto;
    }

    @Override
    @Transactional
    public MessageResponseDTO deleteMessage(Long messageId, Long userId) {
        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));
        Conversation conversation = conversationRepo.findById(message.getConversationId())
                .orElseThrow(() -> new ConversationNotFoundException(message.getConversationId()));

        verifyParticipant(conversation, userId);
        verifyMessageOwner(message, userId);

        message.setContent("");
        message.setIsDeleted(true);
        message.setDeletedAt(LocalDateTime.now());

        Message saved = messageRepo.save(message);
        MessageResponseDTO responseDto = toMessageDTO(saved);
        broadcastMessageUpdate(conversation, responseDto);
        return responseDto;
    }

    @Override
    @Transactional
    public void markAsRead(Long conversationId, Long userId) {
        Conversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        verifyParticipant(conversation, userId);

        int updated = messageRepo.markAsRead(conversationId, userId);
        
        // Notify the other user that their messages were read
        Long otherUserId = conversation.getUser1Id().equals(userId) ? conversation.getUser2Id() : conversation.getUser1Id();
        messagingTemplate.convertAndSend("/topic/user/" + otherUserId + "/read", conversationId);

        log.debug("Marked {} messages as read in conversation={} for user={}", updated, conversationId, userId);
    }

    @Override
    public long getUnreadCount(Long userId) {
        List<Conversation> conversations = conversationRepo.findByUserId(userId);
        if (conversations.isEmpty()) return 0;

        List<Long> conversationIds = conversations.stream()
                .map(Conversation::getConversationId)
                .collect(Collectors.toList());

        return messageRepo.countTotalUnread(conversationIds, userId);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private void verifyParticipant(Conversation conversation, Long userId) {
        if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
            throw new UnauthorizedMessageAccessException("You are not a participant in this conversation");
        }
    }

    private ConversationResponseDTO toConversationDTO(Conversation c, Long currentUserId) {
        Long otherUserId = c.getUser1Id().equals(currentUserId) ? c.getUser2Id() : c.getUser1Id();

        Message lastMsg = messageRepo.findTopByConversationIdOrderByCreatedAtDesc(c.getConversationId());
        long unread = messageRepo.countUnread(c.getConversationId(), currentUserId);

        return ConversationResponseDTO.builder()
                .conversationId(c.getConversationId())
                .otherUserId(otherUserId)
                .lastMessageContent(lastMsg != null ? displayContent(lastMsg) : null)
                .lastMessageAt(c.getLastMessageAt())
                .unreadCount(unread)
                .build();
    }

    private MessageResponseDTO toMessageDTO(Message m) {
        return MessageResponseDTO.builder()
                .messageId(m.getMessageId())
                .conversationId(m.getConversationId())
                .senderId(m.getSenderId())
                .content(displayContent(m))
                .isRead(m.getIsRead())
                .isDeleted(m.getIsDeleted())
                .createdAt(m.getCreatedAt())
                .editedAt(m.getEditedAt())
                .deletedAt(m.getDeletedAt())
                .build();
    }

    private void verifyMessageOwner(Message message, Long userId) {
        if (!message.getSenderId().equals(userId)) {
            throw new UnauthorizedMessageAccessException("You can only modify your own messages");
        }
    }

    private void broadcastMessageUpdate(Conversation conversation, MessageResponseDTO responseDto) {
        messagingTemplate.convertAndSend("/topic/user/" + conversation.getUser1Id() + "/messages/updates", responseDto);
        messagingTemplate.convertAndSend("/topic/user/" + conversation.getUser2Id() + "/messages/updates", responseDto);
    }

    private String displayContent(Message message) {
        return Boolean.TRUE.equals(message.getIsDeleted()) ? "This message was deleted" : message.getContent();
    }
}
