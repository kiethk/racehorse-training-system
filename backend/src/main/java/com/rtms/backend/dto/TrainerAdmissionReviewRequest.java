package com.rtms.backend.dto;

import com.rtms.backend.enums.RacingReadinessStatus;
import java.math.BigDecimal;

/**
 * Trainer hoàn thành đánh giá trong luồng nhập học.
 *
 * KHÔNG có trường decision — Trainer không approve/reject (V37 đã xoá cột
 * trainer_decision). Đánh giá xong là đơn tự sang MANAGER_REVIEW.
 *
 * KHÔNG có fitnessScore: ngựa đang cách ly, không đưa ra đường chạy được.
 * KHÔNG có healthScore: thuộc chuyên môn Thú y (xem horse_health_metrics).
 */
public class TrainerAdmissionReviewRequest {

    /** Bắt buộc. READY | NEEDS_MORE_TRAINING | UNSUITABLE */
    private RacingReadinessStatus readinessStatus;

    /** Dáng vóc, thang 0–10. */
    private BigDecimal conformationScore;

    /** Tính nết, thang 0–10. */
    private BigDecimal temperamentScore;

    /** Bước đi khi dắt tay, thang 0–10. */
    private BigDecimal gaitQualityScore;

    /** Ước tính số tháng nữa mới đua được — Manager dùng để tính chi phí nuôi. */
    private Integer estimatedMonthsToRace;

    /** Nhận xét chuyên môn. Đây là phần Manager đọc kỹ nhất. */
    private String remarks;

    public RacingReadinessStatus getReadinessStatus() { return readinessStatus; }
    public void setReadinessStatus(RacingReadinessStatus v) { this.readinessStatus = v; }

    public BigDecimal getConformationScore() { return conformationScore; }
    public void setConformationScore(BigDecimal v) { this.conformationScore = v; }

    public BigDecimal getTemperamentScore() { return temperamentScore; }
    public void setTemperamentScore(BigDecimal v) { this.temperamentScore = v; }

    public BigDecimal getGaitQualityScore() { return gaitQualityScore; }
    public void setGaitQualityScore(BigDecimal v) { this.gaitQualityScore = v; }

    public Integer getEstimatedMonthsToRace() { return estimatedMonthsToRace; }
    public void setEstimatedMonthsToRace(Integer v) { this.estimatedMonthsToRace = v; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}