package com.rtms.backend.dto;

import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.HorseStatus;
import com.rtms.backend.enums.TrainingDecision;
import com.rtms.backend.enums.CareScheduleStatus;
import java.time.LocalDateTime;

public record VetReviewResponse(
    Long admissionId,
    AdmissionStatus status,
    Long veterinarianId,
    TrainingDecision trainingDecision,
    String restrictionDetails,
    String feedback,
    LocalDateTime reviewedAt,
    Long horseId,
    HorseStatus horseStatus,
    Long quarantineStallId,
    String quarantineStallCode,
    CareScheduleStatus initialExamStatus,
    Long healthRecordId,
    Long vetExamId,
    Long careScheduleId
) { }
