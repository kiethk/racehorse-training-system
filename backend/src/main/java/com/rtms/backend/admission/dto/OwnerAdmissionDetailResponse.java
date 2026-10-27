package com.rtms.backend.admission.dto;

import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.horse.enums.HorseSex;
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
        String managerFeedback,
        /* GROOM_REVIEW only: no groom can take a new horse right now (no free quarantine stall or regular reserve). */
        boolean quarantineFull) {

    public record CandidateSnapshot(
            String name, String breed, LocalDate dateOfBirth, HorseSex sex,
            String registrationNumber, String registryName,
            String sireName, String sireRegistrationNumber,
            String damName, String damRegistrationNumber, String pedigreeNotes) {}
}
