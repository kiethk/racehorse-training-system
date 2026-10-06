package com.rtms.backend.dto;

import com.rtms.backend.entity.CareSchedule;
import com.rtms.backend.enums.CareScheduleStatus;
import com.rtms.backend.enums.CareType;

import java.time.LocalDateTime;

public record CareScheduleResponse(
        Long id,
        Long horseId,
        Long veterinarianId,
        Long trainerId,
        Long admissionId,
        Long sourceIncidentId,
        CareType careType,
        CareScheduleStatus status,
        LocalDateTime scheduledAt,
        int durationMinutes,
        String description,
        LocalDateTime completedAt,
        String cancelReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CareScheduleResponse from(CareSchedule cs) {
        if (cs == null) return null;
        return new CareScheduleResponse(
                cs.getId(),
                cs.getHorseId(),
                cs.getVeterinarianId(),
                cs.getTrainerId(),
                cs.getAdmissionId(),
                cs.getSourceIncidentId(),
                cs.getCareType(),
                cs.getStatus(),
                cs.getScheduledAt(),
                cs.getDurationMinutes(),
                cs.getDescription(),
                cs.getCompletedAt(),
                cs.getCancelReason(),
                cs.getCreatedAt(),
                cs.getUpdatedAt()
        );
    }
}