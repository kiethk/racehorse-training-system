package com.rtms.backend.admission.dto;

import com.rtms.backend.medical.dto.HorseHealthMetricResponse;
import com.rtms.backend.training.enums.TrainingDecision;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only view of the initial admission vet exam: the health record
 * produced by completing the INITIAL CareSchedule for this admission,
 * together with the vet who performed it and all vitals recorded in that
 * session. null values on optional fields mean "not recorded".
 */
public class InitialVetExamResponse {

    /** DB id of the health record (for reference only). */
    private Long healthRecordId;

    /** The vet who completed this exam (HealthRecord.veterinarianId). */
    private Long veterinarianId;
    private String veterinarianName;

    /** When the vet examined the horse. */
    private LocalDateTime examinedAt;

    /** Admission-level training decision set during this exam. */
    private TrainingDecision trainingDecision;
    private String restrictionDetails;

    // Clinical notes captured at examination time
    private String symptoms;
    private String findings;
    private String diagnosis;
    private String treatment;
    private String notes;

    /** All vital-sign measurements attached to this health record. */
    private List<HorseHealthMetricResponse> metrics;

    // ── Getters ────────────────────────────────────────────────────────────────

    public Long getHealthRecordId() { return healthRecordId; }
    public Long getVeterinarianId() { return veterinarianId; }
    public String getVeterinarianName() { return veterinarianName; }
    public LocalDateTime getExaminedAt() { return examinedAt; }
    public TrainingDecision getTrainingDecision() { return trainingDecision; }
    public String getRestrictionDetails() { return restrictionDetails; }
    public String getSymptoms() { return symptoms; }
    public String getFindings() { return findings; }
    public String getDiagnosis() { return diagnosis; }
    public String getTreatment() { return treatment; }
    public String getNotes() { return notes; }
    public List<HorseHealthMetricResponse> getMetrics() { return metrics; }

    // ── Setters ────────────────────────────────────────────────────────────────

    public void setHealthRecordId(Long healthRecordId) { this.healthRecordId = healthRecordId; }
    public void setVeterinarianId(Long veterinarianId) { this.veterinarianId = veterinarianId; }
    public void setVeterinarianName(String veterinarianName) { this.veterinarianName = veterinarianName; }
    public void setExaminedAt(LocalDateTime examinedAt) { this.examinedAt = examinedAt; }
    public void setTrainingDecision(TrainingDecision trainingDecision) { this.trainingDecision = trainingDecision; }
    public void setRestrictionDetails(String restrictionDetails) { this.restrictionDetails = restrictionDetails; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
    public void setFindings(String findings) { this.findings = findings; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public void setTreatment(String treatment) { this.treatment = treatment; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setMetrics(List<HorseHealthMetricResponse> metrics) { this.metrics = metrics; }
}
