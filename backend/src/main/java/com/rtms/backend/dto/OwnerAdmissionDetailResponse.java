package com.rtms.backend.dto;

import com.rtms.backend.enums.AdmissionStatus;
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
        String groomFeedback,
        String vetFeedback,
        String trainerFeedback,
        String managerFeedback) {

    public record CandidateSnapshot(
            String name, String breed, LocalDate dateOfBirth,
            String registrationNumber, String registryName,
            String sireName, String sireRegistrationNumber,
            String damName, String damRegistrationNumber, String pedigreeNotes) {}
}
