package com.rtms.backend.horse.dto;

import com.rtms.backend.admission.enums.AdmissionDocumentType;
import com.rtms.backend.horse.enums.HorseEditRequestStatus;

import java.time.LocalDateTime;
import java.util.List;

public record HorseEditRequestResponse(
        Long id,
        Long horseId,
        String horseName,
        String horseRegistrationNumber,
        Long ownerId,
        String ownerName,
        HorseEditRequestStatus status,
        HorseEditFields original,
        HorseEditFields requested,
        List<Document> documents,
        Long reviewerId,
        String reviewerName,
        String reviewFeedback,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt) {

    /** A file uploaded with the request; {@code fileUrl} is this API, which checks access before streaming. */
    public record Document(Long id, AdmissionDocumentType documentType, String originalFileName, String fileUrl) {
    }
}
