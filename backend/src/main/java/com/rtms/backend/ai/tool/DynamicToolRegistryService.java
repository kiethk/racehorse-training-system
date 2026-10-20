package com.rtms.backend.ai.tool;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Dịch vụ Quản lý Danh mục Công cụ Động (Dynamic Tool Scoping) theo vai trò.
 *
 * Đảm bảo nguyên tắc bảo mật tối cao:
 * - HORSE_OWNER: 100% Read-Only, tuyệt đối không có công cụ mutation.
 * - HEAD_TRAINER, VETERINARIAN, GROOM, CLUB_MANAGER: Chỉ nhận đúng các công cụ
 *   phù hợp với thẩm quyền chuyên môn của mình.
 */
@Service
public class DynamicToolRegistryService {

    private final Map<String, List<AiToolDefinition>> roleToolDefinitions = new HashMap<>();

    public DynamicToolRegistryService() {
        initToolDefinitions();
    }

    /**
     * Lấy danh sách công cụ được phép cho một vai trò cụ thể.
     */
    public List<AiToolDefinition> getToolsForRole(String role) {
        if (role == null || role.isBlank()) {
            return Collections.emptyList();
        }
        String normalized = normalizeRole(role);
        return roleToolDefinitions.getOrDefault(normalized, Collections.emptyList());
    }

    /**
     * Lấy danh sách Function Spec dạng JSON Map cho Groq/OpenAI API.
     */
    public List<Map<String, Object>> getFunctionSpecsForRole(String role) {
        List<AiToolDefinition> tools = getToolsForRole(role);
        List<Map<String, Object>> specs = new ArrayList<>();
        for (AiToolDefinition tool : tools) {
            specs.add(tool.toFunctionSpec());
        }
        return specs;
    }

    /**
     * Kiểm tra xem một vai trò có quyền gọi tool này không.
     */
    public boolean isToolAllowedForRole(String toolName, String role) {
        if (toolName == null || role == null) {
            return false;
        }
        String normalized = normalizeRole(role);
        List<AiToolDefinition> tools = roleToolDefinitions.getOrDefault(normalized, Collections.emptyList());
        return tools.stream().anyMatch(t -> t.name().equalsIgnoreCase(toolName));
    }

