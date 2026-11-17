package com.rtms.backend.analytics.dto.manager;

import java.util.List;

public record TrainerEvaluationEdaDto(
        double overallAdherenceRate,
        double overallCancellationRate,
        int totalWorkoutsScheduled,
        int totalWorkoutsCompleted,
        List<TrainerPerformanceDto> trainers
) {
    public TrainerEvaluationEdaDto(
            double overallAdherenceRate,
            int totalWorkoutsScheduled,
            int totalWorkoutsCompleted,
            List<TrainerPerformanceDto> trainers
    ) {
        this(overallAdherenceRate, 0.0, totalWorkoutsScheduled, totalWorkoutsCompleted, trainers);
    }
}
