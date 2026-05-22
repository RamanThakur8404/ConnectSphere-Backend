package com.connectsphere.message_service.controller;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.connectsphere.message_service.dto.ConversationResponseDTO;
import com.connectsphere.message_service.dto.MessageRequestDTO;
import com.connectsphere.message_service.dto.MessageResponseDTO;
import com.connectsphere.message_service.dto.ApiResponse;
import com.connectsphere.message_service.exception.UnauthorizedMessageAccessException;
import com.connectsphere.message_service.service.MessageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private static final Logger log = LoggerFactory.getLogger(MessageController.class);
    private final MessageService messageService;

    @Operation(summary = "List user's conversations", security = @SecurityRequirement(name = "gateway-auth"))
    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ConversationResponseDTO>>> getConversations(HttpServletRequest request) {
        Long userId = resolveUserId(request);
        log.info("GET /conversations — userId={}", userId);
        return ResponseEntity.ok(ApiResponse.success("Conversations retrieved successfully", messageService.getConversations(userId)));
    }

    @Operation(summary = "Start or get existing conversation", security = @SecurityRequirement(name = "gateway-auth"))
    @PostMapping("/conversations/{otherUserId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ConversationResponseDTO>> getOrCreateConversation(
            @PathVariable Long otherUserId, HttpServletRequest request) {
        Long userId = resolveUserId(request);
        log.info("POST /conversations/{} — userId={}", otherUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("Conversation retrieved successfully", messageService.getOrCreateConversation(userId, otherUserId)));
    }

    @Operation(summary = "Get messages in a conversation", security = @SecurityRequirement(name = "gateway-auth"))
    @GetMapping("/conversations/{conversationId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MessageResponseDTO>>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        log.info("GET /conversations/{}/messages — userId={} page={}", conversationId, userId, page);
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved successfully", messageService.getMessages(conversationId, userId, page, size)));
    }

    @Operation(summary = "Send a message", security = @SecurityRequirement(name = "gateway-auth"))
    @PostMapping("/conversations/{conversationId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MessageResponseDTO>> sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody MessageRequestDTO requestDTO,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        log.info("POST /conversations/{}/messages — senderId={}", conversationId, userId);
        MessageResponseDTO response = messageService.sendMessage(conversationId, userId, requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Message sent successfully", response));
    }

    @Operation(summary = "Edit a sent message", security = @SecurityRequirement(name = "gateway-auth"))
    @PutMapping("/messages/{messageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MessageResponseDTO>> updateMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody MessageRequestDTO requestDTO,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        log.info("PUT /messages/{} - userId={}", messageId, userId);
        return ResponseEntity.ok(ApiResponse.success("Message updated successfully",
                messageService.updateMessage(messageId, userId, requestDTO)));
    }

    @Operation(summary = "Delete a sent message", security = @SecurityRequirement(name = "gateway-auth"))
    @DeleteMapping("/messages/{messageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MessageResponseDTO>> deleteMessage(
            @PathVariable Long messageId,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        log.info("DELETE /messages/{} - userId={}", messageId, userId);
        return ResponseEntity.ok(ApiResponse.success("Message deleted successfully",
                messageService.deleteMessage(messageId, userId)));
    }

    @Operation(summary = "Mark conversation as read", security = @SecurityRequirement(name = "gateway-auth"))
    @PutMapping("/conversations/{conversationId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long conversationId, HttpServletRequest request) {
        Long userId = resolveUserId(request);
        log.info("PUT /conversations/{}/read — userId={}", conversationId, userId);
        messageService.markAsRead(conversationId, userId);
        return ResponseEntity.ok(ApiResponse.success("Conversation marked as read", null));
    }

    @Operation(summary = "Get unread message count", security = @SecurityRequirement(name = "gateway-auth"))
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(HttpServletRequest request) {
        Long userId = resolveUserId(request);
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved", messageService.getUnreadCount(userId)));
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private Long resolveUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("X-User-Id");
        if (attr == null) {
            throw new UnauthorizedMessageAccessException("User identity header is missing.");
        }
        return (Long) attr;
    }
}
