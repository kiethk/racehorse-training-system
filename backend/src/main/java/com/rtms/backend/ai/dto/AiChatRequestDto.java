package com.rtms.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Request sent by the user to the AI Chat Agent.
 */
public record AiChatRequestDto(
        @NotBlank(message = "Question content must not be blank")
        String message,
        Long conversationId,
        List<AiChatMessageDto> conversationHistory
) {
    public AiChatRequestDto(String message, List<AiChatMessageDto> conversationHistory) {
        this(message, null, conversationHistory);
    }
}
