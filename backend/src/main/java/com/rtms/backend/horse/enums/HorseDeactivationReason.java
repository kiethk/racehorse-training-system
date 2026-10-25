package com.rtms.backend.horse.enums;

/**
 * Standard reasons for deactivating a horse (transition to INACTIVE).
 */
public enum HorseDeactivationReason {
    RETIRED("Retired from training & competition"),
    TRANSFERRED("Transferred to another facility / club"),
    MEDICAL_DISCHARGE("Medical discharge / chronic injury"),
    DECEASED("Deceased"),
    OWNER_WITHDRAWAL("Withdrawn by owner request"),
    OTHER("Other");

    private final String displayName;

    HorseDeactivationReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
