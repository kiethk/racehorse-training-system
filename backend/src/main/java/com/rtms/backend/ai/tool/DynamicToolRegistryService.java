package com.rtms.backend.ai.tool;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Registry that manages and exposes the AI Agent tool catalog (100% Read-Only / RAG Advisory)
 * per user role (Role-Based Tool Scoping).
 *
 * Normalized according to MECE principles and the Cognitive Sweet Spot:
 * every role stays within the optimal tool count range (8 to 12 tools), except Trainer with 13
 * because it includes Training Groups.
 */
@Service
public class DynamicToolRegistryService {

    private final Map<String, List<AiToolDefinition>> roleToolDefinitions = new HashMap<>();

    /**
     * Alias mapping for 100% backward compatibility with tools that were standardized and
     * consolidated under MECE principles.
     */
    private static final Map<String, String> TOOL_CONSOLIDATION_ALIASES = Map.ofEntries(
            Map.entry("getHorseTrainingSummary", "getMyHorseTrainingProgress"),
            Map.entry("analyzeWorkoutLoadAndOvertrainingRisk", "getHorseFitnessTrend"),
            Map.entry("getCourseCurriculumOverview", "getTrainerPlansSummary"),
            Map.entry("recommendOptimalRaceDistanceAndTrack", "recommendTrainerStrategy"),
            Map.entry("recommendNextWorkoutAdjustment", "recommendTrainerStrategy"),
            Map.entry("getActivePrescriptions", "getHorseMedicalHistory"),
            Map.entry("getRecentIncidentReports", "getUrgentCareAndIncidentAlerts"),
            Map.entry("getUrgentCareAlerts", "getUrgentCareAndIncidentAlerts"),
            Map.entry("getDailyGroomTasks", "getTodayAggregatedTasks"),
            Map.entry("getAdmissionRequestsOverview", "getAdmissionPipelineOverview"),
            Map.entry("getAdmissionPipelineDetails", "getAdmissionPipelineOverview")
    );

    public DynamicToolRegistryService() {
        initToolDefinitions();
    }

    public List<Map<String, Object>> getFunctionSpecsForRole(String role) {
        if (role == null || role.isBlank()) {
            return Collections.emptyList();
        }
        String normalizedRole = normalizeRole(role);
        List<AiToolDefinition> definitions = roleToolDefinitions.getOrDefault(normalizedRole, Collections.emptyList());

        return definitions.stream().map(def -> {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", def.name());
            function.put("description", def.description());
            function.put("parameters", def.parameters());

            Map<String, Object> toolSpec = new LinkedHashMap<>();
            toolSpec.put("type", "function");
            toolSpec.put("function", function);
            return toolSpec;
        }).toList();
    }

    public List<AiToolDefinition> getToolsForRole(String role) {
        if (role == null || role.isBlank()) {
            return Collections.emptyList();
        }
        String normalizedRole = normalizeRole(role);
        return roleToolDefinitions.getOrDefault(normalizedRole, Collections.emptyList());
    }

    public boolean isToolAllowedForRole(String toolName, String role) {
        if (toolName == null || role == null) {
            return false;
        }
        String normalized = normalizeRole(role);
        List<AiToolDefinition> tools = roleToolDefinitions.getOrDefault(normalized, Collections.emptyList());
        if (tools.stream().anyMatch(t -> t.name().equalsIgnoreCase(toolName))) {
            return true;
        }
        String canonical = TOOL_CONSOLIDATION_ALIASES.get(toolName);
        if (canonical != null) {
            return tools.stream().anyMatch(t -> t.name().equalsIgnoreCase(canonical));
        }
        return false;
    }

    private void initToolDefinitions() {
        // 1. HORSE_OWNER (100% Read-Only - 9 tools, Sweet Spot <= 12)
        roleToolDefinitions.put("HORSE_OWNER", OwnerToolDefinitions.create());

        // 2. HEAD_TRAINER (100% Read-Only - 13 tools: Sweet Spot 12 + Training Group, core trainer workload)
        roleToolDefinitions.put("HEAD_TRAINER", TrainerToolDefinitions.create());

        // 3. VETERINARIAN (100% Read-Only - 11 tools, Sweet Spot <= 12)
        roleToolDefinitions.put("VETERINARIAN", VetToolDefinitions.create());

        // 4. GROOM (100% Read-Only - 8 tools, Sweet Spot <= 12)
        roleToolDefinitions.put("GROOM", GroomToolDefinitions.create());

        // 5. CLUB_MANAGER (100% Read-Only - 9 tools, Sweet Spot <= 12)
        roleToolDefinitions.put("CLUB_MANAGER", ManagerToolDefinitions.create());
    }

    public static String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String r = role.trim().toUpperCase();
        return switch (r) {
            case "HEAD_TRAINER", "TRAINER" -> "HEAD_TRAINER";
            case "VETERINARIAN", "VET" -> "VETERINARIAN";
            case "GROOM", "STABLE_STAFF" -> "GROOM";
            case "HORSE_OWNER", "OWNER" -> "HORSE_OWNER";
            case "CLUB_MANAGER", "RACING_MANAGER", "ADMIN" -> "CLUB_MANAGER";
            default -> r;
        };
    }
}
