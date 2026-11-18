package com.rtms.backend.ai.controller;

import com.rtms.backend.ai.conversation.dto.AiConversationDto;
import com.rtms.backend.ai.conversation.dto.AiChatMessageDetailDto;
import com.rtms.backend.ai.conversation.dto.UpdateConversationTitleRequestDto;
import com.rtms.backend.ai.conversation.service.AiConversationService;
import com.rtms.backend.ai.dto.AiChatRequestDto;
import com.rtms.backend.ai.dto.AiChatResponseDto;
import com.rtms.backend.ai.service.AgentOrchestratorService;
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
 * Controller that handles AI Agent conversation requests (persistent chat session management and real-time SSE streaming).
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiChatController {

    private final AgentOrchestratorService agentOrchestratorService;
    private final AiConversationService conversationService;
    private final AsyncTaskExecutor taskExecutor;

    public AiChatController(
            AgentOrchestratorService agentOrchestratorService,
            AiConversationService conversationService,
            @Qualifier("applicationTaskExecutor") AsyncTaskExecutor taskExecutor) {
        this.agentOrchestratorService = agentOrchestratorService;
        this.conversationService = conversationService;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Gets the current user's conversations.
     */
    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AiConversationDto>> getConversations(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        List<AiConversationDto> conversations = conversationService.getUserConversations(currentUser.getUserId());
        return ResponseEntity.ok(conversations);
    }

    /**
     * Gets the messages in a specific conversation.
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
     * Creates a new conversation.
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
     * Renames a conversation.
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
     * Deletes a conversation and its entire message history.
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
     * Synchronous chat endpoint for the AI Agent.
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
     * Chat endpoint using Server-Sent Events (SSE) for real-time data delivery.
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
