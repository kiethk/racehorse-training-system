package com.rtms.backend.ai.controller;

import com.rtms.backend.ai.conversation.dto.AiConversationDto;
import com.rtms.backend.ai.conversation.dto.AiChatMessageDetailDto;
import com.rtms.backend.ai.conversation.dto.UpdateConversationTitleRequestDto;
import com.rtms.backend.ai.conversation.service.AiConversationService;
import com.rtms.backend.ai.dto.AiChatRequestDto;
import com.rtms.backend.ai.dto.AiChatResponseDto;
import com.rtms.backend.ai.service.GroqAgentOrchestratorService;
import com.rtms.backend.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Controller tiếp nhận yêu cầu hội thoại AI Agent (quản lý phiên chat bền vững và realtime SSE streaming).
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiChatController {

    private final GroqAgentOrchestratorService agentOrchestratorService;
    private final AiConversationService conversationService;
    private final AsyncTaskExecutor taskExecutor;

    public AiChatController(
            GroqAgentOrchestratorService agentOrchestratorService,
            AiConversationService conversationService,
            @Qualifier("applicationTaskExecutor") AsyncTaskExecutor taskExecutor) {
        this.agentOrchestratorService = agentOrchestratorService;
        this.conversationService = conversationService;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Lấy danh sách các cuộc hội thoại của người dùng hiện tại.
     */
    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AiConversationDto>> getConversations(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        List<AiConversationDto> conversations = conversationService.getUserConversations(currentUser.getUserId());
        return ResponseEntity.ok(conversations);
    }

    /**
     * Lấy danh sách tin nhắn của một cuộc hội thoại cụ thể.
     */
    @GetMapping("/conversations/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AiChatMessageDetailDto>> getConversationMessages(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        List<AiChatMessageDetailDto> messages = conversationService.getConversationMessages(id, currentUser.getUserId());
        return ResponseEntity.ok(messages);
    }

    /**
     * Tạo một cuộc trò chuyện mới.
     */
    @PostMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiConversationDto> createConversation(
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        String title = body != null ? body.get("title") : null;
        AiConversationDto created = conversationService.createNewConversation(currentUser.getUserId(), title);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Đổi tên một cuộc hội thoại.
     */
    @PutMapping("/conversations/{id}/title")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiConversationDto> renameConversation(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateConversationTitleRequestDto request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        AiConversationDto updated = conversationService.renameConversation(id, currentUser.getUserId(), request.title());
        return ResponseEntity.ok(updated);
    }

    /**
     * Xóa một cuộc hội thoại và toàn bộ lịch sử tin nhắn.
     */
    @DeleteMapping("/conversations/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> deleteConversation(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        conversationService.deleteConversation(id, currentUser.getUserId());
        return ResponseEntity.ok(Map.of("success", true, "id", id));
    }

    /**
     * Endpoint chat đồng bộ với AI Agent.
     */
    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiChatResponseDto> chat(
            @Valid @RequestBody AiChatRequestDto request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        AiChatResponseDto response = agentOrchestratorService.processChat(request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint chat qua Server-Sent Events (SSE) để truyền dữ liệu thời gian thực.
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter streamChat(
            @Valid @RequestBody AiChatRequestDto request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        SseEmitter emitter = new SseEmitter(60_000L); // 60s timeout

        taskExecutor.execute(() -> {
            try {
                AiChatResponseDto response = agentOrchestratorService.processChat(request, currentUser);
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(response));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(e.getMessage()));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
