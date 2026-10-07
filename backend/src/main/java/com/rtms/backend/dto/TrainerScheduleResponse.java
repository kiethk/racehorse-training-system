package com.rtms.backend.dto;

import com.rtms.backend.entity.TrainerSchedule;
import com.rtms.backend.enums.TrainerScheduleStatus;

import java.time.LocalDateTime;

public record TrainerScheduleResponse(
        Long id,
        Long horseId,
        Long admissionId,
        Long sourceCareScheduleId,
        Long trainerId,
        TrainerScheduleStatus status,
        LocalDateTime scheduledAt,
        int durationMinutes,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String candidateName,
        String breed,
        String quarantineStallCode
) {
    public static TrainerScheduleResponse from(TrainerSchedule ts) {
        return from(ts, null, null, null);
    }

    public static TrainerScheduleResponse from(TrainerSchedule ts, String candidateName, String breed, String quarantineStallCode) {
        if (ts == null) return null;
        return new TrainerScheduleResponse(
                ts.getId(),
                ts.getHorseId(),
                ts.getAdmissionId(),
                ts.getSourceCareScheduleId(),
                ts.getTrainerId(),
                ts.getStatus(),
                ts.getScheduledAt(),
                ts.getDurationMinutes(),
                ts.getCompletedAt(),
                ts.getCreatedAt(),
                ts.getUpdatedAt(),
                candidateName,
                breed,
                quarantineStallCode
        );
    }
}
