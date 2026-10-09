package com.rtms.backend.admission.dto;
import com.rtms.backend.medical.dto.CreateNextScheduleRequest;
import com.rtms.backend.medical.dto.HorseHealthMetricRequest;
import com.rtms.backend.medical.service.CareScheduleService;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rtms.backend.training.enums.TrainingDecision;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.util.List;

/**
 * Thú y hoàn tất lần khám nhập học.
 *
 * Thú y không duyệt hay từ chối đơn — chỉ kết luận ngựa được tập (ALLOWED) hay
 * tạm nghỉ (BLOCKED). Khi BLOCKED phải có lý do và lịch khám lại (nextSchedule);
 * việc kiểm lịch khám lại nằm ở CareScheduleService để áp dụng cho mọi đường vào.
 */
public class VetReviewRequest {

    private Long careScheduleId;

    @NotNull(message = "Training decision is required")
    private TrainingDecision trainingDecision;
    private String restrictionDetails;

    private String feedback;

    @NotNull(message = "Physical exam confirmation is required")
    private Boolean physicalExamConfirmed;

    private String symptoms;
    private String findings;
    private String diagnosis;
    private String treatment;
    private String notes;
    private List<@NotNull @Valid HorseHealthMetricRequest> metrics;
    private CreateNextScheduleRequest nextSchedule;

    public Long getCareScheduleId() { return careScheduleId; }
    public void setCareScheduleId(Long careScheduleId) { this.careScheduleId = careScheduleId; }

    public TrainingDecision getTrainingDecision() { return trainingDecision; }
    public void setTrainingDecision(TrainingDecision trainingDecision) { this.trainingDecision = trainingDecision; }

    public String getRestrictionDetails() { return restrictionDetails; }
    public void setRestrictionDetails(String restrictionDetails) { this.restrictionDetails = restrictionDetails; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public Boolean getPhysicalExamConfirmed() { return physicalExamConfirmed; }
    public void setPhysicalExamConfirmed(Boolean physicalExamConfirmed) { this.physicalExamConfirmed = physicalExamConfirmed; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
    public String getFindings() { return findings; }
    public void setFindings(String findings) { this.findings = findings; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getTreatment() { return treatment; }
    public void setTreatment(String treatment) { this.treatment = treatment; }
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
            return (restrictionDetails != null && !restrictionDetails.isBlank())
                    || (feedback != null && !feedback.isBlank());
        }
        return true;
    }

    @JsonIgnore
    @AssertTrue(message = "Exam findings are required after physical examination")
    public boolean isExamValid() {
        return Boolean.TRUE.equals(physicalExamConfirmed) && findings != null && !findings.isBlank();
    }
}
