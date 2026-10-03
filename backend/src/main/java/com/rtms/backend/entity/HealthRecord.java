package com.rtms.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.rtms.backend.enums.TrainingDecision;
import com.rtms.backend.enums.VetDecision;

@Entity
@Table(name = "health_records")
public class HealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "horse_id", nullable = false)
    private Long horseId;

    @Column(name = "veterinarian_id", nullable = false)
    private Long veterinarianId;

    @Column(name = "care_schedule_id", unique = true)
    private Long careScheduleId;

    @Column(name = "source_training_workout_id")
    private Long sourceTrainingWorkoutId;

    @Column(name = "examined_at", nullable = false)
    private LocalDateTime examinedAt;

    @Column(name = "symptoms", columnDefinition = "TEXT")
    private String symptoms;

    @Column(name = "findings", columnDefinition = "TEXT")
    private String findings;

    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "treatment", columnDefinition = "TEXT")
    private String treatment;

    @Enumerated(EnumType.STRING)
    @Column(name = "training_decision", length = 30)
    private TrainingDecision trainingDecision;

    @Column(name = "restriction_details", columnDefinition = "TEXT")
    private String restrictionDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "vet_decision", length = 30)
    private VetDecision vetDecision;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "record_type", nullable = false)
    private String recordType = "ILLNESS";


    @Column(name = "product_or_service")
    private String productOrService;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getHorseId() { return horseId; }
    public void setHorseId(Long horseId) { this.horseId = horseId; }

    public Long getVeterinarianId() { return veterinarianId; }
    public void setVeterinarianId(Long veterinarianId) { this.veterinarianId = veterinarianId; }

    public Long getCareScheduleId() { return careScheduleId; }
    public void setCareScheduleId(Long careScheduleId) { this.careScheduleId = careScheduleId; }

    public Long getSourceTrainingWorkoutId() { return sourceTrainingWorkoutId; }
    public void setSourceTrainingWorkoutId(Long sourceTrainingWorkoutId) { this.sourceTrainingWorkoutId = sourceTrainingWorkoutId; }

    public LocalDateTime getExaminedAt() { return examinedAt; }
    public void setExaminedAt(LocalDateTime examinedAt) { this.examinedAt = examinedAt; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public String getFindings() { return findings; }
    public void setFindings(String findings) { this.findings = findings; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getTreatment() { return treatment; }
    public void setTreatment(String treatment) { this.treatment = treatment; }

    public TrainingDecision getTrainingDecision() { return trainingDecision; }
    public void setTrainingDecision(TrainingDecision trainingDecision) { this.trainingDecision = trainingDecision; }

    public String getRestrictionDetails() { return restrictionDetails; }
    public void setRestrictionDetails(String restrictionDetails) { this.restrictionDetails = restrictionDetails; }

    public VetDecision getVetDecision() { return vetDecision; }
    public void setVetDecision(VetDecision vetDecision) { this.vetDecision = vetDecision; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }


    public String getProductOrService() { return productOrService; }
    public void setProductOrService(String productOrService) { this.productOrService = productOrService; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDate getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(LocalDate followUpDate) { this.followUpDate = followUpDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}