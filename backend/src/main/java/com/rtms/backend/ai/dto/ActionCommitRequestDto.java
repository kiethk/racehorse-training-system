package com.rtms.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO gửi từ người dùng khi bấm Xác nhận (Commit) hành động HITL.
 */
public record ActionCommitRequestDto(
        @NotBlank(message = "Idempotency key không được để trống")
        String idempotencyKey,

        @NotBlank(message = "Action token không được để trống")
        String actionToken
) {}
