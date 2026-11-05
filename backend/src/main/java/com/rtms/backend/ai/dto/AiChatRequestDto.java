package com.rtms.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Request gửi từ người dùng đến AI Chat Agent.
 */
public record AiChatRequestDto(
        @NotBlank(message = "Nội dung câu hỏi không được để trống")
        String message,
        Long conversationId,
        List<AiChatMessageDto> conversationHistory
) {
    public AiChatRequestDto(String message, List<AiChatMessageDto> conversationHistory) {
        this(message, null, conversationHistory);
    }
}
