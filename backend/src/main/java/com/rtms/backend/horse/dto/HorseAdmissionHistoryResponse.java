package com.rtms.backend.horse.dto;

import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.enums.ReviewDecision;
import com.rtms.backend.training.enums.TrainingDecision;

import java.time.LocalDateTime;

/**
 * Historical admission record linked directly to a horse by horseId.
 * Captures the actual stored milestones (submitted, reviews, arrival).
 */
public record HorseAdmissionHistoryResponse(
        Long admissionId,
        Long horseId,
        AdmissionStatus status,
        LocalDateTime submittedAt,

        // Groom milestone
        Long groomId,
        String groomName,
        LocalDateTime groomReviewedAt,
        ReviewDecision groomDecision,
        String groomFeedback,

        // Arrival milestone
        LocalDateTime arrivedAt,

        // Veterinarian milestone
        Long veterinarianId,
        String vetName,
        LocalDateTime vetReviewedAt,
        TrainingDecision vetTrainingDecision,
        String vetFeedback,

        // Trainer milestone
        Long trainerId,
        String trainerName,
        LocalDateTime trainerReviewedAt,
        String trainerFeedback,

        // Manager milestone
        Long managerId,
        String managerName,
        LocalDateTime managerReviewedAt,
        ReviewDecision managerDecision,
        String managerFeedback
) {}
