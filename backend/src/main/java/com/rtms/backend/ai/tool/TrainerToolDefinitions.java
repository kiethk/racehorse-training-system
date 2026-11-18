package com.rtms.backend.ai.tool;

import java.util.List;
import java.util.Map;

import com.rtms.backend.ai.tool.handler.eda.EdaAnalysisType;

/** Role-scoped AI tool definitions for HEAD TRAINER. */
final class TrainerToolDefinitions {
    private TrainerToolDefinitions() {
    }

    static List<AiToolDefinition> create() {
        return List.of(
                new AiToolDefinition(
                                        "getTrainerHorses",
                                        "Retrieve the horses in training plans managed by the trainer, including assigned groom and stable details.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of())
                                ),
                                new AiToolDefinition(
                                        "getTrainerGroups",
                                        "Look up the trainer's Training Groups: each group has up to 6 horses working together, including the horse list with grooms, the current or upcoming Course Run and schedule; also includes disbanded groups with the horses present at disband time. Use when asked which horses belong to a group, which group a horse belongs to, or which groups are idle.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of())
                                ),
                                new AiToolDefinition(
                                        "getWorkoutHistory",
                                        "Look up up to 20 most recent workouts for a horse (newest first); each workout includes date, session, group, exercise, and outcome: Ran (completed with metrics), Needs result, Did not run, Cancelled. For upcoming sessions use getTrainerSchedule.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getHorseFitnessTrend",
                                        "View the horse's fitness trend (speed, heart rate) across workouts, analyze 14-day training load, and warn about overtraining risk.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getTrainerSchedule",
                                        "Look up trainer Sessions by date (defaults to today) or by fromDate to toDate range (showing session, time, exercise, and horse list). Pass needsResultOnly=true to retrieve sessions in the last 90 days that still have horses with no result (Needs result), including their names: this is the attendance worklist.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "date", Map.of("type", "string", "description", "Date to query (YYYY-MM-DD)"),
                                                "fromDate", Map.of("type", "string", "description", "From date (YYYY-MM-DD)"),
                                                "toDate", Map.of("type", "string", "description", "To date (YYYY-MM-DD)"),
                                                "needsResultOnly", Map.of("type", "boolean", "description", "true: only sessions with horses missing results in the last 90 days (ignores date/fromDate/toDate)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getTrainerPlansSummary",
                                        "Look up Course Runs the trainer's horses have joined or are currently attending, one row per horse per course: course name, group, goal, the horse's course status, and Attendance X/Y (completed sessions over total sessions not fully cancelled), similar to the training history screen.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "status", Map.of("type", "string", "description", "Plan status (ACTIVE, UPCOMING, COMPLETED)"),
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getHorseSpecialtyAnalysis",
                                        "Analyze horse specialties (optimal distance, preferred surface, top speed, average rating averageRating, specialty count specialtyCount). If no horse ID/name is provided, or 'all' is passed, the system automatically returns a RANKING of all horses by specialty count (sortBy='count' or 'specialtyCount') or by average specialty rating (sortBy='rating' or 'averageRating'). Always use when the user requests horse rankings by specialty or average rating.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional. Leave blank or pass 'all' to rank all horses)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional. Leave blank or pass 'all' to rank all horses)"),
                                                "sortBy", Map.of("type", "string", "description", "Ranking criterion: 'rating'/'averageRating' (highest average rating) or 'count'/'specialtyCount' (most specialties)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getTrainerRiskAlerts",
                                        "Aggregate injury risk alerts (high heart rate, poor cardiac recovery, declining form) and the latest race-readiness assessment for horses managed by the trainer.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getTrainerAssignedGrooms",
                                        "Look up the grooms responsible for areas/stalls managed by the trainer, including contact details (phone, email), assigned stalls, and horses under their care.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of())
                                ),
                                new AiToolDefinition(
                                        "getAssignedAdmissionReviews",
                                        "Look up admission applications awaiting trainer assessment of intake capacity and stall allocation.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of())
                                ),
                                new AiToolDefinition(
                                        "recommendTrainerStrategy",
                                        "Recommend a comprehensive racing strategy (optimal distance and Turf/Dirt surface) combined with the next workout adjustment based on fitness and the latest workout.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getEdaAnalytics",
                                        "Perform exploratory data analysis on trainer fleet, horse performance, and biomedical data.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "analysisType", Map.of(
                                                        "type", "string",
                                                        "enum", EdaAnalysisType.allowedForRole("HEAD_TRAINER").stream().map(Enum::name).toList(),
                                                        "description", "Required analysis mode: FLEET_PERFORMANCE, HORSE_PERFORMANCE, or BIO_SPC"
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
