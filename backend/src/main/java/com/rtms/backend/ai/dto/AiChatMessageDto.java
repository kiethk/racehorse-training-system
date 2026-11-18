package com.rtms.backend.ai.dto;

/**
 * Message in the AI conversation history.
 */
public record AiChatMessageDto(
        String role,
        String content
) {}
