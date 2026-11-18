package com.rtms.backend.ai.tool.handler.eda;

import com.rtms.backend.ai.tool.DynamicToolRegistryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public enum EdaAnalysisType {
    FLEET_PERFORMANCE("HEAD_TRAINER", "CLUB_MANAGER"),
    HORSE_PERFORMANCE("HORSE_OWNER", "HEAD_TRAINER", "VETERINARIAN", "CLUB_MANAGER"),
    BIO_SPC("HORSE_OWNER", "HEAD_TRAINER", "VETERINARIAN", "GROOM", "CLUB_MANAGER"),
    SURVIVAL("VETERINARIAN", "CLUB_MANAGER"),
    MANAGER_OVERVIEW("CLUB_MANAGER"),
    ADMISSION_FUNNEL("CLUB_MANAGER"),
    TRAINER_PERFORMANCE("CLUB_MANAGER");

    private final Set<String> allowedRoles;

    EdaAnalysisType(String... allowedRoles) {
        this.allowedRoles = Set.of(allowedRoles);
    }

    public static List<EdaAnalysisType> allowedForRole(String normalizedRole) {
        String role = DynamicToolRegistryService.normalizeRole(normalizedRole);
        List<EdaAnalysisType> allowedModes = new ArrayList<>();
        for (EdaAnalysisType analysisType : values()) {
            if (analysisType.allowedRoles.contains(role)) {
                allowedModes.add(analysisType);
            }
        }
        return List.copyOf(allowedModes);
    }

    public boolean isAllowedForRole(String role) {
        return allowedRoles.contains(DynamicToolRegistryService.normalizeRole(role));
    }

    public static EdaAnalysisType parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("EDA analysis type is required");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown EDA analysis type: " + value, exception);
        }
    }
}
