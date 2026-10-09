package com.rtms.backend.notification.dto;

import com.rtms.backend.stable.enums.IncidentSeverity;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.training.enums.TrainingDecision;

import java.time.LocalDateTime;

public record UrgentAssignmentAlert(
        Long eventId,
        Long scheduleId,
        Long incidentId,
        Long veterinarianId,
        Long horseId,
        String horseName,
        String stableLocation,
        String stallCode,
        Long reportedById,
        String reportedByName,
        LocalDateTime reportedAt,
        IncidentSeverity severity,
        String title,
        String description,
        String imageUrl,
        TrainingDecision trainingDecision,
        CareScheduleStatus status,
        LocalDateTime scheduledAt,
        LocalDateTime assignedAt
) {}
