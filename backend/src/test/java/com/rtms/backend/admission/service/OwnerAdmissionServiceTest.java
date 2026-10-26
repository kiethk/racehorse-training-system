package com.rtms.backend.admission.service;

import com.rtms.backend.common.storage.FileStorageService;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.service.PedigreeBirthDateValidator;
import com.rtms.backend.admission.dto.AdmissionDocumentMetadataRequest;
import com.rtms.backend.admission.dto.CreateOwnerAdmissionRequest;
import com.rtms.backend.horse.enums.HorseSex;
import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.AdmissionDocument;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.admission.enums.AdmissionDocumentType;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.enums.ReviewDecision;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.AdmissionDocumentRepository;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OwnerAdmissionServiceTest {
    private AdmissionApplicationRepository admissions;
    private CandidateHorseProfileRepository candidates;
    private AdmissionDocumentRepository documents;
    private FileStorageService files;
    private AdmissionUelnPolicy uelnPolicy;
    private HorseRepository horses;
    private OwnerAdmissionService service;
    private AtomicLong sequence;

    @BeforeEach
    void setup() {
        admissions = mock(AdmissionApplicationRepository.class);
        candidates = mock(CandidateHorseProfileRepository.class);
        documents = mock(AdmissionDocumentRepository.class);
        files = mock(FileStorageService.class);
        uelnPolicy = mock(AdmissionUelnPolicy.class);
        horses = mock(HorseRepository.class);
        service = new OwnerAdmissionService(admissions, candidates, documents, files, uelnPolicy,
                new PedigreeBirthDateValidator(horses));
        sequence = new AtomicLong(100);
        when(admissions.save(any())).thenAnswer(inv -> {
            AdmissionApplication app = inv.getArgument(0);
            app.setId(sequence.incrementAndGet());
            return app;
        });
        when(candidates.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateOwnerAdmissionRequest request() {
        return new CreateOwnerAdmissionRequest(
                "Pegasus", "Thoroughbred", LocalDate.of(2022, 4, 2), HorseSex.MALE,
                " 250001ab1234567 ", "SIRE", "Sire", null,
                "Dam", null, "Family notes");
    }

    @Test
    void createsOneApplicationAndOneSnapshotWithoutCreatingHorse() {
        var result = service.create(7L, request());
        assertEquals(AdmissionStatus.GROOM_REVIEW, result.status());
        assertEquals("250001AB1234567", result.candidate().registrationNumber());
        assertEquals("Family notes", result.candidate().pedigreeNotes());
        assertEquals(HorseSex.MALE, result.candidate().sex());
        assertEquals(101L, result.admissionId());
        assertTrue(result.documents().isEmpty());
        var admission = ArgumentCaptor.forClass(AdmissionApplication.class);
        verify(admissions, times(1)).save(admission.capture());
        assertEquals(7L, admission.getValue().getOwnerId());
        assertNull(admission.getValue().getHorseId());
        assertNull(admission.getValue().getQuarantineStallId());
        var candidate = ArgumentCaptor.forClass(CandidateHorseProfile.class);
        verify(candidates, times(1)).save(candidate.capture());
        assertEquals(101L, candidate.getValue().getAdmissionId());
        assertEquals(HorseSex.MALE, candidate.getValue().getSex());
    }

    @Test
    void missingSexIsRejectedBeforeCreatingAnAdmissionOrUploadingFiles() {
        var missing = new CreateOwnerAdmissionRequest("Pegasus", null, null, null,
                null, null, null, null, null, null, null);
        var createError = assertThrows(ResponseStatusException.class, () -> service.create(7L, missing));
        var submitError = assertThrows(ResponseStatusException.class,
                () -> service.submit(7L, missing, List.of(), List.of()));
        assertEquals(HttpStatus.BAD_REQUEST, createError.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, submitError.getStatusCode());
        assertTrue(createError.getReason().contains("Sex"));
        verifyNoInteractions(admissions, candidates, documents, files, uelnPolicy, horses);
    }

    @Test
    void submitRejectsMissingRequiredDocumentBeforeSaving() {
        List<AdmissionDocumentMetadataRequest> metadata = List.of(
                new AdmissionDocumentMetadataRequest(
                        AdmissionDocumentType.HORSE_PHOTO, null, null),
                new AdmissionDocumentMetadataRequest(
                        AdmissionDocumentType.REGISTRATION_DOCUMENT, null, null),
                new AdmissionDocumentMetadataRequest(
                        AdmissionDocumentType.PEDIGREE_CERTIFICATE, null, null),
                new AdmissionDocumentMetadataRequest(
                        AdmissionDocumentType.HEALTH_CERTIFICATE, null, null)
                );

        List<MultipartFile> uploads = List.of(
                new MockMultipartFile(
                        "files", "horse.png", "image/png", new byte[]{1}),
                new MockMultipartFile(
                        "files", "registration.pdf", "application/pdf", new byte[]{2}),
                new MockMultipartFile(
                        "files", "pedigree.pdf", "application/pdf", new byte[]{4}),
                new MockMultipartFile(
                        "files", "health.pdf", "application/pdf", new byte[]{4})
        );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.submit(7L, request(), metadata, uploads)
        );

        assertEquals(400, error.getStatusCode().value());

        verifyNoInteractions(admissions, candidates, documents, files);
    }


    @Test
    void submitRejectsMetadataFileCountMismatchBeforeSaving() {
        List<AdmissionDocumentMetadataRequest> metadata = List.of(
                new AdmissionDocumentMetadataRequest(
                        AdmissionDocumentType.HORSE_PHOTO, null, null)
        );

        List<MultipartFile> uploads = List.of();

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.submit(7L, request(), metadata, uploads)
        );

        assertEquals(400, error.getStatusCode().value());
        assertEquals(
                "Document metadata count must match file count",
                error.getReason()
        );

        verifyNoInteractions(admissions, candidates, documents, files);
    }

    @Test
    void resubmittingRejectedHorseCreatesFreshApplicationAndProfileWithSameUeln() {
        var first = service.create(7L, request());
        var second = service.create(7L, request());
        assertNotEquals(first.admissionId(), second.admissionId());
        assertEquals(first.candidate().registrationNumber(), second.candidate().registrationNumber());
        verify(admissions, times(2)).save(any());
        verify(candidates, times(2)).save(any());
    }

    @Test
    void createChecksNormalizedUelnBeforeCreatingAnyRecords() {
        doThrow(new ApiException(HttpStatus.CONFLICT, AdmissionUelnPolicy.CONFLICT_CODE, "UELN in use"))
                .when(uelnPolicy).assertAvailableForSubmission("250001AB1234567");

        assertThrows(ApiException.class, () -> service.create(8L, request()));
        verifyNoInteractions(admissions, candidates, documents, files);
    }

    @Test
    void createRejectsChildBornBeforeInternalSireWithoutSavingAdmission() {
        Horse sire = new Horse();
        sire.setDateOfBirth(LocalDate.of(2023, 1, 1));
        when(horses.findByRegistrationNumber("999999SIR000001")).thenReturn(List.of(sire));
        var candidate = new CreateOwnerAdmissionRequest("Test horse", null, LocalDate.of(2022, 4, 15), HorseSex.MALE,
                "999999BIR000001", null, "Sire", "999999SIR000001", null, null, null);

        var error = assertThrows(ResponseStatusException.class, () -> service.create(7L, candidate));

        assertEquals(400, error.getStatusCode().value());
        assertTrue(error.getReason().contains("sire"));
        verifyNoInteractions(admissions, candidates, documents, files, uelnPolicy);
    }

    @Test
    void submitRejectsChildBornSameDayAsInternalDamBeforeSavingOrUploading() {
        Horse dam = new Horse();
        dam.setDateOfBirth(LocalDate.of(2022, 4, 15));
        when(horses.findByRegistrationNumber("999999DAM000001")).thenReturn(List.of(dam));
        var candidate = new CreateOwnerAdmissionRequest("Test horse", null, LocalDate.of(2022, 4, 15), HorseSex.MALE,
                "999999BIR000001", null, null, null, "Dam", "999999DAM000001", null);
        var metadata = List.of(
                new AdmissionDocumentMetadataRequest(AdmissionDocumentType.HORSE_PHOTO, null, null),
                new AdmissionDocumentMetadataRequest(AdmissionDocumentType.REGISTRATION_DOCUMENT, null, null),
                new AdmissionDocumentMetadataRequest(AdmissionDocumentType.PEDIGREE_CERTIFICATE, null, null),
                new AdmissionDocumentMetadataRequest(AdmissionDocumentType.VACCINATION_RECORD, null, null));
        List<MultipartFile> uploads = List.of(
                new MockMultipartFile("files", "photo.png", "image/png", new byte[]{1}),
                new MockMultipartFile("files", "registration.pdf", "application/pdf", new byte[]{2}),
                new MockMultipartFile("files", "pedigree.pdf", "application/pdf", new byte[]{3}),
                new MockMultipartFile("files", "vaccination.pdf", "application/pdf", new byte[]{4}));

        var error = assertThrows(ResponseStatusException.class, () -> service.submit(7L, candidate, metadata, uploads));

        assertEquals(400, error.getStatusCode().value());
        assertTrue(error.getReason().contains("dam"));
        verifyNoInteractions(admissions, candidates, documents, uelnPolicy);
        verify(files, never()).store(any(), any());
    }

    @Test
    void submitConflictDoesNotSaveRecordsOrUploadFiles() {
        var metadata = List.of(
                new AdmissionDocumentMetadataRequest(AdmissionDocumentType.HORSE_PHOTO, null, null),
                new AdmissionDocumentMetadataRequest(AdmissionDocumentType.REGISTRATION_DOCUMENT, null, null),
                new AdmissionDocumentMetadataRequest(AdmissionDocumentType.PEDIGREE_CERTIFICATE, null, null),
                new AdmissionDocumentMetadataRequest(AdmissionDocumentType.VACCINATION_RECORD, null, null));
        List<MultipartFile> uploads = List.of(
                new MockMultipartFile("files", "photo.png", "image/png", new byte[]{1}),
                new MockMultipartFile("files", "registration.pdf", "application/pdf", new byte[]{2}),
                new MockMultipartFile("files", "pedigree.pdf", "application/pdf", new byte[]{3}),
                new MockMultipartFile("files", "vaccination.pdf", "application/pdf", new byte[]{4}));
        doThrow(new ApiException(HttpStatus.CONFLICT, AdmissionUelnPolicy.CONFLICT_CODE, "UELN in use"))
                .when(uelnPolicy).assertAvailableForSubmission("250001AB1234567");

        assertThrows(ApiException.class, () -> service.submit(8L, request(), metadata, uploads));
        verifyNoInteractions(admissions, candidates, documents);
        verify(files, never()).store(any(), any());
    }

    @Test
    void ownerCanReadOnlyOwnAdmission() {
        var existing = new AdmissionApplication();
        existing.setId(10L); existing.setOwnerId(7L);
        when(admissions.findById(10L)).thenReturn(Optional.of(existing));
        assertThrows(AccessDeniedException.class, () -> service.getMyAdmission(8L, 10L));
        verify(candidates, never()).findByAdmissionId(anyLong());
    }

    @Test
    void documentUploadRefusedOnceGroomCompletesReview() {
        var application = new AdmissionApplication();
        application.setId(3L); application.setOwnerId(7L);
        application.setGroomId(99L);
        application.setGroomDecision(ReviewDecision.APPROVED);
        when(admissions.findByIdForUpdate(3L)).thenReturn(Optional.of(application));
        var file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1,2,3});
        assertThrows(ResponseStatusException.class, () ->
                service.upload(7L, 3L, AdmissionDocumentType.HORSE_PHOTO,
                        null, null, file));
        verifyNoInteractions(files, documents);
    }

    @Test
    void documentUploadAllowedWhileGroomInReviewEvenIfAssigned() {
        var application = new AdmissionApplication();
        application.setId(3L); application.setOwnerId(7L);
        application.setGroomId(99L);
        application.setStatus(AdmissionStatus.GROOM_REVIEW);
        when(admissions.findByIdForUpdate(3L)).thenReturn(Optional.of(application));
        when(files.store(any(), any())).thenReturn("admissions/3/photo.png");
        when(documents.findByAdmissionIdAndDocumentType(3L, AdmissionDocumentType.HORSE_PHOTO))
                .thenReturn(java.util.Collections.emptyList());
        var savedDoc = new AdmissionDocument();
        savedDoc.setId(101L);
        savedDoc.setAdmissionId(3L);
        savedDoc.setDocumentType(AdmissionDocumentType.HORSE_PHOTO);
        savedDoc.setFileUrl("admissions/3/photo.png");
        when(documents.save(any(AdmissionDocument.class))).thenReturn(savedDoc);

        var file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1,2,3});
        var resp = service.upload(7L, 3L, AdmissionDocumentType.HORSE_PHOTO, null, null, file);
        assertNotNull(resp);
        assertEquals(101L, resp.getId());
    }

    @Test
    void documentUploadOverwritesExistingDocumentOfSameType() {
        var application = new AdmissionApplication();
        application.setId(3L); application.setOwnerId(7L);
        application.setStatus(AdmissionStatus.GROOM_REVIEW);
        when(admissions.findByIdForUpdate(3L)).thenReturn(Optional.of(application));
        when(files.store(any(), any())).thenReturn("admissions/3/new_photo.png");

        var existingDoc = new AdmissionDocument();
        existingDoc.setId(50L);
        existingDoc.setAdmissionId(3L);
        existingDoc.setDocumentType(AdmissionDocumentType.HORSE_PHOTO);
        existingDoc.setFileUrl("admissions/3/old_photo.png");

        when(documents.findByAdmissionIdAndDocumentType(3L, AdmissionDocumentType.HORSE_PHOTO))
                .thenReturn(java.util.List.of(existingDoc));
        when(documents.save(any(AdmissionDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var file = new MockMultipartFile("file", "new_photo.png", "image/png", new byte[]{4,5,6});
        var resp = service.upload(7L, 3L, AdmissionDocumentType.HORSE_PHOTO, null, "updated note", file);

        assertNotNull(resp);
        assertEquals(50L, resp.getId());
        assertEquals("/api/admissions/3/documents/50/file", resp.getFileUrl());
        assertEquals("admissions/3/new_photo.png", existingDoc.getFileUrl());
        verify(files).delete("admissions/3/old_photo.png");
    }

    @Test
    void uploadCannotWriteIntoSomeoneElsesAdmission() {
        var application = new AdmissionApplication();
        application.setId(3L); application.setOwnerId(7L);
        when(admissions.findByIdForUpdate(3L)).thenReturn(Optional.of(application));
        var file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1,2,3});
        assertThrows(AccessDeniedException.class, () ->
                service.upload(8L, 3L, AdmissionDocumentType.HORSE_PHOTO,
                        null, null, file));
        verifyNoInteractions(files, documents);
    }

    @Test
    void allowedUploadCreatesDocumentForCorrectAdmission() {
        var application = new AdmissionApplication();
        application.setId(3L); application.setOwnerId(7L);
        when(admissions.findByIdForUpdate(3L)).thenReturn(Optional.of(application));
        when(files.store(any(), any())).thenReturn("local:test.jpg");
        when(documents.save(any())).thenAnswer(inv -> {
            AdmissionDocument doc = inv.getArgument(0);
            doc.setId(33L); return doc;
        });
        var file = new MockMultipartFile("file", "C:\\fakepath\\photo.jpg", "image/jpeg", new byte[]{1,2,3});
        var result = service.upload(7L, 3L, AdmissionDocumentType.HORSE_PHOTO,
                LocalDate.of(2026, 9, 1), "Arrival", file);
        assertEquals(33L, result.getId());
        assertEquals("/api/admissions/3/documents/33/file", result.getFileUrl());
        assertEquals("photo.jpg", result.getOriginalFileName());
        verify(documents, times(1)).save(argThat(d -> d.getAdmissionId() == 3L));
    }

    @Test
    void ownerListIncludesOnlyOwnAdmissions() {
        var a = new AdmissionApplication();
        a.setId(1L); a.setOwnerId(7L);
        a.setStatus(AdmissionStatus.REJECTED);
        when(admissions.findByOwnerIdOrderBySubmittedAtDesc(7L)).thenReturn(List.of(a));
        var c = new CandidateHorseProfile();
        c.setName("Pegasus");
        when(candidates.findByAdmissionId(1L)).thenReturn(Optional.of(c));
        var list = service.getMyAdmissions(7L, null);
        assertEquals(1, list.size());
        assertEquals("Pegasus", list.getFirst().getCandidateName());
        verify(admissions).findByOwnerIdOrderBySubmittedAtDesc(7L);
    }

    @Test
    void createRejectsInvalidUelnFormat() {
        var invalidRequest = new CreateOwnerAdmissionRequest(
                "Pegasus", "Thoroughbred", LocalDate.of(2022, 4, 2), HorseSex.MALE,
                "INVALID_UELN_123", "SIRE", "Sire", null,
                "Dam", null, "Notes");
        var ex = assertThrows(ResponseStatusException.class, () -> service.create(7L, invalidRequest));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("UELN"));
    }

    @Test
    void createAcceptsIsoAlpha3UelnFormat() {
        var validRequest = new CreateOwnerAdmissionRequest(
                "Pegasus", "Thoroughbred", LocalDate.of(2022, 4, 2), HorseSex.MALE,
                "VNM22SC00000001", "SIRE", "Midnight Shadow", "VNM17MS00000002",
                "Dam", null, "Notes");
        assertDoesNotThrow(() -> service.create(7L, validRequest));
    }

    @Test
    void createRejectsChildSameAsSireUeln() {
        var conflictingRequest = new CreateOwnerAdmissionRequest(
                "Pegasus", "Thoroughbred", LocalDate.of(2022, 4, 2), HorseSex.MALE,
                "250001123456789", "SIRE", "Sire", "250001123456789",
                "Dam", null, "Notes");
        var ex = assertThrows(ResponseStatusException.class, () -> service.create(7L, conflictingRequest));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Child UELN and sire UELN cannot be the same"));
    }

    @Test
    void createRejectsChildSameAsDamUeln() {
        var conflictingRequest = new CreateOwnerAdmissionRequest(
                "Pegasus", "Thoroughbred", LocalDate.of(2022, 4, 2), HorseSex.MALE,
                "250001123456789", "SIRE", "Sire", null,
                "Dam", "250001123456789", "Notes");
        var ex = assertThrows(ResponseStatusException.class, () -> service.create(7L, conflictingRequest));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Child UELN and dam UELN cannot be the same"));
    }

    @Test
    void createRejectsSireSameAsDamUeln() {
        var conflictingRequest = new CreateOwnerAdmissionRequest(
                "Pegasus", "Thoroughbred", LocalDate.of(2022, 4, 2), HorseSex.MALE,
                "250001123456789", "SIRE", "Sire", "250001999999999",
                "Dam", "250001999999999", "Notes");
        var ex = assertThrows(ResponseStatusException.class, () -> service.create(7L, conflictingRequest));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Sire UELN and dam UELN cannot be the same"));
    }
}
