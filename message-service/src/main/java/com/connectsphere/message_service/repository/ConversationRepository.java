package com.connectsphere.message_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.connectsphere.message_service.entity.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);

    @Query("SELECT c FROM Conversation c WHERE c.user1Id = :uid OR c.user2Id = :uid ORDER BY c.lastMessageAt DESC")
    List<Conversation> findByUserId(@Param("uid") Long userId);
}
