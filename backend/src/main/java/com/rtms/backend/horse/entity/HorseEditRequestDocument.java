package com.rtms.backend.horse.entity;

import com.rtms.backend.admission.enums.AdmissionDocumentType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A file uploaded with a horse edit request. It becomes the horse's official
 * document of its type ({@code current}) only when the request is approved.
 */
@Entity
@Table(name = "horse_edit_request_documents")
public class HorseEditRequestDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "horse_id", nullable = false)
    private Long horseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private AdmissionDocumentType documentType;

    /** Storage key, never a public URL. */
    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Column(name = "superseded_at")
    private LocalDateTime supersededAt;

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) uploadedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public Long getHorseId() { return horseId; }
    public void setHorseId(Long horseId) { this.horseId = horseId; }

    public AdmissionDocumentType getDocumentType() { return documentType; }
    public void setDocumentType(AdmissionDocumentType documentType) { this.documentType = documentType; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }

    public LocalDateTime getSupersededAt() { return supersededAt; }
    public void setSupersededAt(LocalDateTime supersededAt) { this.supersededAt = supersededAt; }
}
