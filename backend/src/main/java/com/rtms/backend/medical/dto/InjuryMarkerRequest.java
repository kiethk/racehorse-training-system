package com.rtms.backend.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InjuryMarkerRequest {
    @NotBlank
    private String injuryType;
    @NotNull
    private Long bodyRegionId;
    @NotBlank
    private String severity;
    private String description;
    @NotNull
    private LocalDateTime diagnosedAt;
    private BigDecimal positionX;
    private BigDecimal positionY;
    private BigDecimal positionZ;
    private String zoneId;
    private BigDecimal localX;
    private BigDecimal localY;
    private BigDecimal localZ;
    private BigDecimal normalX;
    private BigDecimal normalY;
    private BigDecimal normalZ;
    private String meshName;

    public String getInjuryType() { return injuryType; }
    public void setInjuryType(String injuryType) { this.injuryType = injuryType; }
    public Long getBodyRegionId() { return bodyRegionId; }
    public void setBodyRegionId(Long bodyRegionId) { this.bodyRegionId = bodyRegionId; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDiagnosedAt() { return diagnosedAt; }
    public void setDiagnosedAt(LocalDateTime diagnosedAt) { this.diagnosedAt = diagnosedAt; }
    public BigDecimal getPositionX() { return positionX; }
    public void setPositionX(BigDecimal positionX) { this.positionX = positionX; }
    public BigDecimal getPositionY() { return positionY; }
    public void setPositionY(BigDecimal positionY) { this.positionY = positionY; }
    public BigDecimal getPositionZ() { return positionZ; }
    public void setPositionZ(BigDecimal positionZ) { this.positionZ = positionZ; }
    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
    public BigDecimal getLocalX() { return localX; }
    public void setLocalX(BigDecimal localX) { this.localX = localX; }
    public BigDecimal getLocalY() { return localY; }
    public void setLocalY(BigDecimal localY) { this.localY = localY; }
    public BigDecimal getLocalZ() { return localZ; }
    public void setLocalZ(BigDecimal localZ) { this.localZ = localZ; }
    public BigDecimal getNormalX() { return normalX; }
    public void setNormalX(BigDecimal normalX) { this.normalX = normalX; }
    public BigDecimal getNormalY() { return normalY; }
    public void setNormalY(BigDecimal normalY) { this.normalY = normalY; }
    public BigDecimal getNormalZ() { return normalZ; }
    public void setNormalZ(BigDecimal normalZ) { this.normalZ = normalZ; }
    public String getMeshName() { return meshName; }
    public void setMeshName(String meshName) { this.meshName = meshName; }
}