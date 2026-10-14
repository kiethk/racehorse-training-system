package com.rtms.backend.training.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Trainer ghi nhận kết quả sau buổi tập.
 *
 * Theo đề tài, đây là việc của Head Trainer ("Đánh giá phong độ, ghi nhận chỉ
 * số buổi tập và đưa ra nhận xét chuyên môn sau mỗi buổi tập"), KHÔNG phải của
 * Groom. Groom chỉ tick hoàn thành các việc chăm sóc trong groom_daily_tasks.
 * (The trainer records the result after a workout. Per the project brief this is the head trainer job
 *  ("assess form, record workout metrics and give professional feedback after each workout"), NOT the groom job.
 *  The groom only ticks off care tasks in groom_daily_tasks.)
 */
public class CompleteWorkoutRequest {

    @NotNull(message = "Actual distance is required")
    @DecimalMin(value = "0.1", message = "Actual distance must be greater than 0")
    @DecimalMax(value = "100000", message = "Actual distance is too large")
    private BigDecimal actualDistanceMeters;

    @NotNull(message = "Actual duration is required")
    @DecimalMin(value = "0.1", message = "Actual duration must be greater than 0")
    @DecimalMax(value = "1440", message = "Actual duration cannot exceed 1440 minutes")
    private BigDecimal actualDurationMinutes;

    @NotNull(message = "Top speed is required")
    @DecimalMin(value = "0.1", message = "Top speed must be greater than 0")
    @DecimalMax(value = "100", message = "Top speed cannot exceed 100 km/h")
    private BigDecimal topSpeedKmh;

    @NotNull(message = "Average speed is required")
    @DecimalMin(value = "0.1", message = "Average speed must be greater than 0")
    @DecimalMax(value = "100", message = "Average speed cannot exceed 100 km/h")
    private BigDecimal averageSpeedKmh;

    @NotNull(message = "Average heart rate is required")
    @Min(value = 30, message = "Average heart rate must be at least 30 bpm")
    @Max(value = 240, message = "Average heart rate cannot exceed 240 bpm")
    private Integer averageHeartRate;

    @NotNull(message = "Maximum heart rate is required")
    @Min(value = 30, message = "Maximum heart rate must be at least 30 bpm")
    @Max(value = 240, message = "Maximum heart rate cannot exceed 240 bpm")
    private Integer maxHeartRate;

    @NotNull(message = "Recovery heart rate is required")
    @Min(value = 30, message = "Recovery heart rate must be at least 30 bpm")
    @Max(value = 240, message = "Recovery heart rate cannot exceed 240 bpm")
    private Integer recoveryHeartRate;

    @NotNull(message = "Performance rating is required")
    @Min(value = 1, message = "Performance rating must be between 1 and 10")
    @Max(value = 10, message = "Performance rating must be between 1 and 10")
    private Integer performanceRating;   // 1..10

    @Size(max = 2000, message = "Trainer feedback cannot exceed 2000 characters")
    private String trainerFeedback;

    public BigDecimal getActualDistanceMeters() { return actualDistanceMeters; }
    public void setActualDistanceMeters(BigDecimal v) { this.actualDistanceMeters = v; }

    public BigDecimal getActualDurationMinutes() { return actualDurationMinutes; }
    public void setActualDurationMinutes(BigDecimal v) { this.actualDurationMinutes = v; }

    public BigDecimal getTopSpeedKmh() { return topSpeedKmh; }
    public void setTopSpeedKmh(BigDecimal v) { this.topSpeedKmh = v; }

    public BigDecimal getAverageSpeedKmh() { return averageSpeedKmh; }
    public void setAverageSpeedKmh(BigDecimal v) { this.averageSpeedKmh = v; }

    public Integer getAverageHeartRate() { return averageHeartRate; }
    public void setAverageHeartRate(Integer v) { this.averageHeartRate = v; }

    public Integer getMaxHeartRate() { return maxHeartRate; }
    public void setMaxHeartRate(Integer v) { this.maxHeartRate = v; }

    public Integer getRecoveryHeartRate() { return recoveryHeartRate; }
    public void setRecoveryHeartRate(Integer v) { this.recoveryHeartRate = v; }

    public Integer getPerformanceRating() { return performanceRating; }
    public void setPerformanceRating(Integer v) { this.performanceRating = v; }

    public String getTrainerFeedback() { return trainerFeedback; }
    public void setTrainerFeedback(String v) { this.trainerFeedback = v; }
}
