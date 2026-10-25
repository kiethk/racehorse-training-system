package com.rtms.backend.medical.dto;

import java.math.BigDecimal;

/**
 * Optional 3D marker placement carried by a correction request. Every field is
 * nullable so the caller may override only the samples it captured on the
 * model; absent fields keep the stored marker data unchanged.
 */
public record MarkerPlacementRequest(
        BigDecimal positionX,
        BigDecimal positionY,
        BigDecimal positionZ,
        String zoneId,
        BigDecimal localX,
        BigDecimal localY,
        BigDecimal localZ,
        BigDecimal normalX,
        BigDecimal normalY,
        BigDecimal normalZ,
        String meshName
) {}
