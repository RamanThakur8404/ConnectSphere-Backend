package com.connectsphere.message_service.service;

import java.util.List;

import com.connectsphere.message_service.dto.ConversationResponseDTO;
import com.connectsphere.message_service.dto.MessageRequestDTO;
import com.connectsphere.message_service.dto.MessageResponseDTO;

public interface MessageService {

    List<ConversationResponseDTO> getConversations(Long userId);

    ConversationResponseDTO getOrCreateConversation(Long userId, Long otherUserId);

    List<MessageResponseDTO> getMessages(Long conversationId, Long userId, int page, int size);

    MessageResponseDTO sendMessage(Long conversationId, Long senderId, MessageRequestDTO request);

    MessageResponseDTO updateMessage(Long messageId, Long userId, MessageRequestDTO request);

    MessageResponseDTO deleteMessage(Long messageId, Long userId);

    void markAsRead(Long conversationId, Long userId);

    long getUnreadCount(Long userId);
}
