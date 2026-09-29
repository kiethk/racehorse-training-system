package com.rtms.backend.dto;

import java.time.LocalDateTime;

/**
 * Một cảnh báo thể lực / nguy cơ chấn thương của chiến mã.
 *
 * KHỚP CHÍNH XÁC với interface HorseAlert ở frontend
 * (features/training/types/index.ts). Lệch tên trường ở đây là giao diện hiện
 * "undefined" hoặc "Invalid Date" mà TypeScript KHÔNG bắt được — nó chỉ kiểm
 * nội bộ frontend, không đối chiếu với DTO Java.
 *
 * Cảnh báo được TÍNH KHI ĐỌC, không lưu bảng: ngưỡng có thể đổi, mà bản ghi cũ
 * thì không cập nhật theo được.
 */
public class HorseAlertResponse {

    private String ruleCode;
    private String title;
    private String description;

    /**
     * Chỉ hai mức: DANGER (đỏ) và WARNING (vàng).
     *
     * Trước đây backend trả CRITICAL/HIGH/WARNING còn frontend chỉ hiểu
     * WARNING/DANGER, nên mọi cảnh báo đều rơi vào nhánh vàng — kể cả nhịp tim
     * vượt ngưỡng, thứ đáng lẽ phải đỏ.
     */
    private String severity;

    /**
     * Thời điểm SỰ VIỆC XẢY RA, không phải lúc mở màn hình.
     *
     * Quan trọng với luật 1 và 2: chúng quét 30 ngày gần nhất, nên cảnh báo vẫn
     * hiện dù buổi tập mới nhất đã bình thường trở lại. Giao diện phải hiện ngày
     * này để người dùng biết đây là chuyện cũ, tránh tưởng hệ thống hỏng khi sửa
     * chỉ số rồi mà đèn cảnh báo chưa tắt.
     */
    private LocalDateTime triggeredAt;

    /** Giá trị đo được đã kích hoạt cảnh báo. */
    private Double metricValue;

    /** Ngưỡng bị vượt. */
    private Double thresholdValue;

    public HorseAlertResponse() {
    }

    public HorseAlertResponse(String ruleCode, String title, String description,
                              String severity, LocalDateTime triggeredAt,
                              Double metricValue, Double thresholdValue) {
        this.ruleCode = ruleCode;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.triggeredAt = triggeredAt;
        this.metricValue = metricValue;
        this.thresholdValue = thresholdValue;
    }

    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public LocalDateTime getTriggeredAt() { return triggeredAt; }
    public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }

    public Double getMetricValue() { return metricValue; }
    public void setMetricValue(Double metricValue) { this.metricValue = metricValue; }

    public Double getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(Double thresholdValue) { this.thresholdValue = thresholdValue; }
}
