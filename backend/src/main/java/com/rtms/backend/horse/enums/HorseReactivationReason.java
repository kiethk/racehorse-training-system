package com.rtms.backend.horse.enums;

/**
 * Standard reasons for restoring a horse (transition to ELIGIBLE).
 */
public enum HorseReactivationReason {
    CLEARED_BY_VET("Cleared by veterinary team / recovered"),
    RETURNED_FROM_LOAN("Returned from external facility / loan"),
    MANAGEMENT_APPROVAL("Approved by club management"),
    CORRECTION("Administrative correction"),
    OTHER("Other");

    private final String displayName;

    HorseReactivationReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
