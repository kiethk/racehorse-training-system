package com.rtms.backend.analytics.dto.manager;

public record AdmissionStageLatenciesDto(
        double arrivalLeadTimeHours,
        double groomReviewHours,
        double vetReviewHours,
        double trainerReviewHours,
        double managerReviewHours,
        double totalEndToEndHours
) {}
