package com.connectsphere.notification_service.mapper;

import com.connectsphere.notification_service.dto.CreateRequest;
import com.connectsphere.notification_service.dto.ResponseDTO;
import com.connectsphere.notification_service.dto.SummaryDTO;
import com.connectsphere.notification_service.entity.Notification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class NotificationMapper {

    //  Entity → ResponseDTO ─────────────────────────────────────

    public ResponseDTO toResponse(Notification entity) {
        if (entity == null) return null;

        return ResponseDTO.builder()
                .notificationId(entity.getNotificationId())
                .recipientId(entity.getRecipientId())
                .actorId(entity.getActorId())
                .type(entity.getType())
                .message(entity.getMessage())
                .targetId(entity.getTargetId())
                .targetType(entity.getTargetType())
                .deepLinkUrl(entity.getDeepLinkUrl())
                .isRead(entity.isRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public List<ResponseDTO> toResponseList(List<Notification> entities) {
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    //  Entity → SummaryDTO ─────────────────────────────────────

    public SummaryDTO toSummary(Notification entity) {
        if (entity == null) return null;

        return SummaryDTO.builder()
                .notificationId(entity.getNotificationId())
                .type(entity.getType())
                .message(entity.getMessage())
                .isRead(entity.isRead())
                .createdAt(entity.getCreatedAt())
                .deepLinkUrl(entity.getDeepLinkUrl())
                .build();
    }

    public List<SummaryDTO> toSummaryList(List<Notification> entities) {
        return entities.stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    //  CreateRequest → Entity ─────────────────────────────────

    public Notification toEntity(CreateRequest request) {
        if (request == null) return null;

        Notification entity = new Notification();
        entity.setRecipientId(request.getRecipientId());
        entity.setActorId(request.getActorId());
        entity.setType(request.getType());
        entity.setMessage(request.getMessage());
        entity.setTargetId(request.getTargetId());
        entity.setTargetType(request.getTargetType());
        entity.setDeepLinkUrl(request.getDeepLinkUrl());
        entity.setRead(false); // default

        return entity;
    }
}
