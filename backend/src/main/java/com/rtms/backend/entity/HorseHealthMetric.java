package com.rtms.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "horse_health_metrics")
public class HorseHealthMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "horse_id", nullable = false)
    private Long horseId;
    @Column(name = "health_record_id")
    private Long healthRecordId;
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
    @Column(name = "heart_rate")
    private BigDecimal heartRate;
    private BigDecimal temperature;
    private BigDecimal weight;
    @Column(name = "respiratory_rate")
    private BigDecimal respiratoryRate;
    @Column(name = "hydration_status", length = 50)
    private String hydrationStatus;
    @Column(name = "body_condition_score")
    private BigDecimal bodyConditionScore;
    @Column(length = 2000)
    private String notes;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Long getHorseId() { return horseId; }
    public void setHorseId(Long horseId) { this.horseId = horseId; }
    public Long getHealthRecordId() { return healthRecordId; }
    public void setHealthRecordId(Long healthRecordId) { this.healthRecordId = healthRecordId; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
}
