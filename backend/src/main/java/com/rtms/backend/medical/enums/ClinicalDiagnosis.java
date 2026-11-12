package com.rtms.backend.medical.enums;

/**
 * Standardized clinical diagnosis categories for veterinary examinations.
 */
public enum ClinicalDiagnosis {
    CLINICALLY_HEALTHY("Clinically healthy"),
    MINOR_CONDITION("Minor clinical condition"),
    TREATMENT_REQUIRED("Treatment required"),
    FOLLOW_UP_REQUIRED("Follow-up examination required"),
    INFECTIOUS_SUSPECTED("Infectious disease suspected"),
    MUSCULOSKELETAL_SUSPECTED("Musculoskeletal condition suspected"),
    MEDICALLY_UNSUITABLE("Medically unsuitable for admission");

    private final String displayName;

    ClinicalDiagnosis(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Resolves a string (either enum name or display text) to a canonical ClinicalDiagnosis.
     */
    public static ClinicalDiagnosis fromString(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        for (ClinicalDiagnosis cd : values()) {
            if (cd.name().equalsIgnoreCase(text.trim()) || cd.displayName.equalsIgnoreCase(text.trim())) {
                return cd;
            }
        }
        return null;
    }
}