    private void initToolDefinitions() {
        // 1. HORSE_OWNER (100% Read-Only)
        List<AiToolDefinition> ownerTools = List.of(
                new AiToolDefinition(
                        "getMyHorses",
                        "Lấy danh sách các ngựa mà chủ ngựa hiện tại đang sở hữu.",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getHorseTrainingSummary",
                        "Xem tóm tắt tiến trình huấn luyện của ngựa thuộc sở hữu.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa")
                        ), "horseId")
                ),
                new AiToolDefinition(
                        "getHorseCareSchedule",
                        "Xem lịch chăm sóc thú y sắp tới của ngựa thuộc sở hữu.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa")
                        ), "horseId")
                )
        );
        roleToolDefinitions.put("HORSE_OWNER", ownerTools);

        // 2. HEAD_TRAINER
        List<AiToolDefinition> trainerTools = List.of(
                new AiToolDefinition(
                        "getTrainerHorses",
                        "Lấy danh sách các ngựa đang trong kế hoạch tập luyện do huấn luyện viên quản lý.",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getWorkoutHistory",
                        "Tra cứu lịch sử các buổi tập workout của một con ngựa.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa")
                        ), "horseId")
                ),
                new AiToolDefinition(
                        "getHorseFitnessTrend",
                        "Xem xu hướng thể lực (tốc độ, nhịp tim) của ngựa qua các buổi tập gần nhất.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa")
                        ), "horseId")
                ),
                new AiToolDefinition(
                        "proposeEditWorkoutMetric",
                        "[Tier 3 Mutation HITL] Đề xuất sửa đổi chỉ số của một buổi tập (cự ly, tốc độ, nhịp tim). Hành động này sẽ tạo bản nháp cần Trainer bấm xác nhận.",
                        buildParamSchema(Map.of(
                                "workoutId", Map.of("type", "integer", "description", "ID buổi tập cần sửa"),
                                "actualDistanceMeters", Map.of("type", "number", "description", "Cự ly thực tế (mét)"),
                                "topSpeedKmh", Map.of("type", "number", "description", "Tốc độ tối đa (km/h)"),
                                "averageHeartRate", Map.of("type", "integer", "description", "Nhịp tim trung bình"),
                                "trainerFeedback", Map.of("type", "string", "description", "Đánh giá nhận xét của huấn luyện viên"),
                                "reason", Map.of("type", "string", "description", "Lý do đề xuất thay đổi chỉ số")
                        ), "workoutId", "reason")
                )
        );
        roleToolDefinitions.put("HEAD_TRAINER", trainerTools);

        // 3. VETERINARIAN
        List<AiToolDefinition> vetTools = List.of(
                new AiToolDefinition(
                        "searchClinicalKnowledge",
                        "[RAG Retrieval] Tra cứu tri thức y khoa lâm sàng, hồ sơ bệnh án và triệu chứng liên quan bằng Vector Similarity Search.",
                        buildParamSchema(Map.of(
                                "query", Map.of("type", "string", "description", "Mô tả triệu chứng hoặc câu hỏi y khoa"),
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "limit", Map.of("type", "integer", "description", "Số lượng đoạn trích (mặc định 5)")
                        ), "query")
                ),
                new AiToolDefinition(
                        "getHorseMedicalHistory",
                        "Tra cứu hồ sơ bệnh án và lịch sử chẩn đoán lâm sàng của ngựa.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa")
                        ), "horseId")
                ),
                new AiToolDefinition(
                        "getActivePrescriptions",
                        "Xem các đơn thuốc và phác đồ điều trị đang có hiệu lực của ngựa.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa")
                        ), "horseId")
                ),
                new AiToolDefinition(
                        "proposeUpdateHorseMedicalRestriction",
                        "[Tier 3 Mutation HITL] Đề xuất áp dụng hạn chế y tế hoặc yêu cầu cách ly đối với ngựa. Cần bác sĩ bấm phê duyệt trước khi áp dụng.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa"),
                                "restrictionType", Map.of("type", "string", "description", "Loại hạn chế (SUSPEND_TRAINING, QUARANTINE, LIGHT_WALK)"),
                                "medicalNotes", Map.of("type", "string", "description", "Ghi chú y tế chi tiết"),
                                "reason", Map.of("type", "string", "description", "Lý do y khoa đề xuất")
                        ), "horseId", "restrictionType", "reason")
                )
        );
        roleToolDefinitions.put("VETERINARIAN", vetTools);

        // 4. GROOM
        List<AiToolDefinition> groomTools = List.of(
                new AiToolDefinition(
                        "getDailyGroomTasks",
                        "Lấy danh sách công việc chăm sóc chuồng và dẫn ngựa được giao trong ngày.",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getMyAssignedHorses",
                        "Lấy danh sách các chuồng và chú ngựa mà nhân viên Groom hiện tại đang trực tiếp phụ trách.",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getTodayAggregatedTasks",
                        "Lấy danh sách tổng hợp toàn bộ công việc trong ngày của Groom từ 3 nguồn: nhiệm vụ SOP thường nhật, lịch dắt sân tập luyện và lịch khám thú y.",
                        buildParamSchema(Map.of(
                                "date", Map.of("type", "string", "description", "Ngày cần tra cứu (định dạng YYYY-MM-DD, mặc định là ngày hôm nay)")
                        ))
                ),
                new AiToolDefinition(
                        "getHorseDietaryAndCareAlerts",
                        "Tra cứu cảnh báo y tế, quyết định hạn chế tập luyện và ghi chú chăm sóc đặc biệt của một con ngựa.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của con ngựa")
                        ), "horseId")
                ),
                new AiToolDefinition(
                        "checkStallSuppliesStatus",
                        "Kiểm tra tình trạng chuồng trại, khu vực chuồng và trạng thái sử dụng của chuồng.",
                        buildParamSchema(Map.of(
                                "stallId", Map.of("type", "integer", "description", "ID của chuồng trại")
                        ), "stallId")
                ),
                new AiToolDefinition(
                        "getHorseStallLocation",
                        "Tra cứu vị trí chuồng trại hiện tại và khu vực của một con ngựa.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa")
                        ), "horseId")
                )
        );
        roleToolDefinitions.put("GROOM", groomTools);

        // 5. CLUB_MANAGER
        List<AiToolDefinition> managerTools = List.of(
                new AiToolDefinition(
                        "getStableOccupancyMetrics",
                        "Xem thống kê công suất sử dụng chuồng trại (chuồng thường, chuồng cách ly).",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getUpcomingRaces",
                        "Tra cứu danh sách các buổi tập hoặc giải đua ngựa sắp diễn ra.",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getAdmissionRequestsOverview",
                        "Xem tổng quan số lượng đơn đăng ký nhập chuồng đang chờ xét duyệt.",
                        buildParamSchema(Map.of())
                )
        );
        roleToolDefinitions.put("CLUB_MANAGER", managerTools);
    }

    private Map<String, Object> buildParamSchema(Map<String, Object> properties, String... requiredFields) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (requiredFields != null && requiredFields.length > 0) {
            schema.put("required", Arrays.asList(requiredFields));
        }
        return schema;
    }

    private String normalizeRole(String role) {
        String r = role.trim().toUpperCase();
        return switch (r) {
            case "VET" -> "VETERINARIAN";
            case "STABLE_STAFF" -> "GROOM";
            case "RACING_MANAGER" -> "CLUB_MANAGER";
            default -> r;
        };
    }
}
