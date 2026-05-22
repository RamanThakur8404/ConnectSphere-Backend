package com.connectsphere.message_service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.connectsphere.message_service.entity.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    Message findTopByConversationIdOrderByCreatedAtDesc(Long conversationId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId = :cid AND m.senderId != :uid AND m.isRead = false AND m.isDeleted = false")
    long countUnread(@Param("cid") Long conversationId, @Param("uid") Long userId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId IN :cids AND m.senderId != :uid AND m.isRead = false AND m.isDeleted = false")
    long countTotalUnread(@Param("cids") List<Long> conversationIds, @Param("uid") Long userId);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.conversationId = :cid AND m.senderId != :uid AND m.isRead = false AND m.isDeleted = false")
    int markAsRead(@Param("cid") Long conversationId, @Param("uid") Long userId);
}
