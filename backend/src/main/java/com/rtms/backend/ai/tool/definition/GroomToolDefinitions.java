package com.rtms.backend.ai.tool.definition;

import com.rtms.backend.ai.tool.AiToolDefinition;
import com.rtms.backend.ai.tool.ToolDefinitionSchema;

import java.util.List;
import java.util.Map;

import com.rtms.backend.ai.tool.handler.eda.EdaAnalysisType;

/** Role-scoped AI tool definitions for GROOM. */
public final class GroomToolDefinitions {
    private GroomToolDefinitions() {
    }

    public static List<AiToolDefinition> create() {
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
                                ),
                                new AiToolDefinition(
                                        "getEdaAnalytics",
                                        "Perform exploratory data analysis on allowed biomedical horse data.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "analysisType", Map.of(
                                                        "type", "string",
                                                        "enum", EdaAnalysisType.allowedForRole("GROOM").stream().map(Enum::name).toList(),
                                                        "description", "Required analysis mode: BIO_SPC"
                                                ),
                                                "horseId", Map.of("type", "integer", "description", "Horse ID for individual horse analyses (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name for individual horse analyses (optional)"),
                                                "days", Map.of("type", "integer", "description", "Lookback period in days when supported by the analysis (optional)"),
                                                "metric", Map.of(
                                                        "type", "string",
                                                        "enum", List.of("TEMPERATURE", "RESTING_HEART_RATE"),
                                                        "description", "BIO_SPC metric: TEMPERATURE or RESTING_HEART_RATE (optional)"
                                                ),
                                                "stratifyBy", Map.of(
                                                        "type", "string",
                                                        "enum", List.of("SEVERITY", "BODY_ZONE"),
                                                        "description", "Survival analysis grouping: SEVERITY or BODY_ZONE (optional)"
                                                )
                                        ), "analysisType")
                                )
        );
    }
}
