package com.connectsphere.message_service.dto;

import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponseDTO {
    private Long messageId;
    private Long conversationId;
    private Long senderId;
    private String content;
    private Boolean isRead;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;
    private LocalDateTime deletedAt;
}
