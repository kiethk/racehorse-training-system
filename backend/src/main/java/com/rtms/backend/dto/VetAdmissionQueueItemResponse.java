package com.rtms.backend.dto;

import com.rtms.backend.enums.AdmissionStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record VetAdmissionQueueItemResponse(
        Long admissionId,
        Long ownerId,
        String ownerName,
        String candidateName,
        String breed,
        LocalDate dateOfBirth,
        AdmissionStatus admissionStatus,
        LocalDateTime submittedAt,
        Long quarantineStallId,
        String quarantineStallCode,
        Long horseId,
        Long trainerId,
        String trainerName,
        CareScheduleResponse careSchedule
) {}
