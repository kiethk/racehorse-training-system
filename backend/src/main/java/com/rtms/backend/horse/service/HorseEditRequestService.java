package com.rtms.backend.horse.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.AdmissionDocument;
import com.rtms.backend.admission.enums.AdmissionDocumentType;
import com.rtms.backend.admission.enums.ReviewDecision;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.AdmissionDocumentRepository;
import com.rtms.backend.admission.service.AdmissionGroomReviewService;
import com.rtms.backend.common.storage.FileStorageService;
import com.rtms.backend.horse.dto.CreateHorsePedigreeRequest;
import com.rtms.backend.horse.dto.HorseDocumentSlotResponse;
import com.rtms.backend.horse.dto.HorseEditFields;
import com.rtms.backend.horse.dto.HorseEditRequestResponse;
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
import com.rtms.backend.horse.util.UelnUtils;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.notification.entity.Notification;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.notification.service.NotificationTypes;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Owner-requested changes to an ELIGIBLE horse, applied only after the groom of the
 * horse's stall approves. Files are stored on submit, become official on approval,
 * and are deleted only after the deciding transaction commits.
 */
@Service
public class HorseEditRequestService {

    private static final Logger log = LoggerFactory.getLogger(HorseEditRequestService.class);
    static final String SOURCE_ADMISSION = "ADMISSION";
    static final String SOURCE_EDIT_REQUEST = "EDIT_REQUEST";
    private static final int MAX_TEXT = 255;

    private final HorseEditRequestRepository requests;
    private final HorseEditRequestDocumentRepository documents;
    private final HorseRepository horses;
    private final HorsePedigreeRepository pedigrees;
    private final HorsePedigreeService pedigreeService;
    private final PedigreeBirthDateValidator birthDates;
    private final StableStallRepository stalls;
    private final AdmissionApplicationRepository admissions;
    private final AdmissionDocumentRepository admissionDocuments;
    private final FileStorageService fileStorage;
    private final NotificationService notifications;
    private final UserRepository users;

    public HorseEditRequestService(HorseEditRequestRepository requests, HorseEditRequestDocumentRepository documents,
                                   HorseRepository horses, HorsePedigreeRepository pedigrees,
                                   HorsePedigreeService pedigreeService, PedigreeBirthDateValidator birthDates,
                                   StableStallRepository stalls, AdmissionApplicationRepository admissions,
                                   AdmissionDocumentRepository admissionDocuments, FileStorageService fileStorage,
                                   NotificationService notifications, UserRepository users) {
        this.requests = requests;
        this.documents = documents;
        this.horses = horses;
        this.pedigrees = pedigrees;
        this.pedigreeService = pedigreeService;
        this.birthDates = birthDates;
        this.stalls = stalls;
        this.admissions = admissions;
        this.admissionDocuments = admissionDocuments;
        this.fileStorage = fileStorage;
        this.notifications = notifications;
        this.users = users;
    }

    // ── Owner ────────────────────────────────────────────────────────────────

