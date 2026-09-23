package com.rtms.backend.entity;

import com.rtms.backend.enums.RacingReadinessStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Đánh giá mức độ sẵn sàng thi đấu của chiến mã.
 *
 * Dùng cho HAI giai đoạn, phân biệt bằng admissionId:
 *   - admissionId != null : đánh giá trong luồng nhập học
 *   - admissionId == null : đánh giá định kỳ
 *
 * Vì sao dùng FK chứ không phải cờ boolean: một ngựa có thể có nhiều đơn
 * nhập học (bị từ chối rồi nộp lại cùng UELN), nên cần biết đánh giá thuộc
 * đơn nào — boolean không trả lời được câu đó.
 */
@Entity
@Table(name = "racing_readiness_assessments")
public class RacingReadinessAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "horse_id", nullable = false)
    private Long horseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "readiness_status", nullable = false, length = 50)
    private RacingReadinessStatus readinessStatus;

    /** Thang 0–10. NULL ở bước nhập học vì ngựa đang cách ly, không đo được. */
    @Column(name = "fitness_score")
    private BigDecimal fitnessScore;

    // ===== Trainer đánh giá được trong khu cách ly (quan sát + dắt tay) =====

    /** Dáng vóc, cấu trúc cơ thể. Thang 0–10. */
    @Column(name = "conformation_score")
    private BigDecimal conformationScore;

    /** Tính nết khi tiếp xúc, dắt tay. Thang 0–10. */
    @Column(name = "temperament_score")
    private BigDecimal temperamentScore;

    /** Chất lượng bước đi khi dắt bộ/kiệu. Thang 0–10. */
    @Column(name = "gait_quality_score")
    private BigDecimal gaitQualityScore;

    /** Ước tính số tháng nữa mới đua được — con số Manager dùng để tính chi phí. */
    @Column(name = "estimated_months_to_race")
    private Integer estimatedMonthsToRace;

    @Column(name = "assessment_date", nullable = false)
    private LocalDate assessmentDate;

    /** Đánh giá định kỳ: +90 ngày. Đánh giá nhập học: NULL (có CHECK ràng buộc). */
    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(length = 3000)
    private String remarks;

    @Column(name = "trainer_id")
    private Long trainerId;

    /** NULL = đánh giá định kỳ. */
    @Column(name = "admission_id")
    private Long admissionId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (assessmentDate == null) {
            assessmentDate = LocalDate.now();
        }
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getHorseId() { return horseId; }
    public void setHorseId(Long horseId) { this.horseId = horseId; }

    public RacingReadinessStatus getReadinessStatus() { return readinessStatus; }
    public void setReadinessStatus(RacingReadinessStatus v) { this.readinessStatus = v; }

    public BigDecimal getFitnessScore() { return fitnessScore; }
    public void setFitnessScore(BigDecimal v) { this.fitnessScore = v; }

    public BigDecimal getConformationScore() { return conformationScore; }
    public void setConformationScore(BigDecimal v) { this.conformationScore = v; }

    public BigDecimal getTemperamentScore() { return temperamentScore; }
    public void setTemperamentScore(BigDecimal v) { this.temperamentScore = v; }

    public BigDecimal getGaitQualityScore() { return gaitQualityScore; }
    public void setGaitQualityScore(BigDecimal v) { this.gaitQualityScore = v; }

    public Integer getEstimatedMonthsToRace() { return estimatedMonthsToRace; }
    public void setEstimatedMonthsToRace(Integer v) { this.estimatedMonthsToRace = v; }

    public LocalDate getAssessmentDate() { return assessmentDate; }
    public void setAssessmentDate(LocalDate v) { this.assessmentDate = v; }

    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate v) { this.validUntil = v; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Long getTrainerId() { return trainerId; }
    public void setTrainerId(Long trainerId) { this.trainerId = trainerId; }

    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}