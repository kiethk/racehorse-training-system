package com.rtms.backend.entity;

import com.rtms.backend.enums.VetExamStatus;
import com.rtms.backend.enums.VetExamType;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vet_exams")
public class VetExam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "horse_id", nullable = false)
    private Long horseId;

    @Column(name = "admission_id")
    private Long admissionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false, length = 20)
    private VetExamType examType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VetExamStatus status = VetExamStatus.REQUESTED;

    @Column(nullable = false)
    private int priority;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "assigned_vet_id")
    private Long assignedVetId;

    @Column(name = "preferred_vet_id")
    private Long preferredVetId;

    @Column(name = "requested_for_date")
    private LocalDate requestedForDate;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 30;

    @Column(name = "health_record_id", unique = true)
    private Long healthRecordId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) status = VetExamStatus.REQUESTED;
        if (priority == 0 && examType != null) priority = examType.getDefaultPriority();
        if (durationMinutes <= 0) durationMinutes = 30;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getHorseId() { return horseId; }
    public void setHorseId(Long horseId) { this.horseId = horseId; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public VetExamType getExamType() { return examType; }
    public void setExamType(VetExamType examType) { this.examType = examType; }
    public VetExamStatus getStatus() { return status; }
    public void setStatus(VetExamStatus status) { this.status = status; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }
    public Long getAssignedVetId() { return assignedVetId; }
    public void setAssignedVetId(Long assignedVetId) { this.assignedVetId = assignedVetId; }
    public Long getPreferredVetId() { return preferredVetId; }
    public void setPreferredVetId(Long preferredVetId) { this.preferredVetId = preferredVetId; }
    public LocalDate getRequestedForDate() { return requestedForDate; }
    public void setRequestedForDate(LocalDate requestedForDate) { this.requestedForDate = requestedForDate; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public Long getHealthRecordId() { return healthRecordId; }
    public void setHealthRecordId(Long healthRecordId) { this.healthRecordId = healthRecordId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
