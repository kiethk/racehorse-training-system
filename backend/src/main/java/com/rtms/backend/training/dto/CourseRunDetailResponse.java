package com.rtms.backend.training.dto;

import com.rtms.backend.training.enums.TrainingDay;

import java.util.List;

public record CourseRunDetailResponse(CourseRunSummaryResponse courseRun,
                                      List<TrainingDay> trainingDays,
                                      List<SessionSummaryResponse> sessions) {
}
