package com.rtms.backend.training.dto;

/**
 * Một chiến mã trong một buổi của kế hoạch và kết cục của nó: TO_RUN, RAN, CANCELLED hoặc DID_NOT_RUN.
 * (A horse in one Session of a plan and its outcome: TO_RUN, RAN, CANCELLED or DID_NOT_RUN.)
 */
public record SessionHorseOutcomeResponse(Long workoutId, Long horseId, String horseName, String outcome) {
}
