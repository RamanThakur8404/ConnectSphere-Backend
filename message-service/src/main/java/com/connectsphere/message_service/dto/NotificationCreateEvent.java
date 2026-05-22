package com.connectsphere.message_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCreateEvent {
    private Integer recipientId;
    private Integer actorId;
    private String type;
    private String message;
    private Integer targetId;
    private String targetType;
    private String deepLinkUrl;
}
