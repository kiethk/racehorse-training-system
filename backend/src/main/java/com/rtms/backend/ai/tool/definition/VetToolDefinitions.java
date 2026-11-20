package com.rtms.backend.ai.tool.definition;

import com.rtms.backend.ai.tool.AiToolDefinition;
import com.rtms.backend.ai.tool.ToolDefinitionSchema;

import java.util.List;
import java.util.Map;

import com.rtms.backend.ai.tool.handler.eda.EdaAnalysisType;

/** Role-scoped AI tool definitions for VETERINARIAN. */
public final class VetToolDefinitions {
    private VetToolDefinitions() {
    }

    public static List<AiToolDefinition> create() {
        return List.of(
                new AiToolDefinition(
                                        "searchClinicalKnowledge",
                                        "[RAG Retrieval] Search clinical veterinary knowledge, medical records, and related symptoms using Vector Similarity Search combined with semantic keywords.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "query", Map.of("type", "string", "description", "Symptom description or medical question"),
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional)"),
                                                "limit", Map.of("type", "integer", "description", "Number of excerpts (default 5)")
                                        ), "query")
                                ),
                                new AiToolDefinition(
                                        "getVetExaminationOverview",
                                        "Veterinarian examination overview: in progress (IN_PROGRESS), awaiting exam (SCHEDULED/REQUESTED), completed (COMPLETED), broken down by horse name.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of())
                                ),
                                new AiToolDefinition(
                                        "getHorseMedicalHistory",
                                        "Look up medical records, clinical diagnosis history, and currently effective prescriptions/treatment plans for a horse.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getPreventiveCareSchedule",
                                        "Look up vaccination, deworming, and routine dental care schedules (CareSchedule).",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional)"),
                                                "fromDate", Map.of("type", "string", "description", "From date (YYYY-MM-DD, optional)"),
                                                "toDate", Map.of("type", "string", "description", "To date (YYYY-MM-DD, optional)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getQuarantineStatus",
                                        "List of horses under medical quarantine or in the quarantine barn (training status is always BLOCKED).",
                                        ToolDefinitionSchema.buildParamSchema(Map.of())
                                ),
                                new AiToolDefinition(
                                        "getPendingAdmissionExams",
                                        "List of admission applications awaiting the veterinarian's initial clinical exam to decide approval/block (ALLOWED/BLOCKED).",
                                        ToolDefinitionSchema.buildParamSchema(Map.of())
                                ),
                                new AiToolDefinition(
                                        "getUrgentCareAndIncidentAlerts",
                                        "Look up urgent care duty cases combined with stable health incident reports from the last 48 hours.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of())
                                ),
                                new AiToolDefinition(
                                        "getHorseVitalSignsTrend",
                                        "Look up the horse's most recent vital signs (body temperature, resting heart rate, weight, respiratory rate) from health check logs. Supports a single horse or all recent records when no horse is specified.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional. If blank or 'all', the system returns the 20 most recent vital sign records for all horses)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "analyzeIncidentPatternsAndRecurringRisks",
                                        "Analyze the horse's incident chain and medical records from the last 6 months to detect recurring chronic disease risk.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "recommendClinicalInterventionPlan",
                                        "Recommend a step 1 and step 2 clinical intervention plan plus diagnostic tests based on symptoms and the RAG medical knowledge base.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "symptoms", Map.of("type", "string", "description", "Clinical symptom description (required)"),
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional)")
                                        ), "symptoms")
                                ),
                                new AiToolDefinition(
                                        "evaluateTrainingResumptionReadiness",
                                        "Assess safety and readiness for a horse to resume training (score 0-100%) after quarantine or injury.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getEdaAnalytics",
                                        "Perform exploratory data analysis on horse performance, biomedical, and survival data.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "analysisType", Map.of(
                                                        "type", "string",
                                                        "enum", EdaAnalysisType.allowedForRole("VETERINARIAN").stream().map(Enum::name).toList(),
                                                        "description", "Required analysis mode: HORSE_PERFORMANCE, BIO_SPC, or SURVIVAL"
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
