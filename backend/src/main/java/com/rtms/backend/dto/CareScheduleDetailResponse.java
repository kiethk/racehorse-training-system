package com.rtms.backend.dto;

import com.rtms.backend.entity.CareSchedule;
import com.rtms.backend.entity.HealthRecord;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.User;
import com.rtms.backend.enums.TrainingStatus;

import java.util.List;

public record CareScheduleDetailResponse(
        CareScheduleResponse schedule,
        HorseSummary horse,
        VetSummary veterinarian,
        HealthRecordSummary healthRecord,
        List<VetOfferResponse> offers
) {
    public record HorseSummary(Long id, String name, String breed, String registrationNumber, TrainingStatus trainingStatus) {}
    public record VetSummary(Long id, String fullName, String email) {}
    public record HealthRecordSummary(Long id, String findings, String diagnosis, String treatment, String trainingDecision, String restrictionDetails) {}

    public static CareScheduleDetailResponse of(CareSchedule cs, Horse h, User v, HealthRecord hr, List<VetOfferResponse> offers) {
        HorseSummary horseSummary = h != null ? new HorseSummary(h.getId(), h.getName(), h.getBreed(), h.getRegistrationNumber(), h.getTrainingStatus()) : null;
        VetSummary vetSummary = v != null ? new VetSummary(v.getId(), v.getFullName(), v.getEmail()) : null;
        HealthRecordSummary hrSummary = hr != null ? new HealthRecordSummary(
                hr.getId(), hr.getFindings(), hr.getDiagnosis(), hr.getTreatment(),
                hr.getTrainingDecision() != null ? hr.getTrainingDecision().name() : null,
                hr.getRestrictionDetails()
        ) : null;
        return new CareScheduleDetailResponse(CareScheduleResponse.from(cs), horseSummary, vetSummary, hrSummary, offers);
    }
}