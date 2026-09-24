package com.rtms.backend.service;

import com.rtms.backend.dto.AdmissionDetailResponse;
import com.rtms.backend.dto.AdmissionSummaryResponse;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.AdmissionDocument;
import com.rtms.backend.entity.CandidateHorseProfile;
import com.rtms.backend.entity.HealthRecord;
import com.rtms.backend.entity.StableStall;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.AdmissionDocumentRepository;
import com.rtms.backend.repository.CandidateHorseProfileRepository;
import com.rtms.backend.repository.HealthRecordRepository;
import com.rtms.backend.repository.StableStallRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdmissionQueryService {

    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final CandidateHorseProfileRepository candidateHorseProfileRepository;
    private final AdmissionDocumentRepository admissionDocumentRepository;
    private final StableStallRepository stableStallRepository;
    private final HealthRecordRepository healthRecordRepository;

    public AdmissionQueryService(
            AdmissionApplicationRepository admissionApplicationRepository,
            CandidateHorseProfileRepository candidateHorseProfileRepository,
            AdmissionDocumentRepository admissionDocumentRepository,
            StableStallRepository stableStallRepository,
            HealthRecordRepository healthRecordRepository) {
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.candidateHorseProfileRepository = candidateHorseProfileRepository;
        this.admissionDocumentRepository = admissionDocumentRepository;
        this.stableStallRepository = stableStallRepository;
        this.healthRecordRepository = healthRecordRepository;
    }

    public List<AdmissionSummaryResponse> getAdmissions(AdmissionStatus status) {

        List<AdmissionApplication> admissions;

        if (status != null) {
            admissions = admissionApplicationRepository.findByStatus(status);
        } else {
            admissions = admissionApplicationRepository.findAll();
        }

        return admissions.stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public AdmissionDetailResponse getAdmissionDetail(Long admissionId) {

        AdmissionApplication admission = admissionApplicationRepository
                .findById(admissionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Admission not found"));

        CandidateHorseProfile candidate = candidateHorseProfileRepository
                .findByAdmissionId(admissionId)
                .orElseThrow(() -> new IllegalStateException(
                        "Candidate horse profile not found"));

        List<AdmissionDocument> documents =
                admissionDocumentRepository.findByAdmissionId(admissionId);

        AdmissionDetailResponse response = new AdmissionDetailResponse();

        response.setAdmissionId(admission.getId());
        response.setOwnerId(admission.getOwnerId());
        response.setStatus(admission.getStatus());
        response.setQuarantineStallId(admission.getQuarantineStallId());

        response.setCandidate(candidate);
        response.setDocuments(documents);

        response.setGroomId(admission.getGroomId());
        response.setGroomDecision(admission.getGroomDecision());
        response.setGroomFeedback(admission.getGroomFeedback());
        response.setGroomReviewedAt(admission.getGroomReviewedAt());

        response.setVeterinarianId(admission.getVeterinarianId());
        response.setVetDecision(admission.getVetDecision());
        response.setVetFeedback(admission.getVetFeedback());
        response.setVetReviewedAt(admission.getVetReviewedAt());

        response.setTrainerId(admission.getTrainerId());

        response.setTrainerFeedback(admission.getTrainerFeedback());
        response.setTrainerReviewedAt(admission.getTrainerReviewedAt());

        response.setManagerId(admission.getManagerId());
        response.setManagerDecision(admission.getManagerDecision());
        response.setManagerFeedback(admission.getManagerFeedback());
        response.setManagerReviewedAt(admission.getManagerReviewedAt());

        response.setHorseId(admission.getHorseId());
        response.setSubmittedAt(admission.getSubmittedAt());

        response.setQuarantineStallCode(admission.getQuarantineStallId() == null
                ? null
                : stableStallRepository.findById(admission.getQuarantineStallId())
                        .map(StableStall::getStallCode)
                        .orElse(null));

        // Available REGULAR stalls — always included so Manager can select during approval
        response.setAvailableRegularStalls(
                stableStallRepository.findAllAvailableRegularStallsOrdered());

        // Health records for the horse created during Vet quarantine review
        if (admission.getHorseId() != null) {
            List<HealthRecord> healthRecords =
                    healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(
                            admission.getHorseId());
            response.setHealthRecords(healthRecords);
        }

        return response;
    }

    private AdmissionSummaryResponse toSummaryResponse(
            AdmissionApplication admission) {

        CandidateHorseProfile candidate = candidateHorseProfileRepository
                .findByAdmissionId(admission.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Candidate horse profile not found"));

        String imageUrl = admissionDocumentRepository.findByAdmissionId(admission.getId())
                .stream()
                .filter(doc -> doc.getDocumentType() == com.rtms.backend.enums.AdmissionDocumentType.HORSE_PHOTO)
                .map(com.rtms.backend.entity.AdmissionDocument::getFileUrl)
                .findFirst()
                .orElse(null);

        return new AdmissionSummaryResponse(
                admission.getId(),
                admission.getStatus(),
                candidate.getName(),
                candidate.getBreed(),
                candidate.getDateOfBirth(),
                admission.getSubmittedAt(),
                admission.getQuarantineStallId(),
                imageUrl
        );
    }
}
