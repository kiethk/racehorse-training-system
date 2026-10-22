package com.rtms.backend.admission.dto;

import com.rtms.backend.horse.enums.HorseSex;

public record OcrExtractionResponse(
        boolean success,
        String message,
        ExtractedData data,
        int extractedFieldsCount
) {
    public record ExtractedData(
            String name,
            HorseSex sex,
            String rawSexText,
            String breed,
            String dateOfBirth,
            String registrationNumber,
            String registryName,
            String sireName,
            String sireRegistrationNumber,
            String damName,
            String damRegistrationNumber,
            FileReference fileReference,
            Double confidenceScore,
            boolean hasConflict,
            String conflictMessage
    ) {}

    public record FileReference(
            String tempFileId,
            String fileUrl,
            String originalFilename
    ) {}
}
