package com.rtms.backend.dto;

import com.rtms.backend.enums.IncidentSeverity;
import com.rtms.backend.enums.CareScheduleStatus;
import com.rtms.backend.enums.TrainingStatus;

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
        TrainingStatus trainingStatus,
        CareScheduleStatus status,
        LocalDateTime scheduledAt,
        LocalDateTime assignedAt
) {}
