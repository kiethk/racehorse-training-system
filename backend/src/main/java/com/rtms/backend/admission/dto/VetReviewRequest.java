package com.rtms.backend.admission.dto;
import com.rtms.backend.admission.enums.ReviewDecision;
import com.rtms.backend.medical.dto.CreateNextScheduleRequest;
import com.rtms.backend.medical.dto.HorseHealthMetricRequest;
import com.rtms.backend.medical.dto.InjuryMarkerRequest;
import com.rtms.backend.medical.dto.VaccinationRequest;
import com.rtms.backend.medical.service.CareScheduleService;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rtms.backend.training.enums.TrainingDecision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Thú y hoàn tất lần khám nhập học.
 *
 * Thú y có thể chấp nhận (ACCEPT / null) hoặc từ chối (REJECT) đơn nhập học.
 * Khi chấp nhận, chỉ kết luận ngựa được tập (ALLOWED) hay tạm nghỉ (BLOCKED);
 * khi BLOCKED phải có lý do và lịch khám lại (nextSchedule). Việc kiểm lịch khám
 * lại nằm ở CareScheduleService để áp dụng cho mọi đường vào.
 */
public class VetReviewRequest {

    private Long careScheduleId;

    /** ACCEPT / null = hoàn tất khám bình thường; REJECT = từ chối đơn nhập học. */
    private ReviewDecision decision;

    @Size(max = 2000, message = "Rejection reason must not exceed 2000 characters")
    private String rejectReason;

    private TrainingDecision trainingDecision;
    private String restrictionDetails;

    private String feedback;

    private Boolean physicalExamConfirmed;

    private String findings;
    private String diagnosis;
    private String treatment;
    private String notes;
    private List<@NotNull @Valid HorseHealthMetricRequest> metrics;
    private CreateNextScheduleRequest nextSchedule;
    /** Marker chấn thương ghi nhận cùng ca khám; bắt buộc khi khám nhập trại kết luận BLOCKED. */
    private List<@Valid InjuryMarkerRequest> injuryMarkers;

    /** Mũi tiêm trong ca nhập trại, gồm cả mũi cũ nhập từ sổ tiêm (source = HISTORICAL). */
    private List<@NotNull @Valid VaccinationRequest> vaccinations;

    public Long getCareScheduleId() { return careScheduleId; }
    public void setCareScheduleId(Long careScheduleId) { this.careScheduleId = careScheduleId; }

    public ReviewDecision getDecision() { return decision; }
    public void setDecision(ReviewDecision decision) { this.decision = decision; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public TrainingDecision getTrainingDecision() { return trainingDecision; }
    public void setTrainingDecision(TrainingDecision trainingDecision) { this.trainingDecision = trainingDecision; }

    public String getRestrictionDetails() { return restrictionDetails; }
    public void setRestrictionDetails(String restrictionDetails) { this.restrictionDetails = restrictionDetails; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public Boolean getPhysicalExamConfirmed() { return physicalExamConfirmed; }
    public void setPhysicalExamConfirmed(Boolean physicalExamConfirmed) { this.physicalExamConfirmed = physicalExamConfirmed; }

    public String getFindings() { return findings; }
    public void setFindings(String findings) { this.findings = findings; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getTreatment() { return treatment; }
    public void setTreatment(String treatment) { this.treatment = treatment; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<VaccinationRequest> getVaccinations() { return vaccinations; }
    public void setVaccinations(List<VaccinationRequest> vaccinations) { this.vaccinations = vaccinations; }

    public List<InjuryMarkerRequest> getInjuryMarkers() { return injuryMarkers; }
    public void setInjuryMarkers(List<InjuryMarkerRequest> injuryMarkers) { this.injuryMarkers = injuryMarkers; }

    public CreateNextScheduleRequest getNextSchedule() { return nextSchedule; }
    public void setNextSchedule(CreateNextScheduleRequest nextSchedule) { this.nextSchedule = nextSchedule; }

    public List<HorseHealthMetricRequest> getMetrics() { return metrics; }
    public void setMetrics(List<HorseHealthMetricRequest> metrics) { this.metrics = metrics; }

    private boolean isRejectDecision() {
        return decision == ReviewDecision.REJECT || decision == ReviewDecision.REJECTED;
    }

    @JsonIgnore
    @AssertTrue(message = "Restriction details are required when training is BLOCKED")
    public boolean isRestrictionValid() {
        if (isRejectDecision()) return true;
        if (trainingDecision == TrainingDecision.BLOCKED) {
            return (restrictionDetails != null && !restrictionDetails.isBlank())
                    || (feedback != null && !feedback.isBlank());
        }
        return true;
    }

    @JsonIgnore
    @AssertTrue(message = "Exam findings are required after physical examination")
    public boolean isExamValid() {
        if (isRejectDecision()) return true;
        return Boolean.TRUE.equals(physicalExamConfirmed) && findings != null && !findings.isBlank();
    }

    @JsonIgnore
    @AssertTrue(message = "Training decision is required")
    public boolean isTrainingDecisionValid() {
        if (isRejectDecision()) return true;
        return trainingDecision != null;
    }

    @JsonIgnore
    @AssertTrue(message = "Physical exam confirmation is required")
    public boolean isPhysicalExamConfirmedValid() {
        if (isRejectDecision()) return true;
        return Boolean.TRUE.equals(physicalExamConfirmed);
    }
}
