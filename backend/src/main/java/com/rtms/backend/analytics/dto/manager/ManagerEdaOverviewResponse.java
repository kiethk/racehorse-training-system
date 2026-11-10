package com.rtms.backend.analytics.dto.manager;

public record ManagerEdaOverviewResponse(
        int timeWindowDays,
        VetDelaysEdaDto vetDelays,
        TrainerEvaluationEdaDto trainerEvaluation,
        StallOccupancyEdaDto stallOccupancy,
        AdmissionFunnelEdaDto admissionFunnel
) {}
