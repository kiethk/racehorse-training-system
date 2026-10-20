package com.rtms.backend.ai.controller;

import com.rtms.backend.ai.dto.AiChatRequestDto;
import com.rtms.backend.ai.dto.AiChatResponseDto;
import com.rtms.backend.ai.service.GroqAgentOrchestratorService;
import com.rtms.backend.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * Controller tiếp nhận yêu cầu hội thoại AI Agent (đồng bộ và realtime SSE streaming).
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiChatController {

    private final GroqAgentOrchestratorService agentOrchestratorService;
    private final AsyncTaskExecutor taskExecutor;

    public AiChatController(
            GroqAgentOrchestratorService agentOrchestratorService,
            @Qualifier("applicationTaskExecutor") AsyncTaskExecutor taskExecutor) {
        this.agentOrchestratorService = agentOrchestratorService;
        this.taskExecutor = taskExecutor;
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
