package com.rtms.backend.ai.conversation.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateConversationTitleRequestDto(
        @NotBlank(message = "Tiêu đề cuộc trò chuyện không được để trống")
        String title
) {}
