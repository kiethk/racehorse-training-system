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

    /** Lý do chặn tập — bắt buộc khi BLOCKED, Trainer đọc thấy khi lập kế hoạch. */
    private String restrictionDetails;

    private String symptoms;
    private String notes;
    private List<@NotNull @Valid HorseHealthMetricRequest> metrics;

    /** Lịch khám lại — bắt buộc khi BLOCKED: "tạm nghỉ đến" chính là ngày của lịch này. */
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

    public CreateNextScheduleRequest getNextSchedule() { return nextSchedule; }
    public void setNextSchedule(CreateNextScheduleRequest nextSchedule) { this.nextSchedule = nextSchedule; }

    public List<HorseHealthMetricRequest> getMetrics() { return metrics; }
    public void setMetrics(List<HorseHealthMetricRequest> metrics) { this.metrics = metrics; }

    @JsonIgnore
    @AssertTrue(message = "Restriction details are required when training is BLOCKED")
    public boolean isRestrictionValid() {
        if (trainingDecision == TrainingDecision.BLOCKED) {
            return restrictionDetails != null && !restrictionDetails.isBlank();
        }
        return true;
    }

    @JsonIgnore
    @AssertTrue(message = "A follow-up examination (nextSchedule) is required when training is BLOCKED")
    public boolean isFollowUpValid() {
        return trainingDecision != TrainingDecision.BLOCKED || nextSchedule != null;
    }
}