    @Transactional
    public HorseEditRequestResponse submit(Long ownerId, Long horseId, HorseEditFields fields,
                                           List<AdmissionDocumentType> documentTypes, List<MultipartFile> files) {
        Horse horse = horses.findByIdForUpdate(horseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Horse not found"));
        if (!Objects.equals(horse.getOwnerId(), ownerId)) {
            throw new AccessDeniedException("You can only edit horses you own");
        }
        if (horse.getCurrentStatus() != HorseStatus.ELIGIBLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only eligible horses can be edited");
        }
        Long groomId = groomOf(horse);
        if (groomId == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This horse's stall has no groom yet, so there is nobody to review changes");
        }
        if (requests.existsByHorseIdAndStatus(horseId, HorseEditRequestStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This horse already has a change request waiting for review");
        }

        List<MultipartFile> uploads = files == null ? List.of() : files;
        List<AdmissionDocumentType> types = documentTypes == null ? List.of() : documentTypes;
        validateUploads(types, uploads);

        HorseEditFields original = currentFields(horse);
        HorseEditFields requested = normalize(fields, original);
        validate(horse, requested);
        if (requested.equals(original) && uploads.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There are no changes to submit");
        }

        HorseEditRequest request = new HorseEditRequest();
        request.setHorseId(horseId);
        request.setOwnerId(ownerId);
        request.setStatus(HorseEditRequestStatus.PENDING);
        setRequested(request, requested);
        setOriginal(request, original);
        try {
            request = requests.saveAndFlush(request);
        } catch (DataIntegrityViolationException raced) {
            // uq_her_one_pending_per_horse: another submission for this horse won the race.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This horse already has a change request waiting for review");
        }

        List<String> storedKeys = deleteOnRollback();
        for (int i = 0; i < uploads.size(); i++) {
            MultipartFile file = uploads.get(i);
            String key = fileStorage.store(file, "rtms/horses/" + horseId + "/edit-requests/" + request.getId());
            storedKeys.add(key);
            HorseEditRequestDocument document = new HorseEditRequestDocument();
            document.setRequestId(request.getId());
            document.setHorseId(horseId);
            document.setDocumentType(types.get(i));
            document.setFileUrl(key);
            document.setOriginalFileName(safeFileName(file.getOriginalFilename()));
            documents.save(document);
        }

        String type = NotificationTypes.HORSE_EDIT_REQUESTED;
        String refType = NotificationTypes.REFERENCE_HORSE_EDIT_REQUEST;
        notifications.createNotification(groomId, "Horse change request",
                "The owner of " + horse.getName() + " asked to update its details. Review the request.",
                type, refType, request.getId(),
                Notification.buildDeduplicationKey(groomId, refType, request.getId(), type));
        return toResponse(request);
    }

    @Transactional
    public HorseEditRequestResponse cancel(Long ownerId, Long horseId, Long requestId) {
        HorseEditRequest request = requests.findByIdForUpdate(requestId)
                .filter(r -> Objects.equals(r.getHorseId(), horseId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Change request not found"));
        if (!Objects.equals(request.getOwnerId(), ownerId)) {
            throw new AccessDeniedException("You can only cancel your own change requests");
        }
        assertPending(request);
        request.setStatus(HorseEditRequestStatus.CANCELLED);
        requests.save(request);
        deleteAfterCommit(fileKeys(documents.findByRequestIdOrderByIdAsc(requestId)));
        return toResponse(request);
    }

    /** The horse's most recent request (any status), or null; drives the owner's pending/rejected banner. */
    @Transactional(readOnly = true)
    public HorseEditRequestResponse latestForOwner(Long ownerId, Long horseId) {
        Horse horse = horses.findById(horseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Horse not found"));
        if (!Objects.equals(horse.getOwnerId(), ownerId)) {
            throw new AccessDeniedException("You can only view horses you own");
        }
        return requests.findFirstByHorseIdOrderByCreatedAtDescIdDesc(horseId).map(this::toResponse).orElse(null);
    }

    // ── Groom ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<HorseEditRequestResponse> listForGroom(Long groomId, HorseEditRequestStatus status) {
        return requests.findVisibleToGroom(groomId, status != null,
                        status == null ? HorseEditRequestStatus.PENDING : status).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public HorseEditRequestResponse getForGroom(Long groomId, Long requestId) {
        HorseEditRequest request = requests.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Change request not found"));
        Horse horse = horses.findById(request.getHorseId()).orElse(null);
        boolean visible = Objects.equals(request.getReviewerId(), groomId)
                || (horse != null && Objects.equals(groomOf(horse), groomId));
        if (!visible) throw new AccessDeniedException("This change request is not for a horse you look after");
        return toResponse(request);
    }

    @Transactional
    public HorseEditRequestResponse review(Long groomId, Long requestId, HorseEditReviewRequest decision) {
        HorseEditRequest request = requests.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Change request not found"));
        assertPending(request);
        Horse horse = horses.findByIdForUpdate(request.getHorseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Horse not found"));
        // Checked at decision time: the horse may have moved to another groom's stall since submission.
        if (!Objects.equals(groomOf(horse), groomId)) {
            throw new AccessDeniedException("Only the groom of this horse's stall can review its change requests");
        }
        if (decision == null || decision.decision() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision is required");
        }
        String feedback = AdmissionGroomReviewService.normalizeFeedback(decision.feedback());
        List<HorseEditRequestDocument> uploaded = documents.findByRequestIdOrderByIdAsc(requestId);

        if (decision.decision() == ReviewDecision.REJECTED) {
            if (feedback == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feedback is required to reject a change request");
            }
            stampReview(request, groomId, HorseEditRequestStatus.REJECTED, feedback);
            deleteAfterCommit(fileKeys(uploaded));
            notifyOwner(request, horse, "Your change request for " + horse.getName() + " was rejected: " + feedback);
            return toResponse(request);
        }
        if (decision.decision() != ReviewDecision.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported decision");
        }

        if (horse.getCurrentStatus() != HorseStatus.ELIGIBLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only eligible horses can be edited");
        }
        HorseEditFields original = originalOf(request);
        if (!currentFields(horse).equals(original)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The horse record changed after this request was submitted. Ask the owner to submit it again.");
        }
        HorseEditFields requested = requestedOf(request);
        validate(horse, requested);

        apply(horse, original, requested);
        deleteAfterCommit(promote(uploaded));
        stampReview(request, groomId, HorseEditRequestStatus.APPROVED, feedback);
        notifyOwner(request, horse, "Your change request for " + horse.getName() + " was approved and applied.");
        return toResponse(request);
    }

    // ── Documents ────────────────────────────────────────────────────────────

    /**
     * One slot per document type: the file from the latest approved change if any, otherwise the
     * latest admission file, otherwise empty. Callers check that the viewer may see the horse.
     */
    @Transactional(readOnly = true)
    public List<HorseDocumentSlotResponse> documentSlots(Long horseId) {
        Map<AdmissionDocumentType, HorseDocumentSlotResponse> slots = new EnumMap<>(AdmissionDocumentType.class);
        for (HorseEditRequestDocument current : documents.findByHorseIdAndCurrentTrue(horseId)) {
            slots.put(current.getDocumentType(), new HorseDocumentSlotResponse(current.getDocumentType(),
                    SOURCE_EDIT_REQUEST, current.getId(), current.getOriginalFileName(),
                    editDocumentUrl(horseId, current.getId()), current.getUploadedAt()));
        }
        List<AdmissionApplication> history = admissions.findByHorseIdIn(List.of(horseId)).stream()
                .sorted(Comparator.comparing(AdmissionApplication::getId).reversed())
                .toList();
        for (AdmissionApplication admission : history) {
            admissionDocuments.findByAdmissionId(admission.getId()).stream()
                    .sorted(Comparator.comparing(AdmissionDocument::getId).reversed())
                    .forEach(doc -> slots.putIfAbsent(doc.getDocumentType(), new HorseDocumentSlotResponse(
                            doc.getDocumentType(), SOURCE_ADMISSION, doc.getId(), doc.getOriginalFileName(),
                            "/api/admissions/" + doc.getAdmissionId() + "/documents/" + doc.getId() + "/file",
                            doc.getUploadedAt())));
        }
        return Arrays.stream(AdmissionDocumentType.values())
                .map(type -> slots.getOrDefault(type, HorseDocumentSlotResponse.empty(type)))
                .toList();
    }

    /** An edit-request file of this horse; callers check that the viewer may see the horse. */
    @Transactional(readOnly = true)
    public HorseEditRequestDocument editDocument(Long horseId, Long documentId) {
        return documents.findById(documentId)
                .filter(doc -> Objects.equals(doc.getHorseId(), horseId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    // ── Rules ────────────────────────────────────────────────────────────────

    private void validate(Horse horse, HorseEditFields requested) {
        UelnUtils.validatePedigreeUelns(horse.getRegistrationNumber(),
                requested.sireRegistrationNumber(), requested.damRegistrationNumber());
        birthDates.validateByUeln(horse.getDateOfBirth(),
                requested.sireRegistrationNumber(), requested.damRegistrationNumber());
        pedigreeService.validateParentSexesByUeln(requested.sireRegistrationNumber(), requested.damRegistrationNumber());
    }

    private HorseEditFields normalize(HorseEditFields fields, HorseEditFields original) {
        if (fields == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horse details are required");
        String name = text(fields.name(), "Name");
        if (name == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        HorseEditFields requested = new HorseEditFields(
                name,
                text(fields.breed(), "Breed"),
                text(fields.sireName(), "Sire name"),
                UelnUtils.cleanAndValidate(fields.sireRegistrationNumber(), "Sire UELN"),
                text(fields.damName(), "Dam name"),
                UelnUtils.cleanAndValidate(fields.damRegistrationNumber(), "Dam UELN"));
        // Changing a parent is allowed; erasing a recorded one is not.
        if (original.sireName() != null && requested.sireName() == null
                || original.sireRegistrationNumber() != null && requested.sireRegistrationNumber() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recorded sire details cannot be removed");
        }
        if (original.damName() != null && requested.damName() == null
                || original.damRegistrationNumber() != null && requested.damRegistrationNumber() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recorded dam details cannot be removed");
        }
        return requested;
    }

    private void validateUploads(List<AdmissionDocumentType> types, List<MultipartFile> files) {
        if (types.size() != files.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each uploaded file needs exactly one document type");
        }
        if (types.stream().anyMatch(Objects::isNull) || new HashSet<>(types).size() != types.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload at most one file per document type");
        }
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            fileStorage.validate(file);
            String mediaType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
            if (types.get(i) == AdmissionDocumentType.HORSE_PHOTO && !mediaType.startsWith("image/")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horse photo must be an image");
            }
        }
    }

    private void apply(Horse horse, HorseEditFields original, HorseEditFields requested) {
        horse.setName(requested.name());
        horse.setBreed(requested.breed());
        horses.save(horse);

        boolean sireChanged = !Objects.equals(original.sireName(), requested.sireName())
                || !Objects.equals(original.sireRegistrationNumber(), requested.sireRegistrationNumber());
        boolean damChanged = !Objects.equals(original.damName(), requested.damName())
                || !Objects.equals(original.damRegistrationNumber(), requested.damRegistrationNumber());
        if (!sireChanged && !damChanged) return;

        // Reuse the pedigree service so parent linking and its checks stay in one place.
        // UELN and registry name are passed through unchanged: they are not editable here.
        HorsePedigree current = pedigrees.findByHorseId(horse.getId()).orElse(null);
        CreateHorsePedigreeRequest pedigree = new CreateHorsePedigreeRequest();
        pedigree.setRegistrationNumber(horse.getRegistrationNumber());
        pedigree.setRegistryName(horse.getRegistryName());
        pedigree.setPedigreeNotes(current == null ? null : current.getPedigreeNotes());
        pedigree.setSireName(requested.sireName());
        pedigree.setSireRegistrationNumber(requested.sireRegistrationNumber());
        pedigree.setSireId(!sireChanged && current != null ? current.getSireId() : null);
        pedigree.setDamName(requested.damName());
        pedigree.setDamRegistrationNumber(requested.damRegistrationNumber());
        pedigree.setDamId(!damChanged && current != null ? current.getDamId() : null);
        pedigreeService.createOrUpdate(horse.getId(), pedigree);
    }

    /** Makes the request's files official and returns the storage keys they replace. */
    private List<String> promote(List<HorseEditRequestDocument> uploaded) {
        List<String> replaced = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (HorseEditRequestDocument document : uploaded) {
            documents.findFirstByHorseIdAndDocumentTypeAndCurrentTrue(document.getHorseId(), document.getDocumentType())
                    .ifPresent(previous -> {
                        previous.setCurrent(false);
                        previous.setSupersededAt(now);
                        // Flush first: uq_herd_current_per_type allows one current file per type.
                        documents.saveAndFlush(previous);
                        replaced.add(previous.getFileUrl());
                    });
            document.setCurrent(true);
            documents.saveAndFlush(document);
        }
        return replaced;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Long groomOf(Horse horse) {
        if (horse.getCurrentStallId() == null) return null;
        return stalls.findById(horse.getCurrentStallId()).map(StableStall::getGroomId).orElse(null);
    }

    private HorseEditFields currentFields(Horse horse) {
        HorsePedigree pedigree = pedigrees.findByHorseId(horse.getId()).orElse(null);
        return new HorseEditFields(
                horse.getName(),
                horse.getBreed(),
                pedigree == null ? null : pedigree.getSireName(),
                pedigree == null ? null : pedigree.getSireRegistrationNumber(),
                pedigree == null ? null : pedigree.getDamName(),
                pedigree == null ? null : pedigree.getDamRegistrationNumber());
    }

    private static HorseEditFields originalOf(HorseEditRequest r) {
        return new HorseEditFields(r.getOriginalName(), r.getOriginalBreed(),
                r.getOriginalSireName(), r.getOriginalSireRegistrationNumber(),
                r.getOriginalDamName(), r.getOriginalDamRegistrationNumber());
    }

    private static HorseEditFields requestedOf(HorseEditRequest r) {
        return new HorseEditFields(r.getRequestedName(), r.getRequestedBreed(),
                r.getRequestedSireName(), r.getRequestedSireRegistrationNumber(),
                r.getRequestedDamName(), r.getRequestedDamRegistrationNumber());
    }

    private static void setRequested(HorseEditRequest r, HorseEditFields f) {
        r.setRequestedName(f.name());
        r.setRequestedBreed(f.breed());
        r.setRequestedSireName(f.sireName());
        r.setRequestedSireRegistrationNumber(f.sireRegistrationNumber());
        r.setRequestedDamName(f.damName());
        r.setRequestedDamRegistrationNumber(f.damRegistrationNumber());
    }

    private static void setOriginal(HorseEditRequest r, HorseEditFields f) {
        r.setOriginalName(f.name());
        r.setOriginalBreed(f.breed());
        r.setOriginalSireName(f.sireName());
        r.setOriginalSireRegistrationNumber(f.sireRegistrationNumber());
        r.setOriginalDamName(f.damName());
        r.setOriginalDamRegistrationNumber(f.damRegistrationNumber());
    }

    private void assertPending(HorseEditRequest request) {
        if (request.getStatus() != HorseEditRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This change request is already " + request.getStatus().name().toLowerCase(Locale.ROOT));
        }
    }

    private void stampReview(HorseEditRequest request, Long groomId, HorseEditRequestStatus status, String feedback) {
        request.setStatus(status);
        request.setReviewerId(groomId);
        request.setReviewFeedback(feedback);
        request.setReviewedAt(LocalDateTime.now());
        requests.save(request);
    }

    private void notifyOwner(HorseEditRequest request, Horse horse, String message) {
        String type = NotificationTypes.HORSE_EDIT_REVIEWED;
        String refType = NotificationTypes.REFERENCE_HORSE;
        notifications.createNotification(request.getOwnerId(), "Horse change request reviewed", message,
                type, refType, horse.getId(),
                Notification.buildDeduplicationKey(request.getOwnerId(), refType, horse.getId(), type)
                        + ":request:" + request.getId());
    }

    private static String text(String raw, String label) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.length() > MAX_TEXT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " must be at most " + MAX_TEXT + " characters");
        }
        return value;
    }

    private static String safeFileName(String name) {
        if (name == null || name.isBlank()) return null;
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1).trim();
        return base.length() > MAX_TEXT ? base.substring(0, MAX_TEXT) : base;
    }

    private static List<String> fileKeys(List<HorseEditRequestDocument> docs) {
        return docs.stream().map(HorseEditRequestDocument::getFileUrl).toList();
    }

    private static String editDocumentUrl(Long horseId, Long documentId) {
        return "/api/horses/" + horseId + "/edit-documents/" + documentId + "/file";
    }

    /** Storage keys added to the returned list are deleted if the transaction rolls back. */
    private List<String> deleteOnRollback() {
        List<String> keys = new ArrayList<>();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    keys.forEach(fileStorage::delete);
                } else if (status == STATUS_UNKNOWN) {
                    log.error("Horse edit request transaction outcome unknown; check stored files: {}", keys);
                }
            }
        });
        return keys;
    }

    /** Superseded or discarded files go only once the decision is committed, never before. */
    private void deleteAfterCommit(List<String> keys) {
        if (keys.isEmpty()) return;
        List<String> toDelete = List.copyOf(keys);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                toDelete.forEach(fileStorage::delete);
            }
        });
    }

    private HorseEditRequestResponse toResponse(HorseEditRequest r) {
        Horse horse = horses.findById(r.getHorseId()).orElse(null);
        List<HorseEditRequestResponse.Document> files = r.getId() == null ? List.of()
                : documents.findByRequestIdOrderByIdAsc(r.getId()).stream()
                .map(d -> new HorseEditRequestResponse.Document(d.getId(), d.getDocumentType(),
                        d.getOriginalFileName(), editDocumentUrl(r.getHorseId(), d.getId())))
                .toList();
        return new HorseEditRequestResponse(
                r.getId(), r.getHorseId(),
                horse == null ? null : horse.getName(),
                horse == null ? null : horse.getRegistrationNumber(),
                r.getOwnerId(), userName(r.getOwnerId()),
                r.getStatus(), originalOf(r), requestedOf(r), files,
                r.getReviewerId(), userName(r.getReviewerId()),
                r.getReviewFeedback(), r.getReviewedAt(), r.getCreatedAt());
    }

    private String userName(Long userId) {
        return userId == null ? null : users.findById(userId).map(User::getFullName).orElse(null);
    }
}
