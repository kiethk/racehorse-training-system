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
    @DisplayName("HEAD_TRAINER có đầy đủ các công cụ phân tích, tra cứu, đánh giá sẵn sàng thi đấu và Groom phụ trách")
    void testHeadTrainerTools() {
        List<AiToolDefinition> tools = registryService.getToolsForRole("HEAD_TRAINER");
        assertNotNull(tools);

        List<String> toolNames = tools.stream().map(AiToolDefinition::name).toList();
        assertTrue(toolNames.contains("getTrainerHorses"));
        assertTrue(toolNames.contains("getWorkoutHistory"));
        assertTrue(toolNames.contains("getHorseFitnessTrend"));
        assertTrue(toolNames.contains("getTrainerSchedule"));
        assertTrue(toolNames.contains("getTrainerPlansSummary"));
        assertTrue(toolNames.contains("getHorseSpecialtyAnalysis"));
        assertTrue(toolNames.contains("getTrainerRiskAlerts"));
        assertTrue(toolNames.contains("getRacingReadinessAssessment"));
        assertTrue(toolNames.contains("checkGroomConflictForLot"));
        assertTrue(toolNames.contains("getTrainerAssignedGrooms"));

        assertTrue(registryService.isToolAllowedForRole("getTrainerSchedule", "HEAD_TRAINER"));
        assertTrue(registryService.isToolAllowedForRole("getHorseSpecialtyAnalysis", "HEAD_TRAINER"));
        assertTrue(registryService.isToolAllowedForRole("getTrainerAssignedGrooms", "HEAD_TRAINER"));
        assertFalse(registryService.isToolAllowedForRole("searchClinicalKnowledge", "HEAD_TRAINER"));
    }

    @Test
    @DisplayName("VETERINARIAN có đầy đủ các công cụ RAG, tổng quan khám bệnh, tiền sử bệnh án, lịch tiêm phòng, cách ly và sự cố")
    void testVeterinarianTools() {
        List<AiToolDefinition> tools = registryService.getToolsForRole("VETERINARIAN");
        assertNotNull(tools);

        List<String> toolNames = tools.stream().map(AiToolDefinition::name).toList();
        assertTrue(toolNames.contains("searchClinicalKnowledge"));
        assertTrue(toolNames.contains("getVetExaminationOverview"));
        assertTrue(toolNames.contains("getHorseMedicalHistory"));
        assertTrue(toolNames.contains("getActivePrescriptions"));
        assertTrue(toolNames.contains("getPreventiveCareSchedule"));
        assertTrue(toolNames.contains("getQuarantineStatus"));
        assertTrue(toolNames.contains("getRecentIncidentReports"));

        // Alias VET
        assertTrue(registryService.isToolAllowedForRole("searchClinicalKnowledge", "VET"));
        assertTrue(registryService.isToolAllowedForRole("getVetExaminationOverview", "VET"));
        assertTrue(registryService.isToolAllowedForRole("getQuarantineStatus", "VET"));
    }

    @Test
    @DisplayName("HORSE_OWNER là 100% Read-Only và có đầy đủ công cụ theo dõi ngựa của mình")
    void testHorseOwnerTools() {
        List<AiToolDefinition> tools = registryService.getToolsForRole("HORSE_OWNER");
        assertNotNull(tools);

        List<String> toolNames = tools.stream().map(AiToolDefinition::name).toList();
        assertTrue(toolNames.contains("getMyHorses"));
        assertTrue(toolNames.contains("getMyHorseHealthSummary"));
        assertTrue(toolNames.contains("getMyHorseTrainingProgress"));
        assertTrue(toolNames.contains("getMyHorseUpcomingSchedule"));
        assertTrue(toolNames.contains("getMyHorseAdmissions"));

        assertTrue(registryService.isToolAllowedForRole("getMyHorses", "HORSE_OWNER"));
        assertTrue(registryService.isToolAllowedForRole("getMyHorseUpcomingSchedule", "HORSE_OWNER"));
        assertFalse(registryService.isToolAllowedForRole("getTrainerHorses", "HORSE_OWNER"));
    }

    @Test
    @DisplayName("GROOM có đầy đủ các công cụ tác vụ và chuồng trại (đã loại bỏ quản lý vật tư)")
    void testGroomTools() {
        List<AiToolDefinition> tools = registryService.getToolsForRole("GROOM");
        assertNotNull(tools);

        List<String> toolNames = tools.stream().map(AiToolDefinition::name).toList();
        assertTrue(toolNames.contains("getMyAssignedHorses"));
        assertTrue(toolNames.contains("getTodayAggregatedTasks"));
        assertTrue(toolNames.contains("getHorseDietaryAndCareAlerts"));
        assertTrue(toolNames.contains("getHorseStallLocation"));
        assertFalse(toolNames.contains("checkStallSuppliesStatus"), "checkStallSuppliesStatus phải được bỏ");

        // Alias STABLE_STAFF
        assertTrue(registryService.isToolAllowedForRole("getMyAssignedHorses", "STABLE_STAFF"));
    }

    @Test
    @DisplayName("CLUB_MANAGER có đầy đủ công cụ quản lý toàn trại, phân bổ khu vực và tổng quan sức khỏe (đã bỏ upcoming races)")
    void testClubManagerTools() {
        List<AiToolDefinition> tools = registryService.getToolsForRole("CLUB_MANAGER");
        assertNotNull(tools);

        List<String> toolNames = tools.stream().map(AiToolDefinition::name).toList();
        assertTrue(toolNames.contains("getStableOccupancyMetrics"));
        assertTrue(toolNames.contains("getAdmissionRequestsOverview"));
        assertTrue(toolNames.contains("getTrainerAreaDistribution"));
        assertTrue(toolNames.contains("getClubHealthOverview"));
        assertFalse(toolNames.contains("getUpcomingRacesAndCompetitions"), "getUpcomingRacesAndCompetitions phải được bỏ");

        // Alias RACING_MANAGER
        assertTrue(registryService.isToolAllowedForRole("getTrainerAreaDistribution", "RACING_MANAGER"));
    }

    @Test
    @DisplayName("getFunctionSpecsForRole tạo Function Spec đúng chuẩn JSON Schema cho Groq/OpenAI")
    void testGetFunctionSpecsForRole() {
        List<Map<String, Object>> specs = registryService.getFunctionSpecsForRole("HEAD_TRAINER");
        assertNotNull(specs);
        assertFalse(specs.isEmpty());

        Map<String, Object> first = specs.get(0);
        assertEquals("function", first.get("type"));
        assertTrue(first.containsKey("function"));

        Map<?, ?> fn = (Map<?, ?>) first.get("function");
        assertNotNull(fn.get("name"));
        assertNotNull(fn.get("description"));
        assertNotNull(fn.get("parameters"));
    }
}
