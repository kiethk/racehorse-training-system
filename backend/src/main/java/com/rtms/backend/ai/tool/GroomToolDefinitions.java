package com.rtms.backend.ai.tool;

import java.util.List;
import java.util.Map;

/** Role-scoped AI tool definitions for GROOM. */
final class GroomToolDefinitions {
    private GroomToolDefinitions() {
    }

    static List<AiToolDefinition> create() {
        return List.of(
                new AiToolDefinition(
                                        "getMyAssignedHorses",
                                        "Retrieve the stalls and horses that the current groom directly manages.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of())
                                ),
                                new AiToolDefinition(
                                        "getTodayAggregatedTasks",
                                        "Retrieve the groom's aggregated task list for the day from 3 sources: routine SOP tasks, training track schedules, and veterinary examination schedules.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "date", Map.of("type", "string", "description", "Date to query (YYYY-MM-DD format, defaults to today)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getHorseDietaryAndCareAlerts",
                                        "Look up medical alerts, training restriction decisions, and special care notes for a horse.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getHorseStallLocation",
                                        "Look up a horse's current stall location, area, and assigned caretaker by name or code.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getMyReportedIncidents",
                                        "Look up stable incidents or horse injuries reported by the current groom in the last 7 days and their handling status.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of())
                                ),
                                new AiToolDefinition(
                                        "getGroomTaskCompletionRate",
                                        "Check the groom's daily care task completion progress (stall cleaning, feeding, exercise handoff).",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "date", Map.of("type", "string", "description", "Date to check (YYYY-MM-DD, defaults to today)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "detectHorseBehavioralAnomaly",
                                        "Detect early behavioral abnormalities (loss of appetite, pawing, lethargy) from logs and warn about acute disease risk (Colic).",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "recommendDailyCarePriorityOrder",
                                        "Recommend the priority order of daily care tasks (horses needing medical exams -> early training sessions -> quarantine stall cleaning last).",
                                        ToolDefinitionSchema.buildParamSchema(Map.of())
                                )
        );
    }
}
