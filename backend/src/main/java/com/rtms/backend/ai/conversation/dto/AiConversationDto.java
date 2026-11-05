package com.rtms.backend.ai.conversation.dto;

import java.time.LocalDateTime;

public record AiConversationDto(
        Long id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
