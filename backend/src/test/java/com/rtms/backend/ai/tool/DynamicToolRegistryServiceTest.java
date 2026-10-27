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
    @DisplayName("Tất cả 5 vai trò đều không chứa bất kỳ HITL proposal mutation tool nào")
    void testNoHitlToolsInAnyRole() {
        List<String> roles = List.of("HEAD_TRAINER", "VETERINARIAN", "HORSE_OWNER", "GROOM", "CLUB_MANAGER");
        List<String> hitlToolNames = List.of("proposeEditWorkoutMetric", "proposeUpdateHorseMedicalRestriction");

        for (String role : roles) {
            List<AiToolDefinition> tools = registryService.getToolsForRole(role);
            for (String hitlTool : hitlToolNames) {
                boolean hasHitl = tools.stream().anyMatch(t -> t.name().equalsIgnoreCase(hitlTool));
                assertFalse(hasHitl, "Vai trò " + role + " không được chứa tool mutation HITL " + hitlTool);
                assertFalse(registryService.isToolAllowedForRole(hitlTool, role),
                        "isToolAllowedForRole phải trả về false cho HITL tool " + hitlTool + " với vai trò " + role);
            }
        }
    }

    @Test
    @DisplayName("Ngưỡng nhận thức Cognitive Sweet Spot: Mỗi vai trò đều có số lượng tool tối ưu <= 12")
    void testCognitiveSweetSpotToolLimits() {
        List<AiToolDefinition> ownerTools = registryService.getToolsForRole("HORSE_OWNER");
        assertEquals(9, ownerTools.size(), "HORSE_OWNER phải có đúng 9 tools (<= 12)");

        List<AiToolDefinition> groomTools = registryService.getToolsForRole("GROOM");
        assertEquals(8, groomTools.size(), "GROOM phải có đúng 8 tools (<= 12)");

        List<AiToolDefinition> trainerTools = registryService.getToolsForRole("HEAD_TRAINER");
        assertEquals(12, trainerTools.size(), "HEAD_TRAINER phải có đúng 12 tools (<= 12)");

        List<AiToolDefinition> vetTools = registryService.getToolsForRole("VETERINARIAN");
        assertEquals(11, vetTools.size(), "VETERINARIAN phải có đúng 11 tools (<= 12)");

        List<AiToolDefinition> managerTools = registryService.getToolsForRole("CLUB_MANAGER");
        assertEquals(10, managerTools.size(), "CLUB_MANAGER phải có đúng 10 tools (<= 12)");
    }

    @Test
    @DisplayName("HEAD_TRAINER có 12 công cụ tối ưu và hỗ trợ tương thích ngược qua alias")
    void testHeadTrainerTools() {
        List<AiToolDefinition> tools = registryService.getToolsForRole("HEAD_TRAINER");
        assertNotNull(tools);
        assertEquals(12, tools.size());

        List<String> toolNames = tools.stream().map(AiToolDefinition::name).toList();
        assertTrue(toolNames.contains("getTrainerHorses"));
        assertTrue(toolNames.contains("getWorkoutHistory"));
        assertTrue(toolNames.contains("getHorseFitnessTrend"));
        assertTrue(toolNames.contains("getTrainerSchedule"));
        assertTrue(toolNames.contains("getTrainerPlansSummary"));
        assertTrue(toolNames.contains("getHorseSpecialtyAnalysis"));
        assertTrue(toolNames.contains("getTrainerRiskAlerts"));
        assertTrue(toolNames.contains("checkGroomConflictForLot"));
        assertTrue(toolNames.contains("getTrainerAssignedGrooms"));
        assertTrue(toolNames.contains("getMyHorseRaceRegistrations"));
        assertTrue(toolNames.contains("getAssignedAdmissionReviews"));
        assertTrue(toolNames.contains("recommendTrainerStrategy"));

        // Kiểm tra tương thích ngược alias
        assertTrue(registryService.isToolAllowedForRole("getRacingReadinessAssessment", "HEAD_TRAINER"));
        assertTrue(registryService.isToolAllowedForRole("analyzeWorkoutLoadAndOvertrainingRisk", "HEAD_TRAINER"));
        assertTrue(registryService.isToolAllowedForRole("recommendOptimalRaceDistanceAndTrack", "HEAD_TRAINER"));
        assertTrue(registryService.isToolAllowedForRole("recommendNextWorkoutAdjustment", "HEAD_TRAINER"));
        assertTrue(registryService.isToolAllowedForRole("getCourseCurriculumOverview", "HEAD_TRAINER"));
        assertFalse(registryService.isToolAllowedForRole("searchClinicalKnowledge", "HEAD_TRAINER"));

        AiToolDefinition specialtyDef = tools.stream().filter(t -> t.name().equals("getHorseSpecialtyAnalysis")).findFirst().orElseThrow();
        Map<?, ?> props = (Map<?, ?>) specialtyDef.parameters().get("properties");
        assertTrue(props.containsKey("sortBy"), "getHorseSpecialtyAnalysis phải có tham số sortBy");
    }

    @Test
    @DisplayName("VETERINARIAN có 11 công cụ tối ưu và hỗ trợ tương thích ngược qua alias")
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

        // Kiểm tra tương thích ngược alias
        assertTrue(registryService.isToolAllowedForRole("getActivePrescriptions", "VETERINARIAN"));
        assertTrue(registryService.isToolAllowedForRole("getRecentIncidentReports", "VETERINARIAN"));
        assertTrue(registryService.isToolAllowedForRole("getUrgentCareAlerts", "VETERINARIAN"));
        assertFalse(registryService.isToolAllowedForRole("getTrainerSchedule", "VETERINARIAN"));
    }

    @Test
    @DisplayName("HORSE_OWNER là 100% Read-Only và có 9 công cụ tối ưu")
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

        // Kiểm tra alias
        assertTrue(registryService.isToolAllowedForRole("getHorseTrainingSummary", "HORSE_OWNER"));
        assertFalse(registryService.isToolAllowedForRole("getTrainerHorses", "HORSE_OWNER"));
    }

    @Test
    @DisplayName("GROOM có 8 công cụ tối ưu sau khi gom getDailyGroomTasks vào getTodayAggregatedTasks")
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

        // Tương thích ngược alias
        assertTrue(registryService.isToolAllowedForRole("getDailyGroomTasks", "GROOM"));
        assertTrue(registryService.isToolAllowedForRole("getMyAssignedHorses", "STABLE_STAFF"));
        assertFalse(registryService.isToolAllowedForRole("getStableOccupancyMetrics", "GROOM"));
    }

    @Test
    @DisplayName("CLUB_MANAGER có 10 công cụ tối ưu sau khi hợp nhất đơn nhập đàn")
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
        assertFalse(toolNames.contains("getUpcomingRacesAndCompetitions"), "getUpcomingRacesAndCompetitions phải được bỏ");

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

        // Tương thích ngược alias
        assertTrue(registryService.isToolAllowedForRole("getAdmissionRequestsOverview", "CLUB_MANAGER"));
        assertTrue(registryService.isToolAllowedForRole("getAdmissionPipelineDetails", "CLUB_MANAGER"));
        assertTrue(registryService.isToolAllowedForRole("getAdmissionPipelineOverview", "ADMIN"));

        AiToolDefinition mgrSpecialtyDef = tools.stream().filter(t -> t.name().equals("getHorseSpecialtyAnalysis")).findFirst().orElseThrow();
        Map<?, ?> mgrProps = (Map<?, ?>) mgrSpecialtyDef.parameters().get("properties");
        assertTrue(mgrProps.containsKey("sortBy"), "getHorseSpecialtyAnalysis của Manager phải có tham số sortBy");
    }

    @Test
    @DisplayName("100% Phân vùng bảo mật RBAC: Chặn triệt để mọi hành vi gọi chéo quyền giữa 5 vai trò")
    void testStrictRbacIsolationAcrossAllRoles() {
        // Owner không thể gọi tool quản lý/thú y/huấn luyện
        assertFalse(registryService.isToolAllowedForRole("getStableOccupancyMetrics", "HORSE_OWNER"));
        assertFalse(registryService.isToolAllowedForRole("searchClinicalKnowledge", "HORSE_OWNER"));
        assertFalse(registryService.isToolAllowedForRole("checkGroomConflictForLot", "HORSE_OWNER"));
        assertFalse(registryService.isToolAllowedForRole("getTodayAggregatedTasks", "HORSE_OWNER"));

        // Groom không thể gọi tool thú y hoặc quản lý
        assertFalse(registryService.isToolAllowedForRole("searchClinicalKnowledge", "GROOM"));
        assertFalse(registryService.isToolAllowedForRole("getRecentAuditLogs", "GROOM"));
        assertFalse(registryService.isToolAllowedForRole("getTrainerSchedule", "GROOM"));

        // Veterinarian không thể gọi tool tập luyện hoặc quản trị chuồng trại
        assertFalse(registryService.isToolAllowedForRole("getTrainerSchedule", "VETERINARIAN"));
        assertFalse(registryService.isToolAllowedForRole("getStableOccupancyMetrics", "VETERINARIAN"));

        // Head Trainer không thể gọi tool chẩn đoán thú y hoặc kiểm toán hệ thống
        assertFalse(registryService.isToolAllowedForRole("searchClinicalKnowledge", "HEAD_TRAINER"));
        assertFalse(registryService.isToolAllowedForRole("getRecentAuditLogs", "HEAD_TRAINER"));
    }

    @Test
    @DisplayName("getFunctionSpecsForRole tạo Function Spec đúng chuẩn JSON Schema cho AI / OpenAI Function Calling")
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
