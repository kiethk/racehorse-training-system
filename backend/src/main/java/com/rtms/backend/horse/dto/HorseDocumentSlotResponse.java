package com.rtms.backend.horse.dto;

import com.rtms.backend.admission.enums.AdmissionDocumentType;

import java.time.LocalDateTime;

/**
 * The horse's official document of one type, or an empty slot when none exists.
 * {@code source} is ADMISSION (the admission snapshot) or EDIT_REQUEST (an approved change).
 */
public record HorseDocumentSlotResponse(
        AdmissionDocumentType documentType,
        String source,
        Long documentId,
        String originalFileName,
        String fileUrl,
        LocalDateTime updatedAt) {

    public static HorseDocumentSlotResponse empty(AdmissionDocumentType type) {
        return new HorseDocumentSlotResponse(type, null, null, null, null, null);
    }
}
