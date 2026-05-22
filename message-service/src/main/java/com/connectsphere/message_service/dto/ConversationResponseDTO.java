package com.connectsphere.message_service.dto;

import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponseDTO {
    private Long conversationId;
    private Long otherUserId;
    private String lastMessageContent;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
}
