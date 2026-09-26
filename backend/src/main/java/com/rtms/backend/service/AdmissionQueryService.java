package com.rtms.backend.service;

import com.rtms.backend.dto.AdmissionDetailResponse;
import com.rtms.backend.dto.AdmissionCapacitySummary;
import com.rtms.backend.dto.AdmissionDocumentResponse;
import com.rtms.backend.dto.AdmissionSummaryResponse;
import com.rtms.backend.dto.GroomAdmissionQueueResponse;
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
import com.rtms.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

@Service
public class AdmissionQueryService {

    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final CandidateHorseProfileRepository candidateHorseProfileRepository;
    private final AdmissionDocumentRepository admissionDocumentRepository;
    private final StableStallRepository stableStallRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final AdmissionFileStorage fileStorage;
    private final UserRepository userRepository;

    public AdmissionQueryService(
            AdmissionApplicationRepository admissionApplicationRepository,
            CandidateHorseProfileRepository candidateHorseProfileRepository,
            AdmissionDocumentRepository admissionDocumentRepository,
            StableStallRepository stableStallRepository,
            HealthRecordRepository healthRecordRepository,
            AdmissionFileStorage fileStorage,
            UserRepository userRepository) {
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.candidateHorseProfileRepository = candidateHorseProfileRepository;
        this.admissionDocumentRepository = admissionDocumentRepository;
        this.stableStallRepository = stableStallRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.fileStorage = fileStorage;
        this.userRepository = userRepository;
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

    public GroomAdmissionQueueResponse getGroomQueue(String candidateName, AdmissionStatus status,
            java.time.LocalDate submittedFrom, java.time.LocalDate submittedTo, int page, int size) {
        if (page < 0) throw new IllegalArgumentException("Page must be zero or greater");
        if (submittedFrom != null && submittedTo != null && submittedFrom.isAfter(submittedTo)) {
            throw new IllegalArgumentException("Submitted-from date must not be after submitted-to date");
        }
        int pageSize = size <= 0 ? 10 : Math.min(size, 10);
        String nameFilter = candidateName == null || candidateName.isBlank()
                ? "" : candidateName.trim();
        var pageable = PageRequest.of(page, pageSize,
                Sort.by(Sort.Order.desc("submittedAt"), Sort.Order.desc("id")));
        Page<AdmissionApplication> result = admissionApplicationRepository.findGroomQueue(
                status != null,
                status == null ? AdmissionStatus.GROOM_REVIEW : status,
                !nameFilter.isEmpty(),
                nameFilter,
                submittedFrom != null,
                submittedFrom == null ? java.time.LocalDate.of(1, 1, 1).atStartOfDay()
                        : submittedFrom.atStartOfDay(),
                submittedTo != null,
                submittedTo == null ? java.time.LocalDate.of(9999, 12, 31).atTime(23, 59, 59, 999_999_000)
                        : submittedTo.plusDays(1).atStartOfDay(),
                pageable);
        return new GroomAdmissionQueueResponse(
                result.getContent().stream().map(this::toSummaryResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
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
        response.setOwnerName(userRepository.findById(admission.getOwnerId())
                .map(com.rtms.backend.entity.User::getFullName).orElse(null));
        response.setStatus(admission.getStatus());
        response.setQuarantineStallId(admission.getQuarantineStallId());

        response.setCandidate(candidate);
        response.setDocuments(documents.stream().map(this::toDocumentResponse).toList());

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
        long availableQuarantine = stableStallRepository.countAvailableQuarantineStalls();
        long availableRegular = stableStallRepository.countAvailableRegularStalls();
        long occupiedQuarantine = stableStallRepository.countOccupiedQuarantineStalls();
        response.setCapacity(new AdmissionCapacitySummary(
                availableQuarantine, availableRegular, occupiedQuarantine,
                AdmissionCapacityPolicy.isAvailable(availableQuarantine, availableRegular, occupiedQuarantine),
                AdmissionCapacityPolicy.blockingReason(availableQuarantine, availableRegular, occupiedQuarantine)));

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
                .map(fileStorage::downloadUrl)
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

    private AdmissionDocumentResponse toDocumentResponse(AdmissionDocument document) {
        return new AdmissionDocumentResponse(document.getId(), document.getDocumentType(),
                fileStorage.downloadUrl(document), document.getOriginalFileName(), document.getRecordDate(),
                document.getNote(), document.getUploadedAt(), isMedical(document));
    }

    private boolean isMedical(AdmissionDocument document) {
        return switch (document.getDocumentType()) {
            case VACCINATION_RECORD, DEWORMING_RECORD, HEALTH_CERTIFICATE,
                    PREVIOUS_MEDICAL_RECORD, PREVIOUS_INJURY_RECORD -> true;
            default -> false;
        };
    }
}
