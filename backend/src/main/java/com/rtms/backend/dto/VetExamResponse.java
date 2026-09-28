package com.rtms.backend.dto;

import com.rtms.backend.entity.VetExam;
import com.rtms.backend.enums.VetExamStatus;
import com.rtms.backend.enums.VetExamType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record VetExamResponse(Long id, Long horseId, Long admissionId, VetExamType examType,
        VetExamStatus status, int priority, String reason, Long createdByUserId,
        Long assignedVetId, Long preferredVetId, LocalDate requestedForDate,
        LocalDateTime scheduledAt, int durationMinutes, Long healthRecordId,
        LocalDateTime createdAt) {
    public static VetExamResponse from(VetExam exam) {
        return new VetExamResponse(exam.getId(), exam.getHorseId(), exam.getAdmissionId(),
                exam.getExamType(), exam.getStatus(), exam.getPriority(), exam.getReason(),
                exam.getCreatedByUserId(), exam.getAssignedVetId(), exam.getPreferredVetId(),
                exam.getRequestedForDate(), exam.getScheduledAt(), exam.getDurationMinutes(),
                exam.getHealthRecordId(), exam.getCreatedAt());
    }
}
