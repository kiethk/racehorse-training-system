package com.rtms.backend.ai.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DynamicToolRegistryServiceTest {

    private DynamicToolRegistryService registryService;

    @BeforeEach
    void setUp() {
        registryService = new DynamicToolRegistryService();
    }

    @Test
    @DisplayName("None of the five roles includes any HITL proposal mutation tools")
    void testNoHitlToolsInAnyRole() {
        List<String> roles = List.of("HEAD_TRAINER", "VETERINARIAN", "HORSE_OWNER", "GROOM", "CLUB_MANAGER");
        List<String> hitlToolNames = List.of("proposeEditWorkoutMetric", "proposeUpdateHorseMedicalRestriction");

        for (String role : roles) {
            List<AiToolDefinition> tools = registryService.getToolsForRole(role);
            for (String hitlTool : hitlToolNames) {
                boolean hasHitl = tools.stream().anyMatch(t -> t.name().equalsIgnoreCase(hitlTool));
                assertFalse(hasHitl, "Role " + role + " must not include the HITL mutation tool " + hitlTool);
                assertFalse(registryService.isToolAllowedForRole(hitlTool, role),
                        "isToolAllowedForRole must return false for HITL tool " + hitlTool + " for role " + role);
            }
        }
    }

    @Test
    @DisplayName("Cognitive Sweet Spot limit: at most 12 tools per role, and 11 for the Trainer")
    void testCognitiveSweetSpotToolLimits() {
        List<AiToolDefinition> ownerTools = registryService.getToolsForRole("HORSE_OWNER");
        assertEquals(9, ownerTools.size(), "HORSE_OWNER must have exactly 9 tools (<= 12)");

        List<AiToolDefinition> groomTools = registryService.getToolsForRole("GROOM");
        assertEquals(8, groomTools.size(), "GROOM must have exactly 8 tools (<= 12)");

        List<AiToolDefinition> trainerTools = registryService.getToolsForRole("HEAD_TRAINER");
        assertEquals(11, trainerTools.size(), "HEAD_TRAINER must have exactly 11 tools");

        List<AiToolDefinition> vetTools = registryService.getToolsForRole("VETERINARIAN");
        assertEquals(11, vetTools.size(), "VETERINARIAN must have exactly 11 tools (<= 12)");

        List<AiToolDefinition> managerTools = registryService.getToolsForRole("CLUB_MANAGER");
        assertEquals(10, managerTools.size(), "CLUB_MANAGER must have exactly 10 tools (<= 12)");
    }

    @Test
    @DisplayName("HEAD_TRAINER has 11 tools, including Training Group, and supports backward-compatible aliases")
    void testHeadTrainerTools() {
        List<AiToolDefinition> tools = registryService.getToolsForRole("HEAD_TRAINER");
        assertNotNull(tools);
        assertEquals(11, tools.size());

        List<String> toolNames = tools.stream().map(AiToolDefinition::name).toList();
        assertTrue(toolNames.contains("getTrainerHorses"));
        assertTrue(toolNames.contains("getTrainerGroups"));
        assertTrue(toolNames.contains("getWorkoutHistory"));
        assertTrue(toolNames.contains("getHorseFitnessTrend"));
        assertTrue(toolNames.contains("getTrainerSchedule"));
        assertTrue(toolNames.contains("getTrainerPlansSummary"));
        assertTrue(toolNames.contains("getHorseSpecialtyAnalysis"));
        assertTrue(toolNames.contains("getTrainerRiskAlerts"));
        assertTrue(toolNames.contains("getTrainerAssignedGrooms"));
        assertTrue(toolNames.contains("getAssignedAdmissionReviews"));
        assertTrue(toolNames.contains("recommendTrainerStrategy"));

        // Verify backward-compatible aliases
        assertTrue(registryService.isToolAllowedForRole("getRacingReadinessAssessment", "HEAD_TRAINER"));
        assertTrue(registryService.isToolAllowedForRole("analyzeWorkoutLoadAndOvertrainingRisk", "HEAD_TRAINER"));
        assertTrue(registryService.isToolAllowedForRole("recommendOptimalRaceDistanceAndTrack", "HEAD_TRAINER"));
        assertTrue(registryService.isToolAllowedForRole("recommendNextWorkoutAdjustment", "HEAD_TRAINER"));
        assertTrue(registryService.isToolAllowedForRole("getCourseCurriculumOverview", "HEAD_TRAINER"));
        assertFalse(registryService.isToolAllowedForRole("searchClinicalKnowledge", "HEAD_TRAINER"));

        AiToolDefinition specialtyDef = tools.stream().filter(t -> t.name().equals("getHorseSpecialtyAnalysis")).findFirst().orElseThrow();
        Map<?, ?> props = (Map<?, ?>) specialtyDef.parameters().get("properties");
        assertTrue(props.containsKey("sortBy"), "getHorseSpecialtyAnalysis must include the sortBy parameter");
    }

    @Test
    @DisplayName("VETERINARIAN has 11 optimized tools and supports backward-compatible aliases")
    void testVeterinarianTools() {
        List<AiToolDefinition> tools = registryService.getToolsForRole("VETERINARIAN");
        assertNotNull(tools);
        assertEquals(11, tools.size());

        List<String> toolNames = tools.stream().map(AiToolDefinition::name).toList();
        assertTrue(toolNames.contains("searchClinicalKnowledge"));
        assertTrue(toolNames.contains("getVetExaminationOverview"));
        assertTrue(toolNames.contains("getHorseMedicalHistory"));
        assertTrue(toolNames.contains("getPreventiveCareSchedule"));
        assertTrue(toolNames.contains("getQuarantineStatus"));
        assertTrue(toolNames.contains("getPendingAdmissionExams"));
        assertTrue(toolNames.contains("getUrgentCareAndIncidentAlerts"));
        assertTrue(toolNames.contains("getHorseVitalSignsTrend"));
        assertTrue(toolNames.contains("analyzeIncidentPatternsAndRecurringRisks"));
        assertTrue(toolNames.contains("recommendClinicalInterventionPlan"));
        assertTrue(toolNames.contains("evaluateTrainingResumptionReadiness"));

        // Alias VET
        assertTrue(registryService.isToolAllowedForRole("searchClinicalKnowledge", "VET"));
        assertTrue(registryService.isToolAllowedForRole("getVetExaminationOverview", "VET"));
        assertTrue(registryService.isToolAllowedForRole("getQuarantineStatus", "VET"));
        assertTrue(registryService.isToolAllowedForRole("getPendingAdmissionExams", "VET"));
        assertTrue(registryService.isToolAllowedForRole("getUrgentCareAndIncidentAlerts", "VETERINARIAN"));

        // Verify backward-compatible aliases
        assertTrue(registryService.isToolAllowedForRole("getActivePrescriptions", "VETERINARIAN"));
        assertTrue(registryService.isToolAllowedForRole("getRecentIncidentReports", "VETERINARIAN"));
        assertTrue(registryService.isToolAllowedForRole("getUrgentCareAlerts", "VETERINARIAN"));
        assertFalse(registryService.isToolAllowedForRole("getTrainerSchedule", "VETERINARIAN"));
    }

    @Test
    @DisplayName("HORSE_OWNER is 100% read-only and has 9 optimized tools")
    void testHorseOwnerTools() {
        List<AiToolDefinition> tools = registryService.getToolsForRole("HORSE_OWNER");
        assertNotNull(tools);
        assertEquals(9, tools.size());

        List<String> toolNames = tools.stream().map(AiToolDefinition::name).toList();
        assertTrue(toolNames.contains("getMyHorses"));
        assertTrue(toolNames.contains("getMyHorseHealthSummary"));
        assertTrue(toolNames.contains("getMyHorseTrainingProgress"));
        assertTrue(toolNames.contains("getMyHorseUpcomingSchedule"));
        assertTrue(toolNames.contains("getMyHorseAdmissions"));
        assertTrue(toolNames.contains("getHorseCareSchedule"));
        assertTrue(toolNames.contains("getMyHorseIncidentHistory"));
        assertTrue(toolNames.contains("getMyHorsePedigreeInfo"));
        assertTrue(toolNames.contains("analyzeHorseInvestmentAndFitnessROI"));

        // Verify aliases
        assertTrue(registryService.isToolAllowedForRole("getHorseTrainingSummary", "HORSE_OWNER"));
        assertFalse(registryService.isToolAllowedForRole("getTrainerHorses", "HORSE_OWNER"));
    }

    @Test
    @DisplayName("GROOM has 8 optimized tools after consolidating getDailyGroomTasks into getTodayAggregatedTasks")
    void testGroomTools() {
        List<AiToolDefinition> tools = registryService.getToolsForRole("GROOM");
        assertNotNull(tools);
        assertEquals(8, tools.size());

        List<String> toolNames = tools.stream().map(AiToolDefinition::name).toList();
        assertTrue(toolNames.contains("getMyAssignedHorses"));
        assertTrue(toolNames.contains("getTodayAggregatedTasks"));
        assertTrue(toolNames.contains("getHorseDietaryAndCareAlerts"));
        assertTrue(toolNames.contains("getHorseStallLocation"));
        assertTrue(toolNames.contains("getMyReportedIncidents"));
        assertTrue(toolNames.contains("getGroomTaskCompletionRate"));
        assertTrue(toolNames.contains("detectHorseBehavioralAnomaly"));
        assertTrue(toolNames.contains("recommendDailyCarePriorityOrder"));

        // Backward-compatible aliases
        assertTrue(registryService.isToolAllowedForRole("getDailyGroomTasks", "GROOM"));
        assertTrue(registryService.isToolAllowedForRole("getMyAssignedHorses", "STABLE_STAFF"));
        assertFalse(registryService.isToolAllowedForRole("getStableOccupancyMetrics", "GROOM"));
    }

    @Test
    @DisplayName("CLUB_MANAGER has 10 optimized tools after consolidating admission tools")
    void testClubManagerTools() {
        List<AiToolDefinition> tools = registryService.getToolsForRole("CLUB_MANAGER");
        assertNotNull(tools);
        assertEquals(10, tools.size());

        List<String> toolNames = tools.stream().map(AiToolDefinition::name).toList();
        assertTrue(toolNames.contains("getStableOccupancyMetrics"));
        assertTrue(toolNames.contains("getAdmissionPipelineOverview"));
        assertTrue(toolNames.contains("getTrainerAreaDistribution"));
        assertTrue(toolNames.contains("getClubHealthOverview"));
        assertTrue(toolNames.contains("getSystemStaffOverview"));
        assertTrue(toolNames.contains("getClubHorsesDirectory"));
        assertTrue(toolNames.contains("getRecentAuditLogs"));
        assertTrue(toolNames.contains("analyzeStableCapacityAndStallBottlenecks"));
        assertTrue(toolNames.contains("recommendGroomWorkloadRebalancing"));
        assertTrue(toolNames.contains("getHorseSpecialtyAnalysis"));
        assertFalse(toolNames.contains("getUpcomingRacesAndCompetitions"), "getUpcomingRacesAndCompetitions must be excluded");

        // Alias RACING_MANAGER & ADMIN
        assertTrue(registryService.isToolAllowedForRole("getTrainerAreaDistribution", "RACING_MANAGER"));
        assertTrue(registryService.isToolAllowedForRole("getSystemStaffOverview", "ADMIN"));
        assertTrue(registryService.isToolAllowedForRole("getClubHorsesDirectory", "ADMIN"));
        assertTrue(registryService.isToolAllowedForRole("getRecentAuditLogs", "ADMIN"));
        assertTrue(registryService.isToolAllowedForRole("analyzeStableCapacityAndStallBottlenecks", "ADMIN"));
        assertTrue(registryService.isToolAllowedForRole("recommendGroomWorkloadRebalancing", "CLUB_MANAGER"));
        assertTrue(registryService.isToolAllowedForRole("getAdmissionPipelineDetails", "ADMIN"));
        assertTrue(registryService.isToolAllowedForRole("getHorseSpecialtyAnalysis", "CLUB_MANAGER"));
        assertTrue(registryService.isToolAllowedForRole("getHorseSpecialtyAnalysis", "ADMIN"));

        // Backward-compatible aliases
        assertTrue(registryService.isToolAllowedForRole("getAdmissionRequestsOverview", "CLUB_MANAGER"));
        assertTrue(registryService.isToolAllowedForRole("getAdmissionPipelineDetails", "CLUB_MANAGER"));
        assertTrue(registryService.isToolAllowedForRole("getAdmissionPipelineOverview", "ADMIN"));

        AiToolDefinition mgrSpecialtyDef = tools.stream().filter(t -> t.name().equals("getHorseSpecialtyAnalysis")).findFirst().orElseThrow();
        Map<?, ?> mgrProps = (Map<?, ?>) mgrSpecialtyDef.parameters().get("properties");
        assertTrue(mgrProps.containsKey("sortBy"), "The Manager's getHorseSpecialtyAnalysis must include the sortBy parameter");
    }

    @Test
    @DisplayName("100% RBAC isolation: block all cross-role access across the five roles")
    void testStrictRbacIsolationAcrossAllRoles() {
        // Owners cannot call management, veterinary, or training tools
        assertFalse(registryService.isToolAllowedForRole("getStableOccupancyMetrics", "HORSE_OWNER"));
        assertFalse(registryService.isToolAllowedForRole("searchClinicalKnowledge", "HORSE_OWNER"));
        assertFalse(registryService.isToolAllowedForRole("getTodayAggregatedTasks", "HORSE_OWNER"));

        // Grooms cannot call veterinary or management tools
        assertFalse(registryService.isToolAllowedForRole("searchClinicalKnowledge", "GROOM"));
        assertFalse(registryService.isToolAllowedForRole("getRecentAuditLogs", "GROOM"));
        assertFalse(registryService.isToolAllowedForRole("getTrainerSchedule", "GROOM"));

        // Veterinarians cannot call training or stable-management tools
        assertFalse(registryService.isToolAllowedForRole("getTrainerSchedule", "VETERINARIAN"));
        assertFalse(registryService.isToolAllowedForRole("getStableOccupancyMetrics", "VETERINARIAN"));

        // Head Trainers cannot call veterinary diagnostic or system-audit tools
        assertFalse(registryService.isToolAllowedForRole("searchClinicalKnowledge", "HEAD_TRAINER"));
        assertFalse(registryService.isToolAllowedForRole("getRecentAuditLogs", "HEAD_TRAINER"));
    }

    @Test
    @DisplayName("getFunctionSpecsForRole creates valid JSON Schema function specs for AI / OpenAI function calling")
    void testGetFunctionSpecsForRole() {
        List<String> roles = List.of("HORSE_OWNER", "GROOM", "HEAD_TRAINER", "VETERINARIAN", "CLUB_MANAGER");
        for (String role : roles) {
            List<Map<String, Object>> specs = registryService.getFunctionSpecsForRole(role);
            assertNotNull(specs);
            assertFalse(specs.isEmpty());

            for (Map<String, Object> spec : specs) {
                assertEquals("function", spec.get("type"));
                assertTrue(spec.containsKey("function"));

                Map<?, ?> fn = (Map<?, ?>) spec.get("function");
                assertNotNull(fn.get("name"), "Tool name must not be null for role " + role);
                assertNotNull(fn.get("description"), "Tool description must not be null for role " + role);
                assertNotNull(fn.get("parameters"), "Tool parameters must not be null for role " + role);
            }
        }
    }
}
