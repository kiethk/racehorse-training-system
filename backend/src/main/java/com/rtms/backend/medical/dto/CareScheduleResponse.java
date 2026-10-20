package com.rtms.backend.medical.dto;

import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareSchedulePurpose;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.enums.RescheduleReason;

import java.time.LocalDateTime;

public record CareScheduleResponse(
        Long id,
        Long horseId,
        Long veterinarianId,
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
        LocalDateTime updatedAt,
        CareSchedulePurpose purpose,
        LocalDateTime requestedAt,
        RescheduleReason rescheduleReason,
        String rescheduleNote
) {
    public static CareScheduleResponse from(CareSchedule cs) {
        if (cs == null) return null;
        return new CareScheduleResponse(
                cs.getId(),
                cs.getHorseId(),
                cs.getVeterinarianId(),
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
                cs.getUpdatedAt(),
                cs.getPurpose(),
                cs.getRequestedAt(),
                cs.getRescheduleReason(),
                cs.getRescheduleNote()
        );
    }
}