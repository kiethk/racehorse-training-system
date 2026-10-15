package com.rtms.backend.dto;

import com.rtms.backend.enums.TrainingDecision;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class BatchCareScheduleRowRequest {
    @NotNull
    private Long scheduleId;
    private BigDecimal heartRate;
    private BigDecimal temperature;
    private BigDecimal respiratoryRate;
    private BigDecimal weight;
    private BigDecimal bodyConditionScore;
    private TrainingDecision trainingDecision = TrainingDecision.ALLOWED;
    private String restrictionDetails;
    private LocalDate nextScheduleDate;
    private String notes;

    public BatchCareScheduleRowRequest() {}

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
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
    public TrainingDecision getTrainingDecision() { return trainingDecision; }
    public void setTrainingDecision(TrainingDecision trainingDecision) { this.trainingDecision = trainingDecision; }
    public String getRestrictionDetails() { return restrictionDetails; }
    public void setRestrictionDetails(String restrictionDetails) { this.restrictionDetails = restrictionDetails; }
    public LocalDate getNextScheduleDate() { return nextScheduleDate; }
    public void setNextScheduleDate(LocalDate nextScheduleDate) { this.nextScheduleDate = nextScheduleDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
