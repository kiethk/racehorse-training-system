package com.rtms.backend.horse.service;

import com.rtms.backend.admission.enums.AdmissionDocumentType;
import com.rtms.backend.admission.enums.ReviewDecision;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.AdmissionDocumentRepository;
import com.rtms.backend.common.storage.FileStorageService;
import com.rtms.backend.horse.dto.CreateHorsePedigreeRequest;
import com.rtms.backend.horse.dto.HorseEditFields;
import com.rtms.backend.horse.dto.HorseEditReviewRequest;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.entity.HorseEditRequest;
import com.rtms.backend.horse.entity.HorseEditRequestDocument;
import com.rtms.backend.horse.entity.HorsePedigree;
import com.rtms.backend.horse.enums.HorseEditRequestStatus;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseEditRequestDocumentRepository;
import com.rtms.backend.horse.repository.HorseEditRequestRepository;
import com.rtms.backend.horse.repository.HorsePedigreeRepository;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.notification.service.NotificationTypes;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HorseEditRequestServiceTest {

    private static final long OWNER = 7L;
    private static final long GROOM = 31L;
    private static final long HORSE = 40L;
    private static final long STALL = 500L;

    private HorseEditRequestRepository requests;
    private HorseEditRequestDocumentRepository documents;
    private HorseRepository horses;
    private HorsePedigreeRepository pedigrees;
    private HorsePedigreeService pedigreeService;
    private StableStallRepository stalls;
    private FileStorageService storage;
    private NotificationService notifications;
    private HorseEditRequestService service;
    private Horse horse;

    @BeforeEach
    void setUp() {
        requests = mock(HorseEditRequestRepository.class);
        documents = mock(HorseEditRequestDocumentRepository.class);
        horses = mock(HorseRepository.class);
        pedigrees = mock(HorsePedigreeRepository.class);
        pedigreeService = mock(HorsePedigreeService.class);
        stalls = mock(StableStallRepository.class);
        storage = mock(FileStorageService.class);
        notifications = mock(NotificationService.class);
        service = new HorseEditRequestService(requests, documents, horses, pedigrees, pedigreeService,
                new PedigreeBirthDateValidator(horses), stalls, mock(AdmissionApplicationRepository.class),
                mock(AdmissionDocumentRepository.class), storage, notifications, mock(UserRepository.class));

        horse = new Horse();
        horse.setId(HORSE);
        horse.setOwnerId(OWNER);
        horse.setName("Mercury");
        horse.setBreed("Arabian");
        horse.setRegistrationNumber("999999TST000001");
        horse.setDateOfBirth(LocalDate.of(2021, 1, 1));
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        horse.setCurrentStallId(STALL);
        when(horses.findByIdForUpdate(HORSE)).thenReturn(Optional.of(horse));
        when(horses.findById(HORSE)).thenReturn(Optional.of(horse));
        StableStall stall = new StableStall();
        stall.setId(STALL);
        stall.setGroomId(GROOM);
        when(stalls.findById(STALL)).thenReturn(Optional.of(stall));
        when(requests.saveAndFlush(any())).thenAnswer(inv -> {
            HorseEditRequest r = inv.getArgument(0);
            r.setId(900L);
            return r;
        });
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private HorseEditFields fields(String name, String sireUeln) {
        return new HorseEditFields(name, "Arabian", null, sireUeln, null, null);
    }

    // ── Submit ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Submit stores the request and its file, records originals and notifies the stall groom")
    void submitStoresRequestAndNotifiesGroom() {
        when(storage.store(any(), anyString())).thenReturn("local:new-photo.png");
        var photo = new MockMultipartFile("files", "photo.png", "image/png", new byte[]{1});

        var response = service.submit(OWNER, HORSE, fields(" Mercury Star ", null),
                List.of(AdmissionDocumentType.HORSE_PHOTO), List.of(photo));

        assertEquals(HorseEditRequestStatus.PENDING, response.status());
        assertEquals("Mercury Star", response.requested().name());
        assertEquals("Mercury", response.original().name());
        assertEquals("Mercury", horse.getName(), "official record untouched until approval");
        verify(storage).store(same(photo), eq("rtms/horses/40/edit-requests/900"));
        var saved = ArgumentCaptor.forClass(HorseEditRequestDocument.class);
        verify(documents).save(saved.capture());
        assertFalse(saved.getValue().isCurrent());
        verify(notifications).createNotification(eq(GROOM), anyString(), anyString(),
                eq(NotificationTypes.HORSE_EDIT_REQUESTED), eq(NotificationTypes.REFERENCE_HORSE_EDIT_REQUEST),
                eq(900L), anyString());
    }

    @Test
    void submitIsRefusedForAnotherOwnersHorse() {
        assertThrows(AccessDeniedException.class, () -> service.submit(8L, HORSE, fields("X", null), null, null));
        verify(requests, never()).saveAndFlush(any());
    }

    @Test
    void submitIsRefusedWhenHorseIsNotEligibleHasNoGroomOrAlreadyHasPendingRequest() {
        horse.setCurrentStatus(HorseStatus.CANDIDATE);
        assertEquals(409, status(() -> service.submit(OWNER, HORSE, fields("X", null), null, null)));

        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        StableStall noGroom = new StableStall();
        noGroom.setId(STALL);
        when(stalls.findById(STALL)).thenReturn(Optional.of(noGroom));
        assertEquals(409, status(() -> service.submit(OWNER, HORSE, fields("X", null), null, null)));

        StableStall staffed = new StableStall();
        staffed.setId(STALL);
        staffed.setGroomId(GROOM);
        when(stalls.findById(STALL)).thenReturn(Optional.of(staffed));
        when(requests.existsByHorseIdAndStatus(HORSE, HorseEditRequestStatus.PENDING)).thenReturn(true);
        assertEquals(409, status(() -> service.submit(OWNER, HORSE, fields("X", null), null, null)));
        verify(requests, never()).saveAndFlush(any());
    }

    @Test
    void submitWithoutAnyChangeIsRejected() {
        assertEquals(400, status(() -> service.submit(OWNER, HORSE, fields("Mercury", null), null, null)));
    }

    @Test
    @DisplayName("Parent UELN rules: sire equal to the horse itself is rejected before anything is stored")
    void submitRejectsHorseAsItsOwnSire() {
        assertEquals(400, status(() -> service.submit(OWNER, HORSE, fields("Mercury", "999999TST000001"), null, null)));
        verifyNoInteractions(storage);
    }

    @Test
    void submitRunsTheSharedParentSexRule() {
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "The sire must be a male horse"))
                .when(pedigreeService).validateParentSexesByUeln("999999SIR000001", null);

        assertEquals(400, status(() -> service.submit(OWNER, HORSE, fields("Mercury", "999999SIR000001"), null, null)));
    }

    @Test
    void recordedParentsCannotBeErased() {
        HorsePedigree pedigree = new HorsePedigree();
        pedigree.setSireRegistrationNumber("999999SIR000001");
        when(pedigrees.findByHorseId(HORSE)).thenReturn(Optional.of(pedigree));

        assertEquals(400, status(() -> service.submit(OWNER, HORSE, fields("Mercury", " "), null, null)));
    }

    @Test
    void uploadsNeedOneTypePerFileAndImagesForPhotos() {
        var pdf = new MockMultipartFile("files", "photo.pdf", "application/pdf", new byte[]{1});
        assertEquals(400, status(() -> service.submit(OWNER, HORSE, fields("New", null),
                List.of(), List.of(pdf))));
        assertEquals(400, status(() -> service.submit(OWNER, HORSE, fields("New", null),
                List.of(AdmissionDocumentType.HORSE_PHOTO), List.of(pdf))));
        verify(storage, never()).store(any(), anyString());
    }

    @Test
    @DisplayName("A failure after storing a file removes the stored file on rollback")
    void storedFilesAreDeletedOnRollback() {
        when(storage.store(any(), anyString())).thenReturn("local:orphan.png");
        when(documents.save(any())).thenThrow(new IllegalStateException("db down"));
        var photo = new MockMultipartFile("files", "p.png", "image/png", new byte[]{1});

        assertThrows(IllegalStateException.class, () -> service.submit(OWNER, HORSE, fields("New", null),
                List.of(AdmissionDocumentType.HORSE_PHOTO), List.of(photo)));
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(storage).delete("local:orphan.png");
    }

    // ── Review ───────────────────────────────────────────────────────────────

    private HorseEditRequest pending(String requestedName) {
        HorseEditRequest r = new HorseEditRequest();
        r.setId(900L);
        r.setHorseId(HORSE);
        r.setOwnerId(OWNER);
        r.setStatus(HorseEditRequestStatus.PENDING);
        r.setOriginalName("Mercury");
        r.setOriginalBreed("Arabian");
        r.setRequestedName(requestedName);
        r.setRequestedBreed("Arabian");
        when(requests.findByIdForUpdate(900L)).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    @DisplayName("BR-02: rejecting needs feedback; with feedback the pending files are deleted only after commit")
    void rejectRequiresFeedbackAndDeletesFilesAfterCommit() {
        HorseEditRequest r = pending("Mercury Star");
        HorseEditRequestDocument file = new HorseEditRequestDocument();
        file.setFileUrl("local:pending.png");
        when(documents.findByRequestIdOrderByIdAsc(900L)).thenReturn(List.of(file));

        assertEquals(400, status(() -> service.review(GROOM, 900L, new HorseEditReviewRequest(ReviewDecision.REJECTED, "  "))));

        service.review(GROOM, 900L, new HorseEditReviewRequest(ReviewDecision.REJECTED, "Blurry photo"));
        assertEquals(HorseEditRequestStatus.REJECTED, r.getStatus());
        assertEquals("Mercury", horse.getName());
        verify(storage, never()).delete(anyString());
        complete(TransactionSynchronization.STATUS_COMMITTED);
        verify(storage).delete("local:pending.png");
    }

    @Test
    void onlyTheGroomOfTheHorsesStallMayReview() {
        pending("Mercury Star");
        assertThrows(AccessDeniedException.class,
                () -> service.review(99L, 900L, new HorseEditReviewRequest(ReviewDecision.APPROVED, null)));
        assertEquals("Mercury", horse.getName());
    }

    @Test
    @DisplayName("Approve refuses a stale request when the horse changed after submission")
    void approveRefusesStaleRequest() {
        pending("Mercury Star");
        horse.setName("Renamed by manager");

        assertEquals(409, status(() -> service.review(GROOM, 900L, new HorseEditReviewRequest(ReviewDecision.APPROVED, null))));
        verify(horses, never()).save(any());
    }

    @Test
    @DisplayName("Approve applies the change, promotes the new file and deletes the replaced one after commit")
    void approveAppliesChangesAndReplacesDocumentAfterCommit() {
        HorseEditRequest r = pending("Mercury Star");
        r.setRequestedSireRegistrationNumber("999999SIR000001");
        HorseEditRequestDocument incoming = new HorseEditRequestDocument();
        incoming.setHorseId(HORSE);
        incoming.setDocumentType(AdmissionDocumentType.HORSE_PHOTO);
        incoming.setFileUrl("local:new.png");
        HorseEditRequestDocument previous = new HorseEditRequestDocument();
        previous.setHorseId(HORSE);
        previous.setDocumentType(AdmissionDocumentType.HORSE_PHOTO);
        previous.setFileUrl("local:old.png");
        previous.setCurrent(true);
        when(documents.findByRequestIdOrderByIdAsc(900L)).thenReturn(List.of(incoming));
        when(documents.findFirstByHorseIdAndDocumentTypeAndCurrentTrue(HORSE, AdmissionDocumentType.HORSE_PHOTO))
                .thenReturn(Optional.of(previous));

        service.review(GROOM, 900L, new HorseEditReviewRequest(ReviewDecision.APPROVED, null));

        assertEquals(HorseEditRequestStatus.APPROVED, r.getStatus());
        assertEquals(GROOM, r.getReviewerId());
        assertEquals("Mercury Star", horse.getName());
        assertEquals("999999TST000001", horse.getRegistrationNumber(), "UELN is never editable");
        var pedigree = ArgumentCaptor.forClass(CreateHorsePedigreeRequest.class);
        verify(pedigreeService).createOrUpdate(eq(HORSE), pedigree.capture());
        assertEquals("999999SIR000001", pedigree.getValue().getSireRegistrationNumber());
        assertEquals("999999TST000001", pedigree.getValue().getRegistrationNumber());
        assertTrue(incoming.isCurrent());
        assertFalse(previous.isCurrent());
        assertNotNull(previous.getSupersededAt());
        verify(storage, never()).delete(anyString());
        complete(TransactionSynchronization.STATUS_COMMITTED);
        verify(storage).delete("local:old.png");
        verify(storage, never()).delete("local:new.png");
        verify(notifications).createNotification(eq(OWNER), anyString(), anyString(),
                eq(NotificationTypes.HORSE_EDIT_REVIEWED), eq(NotificationTypes.REFERENCE_HORSE), eq(HORSE), anyString());
    }

    @Test
    void approveWithoutParentChangeDoesNotTouchPedigree() {
        pending("Mercury Star");

        service.review(GROOM, 900L, new HorseEditReviewRequest(ReviewDecision.APPROVED, null));

        verify(pedigreeService, never()).createOrUpdate(anyLong(), any());
    }

    @Test
    void decidedRequestsCannotBeReviewedOrCancelledAgain() {
        HorseEditRequest r = pending("Mercury Star");
        r.setStatus(HorseEditRequestStatus.APPROVED);

        assertEquals(409, status(() -> service.review(GROOM, 900L, new HorseEditReviewRequest(ReviewDecision.APPROVED, null))));
        assertEquals(409, status(() -> service.cancel(OWNER, HORSE, 900L)));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static int status(org.junit.jupiter.api.function.Executable call) {
        return assertThrows(ResponseStatusException.class, call).getStatusCode().value();
    }

    private static void complete(int status) {
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            if (status == TransactionSynchronization.STATUS_COMMITTED) sync.afterCommit();
            sync.afterCompletion(status);
        }
    }
}
