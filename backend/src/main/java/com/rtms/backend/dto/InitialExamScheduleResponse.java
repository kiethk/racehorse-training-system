package com.rtms.backend.dto;
import com.rtms.backend.entity.CareSchedule;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InitialExamScheduleResponse(Long scheduleId, String careType, String status,
        Long veterinarianId, LocalDate scheduledDate, LocalDateTime scheduledAt) {

    public static InitialExamScheduleResponse from(CareSchedule schedule) {
        if (schedule == null) return null;
        return new InitialExamScheduleResponse(schedule.getId(), schedule.getCareType().name(),
                schedule.getStatus().name(), schedule.getVeterinarianId(),
                schedule.getScheduledAt() == null ? null : schedule.getScheduledAt().toLocalDate(),
                schedule.getScheduledAt());
    }
}
