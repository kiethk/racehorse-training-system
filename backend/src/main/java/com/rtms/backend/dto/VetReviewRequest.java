package com.rtms.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rtms.backend.enums.TrainingDecision;
import com.rtms.backend.enums.VetDecision;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

public class VetReviewRequest {

    private VetDecision decision;
    private Long careScheduleId;

    public Long getCareScheduleId() { return careScheduleId; }
    public void setCareScheduleId(Long careScheduleId) { this.careScheduleId = careScheduleId; }

    private TrainingDecision trainingDecision;
    private String restrictionDetails;

    private String feedback;

    private Boolean rejectAdmission;

    @NotNull(message = "Physical exam confirmation is required")
    private Boolean physicalExamConfirmed;

    private String symptoms;
    private String findings;
    private String diagnosis;
    private String treatment;
    private String rejectionReason;
    private String notes;
    private LocalDate followUpDate;
    private List<@NotNull @Valid HorseHealthMetricRequest> metrics;

    public TrainingDecision getTrainingDecision() { return trainingDecision; }
    public void setTrainingDecision(TrainingDecision trainingDecision) { this.trainingDecision = trainingDecision; }

    public String getRestrictionDetails() { return restrictionDetails; }
    public void setRestrictionDetails(String restrictionDetails) { this.restrictionDetails = restrictionDetails; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
    public String getFindings() { return findings; }
    public void setFindings(String findings) { this.findings = findings; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getTreatment() { return treatment; }
    public void setTreatment(String treatment) { this.treatment = treatment; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDate getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(LocalDate followUpDate) { this.followUpDate = followUpDate; }
    public List<HorseHealthMetricRequest> getMetrics() { return metrics; }
    public void setMetrics(List<HorseHealthMetricRequest> metrics) { this.metrics = metrics; }

    public VetDecision getDecision() {
        return decision;
    }

    public void setDecision(VetDecision decision) {
        this.decision = decision;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public Boolean getPhysicalExamConfirmed() {
        return physicalExamConfirmed;
    }

    public void setPhysicalExamConfirmed(Boolean physicalExamConfirmed) {
        this.physicalExamConfirmed = physicalExamConfirmed;
    }

    public Boolean getRejectAdmission() {
        return rejectAdmission;
    }

    public void setRejectAdmission(Boolean rejectAdmission) {
        this.rejectAdmission = rejectAdmission;
    }

    @JsonIgnore
    @AssertTrue(message = "Decision or Training Decision is required")
    public boolean isDecisionSpecified() {
        return decision != null || trainingDecision != null;
    }

    @JsonIgnore
    @AssertTrue(message = "Rejection reason or restriction details are required when decision is REJECTED, BLOCKED, or RESTRICTED")
    public boolean isFeedbackValid() {
        if (decision == VetDecision.REJECTED || trainingDecision == TrainingDecision.BLOCKED
                || trainingDecision == TrainingDecision.RESTRICTED) {
            return (rejectionReason != null && !rejectionReason.isBlank())
                    || (feedback != null && !feedback.isBlank())
                    || (restrictionDetails != null && !restrictionDetails.isBlank());
        }
        return true;
    }

    @JsonIgnore
    @AssertTrue(message = "Exam findings are required after physical examination")
    public boolean isExamValid() {
        return Boolean.TRUE.equals(physicalExamConfirmed) && findings != null && !findings.isBlank();
    }

    @JsonIgnore
    @AssertTrue(message = "Follow-up date if provided must be in the future")
    public boolean isFollowUpValid() {
        return followUpDate == null || followUpDate.isAfter(LocalDate.now());
    }
}
