package com.rtms.backend.medical.analytics.dto;

import java.util.List;

public record SurvivalEdaResponse(
        String stratifyBy,
        List<SurvivalStratumDto> strata
) {
    public record SurvivalStratumDto(
            Long stratumId,
            String stratumName,
            Integer totalCases,
            Integer recoveredCount,
            Integer censoredCount,
            Double medianRecoveryDays,
            List<KaplanMeierStepDto> steps
    ) {}

    public record KaplanMeierStepDto(
            Integer days,
            Double survivalProbability,
            Integer atRiskCount,
            Integer recoveredEventsCount
    ) {}
}
