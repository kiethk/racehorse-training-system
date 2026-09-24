package com.rtms.backend.dto;

import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.RacingReadinessAssessment;

import java.util.List;

/**
 * Màn hình Trainer xem hồ sơ candidate.
 *
 * Bọc AdmissionDetailResponse (đã có sẵn: CandidateHorseProfile, kết quả Groom,
 * kết quả Vet, chuồng cách ly, documents) rồi bổ sung Horse + phần y tế.
 *
 * KHÔNG sửa AdmissionDetailResponse vì nó dùng chung với controller của Vet
 * và Manager — đó là code của đồng đội.
 */
public class TrainerAdmissionViewResponse {

    private AdmissionDetailResponse admission;
    private Horse horse;

    /** RỖNG nếu module Thú y chưa ghi dữ liệu — không phải lỗi. */
    private List<Object> healthRecords;
    private List<Object> healthMetrics;

    /** Đã đánh giá rồi thì trả về để FE hiển thị lại; chưa thì null. */
    private RacingReadinessAssessment existingAssessment;

    public TrainerAdmissionViewResponse(AdmissionDetailResponse admission, Horse horse,
                                        List<Object> healthRecords, List<Object> healthMetrics,
                                        RacingReadinessAssessment existingAssessment) {
        this.admission = admission;
        this.horse = horse;
        this.healthRecords = healthRecords;
        this.healthMetrics = healthMetrics;
        this.existingAssessment = existingAssessment;
    }

    public AdmissionDetailResponse getAdmission() { return admission; }
    public Horse getHorse() { return horse; }
    public List<Object> getHealthRecords() { return healthRecords; }
    public List<Object> getHealthMetrics() { return healthMetrics; }
    public RacingReadinessAssessment getExistingAssessment() { return existingAssessment; }
}