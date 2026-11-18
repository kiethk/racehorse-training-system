package com.rtms.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO sent by the user when they confirm (commit) an HITL action.
 */
public record ActionCommitRequestDto(
        @NotBlank(message = "Idempotency key must not be blank")
        String idempotencyKey,

        @NotBlank(message = "Action token must not be blank")
        String actionToken
) {}
