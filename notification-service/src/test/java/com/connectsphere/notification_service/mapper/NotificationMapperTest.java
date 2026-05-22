package com.connectsphere.notification_service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.connectsphere.notification_service.constants.NotificationTarget;
import com.connectsphere.notification_service.constants.NotificationType;
import com.connectsphere.notification_service.dto.CreateRequest;
import com.connectsphere.notification_service.dto.ResponseDTO;
import com.connectsphere.notification_service.dto.SummaryDTO;
import com.connectsphere.notification_service.entity.Notification;

@DisplayName("NotificationMapper Tests")
class NotificationMapperTest {

    private NotificationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new NotificationMapper();
    }

    @Test
    @DisplayName("toResponse maps entity to DTO")
    void toResponse() {
        Notification n = new Notification();
        n.setNotificationId(1);
        n.setRecipientId(100);
        n.setActorId(200);
        n.setType(NotificationType.LIKE);
        n.setMessage("Liked your post");
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());

        ResponseDTO dto = mapper.toResponse(n);

        assertThat(dto.getNotificationId()).isEqualTo(1);
        assertThat(dto.getRecipientId()).isEqualTo(100);
        assertThat(dto.getActorId()).isEqualTo(200);
        assertThat(dto.getMessage()).isEqualTo("Liked your post");
        assertThat(dto.isRead()).isFalse();
    }

    @Test
    @DisplayName("toResponse returns null for null input")
    void toResponse_null() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    @DisplayName("toResponseList maps list")
    void toResponseList() {
        Notification n1 = new Notification();
        n1.setNotificationId(1);
        Notification n2 = new Notification();
        n2.setNotificationId(2);

        List<ResponseDTO> result = mapper.toResponseList(List.of(n1, n2));
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("toSummary maps entity to SummaryDTO")
    void toSummary() {
        Notification n = new Notification();
        n.setNotificationId(1);
        n.setMessage("Test");
        n.setRead(true);
        n.setCreatedAt(LocalDateTime.now());

        SummaryDTO dto = mapper.toSummary(n);
        assertThat(dto.getNotificationId()).isEqualTo(1);
        assertThat(dto.getMessage()).isEqualTo("Test");
        assertThat(dto.isRead()).isTrue();
    }

    @Test
    @DisplayName("toSummary returns null for null")
    void toSummary_null() {
        assertThat(mapper.toSummary(null)).isNull();
    }

    @Test
    @DisplayName("toSummaryList maps list")
    void toSummaryList() {
        Notification n = new Notification();
        n.setNotificationId(1);
        List<SummaryDTO> result = mapper.toSummaryList(List.of(n));
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("toEntity maps CreateRequest to entity")
    void toEntity() {
        CreateRequest req = new CreateRequest();
        req.setRecipientId(100);
        req.setActorId(200);
        req.setType(NotificationType.COMMENT);
        req.setMessage("Commented");

        Notification entity = mapper.toEntity(req);

        assertThat(entity.getRecipientId()).isEqualTo(100);
        assertThat(entity.getActorId()).isEqualTo(200);
        assertThat(entity.getType()).isEqualTo(NotificationType.COMMENT);
        assertThat(entity.isRead()).isFalse();
    }

    @Test
    @DisplayName("toEntity returns null for null")
    void toEntity_null() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}
