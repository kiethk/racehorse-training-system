package com.rtms.backend.ai.tool;

import java.util.List;
import java.util.Map;

/** Role-scoped AI tool definitions for CLUB MANAGER. */
final class ManagerToolDefinitions {
    private ManagerToolDefinitions() {
    }

    static List<AiToolDefinition> create() {
        return List.of(
                new AiToolDefinition(
                                        "getStableOccupancyMetrics",
                                        "View stable occupancy statistics (regular stalls, quarantine stalls).",
                                        ToolDefinitionSchema.buildParamSchema(Map.of())
                                ),
                                new AiToolDefinition(
                                        "getAdmissionPipelineOverview",
                                        "View an overview of admission application counts and approval progress by stage (awaiting approval, awaiting medical exam, awaiting stall allocation).",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "status", Map.of("type", "string", "description", "Filter by approval stage: PENDING (in progress), APPROVED (approved), REJECTED (rejected)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getTrainerAreaDistribution",
                                        "Statistics on stable area distribution, responsible trainer, stall count, and assigned grooms.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of())
                                ),
                                new AiToolDefinition(
                                        "getClubHealthOverview",
                                        "Statistics on total horse count and club-wide health status (total horses, horses cleared for training vs medically restricted, incidents this week). Always use this tool when asked how many horses are in the club or the size of the herd.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of())
                                ),
                                new AiToolDefinition(
                                        "getSystemStaffOverview",
                                        "Look up the staff directory and staff count statistics in the system (total staff 'totalStaff', breakdown by role 'roleBreakdown': Head Trainer, Veterinarian, Groom, Manager). Supports filtering by role, status (isActive), or search keyword. Always use this tool when asked how many staff, grooms, trainers, or veterinarians are in the club.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "role", Map.of("type", "string", "description", "Staff role to filter (HEAD_TRAINER, VETERINARIAN, GROOM, CLUB_MANAGER)"),
                                                "isActive", Map.of("type", "boolean", "description", "Active status (true: active, false: locked)"),
                                                "keyword", Map.of("type", "string", "description", "Search keyword by full name, email, or phone number")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getClubHorsesDirectory",
                                        "Look up the detailed horse roster in the club (horse name, UELN, stall, responsible trainer, specialty count specialtyCount, specialty list specialties). Filter by status or horse name / owner name.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "status", Map.of("type", "string", "description", "Horse status (ACTIVE, IN_TRAINING, RESTING, QUARANTINE)"),
                                                "keyword", Map.of("type", "string", "description", "Search keyword by horse name or owner name")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getHorseSpecialtyAnalysis",
                                        "Analyze horse specialties (optimal distance, preferred surface, specialty count, average rating averageRating). If no horse ID/name is provided, or 'all' is passed, the system automatically returns a RANKING of all horses by specialty count (sortBy='count') or by average specialty rating (sortBy='rating'). Always use when the user requests horse rankings by specialty or average rating.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "horseId", Map.of("type", "integer", "description", "Horse ID (optional. Leave blank or pass 'all' to rank all horses)"),
                                                "horseName", Map.of("type", "string", "description", "Horse name (optional. Leave blank or pass 'all' to rank all horses)"),
                                                "sortBy", Map.of("type", "string", "description", "Ranking criterion when scanning all horses: 'rating' (by average specialty rating, highest first) or 'count' (by specialty count, defaults to combining both)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "getRecentAuditLogs",
                                        "Look up the system audit trail for recent sensitive operations (total audit logs totalAuditLogs, detailed log list auditLogList).",
                                        ToolDefinitionSchema.buildParamSchema(Map.of(
                                                "limit", Map.of("type", "integer", "description", "Number of audit log records (default 10)")
                                        ))
                                ),
                                new AiToolDefinition(
                                        "analyzeStableCapacityAndStallBottlenecks",
                                        "Analyze stable capacity and forecast horse intake bottlenecks over the next 30-60 days.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of())
                                ),
                                new AiToolDefinition(
                                        "recommendGroomWorkloadRebalancing",
                                        "Recommend workload rebalancing and stall redistribution across grooms to prevent overload.",
                                        ToolDefinitionSchema.buildParamSchema(Map.of())
                                )
        );
    }
}
