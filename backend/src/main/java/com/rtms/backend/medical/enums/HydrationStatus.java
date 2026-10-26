package com.rtms.backend.medical.enums;

/**
 * Clinical hydration status recorded during an examination (CRT = capillary refill time).
 * Stored as the enum name in horse_health_metrics.hydration_status.
 */
public enum HydrationStatus {
    NORMAL,
    MILD_DEHYDRATION,
    MODERATE_DEHYDRATION,
    SEVERE_DEHYDRATION,
    ABNORMAL_CRT
}
