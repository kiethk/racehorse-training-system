package com.rtms.backend.ai.dto;

/**
 * Tin nhắn trong lịch sử hội thoại AI.
 */
public record AiChatMessageDto(
        String role,
        String content
) {}
