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
        int rolloverCount,
        /** D1 cờ dẫn xuất: IN_PROGRESS quá 30 phút so với scheduled_at (không phải trạng thái DB). */
        boolean overdue,
        /** D2 thời điểm ca khám bị tạm dừng do ưu tiên ca cấp cứu khác (legacy null). */
        LocalDateTime pausedFrom
) {
    public static CareScheduleResponse from(CareSchedule cs) {
        if (cs == null) return null;
        // AD-022: OVERDUE is a real status; the flag mirrors it for clients that only read the boolean.
        boolean overdue = CareScheduleStatus.ACTIVE_EXAM.contains(cs.getStatus())
                && cs.getScheduledAt() != null
                && java.time.LocalDateTime.now().isAfter(cs.getScheduledAt().plusMinutes(30));
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
                cs.getRolloverCount(),
                overdue,
                null
        );
    }
}
