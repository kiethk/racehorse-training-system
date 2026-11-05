package com.rtms.backend.ai.dto;

import java.util.Map;

/**
 * DTO đóng gói đề xuất hành động HITL gửi về cho người dùng (UI Modal).
 */
public record ActionProposalDto(
        Long actionId,
        String actionToken,
        String toolName,
        String title,
        String targetEntityId,
        Map<String, Object> currentValues,
        Map<String, Object> proposedValues,
        String reason,
        long expiresInSeconds
) {}
