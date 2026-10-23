package com.rtms.backend.ai.tool;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Registry quản lý và cung cấp danh mục công cụ AI Agent (100% Read-Only / RAG Advisory) theo vai trò người dùng (Role-Based Tool Scoping).
 */
@Service
public class DynamicToolRegistryService {

    private final Map<String, List<AiToolDefinition>> roleToolDefinitions = new HashMap<>();

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
        return tools.stream().anyMatch(t -> t.name().equalsIgnoreCase(toolName));
    }

    private void initToolDefinitions() {
        // 1. HORSE_OWNER (100% Read-Only)
        List<AiToolDefinition> ownerTools = List.of(
                new AiToolDefinition(
                        "getMyHorses",
                        "Lấy danh sách các ngựa mà chủ ngựa hiện tại đang sở hữu (tên ngựa, UELN, chuồng, tình trạng).",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getMyHorseHealthSummary",
                        "Xem tóm tắt tình trạng sức khỏe tổng quát, quyết định huấn luyện và ghi chú y tế của ngựa thuộc sở hữu.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "getMyHorseTrainingProgress",
                        "Xem tiến độ khóa huấn luyện và số buổi tập đã hoàn thành của ngựa thuộc sở hữu.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "getMyHorseUpcomingSchedule",
                        "Tra cứu lịch tập luyện sắp tới của các ngựa thuộc sở hữu của chủ ngựa.",
                        buildParamSchema(Map.of(
                                "fromDate", Map.of("type", "string", "description", "Ngày bắt đầu (YYYY-MM-DD, mặc định hôm nay)"),
                                "toDate", Map.of("type", "string", "description", "Ngày kết thúc (YYYY-MM-DD, mặc định 14 ngày tới)")
                        ))
                ),
                new AiToolDefinition(
                        "getMyHorseAdmissions",
                        "Lấy danh sách các đơn đăng ký nhập chuồng của chủ sở hữu.",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getHorseTrainingSummary",
                        "Xem tóm tắt tiến trình huấn luyện của ngựa thuộc sở hữu.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "getHorseCareSchedule",
                        "Xem lịch chăm sóc thú y sắp tới của ngựa thuộc sở hữu.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "getMyHorseIncidentHistory",
                        "Xem lịch sử các sự cố sức khỏe hoặc trầy xước nhẹ của ngựa thuộc sở hữu được nhân viên ghi nhận.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "getMyHorsePedigreeInfo",
                        "Tra cứu thông tin phả hệ / huyết thống của chiến mã (Ngựa cha - Sire, Ngựa mẹ - Dam, xuất xứ).",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "analyzeHorseInvestmentAndFitnessROI",
                        "Báo cáo phân tích tổng hợp tiến độ phát triển thể lực và hiệu quả huấn luyện của chiến mã thuộc sở hữu.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                )
        );
        roleToolDefinitions.put("HORSE_OWNER", ownerTools);

        // 2. HEAD_TRAINER (100% Read-Only)
        List<AiToolDefinition> trainerTools = List.of(
                new AiToolDefinition(
                        "getTrainerHorses",
                        "Lấy danh sách các ngựa đang trong kế hoạch tập luyện do huấn luyện viên quản lý kèm theo Groom phụ trách và chuồng trại.",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getWorkoutHistory",
                        "Tra cứu lịch sử các buổi tập workout của một con ngựa.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "getHorseFitnessTrend",
                        "Xem xu hướng thể lực (tốc độ, nhịp tim) của ngựa qua các buổi tập gần nhất.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "getTrainerSchedule",
                        "Tra cứu các ca tập TrainingLot của HLV theo ngày date (mặc định hôm nay) hoặc khoảng fromDate đến toDate (hiển thị rõ Ca tập, thời gian, bài tập, danh sách ngựa).",
                        buildParamSchema(Map.of(
                                "date", Map.of("type", "string", "description", "Ngày cần tra cứu (YYYY-MM-DD)"),
                                "fromDate", Map.of("type", "string", "description", "Từ ngày (YYYY-MM-DD)"),
                                "toDate", Map.of("type", "string", "description", "Đến ngày (YYYY-MM-DD)")
                        ))
                ),
                new AiToolDefinition(
                        "getTrainerPlansSummary",
                        "Tra cứu danh sách kế hoạch huấn luyện do HLV quản lý (hiển thị Tên kế hoạch, Tên ngựa, Mục tiêu, Trạng thái, số buổi hoàn thành).",
                        buildParamSchema(Map.of(
                                "status", Map.of("type", "string", "description", "Trạng thái kế hoạch (ACTIVE, UPCOMING, COMPLETED)"),
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "getHorseSpecialtyAnalysis",
                        "Phân tích sở trường của ngựa (cự ly tối ưu, mặt sân ưa thích rõ ràng, tốc độ đỉnh).",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của con ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên con ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "getTrainerRiskAlerts",
                        "Tổng hợp các cảnh báo nguy cơ chấn thương (nhịp tim cao, tụt phong độ, sự cố lặp lại) của các ngựa do HLV quản lý.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "getRacingReadinessAssessment",
                        "Tra cứu đánh giá sẵn sàng thi đấu gần nhất của ngựa.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "checkGroomConflictForLot",
                        "Kiểm tra quy tắc BR-09 khi gán Groom cho ngựa trong ca tập (lot).",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)"),
                                "groomId", Map.of("type", "integer", "description", "ID của Groom cần kiểm tra"),
                                "date", Map.of("type", "string", "description", "Ngày bắt đầu kiểm tra (YYYY-MM-DD, mặc định hôm nay)")
                        ))
                ),
                new AiToolDefinition(
                        "getTrainerAssignedGrooms",
                        "Tra cứu danh sách nhân viên Groom phụ trách các khu vực/chuồng thuộc quản lý của huấn luyện viên bao gồm thông tin liên hệ (số điện thoại, email), các chuồng phụ trách và ngựa đang chăm sóc.",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getMyHorseRaceRegistrations",
                        "Tra cứu các lượt đăng ký giải đua của ngựa thuộc quyền quản lý của HLV, xem trạng thái phê duyệt (PROPOSED, CONFIRMED, REJECTED).",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "getAssignedAdmissionReviews",
                        "Tra cứu các đơn đăng ký nhập đàn đang chờ Huấn luyện viên thẩm định khả năng tiếp nhận và bố trí chuồng trại.",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getCourseCurriculumOverview",
                        "Tra cứu danh mục khóa học (Course), môn học (Subject) và các bài tập kỹ thuật có trong hệ thống.",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "analyzeWorkoutLoadAndOvertrainingRisk",
                        "Phân tích khối lượng vận động 14 ngày qua (cự ly, cường độ, nhịp tim hồi phục sau bài tập) và đánh giá nguy cơ quá tải/chấn thương của ngựa.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "recommendOptimalRaceDistanceAndTrack",
                        "Đề xuất cự ly thi đấu và loại mặt sân (Turf / Dirt) tối ưu nhất cho ngựa dựa trên phân tích sở trường và dữ liệu thi đấu thực tế.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "recommendNextWorkoutAdjustment",
                        "Đề xuất điều chỉnh bài tập kế tiếp (tăng/giảm cự ly, đổi mặt sân hoặc nghỉ hồi phục) dựa trên kết quả buổi tập gần nhất.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                )
        );
        roleToolDefinitions.put("HEAD_TRAINER", trainerTools);

        // 3. VETERINARIAN (100% Read-Only)
        List<AiToolDefinition> vetTools = List.of(
                new AiToolDefinition(
                        "searchClinicalKnowledge",
                        "[RAG Retrieval] Tra cứu tri thức y khoa lâm sàng, hồ sơ bệnh án và triệu chứng liên quan bằng Vector Similarity Search kết hợp từ khóa ngữ nghĩa.",
                        buildParamSchema(Map.of(
                                "query", Map.of("type", "string", "description", "Mô tả triệu chứng hoặc câu hỏi y khoa"),
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)"),
                                "limit", Map.of("type", "integer", "description", "Số lượng đoạn trích (mặc định 5)")
                        ), "query")
                ),
                new AiToolDefinition(
                        "getVetExaminationOverview",
                        "Tổng quan tình hình khám chữa bệnh của Bác sĩ thú y: đang khám (IN_PROGRESS), chờ khám (SCHEDULED/REQUESTED), đã khám (COMPLETED) phân theo tên từng con ngựa.",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getHorseMedicalHistory",
                        "Tra cứu hồ sơ bệnh án và lịch sử chẩn đoán lâm sàng của ngựa.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "getActivePrescriptions",
                        "Xem các đơn thuốc và phác đồ điều trị đang có hiệu lực của ngựa.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "getPreventiveCareSchedule",
                        "Tra cứu lịch tiêm phòng, tẩy giun, chăm sóc nha khoa định kỳ (CareSchedule).",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)"),
                                "fromDate", Map.of("type", "string", "description", "Từ ngày (YYYY-MM-DD, tùy chọn)"),
                                "toDate", Map.of("type", "string", "description", "Đến ngày (YYYY-MM-DD, tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "getQuarantineStatus",
                        "Danh sách ngựa đang cách ly y tế hoặc ở khu chuồng cách ly (trạng thái tập luyện luôn là BLOCKED).",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getRecentIncidentReports",
                        "Danh sách báo cáo sự cố sức khỏe chuồng trại do Groom gửi trong 48h qua.",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getPendingAdmissionExams",
                        "Danh sách đơn đăng ký nhập đàn đang chờ Bác sĩ thú y đến khám lâm sàng đầu vào để quyết định duyệt/chặn (ALLOWED/BLOCKED).",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getUrgentCareAlerts",
                        "Tra cứu các ca trực cấp cứu hoặc ca chăm sóc y tế khẩn cấp phát sinh từ chuồng trại.",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getHorseVitalSignsTrend",
                        "Tra cứu các chỉ số sinh hiệu gần nhất của ngựa (Thân nhiệt, Nhịp tim khi nghỉ, Cân nặng, Nhịp thở) từ nhật ký kiểm tra sức khỏe. Hỗ trợ xem cho từng ngựa hoặc toàn bộ các bản ghi gần nhất nếu không chỉ định ngựa.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn. Nếu để trống hoặc 'all'/'tất cả', hệ thống trả về 20 chỉ số sinh hiệu gần nhất của toàn bộ các ngựa)")
                        ))
                ),
                new AiToolDefinition(
                        "analyzeIncidentPatternsAndRecurringRisks",
                        "Phân tích chuỗi sự cố chuồng trại và hồ sơ bệnh án trong 6 tháng qua của ngựa để phát hiện nguy cơ bệnh lý mãn tính tái phát.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "recommendClinicalInterventionPlan",
                        "Đề xuất phác đồ can thiệp lâm sàng bước 1, bước 2 và chỉ định xét nghiệm dựa trên triệu chứng và kho tri thức y khoa RAG.",
                        buildParamSchema(Map.of(
                                "symptoms", Map.of("type", "string", "description", "Mô tả triệu chứng lâm sàng (bắt buộc)"),
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ), "symptoms")
                ),
                new AiToolDefinition(
                        "evaluateTrainingResumptionReadiness",
                        "Đánh giá mức độ an toàn và sẵn sàng cho phép ngựa quay lại tập luyện (thang điểm 0-100%) sau thời gian cách ly hoặc chấn thương.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                )
        );
        roleToolDefinitions.put("VETERINARIAN", vetTools);

        // 4. GROOM (100% Read-Only) - Đã bỏ checkStallSuppliesStatus theo yêu cầu
        List<AiToolDefinition> groomTools = List.of(
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
                                "horseId", Map.of("type", "integer", "description", "ID của con ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên của con ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "getHorseStallLocation",
                        "Tra cứu vị trí chuồng trại hiện tại, khu vực và người phụ trách của một con ngựa theo tên hoặc mã.",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "getDailyGroomTasks",
                        "Lấy danh sách công việc chăm sóc chuồng và dẫn ngựa được giao trong ngày.",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getMyReportedIncidents",
                        "Tra cứu các sự cố chuồng trại hoặc vết thương của ngựa do chính Groom báo cáo trong 7 ngày qua và trạng thái xử lý.",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getGroomTaskCompletionRate",
                        "Kiểm tra tiến độ hoàn thành các đầu việc chăm sóc trong ngày của Groom (Vệ sinh chuồng, Cho ăn, Dẫn tập).",
                        buildParamSchema(Map.of(
                                "date", Map.of("type", "string", "description", "Ngày cần kiểm tra (YYYY-MM-DD, mặc định hôm nay)")
                        ))
                ),
                new AiToolDefinition(
                        "detectHorseBehavioralAnomaly",
                        "Phát hiện sớm các bất thường về hành vi (bỏ ăn, cào đất, uể oải) của ngựa qua nhật ký và cảnh báo nguy cơ bệnh cấp tính (Colic).",
                        buildParamSchema(Map.of(
                                "horseId", Map.of("type", "integer", "description", "ID của ngựa (tùy chọn)"),
                                "horseName", Map.of("type", "string", "description", "Tên ngựa (tùy chọn)")
                        ))
                ),
                new AiToolDefinition(
                        "recommendDailyCarePriorityOrder",
                        "Đề xuất thứ tự ưu tiên các công việc chăm sóc trong ngày (ngựa cần khám y tế -> ca tập sớm -> vệ sinh chuồng cách ly sau cùng).",
                        buildParamSchema(Map.of())
                )
        );
        roleToolDefinitions.put("GROOM", groomTools);

        // 5. CLUB_MANAGER (100% Read-Only) - Quản trị chuồng trại, nhân sự, chiến mã, audit log và tuyển nhập đàn
        List<AiToolDefinition> managerTools = List.of(
                new AiToolDefinition(
                        "getStableOccupancyMetrics",
                        "Xem thống kê công suất sử dụng chuồng trại (chuồng thường, chuồng cách ly).",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getAdmissionRequestsOverview",
                        "Thống kê tổng quan số lượng đơn đăng ký nhập đàn/nhập chuồng trong toàn hệ thống (tổng số đơn toàn hệ thống, số đơn đang chờ Club Manager phê duyệt cuối, số đơn đang trong tiến trình xử lý liên phòng ban, số đơn đã hoàn tất nhập đàn và từ chối/quá hạn).",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getTrainerAreaDistribution",
                        "Thống kê phân bổ khu vực chuồng trại, HLV phụ trách, số chuồng và Groom được gán.",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getClubHealthOverview",
                        "Thống kê tổng số lượng ngựa và tình trạng sức khỏe toàn đàn trong câu lạc bộ (tổng số chiến mã, số ngựa được phép tập luyện vs cấm tập y tế, số ca sự cố trong tuần). Bắt buộc dùng công cụ này khi người dùng hỏi có bao nhiêu ngựa trong câu lạc bộ hoặc quy mô đàn ngựa.",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "getSystemStaffOverview",
                        "Tra cứu danh bạ nhân sự trong hệ thống (HLV, Bác sĩ, Groom), có thể lọc theo vai trò (role), trạng thái (isActive) hoặc từ khóa tìm kiếm.",
                        buildParamSchema(Map.of(
                                "role", Map.of("type", "string", "description", "Vai trò nhân sự cần lọc (HEAD_TRAINER, VETERINARIAN, GROOM, CLUB_MANAGER)"),
                                "isActive", Map.of("type", "boolean", "description", "Trạng thái hoạt động (true: đang hoạt động, false: đã khóa)"),
                                "keyword", Map.of("type", "string", "description", "Từ khóa tìm kiếm theo họ tên, email hoặc số điện thoại")
                        ))
                ),
                new AiToolDefinition(
                        "getClubHorsesDirectory",
                        "Tra cứu danh sách chi tiết các chiến mã trong câu lạc bộ (tên ngựa, UELN, chuồng, HLV phụ trách). Lọc theo trạng thái (status) hoặc tên ngựa / tên chủ ngựa.",
                        buildParamSchema(Map.of(
                                "status", Map.of("type", "string", "description", "Trạng thái chiến mã (ACTIVE, IN_TRAINING, RESTING, QUARANTINE)"),
                                "keyword", Map.of("type", "string", "description", "Từ khóa tìm kiếm theo tên ngựa hoặc tên chủ ngựa")
                        ))
                ),
                new AiToolDefinition(
                        "getRecentAuditLogs",
                        "Tra cứu nhật ký kiểm toán hệ thống (Audit Trail) các thao tác nhạy cảm gần đây.",
                        buildParamSchema(Map.of(
                                "limit", Map.of("type", "integer", "description", "Số lượng bản ghi nhật ký kiểm toán (mặc định 10)")
                        ))
                ),
                new AiToolDefinition(
                        "getAdmissionPipelineDetails",
                        "Chi tiết các đơn đăng ký nhập đàn theo từng giai đoạn phê duyệt (chờ duyệt, chờ khám y tế, chờ xếp chuồng).",
                        buildParamSchema(Map.of(
                                "status", Map.of("type", "string", "description", "Lọc theo giai đoạn phê duyệt: PENDING (đang xử lý), APPROVED (đã duyệt), REJECTED (từ chối)")
                        ))
                ),
                new AiToolDefinition(
                        "analyzeStableCapacityAndStallBottlenecks",
                        "Phân tích công suất chuồng trại và dự báo điểm nghẽn tiếp nhận ngựa trong 30-60 ngày tới.",
                        buildParamSchema(Map.of())
                ),
                new AiToolDefinition(
                        "recommendGroomWorkloadRebalancing",
                        "Đề xuất tái cân bằng tải công việc và phân bổ chuồng giữa các nhân viên Groom để tránh quá tải.",
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
