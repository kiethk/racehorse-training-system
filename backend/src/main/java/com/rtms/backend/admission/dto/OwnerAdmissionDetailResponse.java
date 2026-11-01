package com.rtms.backend.admission.dto;

import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.enums.ReviewDecision;
import com.rtms.backend.horse.enums.HorseSex;
import com.rtms.backend.training.enums.TrainingDecision;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Owner-safe detail: no internal horse/stall/actor IDs or private clinical records. */
public record OwnerAdmissionDetailResponse(
        Long admissionId,
        AdmissionStatus status,
        LocalDateTime submittedAt,
        CandidateSnapshot candidate,
        List<AdmissionDocumentResponse> documents,

        // Groom review milestone
        ReviewDecision groomDecision,
        LocalDateTime groomReviewedAt,
        String groomFeedback,

        // Arrival milestone
        LocalDateTime arrivedAt,
        LocalDateTime arrivalDeadlineAt,

        // Vet review milestone
        TrainingDecision vetTrainingDecision,
        LocalDateTime vetReviewedAt,
        String vetFeedback,

        // Trainer review milestone
        LocalDateTime trainerReviewedAt,
        String trainerFeedback,

        // Manager review milestone
        ReviewDecision managerDecision,
        LocalDateTime managerReviewedAt,
        String managerFeedback) {

    public record CandidateSnapshot(
            String name, String breed, LocalDate dateOfBirth, HorseSex sex,
            String registrationNumber, String registryName,
            String sireName, String sireRegistrationNumber,
            String damName, String damRegistrationNumber, String pedigreeNotes) {}
}
