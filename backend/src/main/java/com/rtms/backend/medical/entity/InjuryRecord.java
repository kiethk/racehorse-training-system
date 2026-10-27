package com.rtms.backend.medical.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "injury_records")
public class InjuryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "horse_id", nullable = false)
    private Long horseId;

    @Column(name = "health_record_id", nullable = false)
    private Long healthRecordId;

    @Column(name = "injury_type", nullable = false)
    private String injuryType;

    @Column(name = "body_region_id", nullable = false)
    private Long bodyRegionId;

    @Column(name = "severity", nullable = false, length = 50)
    private String severity;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "diagnosed_at", nullable = false)
    private LocalDateTime diagnosedAt;

    @Column(name = "expected_recovery_date")
    private LocalDate expectedRecoveryDate;

    @Column(name = "recovered_at")
    private LocalDateTime recoveredAt;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "ACTIVE";

    @Column(name = "position_x", precision = 10, scale = 4)
    private BigDecimal positionX;

    @Column(name = "position_y", precision = 10, scale = 4)
    private BigDecimal positionY;

    @Column(name = "position_z", precision = 10, scale = 4)
    private BigDecimal positionZ;

    @Column(name = "zone_id", length = 64)
    private String zoneId;

    @Column(name = "local_x", precision = 10, scale = 4)
    private BigDecimal localX;

    @Column(name = "local_y", precision = 10, scale = 4)
    private BigDecimal localY;

    @Column(name = "local_z", precision = 10, scale = 4)
    private BigDecimal localZ;

    @Column(name = "normal_x", precision = 10, scale = 4)
    private BigDecimal normalX;

    @Column(name = "normal_y", precision = 10, scale = 4)
    private BigDecimal normalY;

    @Column(name = "normal_z", precision = 10, scale = 4)
    private BigDecimal normalZ;

    @Column(name = "mesh_name", length = 128)
    private String meshName;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getHorseId() {
        return horseId;
    }

    public void setHorseId(Long horseId) {
        this.horseId = horseId;
    }

    public Long getHealthRecordId() {
        return healthRecordId;
    }

    public void setHealthRecordId(Long healthRecordId) {
        this.healthRecordId = healthRecordId;
    }

    public String getInjuryType() {
        return injuryType;
    }

    public void setInjuryType(String injuryType) {
        this.injuryType = injuryType;
    }

    public Long getBodyRegionId() {
        return bodyRegionId;
    }

    public void setBodyRegionId(Long bodyRegionId) {
        this.bodyRegionId = bodyRegionId;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDiagnosedAt() {
        return diagnosedAt;
    }

    public void setDiagnosedAt(LocalDateTime diagnosedAt) {
        this.diagnosedAt = diagnosedAt;
    }

    public LocalDate getExpectedRecoveryDate() {
        return expectedRecoveryDate;
    }

    public void setExpectedRecoveryDate(LocalDate expectedRecoveryDate) {
        this.expectedRecoveryDate = expectedRecoveryDate;
    }

    public LocalDateTime getRecoveredAt() {
        return recoveredAt;
    }

    public void setRecoveredAt(LocalDateTime recoveredAt) {
        this.recoveredAt = recoveredAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getPositionX() {
        return positionX;
    }

    public void setPositionX(BigDecimal positionX) {
        this.positionX = positionX;
    }

    public BigDecimal getPositionY() {
        return positionY;
    }

    public void setPositionY(BigDecimal positionY) {
        this.positionY = positionY;
    }

    public BigDecimal getPositionZ() {
        return positionZ;
    }

    public void setPositionZ(BigDecimal positionZ) {
        this.positionZ = positionZ;
    }

    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
    public BigDecimal getLocalX() { return localX; }
    public void setLocalX(BigDecimal v) { this.localX = v; }
    public BigDecimal getLocalY() { return localY; }
    public void setLocalY(BigDecimal v) { this.localY = v; }
    public BigDecimal getLocalZ() { return localZ; }
    public void setLocalZ(BigDecimal v) { this.localZ = v; }
    public BigDecimal getNormalX() { return normalX; }
    public void setNormalX(BigDecimal v) { this.normalX = v; }
    public BigDecimal getNormalY() { return normalY; }
    public void setNormalY(BigDecimal v) { this.normalY = v; }
    public BigDecimal getNormalZ() { return normalZ; }
    public void setNormalZ(BigDecimal v) { this.normalZ = v; }
    public String getMeshName() { return meshName; }
    public void setMeshName(String meshName) { this.meshName = meshName; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
