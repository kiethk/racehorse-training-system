package com.rtms.backend.medical.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rtms.backend.training.enums.TrainingDecision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CompleteCareScheduleRequest {

    @NotBlank(message = "Findings are required")
    private String findings;

    @NotBlank(message = "Diagnosis is required")
    private String diagnosis;

    private String treatment;

    /** Hướng dẫn chăm sóc chung cho toàn đợt điều trị (WF-04 §3.4) — lưu vào treatment_plan.instructions. */
    private String generalCareInstructions;

    @NotNull(message = "Training decision is required")
    private TrainingDecision trainingDecision;

    /** Lý do chặn tập — bắt buộc khi BLOCKED, Trainer đọc thấy khi lập kế hoạch. */
    private String restrictionDetails;

    private String notes;
    private List<@NotNull @Valid HorseHealthMetricRequest> metrics;

    /** Marker chấn thương ghi nhận cùng ca khám; lưu nguyên tử với HealthRecord. */
    private List<@Valid InjuryMarkerRequest> injuryMarkers;

    /** Lịch khám lại — bắt buộc khi BLOCKED: "tạm nghỉ đến" chính là ngày của lịch này. */
    private CreateNextScheduleRequest nextSchedule;

    /**
     * Nhiều lịch tái khám cùng lúc (Vet luân phiên thăm ngựa vài ngày gần nhau).
     * nextSchedule đơn vẫn dùng được cho compatibility.
     */
    private List<@Valid CreateNextScheduleRequest> nextSchedules;

    /** Mũi tiêm phòng / tẩy giun ghi nhận cùng ca khám; lưu nguyên tử với kết quả khám. */
    private List<@NotNull @Valid VaccinationRequest> vaccinations;

    /**
     * Hoãn tiêm khi hoàn tất mốc tiêm phòng mà ngựa chưa đủ khỏe (sốt, đang điều trị):
     * mốc mới đặt sau số ngày này, lý do UNDER_TREATMENT. Chỉ dùng khi không gửi vaccinations.
     */
    @Min(value = 7, message = "Vaccination can be deferred by 7 to 10 days")
    @Max(value = 10, message = "Vaccination can be deferred by 7 to 10 days")
    private Integer deferVaccinationDays;

    /** Đơn thuốc kê từ danh mục chuẩn, lưu nguyên tử cùng bệnh án khi hoàn tất ca. */
    private List<@NotNull @Valid PrescriptionItemRequest> prescriptions;

    /** Bệnh lý/chấn thương cũ Vet xác nhận khỏi — CHỈ các id này được đóng (C7: không auto mass-recovery). */
    private List<Long> resolvedConditionIds;

    public List<PrescriptionItemRequest> getPrescriptions() { return prescriptions; }
    public void setPrescriptions(List<PrescriptionItemRequest> prescriptions) { this.prescriptions = prescriptions; }

    public List<Long> getResolvedConditionIds() { return resolvedConditionIds; }
    public void setResolvedConditionIds(List<Long> resolvedConditionIds) { this.resolvedConditionIds = resolvedConditionIds; }

    public Integer getDeferVaccinationDays() { return deferVaccinationDays; }
    public void setDeferVaccinationDays(Integer deferVaccinationDays) { this.deferVaccinationDays = deferVaccinationDays; }

    public List<VaccinationRequest> getVaccinations() { return vaccinations; }
    public void setVaccinations(List<VaccinationRequest> vaccinations) { this.vaccinations = vaccinations; }

    public String getFindings() { return findings; }
    public void setFindings(String findings) { this.findings = findings; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getTreatment() { return treatment; }
    public void setTreatment(String treatment) { this.treatment = treatment; }

    public String getGeneralCareInstructions() { return generalCareInstructions; }
    public void setGeneralCareInstructions(String generalCareInstructions) { this.generalCareInstructions = generalCareInstructions; }

    public TrainingDecision getTrainingDecision() { return trainingDecision; }
    public void setTrainingDecision(TrainingDecision trainingDecision) { this.trainingDecision = trainingDecision; }

    public String getRestrictionDetails() { return restrictionDetails; }
    public void setRestrictionDetails(String restrictionDetails) { this.restrictionDetails = restrictionDetails; }


    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public CreateNextScheduleRequest getNextSchedule() { return nextSchedule; }
    public void setNextSchedule(CreateNextScheduleRequest nextSchedule) { this.nextSchedule = nextSchedule; }

    public List<CreateNextScheduleRequest> getNextSchedules() { return nextSchedules; }
    public void setNextSchedules(List<CreateNextScheduleRequest> nextSchedules) { this.nextSchedules = nextSchedules; }

    public List<HorseHealthMetricRequest> getMetrics() { return metrics; }
    public void setMetrics(List<HorseHealthMetricRequest> metrics) { this.metrics = metrics; }

    public List<InjuryMarkerRequest> getInjuryMarkers() { return injuryMarkers; }
    public void setInjuryMarkers(List<InjuryMarkerRequest> injuryMarkers) { this.injuryMarkers = injuryMarkers; }

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
        boolean hasFollowUp = nextSchedule != null || (nextSchedules != null && !nextSchedules.isEmpty());
        return trainingDecision != TrainingDecision.BLOCKED || hasFollowUp;
    }
}
