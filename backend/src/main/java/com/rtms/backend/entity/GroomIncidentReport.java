package com.rtms.backend.entity;

import com.rtms.backend.enums.IncidentSeverity;
import com.rtms.backend.enums.IncidentStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "groom_incident_reports")
public class GroomIncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "groom_id", nullable = false)
    private Long groomId;

    @Column(name = "horse_id", nullable = false)
    private Long horseId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private IncidentSeverity severity = IncidentSeverity.MEDIUM;

    @Column(name = "reported_at")
    private LocalDateTime reportedAt;

    // ===== THÊM MỚI (V45) =====

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IncidentStatus status = IncidentStatus.REPORTED;

    /** Thú y (hoặc Quản lý) đã tiếp nhận sự cố này. */
    @Column(name = "handled_by_id")
    private Long handledById;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "handler_note", columnDefinition = "TEXT")
    private String handlerNote;

    public GroomIncidentReport() {
    }

    public GroomIncidentReport(Long groomId, Long horseId, String title, String description, String imageUrl, IncidentSeverity severity) {
        this.groomId = groomId;
        this.horseId = horseId;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.severity = severity != null ? severity : IncidentSeverity.MEDIUM;
    }

    @PrePersist
    protected void onCreate() {
        if (reportedAt == null) {
            reportedAt = LocalDateTime.now();
        }
        if (severity == null) {
            severity = IncidentSeverity.MEDIUM;
        }
        if (status == null) {
            status = IncidentStatus.REPORTED;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroomId() {
        return groomId;
    }

    public void setGroomId(Long groomId) {
        this.groomId = groomId;
    }

    public Long getHorseId() {
        return horseId;
    }

    public void setHorseId(Long horseId) {
        this.horseId = horseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(IncidentSeverity severity) {
        this.severity = severity;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public Long getHandledById() {
        return handledById;
    }

    public void setHandledById(Long handledById) {
        this.handledById = handledById;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }

    public String getHandlerNote() {
        return handlerNote;
    }

    public void setHandlerNote(String handlerNote) {
        this.handlerNote = handlerNote;
    }
}
