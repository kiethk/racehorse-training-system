package com.rtms.backend.dto;

import com.rtms.backend.enums.AdmissionDocumentType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdmissionDocumentResponse {

    private final Long id;
    private final AdmissionDocumentType documentType;
    private final String fileUrl;
    private final String originalFileName;
    private final LocalDate recordDate;
    private final String note;
    private final LocalDateTime uploadedAt;
    private final boolean medical;

    public AdmissionDocumentResponse(
            Long id,
            AdmissionDocumentType documentType,
            String fileUrl,
            String originalFileName,
            LocalDate recordDate,
            String note,
            LocalDateTime uploadedAt,
            boolean medical) {
        this.id = id;
        this.documentType = documentType;
        this.fileUrl = fileUrl;
        this.originalFileName = originalFileName;
        this.recordDate = recordDate;
        this.note = note;
        this.uploadedAt = uploadedAt;
        this.medical = medical;
    }

    public Long getId() { return id; }
    public AdmissionDocumentType getDocumentType() { return documentType; }
    public String getFileUrl() { return fileUrl; }
    public String getOriginalFileName() { return originalFileName; }
    public LocalDate getRecordDate() { return recordDate; }
    public String getNote() { return note; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public boolean isMedical() { return medical; }
}
