package com.rtms.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public class LatestVitalsResponse {
    private Long horseId;
    private BigDecimal heartRate;
    private BigDecimal temperature;
    private BigDecimal respiratoryRate;
    private BigDecimal weight;
    private BigDecimal bodyConditionScore;
    private LocalDateTime recordedAt;
    private Map<String, VitalThresholdDto> thresholds;

    public LatestVitalsResponse() {}

    public Long getHorseId() { return horseId; }
    public void setHorseId(Long horseId) { this.horseId = horseId; }
    public BigDecimal getHeartRate() { return heartRate; }
    public void setHeartRate(BigDecimal heartRate) { this.heartRate = heartRate; }
    public BigDecimal getTemperature() { return temperature; }
    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
    public BigDecimal getRespiratoryRate() { return respiratoryRate; }
    public void setRespiratoryRate(BigDecimal respiratoryRate) { this.respiratoryRate = respiratoryRate; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public BigDecimal getBodyConditionScore() { return bodyConditionScore; }
    public void setBodyConditionScore(BigDecimal bodyConditionScore) { this.bodyConditionScore = bodyConditionScore; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
    public Map<String, VitalThresholdDto> getThresholds() { return thresholds; }
    public void setThresholds(Map<String, VitalThresholdDto> thresholds) { this.thresholds = thresholds; }
}
