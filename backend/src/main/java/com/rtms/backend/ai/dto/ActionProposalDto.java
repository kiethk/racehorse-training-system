package com.rtms.backend.ai.dto;

import java.util.Map;

/**
 * DTO that packages an HITL action proposal for the user (UI modal).
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
