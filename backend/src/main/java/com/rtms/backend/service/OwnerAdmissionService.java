package com.rtms.backend.service;

import com.rtms.backend.dto.*;
import com.rtms.backend.entity.*;
import com.rtms.backend.enums.*;
import com.rtms.backend.repository.*;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class OwnerAdmissionService {
    private static final Set<AdmissionDocumentType> MEDICAL_TYPES = EnumSet.of(
            AdmissionDocumentType.VACCINATION_RECORD, AdmissionDocumentType.DEWORMING_RECORD,
            AdmissionDocumentType.HEALTH_CERTIFICATE, AdmissionDocumentType.PREVIOUS_MEDICAL_RECORD,
            AdmissionDocumentType.PREVIOUS_INJURY_RECORD);

    private final AdmissionApplicationRepository admissions;
    private final CandidateHorseProfileRepository candidates;
    private final AdmissionDocumentRepository documents;
    private final AdmissionFileStorage fileStorage;

    public OwnerAdmissionService(AdmissionApplicationRepository admissions,
            CandidateHorseProfileRepository candidates, AdmissionDocumentRepository documents,
            AdmissionFileStorage fileStorage) {
        this.admissions = admissions;
        this.candidates = candidates;
        this.documents = documents;
        this.fileStorage = fileStorage;
    }

    /** The transaction creates exactly one new Application and one new immutable snapshot. */
    @Transactional
    public OwnerAdmissionDetailResponse create(Long ownerId, CreateOwnerAdmissionRequest request) {
        AdmissionApplication application = new AdmissionApplication();
        application.setOwnerId(ownerId);
        application.setStatus(AdmissionStatus.GROOM_REVIEW);
        application = admissions.save(application);

        CandidateHorseProfile snapshot = new CandidateHorseProfile();
        snapshot.setAdmissionId(application.getId());
        snapshot.setName(request.name().trim());
        snapshot.setBreed(trimNullable(request.breed()));
        snapshot.setDateOfBirth(request.dateOfBirth());
        snapshot.setRegistrationNumber(ueln(request.registrationNumber()));
        snapshot.setRegistryName(trimNullable(request.registryName()));
        snapshot.setSireName(trimNullable(request.sireName()));
        snapshot.setSireRegistrationNumber(ueln(request.sireRegistrationNumber()));
        snapshot.setDamName(trimNullable(request.damName()));
        snapshot.setDamRegistrationNumber(ueln(request.damRegistrationNumber()));
        snapshot.setPedigreeNotes(trimNullable(request.pedigreeNotes()));
        candidates.save(snapshot);

        return toDetail(application, snapshot, List.of());
    }

    @Transactional(readOnly = true)
    public List<AdmissionSummaryResponse> getMyAdmissions(Long ownerId, AdmissionStatus status) {
        return admissions.findByOwnerIdOrderBySubmittedAtDesc(ownerId).stream()
                .filter(admission -> status == null || admission.getStatus() == status)
                .map(admission -> {
                    CandidateHorseProfile candidate = findCandidate(admission.getId());
                    return new AdmissionSummaryResponse(admission.getId(), admission.getStatus(),
                            candidate.getName(), candidate.getBreed(), candidate.getDateOfBirth(),
                            admission.getSubmittedAt(), admission.getQuarantineStallId());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public OwnerAdmissionDetailResponse getMyAdmission(Long ownerId, Long admissionId) {
        AdmissionApplication application = ownedAdmission(ownerId, admissionId);
        return toDetail(application, findCandidate(admissionId), documents.findByAdmissionId(admissionId));
    }

    @Transactional(readOnly = true)
    public List<AdmissionDocumentResponse> getMyDocuments(Long ownerId, Long admissionId) {
        ownedAdmission(ownerId, admissionId);
        return documents.findByAdmissionId(admissionId).stream().map(this::toDocumentResponse).toList();
    }

    @Transactional
    public AdmissionDocumentResponse upload(Long ownerId, Long admissionId,
            AdmissionDocumentType type, LocalDate recordDate, String note, MultipartFile file) {
        AdmissionApplication application = admissions.findByIdForUpdate(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission not found"));
        assertOwner(ownerId, application);
        // No document changes after Groom has claimed/started reviewing this application.
        if (application.getStatus() != AdmissionStatus.GROOM_REVIEW
                || application.getGroomId() != null
                || application.getGroomDecision() != null
                || application.getGroomReviewedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Admission documents are locked after Groom starts review");
        }
        if (note != null && note.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document note is too long");
        }
        String key = fileStorage.store(file);
        try {
            AdmissionDocument document = new AdmissionDocument();
            document.setAdmissionId(admissionId);
            document.setDocumentType(type);
            document.setFileUrl(key);
            document.setRecordDate(recordDate);
            document.setNote(trimNullable(note));
            return toDocumentResponse(documents.save(document));
        } catch (RuntimeException ex) {
            fileStorage.deleteIfLocal(key);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public AdmissionDocument getDocumentForViewer(Long admissionId, Long documentId,
            AuthenticatedUser viewer) {
        AdmissionApplication application = admissions.findById(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission not found"));
        if ("HORSE_OWNER".equals(viewer.getRole())) {
            assertOwner(viewer.getUserId(), application);
        }
        AdmissionDocument document = documents.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        if (!admissionId.equals(document.getAdmissionId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        return document;
    }

    @Transactional(readOnly = true)
    public void assertViewerCanRead(Long admissionId, AuthenticatedUser viewer) {
        if ("HORSE_OWNER".equals(viewer.getRole())) {
            ownedAdmission(viewer.getUserId(), admissionId);
        } else if (!admissions.existsById(admissionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission not found");
        }
    }

    public AdmissionDocumentResponse toDocumentResponse(AdmissionDocument document) {
        return new AdmissionDocumentResponse(document.getId(), document.getDocumentType(),
                fileStorage.downloadUrl(document), document.getRecordDate(), document.getNote(),
                document.getUploadedAt(), MEDICAL_TYPES.contains(document.getDocumentType()));
    }

    private OwnerAdmissionDetailResponse toDetail(AdmissionApplication a, CandidateHorseProfile c,
            List<AdmissionDocument> docs) {
        var candidate = new OwnerAdmissionDetailResponse.CandidateSnapshot(c.getName(), c.getBreed(),
                c.getDateOfBirth(), c.getRegistrationNumber(), c.getRegistryName(),
                c.getSireName(), c.getSireRegistrationNumber(),
                c.getDamName(), c.getDamRegistrationNumber(), c.getPedigreeNotes());
        return new OwnerAdmissionDetailResponse(a.getId(), a.getStatus(), a.getSubmittedAt(),
                candidate, docs.stream().map(this::toDocumentResponse).toList(),
                a.getGroomFeedback(), a.getVetFeedback(), a.getTrainerFeedback(), a.getManagerFeedback());
    }

    private CandidateHorseProfile findCandidate(Long admissionId) {
        return candidates.findByAdmissionId(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate snapshot not found"));
    }

    private AdmissionApplication ownedAdmission(Long ownerId, Long admissionId) {
        AdmissionApplication application = admissions.findById(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission not found"));
        assertOwner(ownerId, application);
        return application;
    }

    private void assertOwner(Long ownerId, AdmissionApplication application) {
        if (!application.getOwnerId().equals(ownerId)) {
            throw new AccessDeniedException("This admission belongs to another owner");
        }
    }

    private static String trimNullable(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private static String ueln(String value) {
        String normalized = trimNullable(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
