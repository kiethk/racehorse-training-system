package com.rtms.backend.analytics.dto.manager;

import java.util.List;

public record TrainerEvaluationEdaDto(
        double overallAdherenceRate,
        int totalWorkoutsScheduled,
        int totalWorkoutsCompleted,
        List<TrainerPerformanceDto> trainers
) {}
