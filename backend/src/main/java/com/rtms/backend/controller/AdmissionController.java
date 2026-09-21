package com.rtms.backend.controller;

import com.rtms.backend.dto.AdmissionDetailResponse;
import com.rtms.backend.dto.AdmissionDocumentResponse;
import com.rtms.backend.dto.AdmissionSummaryResponse;
import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.VetReviewRequest;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.AdmissionDocument;
import com.rtms.backend.entity.CandidateHorseProfile;
import com.rtms.backend.enums.AdmissionDocumentType;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.AdmissionDocumentRepository;
import com.rtms.backend.repository.CandidateHorseProfileRepository;
import com.rtms.backend.repository.StableStallRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.service.AdmissionReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/admissions")
public class AdmissionController {

    private static final Set<AdmissionDocumentType> MEDICAL_DOCUMENT_TYPES = EnumSet.of(
            AdmissionDocumentType.VACCINATION_RECORD,
            AdmissionDocumentType.DEWORMING_RECORD,
            AdmissionDocumentType.HEALTH_CERTIFICATE,
            AdmissionDocumentType.PREVIOUS_MEDICAL_RECORD,
            AdmissionDocumentType.PREVIOUS_INJURY_RECORD);

    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final CandidateHorseProfileRepository candidateHorseProfileRepository;
    private final AdmissionDocumentRepository admissionDocumentRepository;
    private final StableStallRepository stableStallRepository;
    private final AdmissionReviewService admissionReviewService;

    public AdmissionController(
            AdmissionApplicationRepository admissionApplicationRepository,
            CandidateHorseProfileRepository candidateHorseProfileRepository,
            AdmissionDocumentRepository admissionDocumentRepository,
            StableStallRepository stableStallRepository,
            AdmissionReviewService admissionReviewService) {
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.candidateHorseProfileRepository = candidateHorseProfileRepository;
        this.admissionDocumentRepository = admissionDocumentRepository;
        this.stableStallRepository = stableStallRepository;
        this.admissionReviewService = admissionReviewService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_VIEW') and hasAuthority('STABLE_STALL_VIEW')")
    public ApiResponse<List<AdmissionSummaryResponse>> getAdmissions(
            @RequestParam(defaultValue = "VET_REVIEW") AdmissionStatus status) {
        List<AdmissionSummaryResponse> admissions = admissionApplicationRepository.findByStatus(status)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
        return ApiResponse.success(admissions);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_VIEW') and hasAuthority('STABLE_STALL_VIEW')")
    public ApiResponse<AdmissionDetailResponse> getAdmission(@PathVariable Long id) {
        AdmissionApplication admission = findAdmission(id);
        CandidateHorseProfile candidate = findCandidate(id);

        return ApiResponse.success(toDetailResponse(admission, candidate));
    }

    @PostMapping("/{id}/vet-review")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_VET_REVIEW')")
    public ApiResponse<AdmissionDetailResponse> reviewByVet(
            @PathVariable Long id,
            @Valid @RequestBody VetReviewRequest request) {
        AuthenticatedUser currentUser = (AuthenticatedUser) Objects.requireNonNull(
                SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        AdmissionApplication admission = admissionReviewService.reviewByVet(
                id, request, currentUser.getUserId());
        return ApiResponse.success(toDetailResponse(admission, findCandidate(id)));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Object>> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiResponse.error(ex.getReason()));
    }

    private AdmissionDetailResponse toDetailResponse(
            AdmissionApplication admission,
            CandidateHorseProfile candidate) {
        return new AdmissionDetailResponse(
                admission.getId(),
                admission.getStatus(),
                admission.getOwnerId(),
                findStallCode(admission.getQuarantineStallId()),
                candidate,
                admission.getGroomDecision(),
                admission.getGroomFeedback(),
                admission.getGroomReviewedAt(),
                admission.getVetDecision(),
                admission.getVetFeedback(),
                admission.getVetReviewedAt(),
                admission.getPhysicalExamConfirmedAt(),
                admission.getVetReviewedBy(),
                admission.getTrainerDecision(),
                admission.getTrainerFeedback(),
                admission.getTrainerReviewedAt(),
                admission.getManagerDecision(),
                admission.getManagerFeedback(),
                admission.getManagerReviewedAt(),
                admission.getSubmittedAt(),
                admission.getCreatedAt(),
                admission.getUpdatedAt());
    }

    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAuthority('ADMISSION_DOCUMENT_VIEW')")
    public ApiResponse<List<AdmissionDocumentResponse>> getDocuments(@PathVariable Long id) {
        findAdmission(id);
        List<AdmissionDocumentResponse> documents = admissionDocumentRepository.findByAdmissionId(id)
                .stream()
                .map(this::toDocumentResponse)
                .toList();
        return ApiResponse.success(documents);
    }

    private AdmissionSummaryResponse toSummaryResponse(AdmissionApplication admission) {
        CandidateHorseProfile candidate = findCandidate(admission.getId());
        return new AdmissionSummaryResponse(
                admission.getId(),
                admission.getStatus(),
                candidate.getName(),
                candidate.getBreed(),
                findStallCode(admission.getQuarantineStallId()));
    }

    private AdmissionDocumentResponse toDocumentResponse(AdmissionDocument document) {
        return new AdmissionDocumentResponse(
                document.getId(),
                document.getDocumentType(),
                document.getFileUrl(),
                document.getRecordDate(),
                document.getNote(),
                document.getUploadedAt(),
                MEDICAL_DOCUMENT_TYPES.contains(document.getDocumentType()));
    }

    private AdmissionApplication findAdmission(Long id) {
        return admissionApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admission application not found with id: " + id));
    }

    private CandidateHorseProfile findCandidate(Long admissionId) {
        return candidateHorseProfileRepository.findByAdmissionId(admissionId)
                .orElseThrow(() -> new RuntimeException(
                        "Candidate horse profile not found for admission id: " + admissionId));
    }

    private String findStallCode(Long stallId) {
        if (stallId == null) {
            return null;
        }
        return stableStallRepository.findById(stallId)
                .orElseThrow(() -> new RuntimeException("Stable stall not found with id: " + stallId))
                .getStallCode();
    }
}
