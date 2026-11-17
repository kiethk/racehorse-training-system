package com.rtms.backend.admission.dto;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.entity.HorseHealthMetric;

import com.rtms.backend.horse.entity.Horse;

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

    public TrainerAdmissionViewResponse(AdmissionDetailResponse admission, Horse horse,
                                        List<?> healthRecords, List<?> healthMetrics) {
        this.admission = admission;
        this.horse = horse;
        this.healthRecords = healthRecords;
        this.healthMetrics = healthMetrics;
    }

    public AdmissionDetailResponse getAdmission() { return admission; }
    public Horse getHorse() { return horse; }
    public List<?> getHealthRecords() { return healthRecords; }
    public List<?> getHealthMetrics() { return healthMetrics; }
}
