package com.rtms.backend.medical.dto;

import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.training.enums.TrainingDecision;

import java.time.LocalDateTime;

public record CareScheduleDetailResponse(
        CareScheduleResponse schedule,
        HorseSummary horse,
        VetSummary veterinarian,
        HealthRecordSummary healthRecord,
        /** Ca từ báo cáo trên ngựa đang bị chặn: lần khám sẽ quyết định việc mở chặn, null nếu ca này tự quyết định được. */
        LocalDateTime blockHeldUntil,
        /** Lịch Follow-up đang chờ của ngựa, null nếu chưa có. Mỗi ngựa chỉ có một lịch như vậy. */
        WaitingFollowUp waitingFollowUp
) {
    public record WaitingFollowUp(LocalDateTime at, String veterinarianName) {}
    public record HorseSummary(Long id, String name, String breed, String registrationNumber, TrainingDecision trainingDecision) {}
    public record VetSummary(Long id, String fullName, String email) {}
    public record HealthRecordSummary(Long id, String findings, String diagnosis, String treatment, TrainingDecision trainingDecision, String restrictionDetails) {}

    public static CareScheduleDetailResponse of(CareSchedule cs, Horse h, User v, HealthRecord hr,
            LocalDateTime blockHeldUntil, WaitingFollowUp waitingFollowUp) {
        HorseSummary horseSummary = h != null ? new HorseSummary(h.getId(), h.getName(), h.getBreed(), h.getRegistrationNumber(), h.getTrainingDecision()) : null;
        VetSummary vetSummary = v != null ? new VetSummary(v.getId(), v.getFullName(), v.getEmail()) : null;
        HealthRecordSummary hrSummary = hr != null ? new HealthRecordSummary(
                hr.getId(), hr.getFindings(), hr.getDiagnosis(), hr.getTreatment(),
                hr.getTrainingDecision(),
                hr.getRestrictionDetails()
        ) : null;
        return new CareScheduleDetailResponse(CareScheduleResponse.from(cs), horseSummary, vetSummary, hrSummary, blockHeldUntil, waitingFollowUp);
    }
}
