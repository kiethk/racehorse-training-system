package com.rtms.backend.dto;

import com.rtms.backend.entity.HealthRecord;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.HorseHealthMetric;
import com.rtms.backend.entity.RacingReadinessAssessment;

import java.util.List;

public record TrainerScheduleDetailResponse(
        TrainerScheduleResponse schedule,
        AdmissionDetailResponse admission,
        Horse horse,
        List<HealthRecord> healthRecords,
        List<HorseHealthMetric> healthMetrics,
        RacingReadinessAssessment existingAssessment
) {
}
