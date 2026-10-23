package com.rtms.backend.ai.dto;

import com.rtms.backend.ai.hitl.enums.ActionStatus;

/**
 * DTO kết quả sau khi commit hành động HITL thành công.
 */
public record ActionCommitResponseDto(
        Long actionId,
        ActionStatus status,
        String toolName,
        String targetEntityId,
        String message
) {}
