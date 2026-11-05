package com.rtms.backend.horse.dto;

import com.rtms.backend.admission.enums.ReviewDecision;

/** Groom decision on a horse edit request; feedback is required to reject. */
public record HorseEditReviewRequest(ReviewDecision decision, String feedback) {
}
