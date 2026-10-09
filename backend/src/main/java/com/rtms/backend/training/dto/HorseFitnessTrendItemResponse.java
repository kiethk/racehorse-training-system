package com.rtms.backend.training.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class HorseFitnessTrendItemResponse {
    private Long workoutId;
    private LocalDate date;
    private String subjectName;
    private BigDecimal distanceMeters;
    private BigDecimal actualDurationMinutes;
    private BigDecimal averageSpeedKmh;
    private BigDecimal topSpeedKmh;
    private Integer averageHeartRate;
    private Integer maxHeartRate;
    private Integer recoveryHeartRate;
    private Integer performanceRating;

    public HorseFitnessTrendItemResponse() {
    }

    public Long getWorkoutId() {
        return workoutId;
    }

    public void setWorkoutId(Long workoutId) {
        this.workoutId = workoutId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public BigDecimal getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(BigDecimal distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public BigDecimal getActualDurationMinutes() {
        return actualDurationMinutes;
    }

    public void setActualDurationMinutes(BigDecimal actualDurationMinutes) {
        this.actualDurationMinutes = actualDurationMinutes;
    }

    public BigDecimal getAverageSpeedKmh() {
        return averageSpeedKmh;
    }

    public void setAverageSpeedKmh(BigDecimal averageSpeedKmh) {
        this.averageSpeedKmh = averageSpeedKmh;
    }

    public BigDecimal getTopSpeedKmh() {
        return topSpeedKmh;
    }

    public void setTopSpeedKmh(BigDecimal topSpeedKmh) {
        this.topSpeedKmh = topSpeedKmh;
    }

    public Integer getAverageHeartRate() {
        return averageHeartRate;
    }

    public void setAverageHeartRate(Integer averageHeartRate) {
        this.averageHeartRate = averageHeartRate;
    }

    public Integer getMaxHeartRate() {
        return maxHeartRate;
    }

    public void setMaxHeartRate(Integer maxHeartRate) {
        this.maxHeartRate = maxHeartRate;
    }

    public Integer getRecoveryHeartRate() {
        return recoveryHeartRate;
    }

    public void setRecoveryHeartRate(Integer recoveryHeartRate) {
        this.recoveryHeartRate = recoveryHeartRate;
    }

    public Integer getPerformanceRating() {
        return performanceRating;
    }

    public void setPerformanceRating(Integer performanceRating) {
        this.performanceRating = performanceRating;
    }
}
