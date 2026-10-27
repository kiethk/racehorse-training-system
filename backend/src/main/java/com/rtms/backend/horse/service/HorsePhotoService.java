package com.rtms.backend.horse.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.AdmissionDocument;
import com.rtms.backend.admission.enums.AdmissionDocumentType;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.AdmissionDocumentRepository;
import com.rtms.backend.common.storage.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves HORSE_PHOTO documents for horses without exposing admission internals.
 *
 * Strategy (batch):
 * 1. Look up admissions whose horseId is in the requested set — one query.
 * 2. Pick the latest admission per horse (highest id).
 * 3. Look up HORSE_PHOTO documents for those admissions — one query.
 * 4. Return "/api/horses/{id}/photo" as imageUrl, never a raw storage key.
 *
 * Strategy (single photo stream):
 * 1+2+3 as above, but for one horse only.
 * 4. Resolve Resource AND MediaType from the same document in one pass.
 *
 * All access is gated by the caller (HorseController) which already requires HORSE_VIEW.
 */
@Service
public class HorsePhotoService {

    /**
     * Holds the resolved file resource and its content type.
     * Both are derived from the same {@link AdmissionDocument} in a single lookup,
     * so the controller never needs a second query to get the MediaType.
     */
    public record PhotoResult(Resource resource, MediaType mediaType) {}

    private final AdmissionApplicationRepository admissionRepo;
    private final AdmissionDocumentRepository documentRepo;
    private final FileStorageService storage;
    private final com.rtms.backend.horse.repository.HorseEditRequestDocumentRepository editDocuments;

    public HorsePhotoService(AdmissionApplicationRepository admissionRepo,
                             AdmissionDocumentRepository documentRepo,
                             FileStorageService storage,
                             com.rtms.backend.horse.repository.HorseEditRequestDocumentRepository editDocuments) {
        this.admissionRepo = admissionRepo;
        this.documentRepo = documentRepo;
        this.storage = storage;
        this.editDocuments = editDocuments;
    }

    /**
     * Returns a map of horseId → imageUrl for all horses in the given set that have a HORSE_PHOTO.
     * Horses without a photo are absent from the map.
     * Uses exactly two queries regardless of list size (batch, no N+1).
     *
     * @param horseIds IDs of the horses to look up
     * @param baseUrl  e.g. "/api/horses" — the imageUrl is built as baseUrl + "/{id}/photo"
     */
    public Map<Long, String> resolveImageUrls(Collection<Long> horseIds, String baseUrl) {
        if (horseIds == null || horseIds.isEmpty()) return Map.of();

        // An approved edit request may have replaced (or added) the photo.
        Set<Long> withEditedPhoto = editDocuments
                .findByHorseIdInAndDocumentTypeAndCurrentTrue(horseIds, AdmissionDocumentType.HORSE_PHOTO).stream()
                .map(com.rtms.backend.horse.entity.HorseEditRequestDocument::getHorseId)
                .collect(Collectors.toSet());

        // Step 1: Find admissions for these horses; pick latest per horse.
        List<AdmissionApplication> admissions = admissionRepo.findByHorseIdIn(horseIds);

        // Latest admission per horse (highest id wins as proxy for most recent).
        Map<Long, AdmissionApplication> latestPerHorse = admissions.stream()
                .filter(a -> a.getHorseId() != null)
                .collect(Collectors.toMap(
                        AdmissionApplication::getHorseId,
                        Function.identity(),
                        (a, b) -> a.getId() > b.getId() ? a : b));

        if (latestPerHorse.isEmpty()) return photoUrls(withEditedPhoto, baseUrl);

        Set<Long> admissionIds = latestPerHorse.values().stream()
                .map(AdmissionApplication::getId)
                .collect(Collectors.toSet());

        // Step 2: Fetch HORSE_PHOTO docs for those admissions in one query.
        List<AdmissionDocument> photos = documentRepo.findByAdmissionIdInAndDocumentType(
                admissionIds, AdmissionDocumentType.HORSE_PHOTO);

        // Latest photo per admission (in case multiple were uploaded).
        Map<Long, AdmissionDocument> photoByAdmission = photos.stream()
                .collect(Collectors.toMap(
                        AdmissionDocument::getAdmissionId,
                        Function.identity(),
                        (a, b) -> a.getId() > b.getId() ? a : b));

        // Step 3: Build horseId → imageUrl.
        Set<Long> withPhoto = new java.util.HashSet<>(withEditedPhoto);
        latestPerHorse.entrySet().stream()
                .filter(e -> photoByAdmission.containsKey(e.getValue().getId()))
                .forEach(e -> withPhoto.add(e.getKey()));
        return photoUrls(withPhoto, baseUrl);
    }

    private static Map<Long, String> photoUrls(Set<Long> horseIds, String baseUrl) {
        return horseIds.stream().collect(Collectors.toMap(Function.identity(), id -> baseUrl + "/" + id + "/photo"));
    }

    /**
     * Resolves the HORSE_PHOTO for one horse into a {@link PhotoResult} containing
     * the {@link Resource} and {@link MediaType} derived from the same document —
     * so no second lookup is needed in the controller.
     *
     * The caller must have already verified that the current user may view the horse.
     *
     * @throws ResponseStatusException 404 when no HORSE_PHOTO exists for this horse.
     */
    public PhotoResult resolvePhoto(Long horseId) {
        var edited = editDocuments.findFirstByHorseIdAndDocumentTypeAndCurrentTrue(horseId, AdmissionDocumentType.HORSE_PHOTO);
        if (edited.isPresent()) {
            String key = edited.get().getFileUrl();
            return new PhotoResult(storage.load(key), storage.mediaType(key));
        }
        AdmissionDocument photo = findPhotoDocument(horseId);
        Resource resource = storage.load(photo.getFileUrl());
        MediaType mediaType = storage.mediaType(photo.getFileUrl(), photo.getOriginalFileName());
        if (mediaType == null || MediaType.APPLICATION_OCTET_STREAM.equals(mediaType)) {
            mediaType = MediaType.IMAGE_JPEG;
        }
        return new PhotoResult(resource, mediaType);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Finds the latest HORSE_PHOTO document for the given horse using the same
     * two-step batch strategy used by {@link #resolveImageUrls}, but scoped to
     * a single horse ID.
     */
    private AdmissionDocument findPhotoDocument(Long horseId) {
        List<AdmissionApplication> admissions = admissionRepo.findByHorseIdIn(List.of(horseId));

        Optional<AdmissionApplication> latest = admissions.stream()
                .filter(a -> horseId.equals(a.getHorseId()))
                .max(Comparator.comparingLong(AdmissionApplication::getId));

        if (latest.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No photo found for this horse");
        }

        return documentRepo
                .findByAdmissionIdInAndDocumentType(
                        List.of(latest.get().getId()), AdmissionDocumentType.HORSE_PHOTO)
                .stream()
                .max(Comparator.comparingLong(AdmissionDocument::getId))
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "No photo found for this horse"));
    }
}
