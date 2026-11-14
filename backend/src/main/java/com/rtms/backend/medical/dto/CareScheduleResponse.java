package com.rtms.backend.medical.dto;

import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareSchedulePurpose;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.enums.RescheduleReason;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CareScheduleResponse(
        Long id,
        Long horseId,
        Long veterinarianId,
        String veterinarianName,
        Long admissionId,
        Long sourceIncidentId,
        CareType careType,
        CareScheduleStatus status,
        LocalDate scheduledDate,
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
        String rescheduleNote,
        int rolloverCount
) {
    public static CareScheduleResponse from(CareSchedule cs) {
        return from(cs, null);
    }

    public static CareScheduleResponse from(CareSchedule cs, String vetName) {
        if (cs == null) return null;
        LocalDate date = cs.getScheduledDate();
        if (date == null && cs.getScheduledAt() != null) {
            date = cs.getScheduledAt().toLocalDate();
        } else if (date == null && cs.getRequestedAt() != null) {
            date = cs.getRequestedAt().toLocalDate();
        }
        return new CareScheduleResponse(
                cs.getId(),
                cs.getHorseId(),
                cs.getVeterinarianId(),
                vetName,
                cs.getAdmissionId(),
                cs.getSourceIncidentId(),
                cs.getCareType(),
                cs.getStatus(),
                date,
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
                cs.getRescheduleNote(),
                cs.getRolloverCount()
        );
    }
}
