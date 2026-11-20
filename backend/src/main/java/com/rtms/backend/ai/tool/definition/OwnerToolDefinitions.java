package com.rtms.backend.ai.tool.definition;

import com.rtms.backend.ai.tool.AiToolDefinition;
import com.rtms.backend.ai.tool.ToolDefinitionSchema;

import java.util.List;
import java.util.Map;

import com.rtms.backend.ai.tool.handler.eda.EdaAnalysisType;

/** Role-scoped AI tool definitions for HORSE OWNER. */
public final class OwnerToolDefinitions {
    private OwnerToolDefinitions() {
    }

    public static List<AiToolDefinition> create() {
        return List.of(
                new AiToolDefinition(
                                        "getMyHorses",
                                        "Retrieve the list of horses owned by the current owner (total owned horses totalOwnedHorses, horses cleared for training allowedTrainingHorses, horses restricted for medical reasons restrictedHorses, detailed horse list horseList). Always use when asked how many horses the owner has.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of())
                                ),
                                new AiToolDefinition(
                                        "getMyHorseHealthSummary",
                                        "View the overall health summary, training decision, and medical notes for an owned horse.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getMyHorseTrainingProgress",
                                        "View training course progress, training plans, completed workouts, and the training decision for an owned horse.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getMyHorseUpcomingSchedule",
                                        "Look up upcoming training schedules for horses owned by the current owner.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "fromDate", Map.of("type", "string", "description", "Start date (YYYY-MM-DD, defaults to today)"),
                                                "toDate", Map.of("type", "string", "description", "End date (YYYY-MM-DD, defaults to 14 days ahead)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getMyHorseAdmissions",
                                        "Retrieve the owner's admission applications.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of())
                                ),
                                new AiToolDefinition(
                                        "getHorseCareSchedule",
                                        "View the upcoming veterinary care schedule for an owned horse.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getMyHorseIncidentHistory",
                                        "View the history of health incidents or minor injuries recorded by staff for an owned horse.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getMyHorsePedigreeInfo",
                                        "Look up pedigree/bloodline information for a horse (Sire, Dam, origin).",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "analyzeHorseInvestmentAndFitnessROI",
                                        "Generate a combined analysis of fitness development progress and training effectiveness for an owned horse.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getEdaAnalytics",
                                        "Perform exploratory data analysis on horse performance and biomedical data.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "analysisType", Map.of(
                                                        "type", "string",
                                                        "enum", EdaAnalysisType.allowedForRole("HORSE_OWNER").stream().map(Enum::name).toList(),
                                                        "description", "Required analysis mode: HORSE_PERFORMANCE or BIO_SPC"
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
