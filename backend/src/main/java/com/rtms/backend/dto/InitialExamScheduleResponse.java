package com.rtms.backend.dto;

import com.rtms.backend.entity.VetExam;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InitialExamScheduleResponse(Long scheduleId, String careType, String status,
        Long veterinarianId, LocalDate scheduledDate, LocalDateTime scheduledAt) {
    public static InitialExamScheduleResponse from(VetExam exam) {
        return new InitialExamScheduleResponse(exam.getId(), exam.getExamType().name(),
                exam.getStatus().name(), exam.getAssignedVetId(),
                exam.getScheduledAt() == null ? exam.getRequestedForDate() : exam.getScheduledAt().toLocalDate(),
                exam.getScheduledAt());
    }
}
