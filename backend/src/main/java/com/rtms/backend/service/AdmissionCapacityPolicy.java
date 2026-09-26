package com.rtms.backend.service;

public final class AdmissionCapacityPolicy {
    private AdmissionCapacityPolicy() { }

    public static boolean isAvailable(long availableQuarantine, long availableRegular,
            long occupiedQuarantine) {
        return availableQuarantine >= 1 && availableRegular >= occupiedQuarantine + 1;
    }

    public static String blockingReason(long availableQuarantine, long availableRegular,
            long occupiedQuarantine) {
        if (availableQuarantine < 1) return "NO_QUARANTINE_STALL";
        if (availableRegular < occupiedQuarantine + 1) return "REGULAR_RESERVE_INSUFFICIENT";
        return null;
    }
}
