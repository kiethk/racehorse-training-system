package com.rtms.backend.dto;

import java.math.BigDecimal;

public class VitalThresholdDto {
    private BigDecimal warningMin;
    private BigDecimal warningMax;
    private BigDecimal dangerMin;
    private BigDecimal dangerMax;
    private String unit;

    public VitalThresholdDto() {}

    public VitalThresholdDto(BigDecimal warningMin, BigDecimal warningMax,
                             BigDecimal dangerMin, BigDecimal dangerMax, String unit) {
        this.warningMin = warningMin;
        this.warningMax = warningMax;
        this.dangerMin = dangerMin;
        this.dangerMax = dangerMax;
        this.unit = unit;
    }

    public BigDecimal getWarningMin() { return warningMin; }
    public void setWarningMin(BigDecimal warningMin) { this.warningMin = warningMin; }
    public BigDecimal getWarningMax() { return warningMax; }
    public void setWarningMax(BigDecimal warningMax) { this.warningMax = warningMax; }
    public BigDecimal getDangerMin() { return dangerMin; }
    public void setDangerMin(BigDecimal dangerMin) { this.dangerMin = dangerMin; }
    public BigDecimal getDangerMax() { return dangerMax; }
    public void setDangerMax(BigDecimal dangerMax) { this.dangerMax = dangerMax; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}
