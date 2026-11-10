package com.rtms.backend.training.dto;

/**
 * Một chiến mã trong một buổi của kế hoạch và kết cục của nó: TO_RUN, NEEDS_RESULT, RAN, CANCELLED hoặc DID_NOT_RUN.
 * (A horse in one Session of a course run and its outcome: TO_RUN, NEEDS_RESULT, RAN, CANCELLED or DID_NOT_RUN.)
 */
public record SessionHorseOutcomeResponse(Long workoutId, Long horseId, String horseName, String outcome) {
}
