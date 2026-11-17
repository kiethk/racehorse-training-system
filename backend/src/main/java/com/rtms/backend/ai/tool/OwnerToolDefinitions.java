package com.rtms.backend.ai.tool;

import java.util.List;
import java.util.Map;

/** Role-scoped AI tool definitions for HORSE OWNER. */
final class OwnerToolDefinitions {
    private OwnerToolDefinitions() {
    }

    static List<AiToolDefinition> create() {
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
                                )
        );
    }
}
