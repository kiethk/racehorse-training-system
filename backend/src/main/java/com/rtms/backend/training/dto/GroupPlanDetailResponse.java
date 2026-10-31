package com.rtms.backend.training.dto;

import com.rtms.backend.training.enums.TrainingDay;

import java.util.List;

public record GroupPlanDetailResponse(GroupPlanSummaryResponse plan,
                                      List<TrainingDay> trainingDays,
                                      List<PlanSessionResponse> sessions) {
}
