package com.rtms.backend.medical.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

public class HorseHealthMetricRequest {
    @Digits(integer = 4, fraction = 2)
    private BigDecimal heartRate;
    @Digits(integer = 3, fraction = 2)
    private BigDecimal temperature;
    @Digits(integer = 6, fraction = 2)
    private BigDecimal weight;
    @Digits(integer = 4, fraction = 2)
    private BigDecimal respiratoryRate;
    @Size(max = 50)
    private String hydrationStatus;
    @Digits(integer = 2, fraction = 1)
    private BigDecimal bodyConditionScore;
    @Size(max = 2000)
    private String notes;

    public BigDecimal getHeartRate() { return heartRate; }
    public void setHeartRate(BigDecimal heartRate) { this.heartRate = heartRate; }
    public BigDecimal getTemperature() { return temperature; }
    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public BigDecimal getRespiratoryRate() { return respiratoryRate; }
    public void setRespiratoryRate(BigDecimal respiratoryRate) { this.respiratoryRate = respiratoryRate; }
    public String getHydrationStatus() { return hydrationStatus; }
    public void setHydrationStatus(String hydrationStatus) { this.hydrationStatus = hydrationStatus; }
    public BigDecimal getBodyConditionScore() { return bodyConditionScore; }
    public void setBodyConditionScore(BigDecimal bodyConditionScore) { this.bodyConditionScore = bodyConditionScore; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
