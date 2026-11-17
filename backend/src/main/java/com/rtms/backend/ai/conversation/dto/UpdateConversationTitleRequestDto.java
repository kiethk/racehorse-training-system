package com.rtms.backend.ai.conversation.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateConversationTitleRequestDto(
        @NotBlank(message = "Conversation title must not be blank")
        String title
) {}
