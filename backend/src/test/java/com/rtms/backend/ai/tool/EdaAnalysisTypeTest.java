package com.rtms.backend.ai.tool;

import com.rtms.backend.ai.tool.handler.eda.EdaAnalysisType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdaAnalysisTypeTest {

    @Test
    void allowedModesMatchRolePolicy() {
        Map<String, List<EdaAnalysisType>> expectedModes = Map.of(
                "HORSE_OWNER", List.of(EdaAnalysisType.HORSE_PERFORMANCE, EdaAnalysisType.BIO_SPC),
                "HEAD_TRAINER", List.of(
                        EdaAnalysisType.FLEET_PERFORMANCE,
                        EdaAnalysisType.HORSE_PERFORMANCE,
                        EdaAnalysisType.BIO_SPC
                ),
                "VETERINARIAN", List.of(
                        EdaAnalysisType.HORSE_PERFORMANCE,
                        EdaAnalysisType.BIO_SPC,
                        EdaAnalysisType.SURVIVAL
                ),
                "GROOM", List.of(EdaAnalysisType.BIO_SPC),
                "CLUB_MANAGER", List.of(
                        EdaAnalysisType.FLEET_PERFORMANCE,
                        EdaAnalysisType.HORSE_PERFORMANCE,
                        EdaAnalysisType.BIO_SPC,
                        EdaAnalysisType.SURVIVAL,
                        EdaAnalysisType.MANAGER_OVERVIEW,
                        EdaAnalysisType.ADMISSION_FUNNEL,
                        EdaAnalysisType.TRAINER_PERFORMANCE
                )
        );
        Map<String, Integer> expectedToolCounts = Map.of(
                "HORSE_OWNER", 10,
                "HEAD_TRAINER", 12,
                "VETERINARIAN", 12,
                "GROOM", 9,
                "CLUB_MANAGER", 11
        );
        DynamicToolRegistryService registry = new DynamicToolRegistryService();

        expectedModes.forEach((role, modes) -> {
            assertEquals(modes, EdaAnalysisType.allowedForRole(role), role + " EDA modes");
            assertEquals(expectedToolCounts.get(role), registry.getToolsForRole(role).size() + 1,
                    role + " tool count after adding the EDA tool");
        });

        assertEquals(expectedModes.get("CLUB_MANAGER"), EdaAnalysisType.allowedForRole("ADMIN"));
    }

    @Test
    void modeChecksNormalizeRoleAliases() {
        assertTrue(EdaAnalysisType.MANAGER_OVERVIEW.isAllowedForRole("ADMIN"));
        assertTrue(EdaAnalysisType.BIO_SPC.isAllowedForRole("STABLE_STAFF"));
        assertFalse(EdaAnalysisType.FLEET_PERFORMANCE.isAllowedForRole("GROOM"));
    }

    @Test
    void parsesKnownModesAndRejectsUnknownValues() {
        assertEquals(EdaAnalysisType.BIO_SPC, EdaAnalysisType.parse("BIO_SPC"));
        assertThrows(IllegalArgumentException.class, () -> EdaAnalysisType.parse("NOT_A_MODE"));
    }
}
