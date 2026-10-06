package com.rtms.backend.entity;

import com.rtms.backend.enums.CareScheduleStatus;
import com.rtms.backend.enums.CareType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "care_schedule")
public class CareSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "horse_id", nullable = false)
    private Long horseId;

    @Column(name = "veterinarian_id")
    private Long veterinarianId;

    /**
     * Head Trainer responsible for this care schedule. Assigned directly alongside
     * {@code veterinarianId} by the scheduler (Vet Flow MVP §3.3) — only the ID is
     * stored; no separate TrainingSession is created. Null until a Trainer is available.
     */
    @Column(name = "trainer_id")
    private Long trainerId;

    @Column(name = "admission_id")
    private Long admissionId;

    @Column(name = "source_incident_id")
    private Long sourceIncidentId;

    @Column(name = "source_schedule_id")
    private Long sourceScheduleId;

    @Column(name = "requested_by_id")
    private Long requestedById;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", length = 64)
    private String requestFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "care_type", nullable = false, length = 50)
    private CareType careType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CareScheduleStatus status = CareScheduleStatus.REQUESTED;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 30;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) status = CareScheduleStatus.REQUESTED;
        if (durationMinutes <= 0) durationMinutes = 30;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getHorseId() { return horseId; }
    public void setHorseId(Long horseId) { this.horseId = horseId; }

    public Long getVeterinarianId() { return veterinarianId; }
    public void setVeterinarianId(Long veterinarianId) { this.veterinarianId = veterinarianId; }

    public Long getTrainerId() { return trainerId; }
    public void setTrainerId(Long trainerId) { this.trainerId = trainerId; }

    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }

    public Long getSourceIncidentId() { return sourceIncidentId; }
    public void setSourceIncidentId(Long sourceIncidentId) { this.sourceIncidentId = sourceIncidentId; }

    public Long getSourceScheduleId() { return sourceScheduleId; }
    public void setSourceScheduleId(Long sourceScheduleId) { this.sourceScheduleId = sourceScheduleId; }

    public Long getRequestedById() { return requestedById; }
    public void setRequestedById(Long requestedById) { this.requestedById = requestedById; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getRequestFingerprint() { return requestFingerprint; }
    public void setRequestFingerprint(String requestFingerprint) { this.requestFingerprint = requestFingerprint; }

    public CareType getCareType() { return careType; }
    public void setCareType(CareType careType) { this.careType = careType; }

    public CareScheduleStatus getStatus() { return status; }
    public void setStatus(CareScheduleStatus status) { this.status = status; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
