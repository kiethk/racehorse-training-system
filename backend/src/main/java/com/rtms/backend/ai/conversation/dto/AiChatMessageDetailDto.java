package com.rtms.backend.ai.conversation.dto;

import java.time.LocalDateTime;

public record AiChatMessageDetailDto(
        Long id,
        Long conversationId,
        String role,
        String content,
        String toolCalls,
        LocalDateTime createdAt
) {}
