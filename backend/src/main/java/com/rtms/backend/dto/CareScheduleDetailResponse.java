package com.rtms.backend.dto;
import com.rtms.backend.entity.CareSchedule;
import com.rtms.backend.entity.HealthRecord;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.User;
import com.rtms.backend.enums.TrainingDecision;

public record CareScheduleDetailResponse(
        CareScheduleResponse schedule,
        HorseSummary horse,
        VetSummary veterinarian,
        HealthRecordSummary healthRecord
) {
    public record HorseSummary(Long id, String name, String breed, String registrationNumber, TrainingDecision trainingDecision) {}
    public record VetSummary(Long id, String fullName, String email) {}
    public record HealthRecordSummary(Long id, String findings, String diagnosis, String treatment, TrainingDecision trainingDecision, String restrictionDetails) {}

    public static CareScheduleDetailResponse of(CareSchedule cs, Horse h, User v, HealthRecord hr) {
        HorseSummary horseSummary = h != null ? new HorseSummary(h.getId(), h.getName(), h.getBreed(), h.getRegistrationNumber(), h.getTrainingDecision()) : null;
        VetSummary vetSummary = v != null ? new VetSummary(v.getId(), v.getFullName(), v.getEmail()) : null;
        HealthRecordSummary hrSummary = hr != null ? new HealthRecordSummary(
                hr.getId(), hr.getFindings(), hr.getDiagnosis(), hr.getTreatment(),
                hr.getTrainingDecision(),
                hr.getRestrictionDetails()
        ) : null;
        return new CareScheduleDetailResponse(CareScheduleResponse.from(cs), horseSummary, vetSummary, hrSummary);
    }
}
