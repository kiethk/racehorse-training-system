package com.rtms.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rtms.backend.enums.VetDecision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public class CompleteVetExamRequest {
    @NotBlank(message = "Findings are required")
    private String findings;
    private String symptoms;
    private String diagnosis;
    private String treatment;
    private String note;
    @NotNull(message = "Vet decision is required")
    private VetDecision vetDecision;
    private String rejectionReason;
    private LocalDate followUpDate;
    private List<@NotNull @Valid HorseHealthMetricRequest> metrics;

    @JsonIgnore
    @AssertTrue(message = "Rejection reason is required when decision is REJECTED")
    public boolean isRejectionValid() {
        return vetDecision != VetDecision.REJECTED
                || (rejectionReason != null && !rejectionReason.isBlank());
    }

    @JsonIgnore
    @AssertTrue(message = "A future follow-up date is required when decision is RECHECK_REQUIRED")
    public boolean isFollowUpValid() {
        return vetDecision != VetDecision.RECHECK_REQUIRED
                || (followUpDate != null && followUpDate.isAfter(LocalDate.now()));
    }

    public String getFindings() { return findings; }
    public void setFindings(String findings) { this.findings = findings; }
    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getTreatment() { return treatment; }
    public void setTreatment(String treatment) { this.treatment = treatment; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public VetDecision getVetDecision() { return vetDecision; }
    public void setVetDecision(VetDecision vetDecision) { this.vetDecision = vetDecision; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public LocalDate getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(LocalDate followUpDate) { this.followUpDate = followUpDate; }
    public List<HorseHealthMetricRequest> getMetrics() { return metrics; }
    public void setMetrics(List<HorseHealthMetricRequest> metrics) { this.metrics = metrics; }
}
