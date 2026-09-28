package com.rtms.backend.dto;

public record AdmissionCapacitySummary(
        long availableQuarantineStalls,
        long availableRegularStalls,
        long occupiedQuarantineStalls,
        boolean admissionCapacityAvailable,
        String blockingReason) {
}
