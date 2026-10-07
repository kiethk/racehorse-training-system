package com.rtms.backend.dto;
import com.rtms.backend.enums.TrainingDecision;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CompleteCareScheduleRequest {

    @NotBlank(message = "Findings are required")
    private String findings;

    @NotBlank(message = "Diagnosis is required")
    private String diagnosis;

    private String treatment;

    @NotNull(message = "Training decision is required")
    private TrainingDecision trainingDecision;

    private String restrictionDetails;

    private Boolean rejectAdmission = false;
    private String rejectionReason;

    private String symptoms;
    private String notes;
    private java.time.LocalDate followUpDate;
    private List<@NotNull @Valid HorseHealthMetricRequest> metrics;
    private CreateNextScheduleRequest nextSchedule;

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

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public java.time.LocalDate getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(java.time.LocalDate followUpDate) { this.followUpDate = followUpDate; }

    public CreateNextScheduleRequest getNextSchedule() { return nextSchedule; }
    public void setNextSchedule(CreateNextScheduleRequest nextSchedule) { this.nextSchedule = nextSchedule; }

    public List<HorseHealthMetricRequest> getMetrics() { return metrics; }
    public void setMetrics(List<HorseHealthMetricRequest> metrics) { this.metrics = metrics; }

    public Boolean getRejectAdmission() { return rejectAdmission; }
    public void setRejectAdmission(Boolean rejectAdmission) { this.rejectAdmission = rejectAdmission; }
    public boolean isRejectAdmission() { return Boolean.TRUE.equals(rejectAdmission); }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    @JsonIgnore
    @AssertTrue(message = "Restriction details are required when training decision is RESTRICTED or BLOCKED")
    public boolean isRestrictionValid() {
        if (trainingDecision == TrainingDecision.RESTRICTED || trainingDecision == TrainingDecision.BLOCKED) {
            return restrictionDetails != null && !restrictionDetails.isBlank();
        }
        return true;
    }

    @JsonIgnore
    @AssertTrue(message = "Rejection reason is required when rejectAdmission is true")
    public boolean isRejectionReasonValid() {
        if (Boolean.TRUE.equals(rejectAdmission)) {
            return rejectionReason != null && !rejectionReason.isBlank();
        }
        return true;
    }
}