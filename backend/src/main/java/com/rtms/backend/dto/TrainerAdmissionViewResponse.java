package com.rtms.backend.dto;
import com.rtms.backend.entity.CandidateHorseProfile;
import com.rtms.backend.entity.HealthRecord;
import com.rtms.backend.entity.HorseHealthMetric;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.RacingReadinessAssessment;

import java.util.List;

/**
 * Màn hình Trainer xem hồ sơ candidate.
 */
public class TrainerAdmissionViewResponse {

    private AdmissionDetailResponse admission;
    private Horse horse;

    /** Danh sách bản ghi khám của Vet (HealthRecord). */
    private List<?> healthRecords;
    /** Danh sách chỉ số sinh hiệu (HorseHealthMetric). */
    private List<?> healthMetrics;

    /** Đã đánh giá rồi thì trả về để FE hiển thị lại; chưa thì null. */
    private RacingReadinessAssessment existingAssessment;

    /** Lịch đánh giá của Head Trainer */
    private TrainerScheduleResponse trainerSchedule;

    public TrainerAdmissionViewResponse(AdmissionDetailResponse admission, Horse horse,
                                        List<?> healthRecords, List<?> healthMetrics,
                                        RacingReadinessAssessment existingAssessment) {
        this(admission, horse, healthRecords, healthMetrics, existingAssessment, null);
    }

    public TrainerAdmissionViewResponse(AdmissionDetailResponse admission, Horse horse,
                                        List<?> healthRecords, List<?> healthMetrics,
                                        RacingReadinessAssessment existingAssessment,
                                        TrainerScheduleResponse trainerSchedule) {
        this.admission = admission;
        this.horse = horse;
        this.healthRecords = healthRecords;
        this.healthMetrics = healthMetrics;
        this.existingAssessment = existingAssessment;
        this.trainerSchedule = trainerSchedule;
    }

    public AdmissionDetailResponse getAdmission() { return admission; }
    public Horse getHorse() { return horse; }
    public List<?> getHealthRecords() { return healthRecords; }
    public List<?> getHealthMetrics() { return healthMetrics; }
    public RacingReadinessAssessment getExistingAssessment() { return existingAssessment; }
    public TrainerScheduleResponse getTrainerSchedule() { return trainerSchedule; }
}