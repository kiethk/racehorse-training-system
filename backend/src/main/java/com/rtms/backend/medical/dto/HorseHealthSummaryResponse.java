package com.rtms.backend.medical.dto;

import com.rtms.backend.training.enums.TrainingDecision;

import java.time.LocalDateTime;

public record HorseHealthSummaryResponse(
        Long horseId,
        String horseName,
        TrainingDecision trainingDecision,
        String trainingDecisionReason,
        boolean hasPendingUrgent,
        LocalDateTime lastExaminedAt,
        String lastDiagnosis,
        int activeInjuryCount,
        LocalDateTime nextScheduledCare
) {}
