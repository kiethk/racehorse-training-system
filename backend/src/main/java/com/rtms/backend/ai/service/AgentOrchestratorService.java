package com.rtms.backend.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.ai.conversation.dto.AiChatMessageDetailDto;
import com.rtms.backend.ai.conversation.entity.AiConversation;
import com.rtms.backend.ai.conversation.service.AiConversationService;
import com.rtms.backend.ai.dto.AiChatMessageDto;
import com.rtms.backend.ai.dto.AiChatRequestDto;
import com.rtms.backend.ai.dto.AiChatResponseDto;
import com.rtms.backend.ai.tool.AiToolExecutionService;
import com.rtms.backend.ai.tool.DynamicToolRegistryService;
import com.rtms.backend.security.AuthenticatedUser;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Điều phối viên chính cho AI Chat Agent sử dụng mô hình LLM (OpenAI / GPT-4o / Compatible API).
 * 1. Tiếp nhận câu hỏi và quản lý ngữ cảnh hội thoại lưu trữ bền vững (Database Persistence).
 * 2. Cung cấp danh mục công cụ động (Dynamic Tool Scoping) theo vai trò.
 * 3. Tiếp nhận Tool Calls từ mô hình AI, ủy quyền cho AiToolExecutionService thực thi.
 * 4. Bơm kết quả Tool Result ngược lại cho AI để tổng hợp câu trả lời tự nhiên.
 * 5. Lưu vết hội thoại và công cụ thực thi vào DB phục vụ Audit Trail.
 */
@Service
public class AgentOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestratorService.class);
    private static final int MAX_REACT_ITERATIONS = 4;

    private final String aiBaseUrl;
    private final String aiApiKey;
    private final String aiModel;
    private final DynamicToolRegistryService toolRegistryService;
    private final AiToolExecutionService toolExecutionService;
    private final AiConversationService conversationService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public AgentOrchestratorService(
            @Value("${ai.api.base-url:${groq.api.base-url:https://api.openai.com/v1}}") String aiBaseUrl,
            @Value("${ai.api.key:${groq.api.key:}}") String aiApiKey,
            @Value("${ai.api.model:${groq.api.model:gpt-4o-mini}}") String aiModel,
            DynamicToolRegistryService toolRegistryService,
            AiToolExecutionService toolExecutionService,
            @Autowired(required = false) AiConversationService conversationService,
            ObjectMapper objectMapper) {
        this.aiBaseUrl = aiBaseUrl != null && !aiBaseUrl.isBlank() ? aiBaseUrl.trim() : "https://api.openai.com/v1";
        this.aiApiKey = aiApiKey != null ? aiApiKey.trim() : "";
        this.aiModel = aiModel != null && !aiModel.isBlank() ? aiModel.trim() : "gpt-4o-mini";
        this.toolRegistryService = toolRegistryService;
        this.toolExecutionService = toolExecutionService;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public AgentOrchestratorService(
            String aiBaseUrl,
            String aiApiKey,
            String aiModel,
            DynamicToolRegistryService toolRegistryService,
            AiToolExecutionService toolExecutionService,
            ObjectMapper objectMapper) {
        this(aiBaseUrl, aiApiKey, aiModel, toolRegistryService, toolExecutionService, null, objectMapper);
    }

    @PostConstruct
    public void init() {
        if (this.aiApiKey.isBlank()) {
            log.warn("AgentOrchestratorService: AI_API_KEY chưa được cấu hình.");
        } else {
            String masked = this.aiApiKey.length() > 8
                    ? this.aiApiKey.substring(0, 7) + "..." + this.aiApiKey.substring(this.aiApiKey.length() - 4)
                    : "***";
            log.info("AgentOrchestratorService: Đã nạp thành công AI_API_KEY [{}] với model [{}]", masked, aiModel);
        }
    }

    /**
     * Xử lý hội thoại chat với người dùng và đồng bộ vào DB.
     */
    @SuppressWarnings("unchecked")
    public AiChatResponseDto processChat(AiChatRequestDto request, AuthenticatedUser currentUser) {
        Long conversationId = request.conversationId();

        // 1. Quản lý Conversation Thread bền vững trong Database
        if (conversationService != null && currentUser != null && currentUser.getUserId() != null) {
            try {
                AiConversation conv = conversationService.getOrCreateConversation(
                        conversationId,
                        currentUser.getUserId(),
                        request.message()
                );
                conversationId = conv.getId();
                conversationService.saveMessage(conversationId, "user", request.message(), null);
            } catch (Exception e) {
                log.error("Lỗi khi lưu trữ phiên hội thoại vào database: {}", e.getMessage());
            }
        }

        if (aiApiKey == null || aiApiKey.isBlank()) {
            log.warn("AgentOrchestratorService: AI_API_KEY chưa được cấu hình.");
            String fallbackReply = "Chưa cấu hình AI_API_KEY. Vui lòng thiết lập biến môi trường AI_API_KEY trong Run Configuration của IDE hoặc application.properties để sử dụng trợ lý AI.";
            if (conversationService != null && conversationId != null) {
                conversationService.saveMessage(conversationId, "assistant", fallbackReply, null);
            }
            return new AiChatResponseDto(
                    fallbackReply,
                    conversationId,
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        List<Map<String, Object>> executedTools = new ArrayList<>();

        // 2. Xây dựng danh sách tin nhắn hội thoại
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", buildSystemPrompt(currentUser)
        ));

        // Nạp lịch sử trò chuyện
        if (request.conversationHistory() != null && !request.conversationHistory().isEmpty()) {
            for (AiChatMessageDto msg : request.conversationHistory()) {
                if (msg != null && msg.content() != null && !msg.content().isBlank()) {
                    messages.add(Map.of(
                            "role", msg.role() != null ? msg.role() : "user",
                            "content", msg.content()
                    ));
                }
            }
        } else if (conversationService != null && conversationId != null && currentUser != null && currentUser.getUserId() != null) {
            // Nạp từ Database nếu client không gửi mảng history thủ công
            try {
                List<AiChatMessageDetailDto> dbHistory = conversationService.getConversationMessages(conversationId, currentUser.getUserId());
                int totalMsgs = dbHistory.size();
                int startIndex = Math.max(0, totalMsgs - 11);
                int endIndex = Math.max(0, totalMsgs - 1); // Bỏ qua tin cuối vừa lưu
                for (int i = startIndex; i < endIndex; i++) {
                    AiChatMessageDetailDto m = dbHistory.get(i);
                    messages.add(Map.of(
                            "role", m.role() != null ? m.role() : "user",
                            "content", m.content()
                    ));
                }
            } catch (Exception e) {
                log.warn("Không thể tải lịch sử từ DB cho hội thoại #{}: {}", conversationId, e.getMessage());
            }
        }

        messages.add(Map.of(
                "role", "user",
                "content", request.message()
        ));

        // 3. Lấy danh sách công cụ theo vai trò người dùng (Dynamic Tool Scoping)
        List<Map<String, Object>> tools = toolRegistryService.getFunctionSpecsForRole(currentUser.getRole());

        // 4. Vòng lặp suy luận và gọi công cụ (ReAct Loop)
        String finalAnswer = "";
        int iterations = 0;

        while (iterations < MAX_REACT_ITERATIONS) {
            iterations++;
            Map<String, Object> aiResponse = callAiChatCompletions(messages, tools);

            if (aiResponse == null || !aiResponse.containsKey("choices")) {
                finalAnswer = "Xin lỗi, hiện tại không thể kết nối đến máy chủ AI. Vui lòng thử lại sau.";
                break;
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) aiResponse.get("choices");
            if (choices == null || choices.isEmpty()) {
                finalAnswer = "Không nhận được phản hồi hợp lệ từ máy chủ AI.";
                break;
            }

            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> assistantMessage = (Map<String, Object>) firstChoice.get("message");
            if (assistantMessage == null) {
                break;
            }

            // Kiểm tra xem LLM có gọi tool hay không
            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) assistantMessage.get("tool_calls");
            String content = (String) assistantMessage.get("content");

            if (toolCalls == null || toolCalls.isEmpty()) {
                finalAnswer = content != null ? content : "";
                break;
            }

            // LLM yêu cầu gọi tool: Thêm message trợ lý có chứa tool_calls vào context
            messages.add(assistantMessage);

            for (Map<String, Object> toolCall : toolCalls) {
                String toolCallId = (String) toolCall.get("id");
                Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                String toolName = (String) function.get("name");
                String argumentsJson = (String) function.get("arguments");

                log.info("Agent ReAct loop [{}]: Thực thi tool [{}] với args [{}]", iterations, toolName, argumentsJson);

                Map<String, Object> arguments = parseArguments(argumentsJson);

                // Thực thi công cụ qua AiToolExecutionService
                Object result;
                try {
                    result = toolExecutionService.executeTool(toolName, arguments, currentUser);
                } catch (Exception e) {
                    log.warn("Lỗi khi AI gọi tool [{}]: {}", toolName, e.getMessage());
                    result = Map.of(
                            "status", "ERROR",
                            "error", e.getMessage() != null ? e.getMessage() : "Lỗi thực thi công cụ",
                            "huongDanAI", "Công cụ gặp lỗi hoặc thiếu tham số. Vui lòng tự tra cứu tham số còn thiếu qua công cụ danh bạ/danh sách trước hoặc giải thích lịch sự cho người dùng."
                    );
                }

                executedTools.add(Map.of(
                        "tool", toolName,
                        "arguments", arguments,
                        "result", result != null ? result : Map.of()
                ));

                // Bơm kết quả tool result lại vào context
                try {
                    String resultJson = objectMapper.writeValueAsString(result);
                    messages.add(Map.of(
                            "role", "tool",
                            "tool_call_id", toolCallId != null ? toolCallId : "",
                            "name", toolName,
                            "content", resultJson
                    ));
                } catch (Exception e) {
                    log.error("Lỗi khi serialize kết quả tool result cho AI model: {}", e.getMessage());
                    messages.add(Map.of(
                            "role", "tool",
                            "tool_call_id", toolCallId != null ? toolCallId : "",
                            "name", toolName,
                            "content", "{\"error\": \"Lỗi serialize kết quả\"}"
                    ));
                }
            }
        }

        boolean isMeaninglessAnswer = finalAnswer == null || finalAnswer.isBlank()
                || finalAnswer.trim().equals("Tôi đã hoàn thành việc tra cứu dữ liệu theo yêu cầu của bạn.");

        if (isMeaninglessAnswer && !executedTools.isEmpty()) {
            // Thử gọi 1 turn tổng hợp cuối cùng yêu cầu LLM tóm tắt kết quả tool
            try {
                List<Map<String, Object>> summaryMessages = new ArrayList<>(messages);
                summaryMessages.add(Map.of(
                        "role", "user",
                        "content", "Dựa trên dữ liệu từ các công cụ đã thực thi ở trên, hãy tổng hợp câu trả lời chi tiết và rõ ràng cho tôi. Trình bày bằng bảng Markdown, gạch đầu dòng phân tích cụ thể, và điều hướng liên kết phù hợp. Tuyệt đối không chỉ trả lời ngắn ngủi hoặc chung chung."
                ));
                Map<String, Object> summaryResponse = callAiChatCompletions(summaryMessages, null);
                if (summaryResponse != null && summaryResponse.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) summaryResponse.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                        if (msg != null && msg.get("content") != null) {
                            String summaryContent = ((String) msg.get("content")).trim();
                            if (!summaryContent.isBlank() && !summaryContent.equals("Tôi đã hoàn thành việc tra cứu dữ liệu theo yêu cầu của bạn.")) {
                                finalAnswer = summaryContent;
                                isMeaninglessAnswer = false;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Không thể gọi LLM tổng hợp tóm tắt kết quả tool: {}", e.getMessage());
            }

            // Nếu LLM vẫn không trả lời hoặc trả về câu vô nghĩa, tự động định dạng dữ liệu từ executedTools
            if (isMeaninglessAnswer) {
                finalAnswer = formatFallbackToolSummary(executedTools);
            }
        } else if (finalAnswer == null || finalAnswer.isBlank()) {
            finalAnswer = "Tôi đã hoàn thành việc tra cứu dữ liệu theo yêu cầu của bạn.";
        }

        // 5. Lưu phản hồi của trợ lý vào Database
        if (conversationService != null && conversationId != null) {
            try {
                String toolCallsJson = executedTools.isEmpty() ? null : objectMapper.writeValueAsString(executedTools);
                conversationService.saveMessage(conversationId, "assistant", finalAnswer, toolCallsJson);
            } catch (Exception e) {
                log.error("Lỗi khi lưu phản hồi AI vào DB: {}", e.getMessage());
            }
        }

        return new AiChatResponseDto(finalAnswer, conversationId, executedTools, Collections.emptyList());
    }

    String buildSystemPrompt(AuthenticatedUser user) {
        String role = user != null && user.getRole() != null ? user.getRole() : "ANONYMOUS";
        String userName = user != null && user.getEmail() != null ? user.getEmail() : "Người dùng";

        return String.format("""
                Bạn là Trợ lý AI Thông minh RTMS (Racehorse Training Management System) chuyên nghiệp cho trường đua ngựa.
                Thông tin người dùng hiện tại: %s, Vai trò: %s.

                QUY TẮC BẮT BUỘC (TUÂN THỦ TUYỆT ĐỐI 100%%):
                1. TUYỆT ĐỐI KHÔNG HIỂN THỊ ID SỐ NỘI BỘ TRONG DATABASE TRONG CÂU TRẢ LỜI (Ví dụ: không viết "Ngựa #1", "Ngựa ID 5", "Stall 10", "Lot #12", "Groom #3"). Luôn luôn chuyển đổi và hiển thị bằng TÊN ĐẦY ĐỦ VÀ CÓ Ý NGHĨA (Ví dụ: "Chiến mã Thunder Flash", "Chuồng A-01", "Groom Nguyễn Văn A", "Ca Sáng (08:00 - 09:30)").
                2. TUYỆT ĐỐI KHÔNG HIỂN THỊ TÊN HÀM / TÊN TOOL CODE NỘI BỘ TRONG ĐOẠN CHAT (Ví dụ: không bao giờ nhắc đến "getTrainerSchedule", "getTrainerHorses", "getQuarantineStatus", "handleGet..."). Thay vào đó hãy dùng ngôn từ nghiệp vụ tự nhiên (Ví dụ: "Hệ thống ghi nhận lịch huấn luyện...", "Theo phân công chuồng trại...").
                3. HẠN CHẾ CONFLICT DỮ LIỆU:
                   - Tất cả các ngựa đang trong diện cách ly y tế hoặc ở khu chuồng cách ly (Quarantine) TUYỆT ĐỐI KHÔNG ĐƯỢC PHÉP TẬP LUYỆN (trạng thái luyện tập luôn là CẤM TẬP / BLOCKED). Không bao giờ trả lời mâu thuẫn giữa cách ly y tế và cho phép tập luyện.
                4. VẬN HÀNH 100%% READ-ONLY / RAG ADVISORY: Bạn chỉ giải đáp, phân tích và tra cứu thông tin dựa trên quyền hạn vai trò người dùng.
                5. BẢO CHỨNG DỮ LIỆU THỰC TẾ & CHỐNG ẢO GIÁC (GROUNDEDNESS & ZERO HALLUCINATION):
                   - ĐỐI VỚI DỮ LIỆU THỰC THỂ & NGHIỆP VỤ NỘI BỘ (Tên ngựa, mã UELN, chuồng trại, lịch tập, chỉ số sinh hiệu, bệnh án, đơn đăng ký, nhân sự):
                     + TUYỆT ĐỐI KHÔNG tự suy đoán, phỏng đoán hoặc bịa đặt dữ liệu. MỌI dữ kiện thực tế về hệ thống RTMS bắt buộc phải được lấy thông qua việc gọi công cụ (tool_calls) tương ứng với vai trò.
                     + Nếu công cụ trả về rỗng hoặc không tìm thấy dữ liệu: BẮT BUỘC thông báo rõ ràng là hệ thống không tìm thấy bản ghi hoặc dữ liệu chưa tồn tại, TUYỆT ĐỐI KHÔNG tự sáng tác số liệu thay thế.
                   - ĐỐI VỚI KIẾN THỨC CHUYÊN MÔN & SUY LUẬN TỔNG QUÁT:
                     + Bạn ĐƯỢC PHÉP sử dụng tri thức thú y và huấn luyện đua ngựa tiêu chuẩn để phân tích, đánh giá và đối chiếu các số liệu thu thập được từ công cụ (ví dụ: đánh giá nhịp tim đo được so với ngưỡng sinh lý an toàn).
                     + Bạn ĐƯỢC PHÉP phản hồi trực tiếp các câu chào hỏi xã giao, giải thích quyền hạn vai trò và hướng dẫn các câu hỏi/chức năng mà bạn có thể hỗ trợ mà không cần gọi tool.

                6. QUY TRÌNH TỰ ĐỘNG THU THẬP THAM SỐ (CHAIN OF ACTION / PARAMETER DISCOVERY):
                   - Khi người dùng hỏi phân tích hoặc tra cứu cho một con ngựa mà chưa rõ ID hoặc tên chính xác: AI PHẢI TỰ ĐỘNG gọi các công cụ danh sách/danh bạ trước (`getTrainerHorses`, `getMyAssignedHorses`, `getMyHorses`, `getClubHorsesDirectory`) để lấy thông tin tên và ID ngựa, SAU ĐÓ tự động gọi tiếp công cụ mục tiêu (ví dụ `recommendOptimalRaceDistanceAndTrack`, `getHorseVitalSignsTrend`, `getHorseStallLocation`).
                   - TUYỆT ĐỐI KHÔNG bắt người dùng phải tự chạy tool tra cứu trước và KHÔNG phàn nàn thiếu thông số khi hoàn toàn có thể tự tra cứu qua chuỗi công cụ.

                7. CHUỖI SUY LUẬN CHO GROOM (NGỰA THUỘC CHUỒNG NÀO / VỊ TRÍ CHUỒNG):
                   - Khi nhân viên Groom hỏi về vị trí chuồng hoặc ngựa nào thuộc chuồng nào: Tự động gọi `getMyAssignedHorses` hoặc `getHorseStallLocation`. Kết hợp mã chuồng (`viTriChuong`) và khu vực (`khuVuc`) để trả lời cụ thể, rõ ràng cho Groom.

                8. ĐỐI SOÁT CHỈ SỐ SINH HIỆU CHO BÁC SĨ THÚ Y (VETERINARIAN):
                   - Khi Bác sĩ thú y hỏi về ngựa (ví dụ: ngựa Bella): Tự động đối soát tên ngựa qua `getVetExaminationOverview` hoặc `getQuarantineStatus` để lấy thông tin, sau đó gọi `getHorseVitalSignsTrend`.
                   - Nếu người dùng yêu cầu xem tổng thể hoặc "tất cả bản ghi", gọi `getHorseVitalSignsTrend` không truyền tên ngựa (hoặc truyền 'all'/'tất cả') để nhận toàn bộ bản ghi sinh hiệu gần nhất.
                   - Khi người dùng yêu cầu "vẽ biểu đồ tất cả các bản ghi có số liệu đường sinh hiệu" hoặc trích xuất biểu đồ, hãy đóng gói số liệu thân nhiệt, nhịp tim thành khối mã:
                     ```chart
                     {
                       "type": "bar",
                       "title": "Biểu đồ theo dõi chỉ số sinh hiệu gần nhất",
                       "data": [
                         { "label": "Bella (Thân nhiệt)", "value": 38.2, "color": "#10b981" },
                         { "label": "Bella (Nhịp tim)", "value": 36, "color": "#3b82f6" }
                       ]
                     }
                     ```
                     (Hỗ trợ các type: "bar", "donut", "pie", "line").

                9. ĐỊNH DẠNG TRẢ LỜI BẰNG MARKDOWN RÕ RÀNG VÀ TỔNG HỢP CHUYÊN SÂU:
                   - BẮT BUỘC tổng hợp và giải thích sâu dữ liệu thu thập được từ các công cụ. Sử dụng bảng Markdown cho danh sách nhiều cột (ví dụ: Ca tập, Bài tập, Danh sách ngựa; hoặc Tên kế hoạch, Mục tiêu, Trạng thái, Tiến độ; Sinh hiệu ngựa), bullet points cho các phân tích chi tiết, in đậm các số liệu quan trọng.
                   - CẤM TUYỆT ĐỐI chỉ trả lời câu cụt lủn, chung chung sáo rỗng vô nghĩa như "Tôi đã hoàn thành việc tra cứu dữ liệu theo yêu cầu của bạn." AI luôn phải trình bày dữ liệu chi tiết kèm phân tích nghiệp vụ.
                   - Luôn trả lời bằng tiếng Việt lịch sự, gãy gọn, chuyên nghiệp và súc tích.

                10. BẢNG TRA CỨU ROUTE FRONTEND CHÍNH XÁC & LIÊN KẾT ĐIỀU HƯỚNG (TUYỆT ĐỐI TRÁNH LỖI 404):
                   - Khi tạo action link điều hướng nội bộ Markdown `[Xem chi tiết](route)`, BẮT BUỘC:
                     * ƯU TIÊN SỐ 1: NẾU KẾT QUẢ TOOL CÓ TRƯỜNG `actionUrl` HOẶC `navigationUrl`: LUÔN LUÔN DÙNG CHÍNH XÁC URL ĐÓ ĐỂ TẠO LINK TRỰC TIẾP (DEEP LINK) (Ví dụ: `[Chi tiết đơn](/manager/admissions/123)`, `[Hồ sơ ngựa](/owner/horses/10)`, `[Chi tiết cấp cứu](/veterinarian/urgent/501)`, `[Khám sàng lọc](/veterinarian/admissions?id=401)`).
                     * NẾU CẦN TẠO LINK CHUNG TOÀN HỆ THỐNG: Dùng chính xác bảng route sau:
                       * Head Trainer:
                         - Lịch tập: `/trainer/schedule`
                         - Kế hoạch huấn luyện: `/trainer/plans` (hoặc `/trainer/plans/[id]`)
                         - Dashboard & Cảnh báo rủi ro: `/trainer/dashboard` (LƯU Ý: KHÔNG CÓ ROUTE `/trainer/risk-alerts`, chỉ dùng `/trainer/dashboard`)
                         - Đăng ký đua & Thành tích: `/trainer/racing` (hoặc `/trainer/racing/[id]`)
                         - Thẩm định nhập đàn: `/trainer/admissions` (hoặc `/trainer/admissions/[id]`)
                         - Chuồng trại & Phân công: `/trainer/stable`
                         - Giáo trình khóa học: `/trainer/courses`
                         - Hồ sơ chiến mã: `/trainer/horses` (hoặc `/trainer/horses/[id]`)
                       * Veterinarian:
                         - Đơn khám nhập đàn: `/veterinarian/admissions` (hoặc `/veterinarian/admissions?id=[id]`)
                         - Bệnh án & Theo dõi sức khỏe: `/veterinarian/health` (hoặc `/veterinarian/health?horseId=[id]`)
                         - Chăm sóc định kỳ: `/veterinarian/preventive-care`
                         - Cấp cứu khẩn cấp: `/veterinarian/urgent` (hoặc `/veterinarian/urgent/[scheduleId]`)
                         - Lịch trình khám: `/veterinarian/schedule`
                       * Club Manager:
                         - Chuồng trại: `/manager/stable` (LƯU Ý: KHÔNG CÓ CHỮ S `/manager/stables`, đường dẫn đúng là `/manager/stable`)
                         - Quản lý nhân sự: `/manager/staff`
                         - Quản lý đàn ngựa: `/manager/horses` (hoặc `/manager/horses/[id]`)
                         - Tuyển nhận nhập đàn: `/manager/admissions` (hoặc `/manager/admissions/[id]`)
                         - Nhật ký kiểm toán: `/manager/audit-log`
                         - Phân quyền & Tài khoản: `/manager/access-control`
                       * Groom:
                         - Nhiệm vụ hôm nay: `/groom/tasks`
                         - Báo cáo sự cố: `/groom/incidents` (hoặc tạo mới `/groom/incidents/new`)
                         - Chuồng trại phụ trách: `/groom/stable`
                         - Tuyển nhận: `/groom/admissions` (hoặc `/groom/admissions/[id]`)
                       * Horse Owner:
                         - Danh sách & Chi tiết ngựa: `/owner/horses` (hoặc `/owner/horses/[id]`)
                         - Đơn đăng ký nhập chuồng: `/owner/admissions` (hoặc `/owner/admissions/[id]`) (LƯU Ý: KHÔNG DÙNG `/owner/health` hay `/owner/incidents` vì hệ thống quản lý tập trung tại `/owner/horses` và `/owner/admissions`)

                11. THÔNG TIN LIÊN HỆ GROOM CHO HUẤN LUYỆN VIÊN:
                   - Khi Huấn luyện viên hỏi thông tin Groom làm việc cùng, luôn hiển thị rõ ràng Tên Groom, Số điện thoại liên hệ, Email, Số lượng chuồng phụ trách, Mã chuồng và Danh sách ngựa đang chăm sóc.

                12. PHÂN BIỆT RÕ RÀNG GIỮA SỐ LƯỢNG NGỰA VÀ SỐ LƯỢNG CHUỒNG TRẠI (STALLS):
                   - "Ngựa / Chiến mã" (Horses) và "Chuồng trại" (Stalls) là hai thực thể hoàn toàn khác biệt. Chuồng trại là số lượng ô chuồng vật lý (`totalStalls`), TUYỆT ĐỐI KHÔNG đánh đồng hoặc lấy số lượng chuồng trại để trả lời thay cho số lượng ngựa.
                   - Khi người dùng hỏi "Có bao nhiêu ngựa trong câu lạc bộ / trang trại?" hoặc hỏi quy mô đàn ngựa:
                     + BẮT BUỘC ưu tiên gọi `getClubHealthOverview` (chứa `totalClubHorses`) hoặc xem `totalClubHorses` trong `getClubHorsesDirectory`.
                     + TUYỆT ĐỐI KHÔNG lấy kết quả của `getStableOccupancyMetrics` (vốn là số lượng chuồng) để báo là số lượng ngựa.

                13. THỐNG KÊ ĐƠN ĐĂNG KÝ NHẬP ĐÀN (ADMISSION APPLICATIONS):
                   - Khi người dùng hỏi tổng số đơn đăng ký nhập đàn/nhập chuồng trong hệ thống hoặc tình hình tuyển sinh:
                     + Báo cáo rõ TỔNG SỐ ĐƠN (`totalApplications`, ví dụ 56 đơn) và giải thích chi tiết cơ cấu:
                       * Số đơn đang trong tiến trình xử lý liên phòng ban (`inProgressApplications`, ví dụ 12 đơn): bao gồm Groom kiểm tra sơ bộ, chờ khám thú y, HLV tiếp nhận và chờ Club Manager phê duyệt cuối.
                       * Số đơn đã hoàn tất phê duyệt nhập đàn (`approvedApplications`, ví dụ 42 đơn).
                       * Số đơn từ chối hoặc quá hạn (`rejectedOrExpiredApplications`, ví dụ 2 đơn).
                     + Phân biệt rõ ràng giữa "Tổng số đơn trong hệ thống" và "Số đơn đang chờ riêng Club Manager duyệt" (`pendingManagerReviewCount`). Tuyệt đối không nhầm lẫn số đơn chờ riêng bước của Manager với tổng số đơn của toàn bộ hệ thống.

                14. THỐNG KÊ NHÂN SỰ TOÀN HỆ THỐNG:
                   - Khi người dùng hỏi tổng số nhân sự hoặc số lượng theo từng vị trí (Huấn luyện viên, Bác sĩ thú y, Groom, Quản lý):
                     + BẮT BUỘC gọi `getSystemStaffOverview`.
                     + ĐỌC VÀ BÁO CÁO CHÍNH XÁC các con số đã được hệ thống tính toán sẵn:
                       * Tổng số nhân sự: trường `totalStaff`
                       * Cơ cấu theo vai trò: trường `roleBreakdown` (ví dụ: Huấn luyện viên trưởng, Bác sĩ thú y, Nhân viên chăm sóc (Groom), Quản lý)
                     + TUYỆT ĐỐI KHÔNG tự đếm thủ công các phần tử trong danh sách JSON. TUYỆT ĐỐI KHÔNG làm tròn hoặc bịa đặt số lượng nhân sự (ví dụ: không được báo 30 hay 36 khi dữ liệu trả về ghi rõ 24 Groom).

                15. XẾP HẠNG SỞ TRƯỜNG & ĐIỂM TRUNG BÌNH PHONG ĐỘ (HORSE SPECIALTY & RATING RANKING):
                    - Khi người dùng hỏi về sở trường thi đấu, cự ly sở trường, mặt sân ưa thích hoặc yêu cầu xếp hạng các con ngựa:
                      + Nếu yêu cầu xếp hạng theo ĐIỂM TRUNG BÌNH / PHONG ĐỘ: Gọi `getHorseSpecialtyAnalysis` với `sortBy='rating'` (hoặc `horseName='all'`). ĐỌC VÀ BÁO CÁO các trường: thứ hạng `rank`, tên ngựa `horseName`, điểm trung bình sở trường `averageRating`, điểm cự ly xuất sắc nhất `bestRating` và chi tiết `specialtyBreakdown`.
                      + Nếu yêu cầu xếp hạng theo SỐ LƯỢNG SỞ TRƯỜNG: Gọi `getHorseSpecialtyAnalysis` với `sortBy='count'` (hoặc `horseName='all'`). ĐỌC VÀ BÁO CÁO: thứ hạng `rank`, số lượng sở trường `specialtyCount`, danh sách cự ly `specialties` và điểm trung bình `averageRating`.
                      + Bảng `rankedHorseList` là kết quả ĐÃ ĐƯỢC HỆ THỐNG TÍNH TOÁN VÀ SẮP XẾP SẴN từ cao xuống thấp (dựa trên các bài tập workout hoàn thành đạt chuẩn trong 90 ngày gần nhất).
                      + Báo cáo rõ ràng thứ hạng (Hạng 1, Hạng 2,...), Tên ngựa, Mã đăng ký, Điểm trung bình (`averageRating`), Số lượng sở trường (`specialtyCount`) và Chi tiết cự ly/mặt sân.
                      + TUYỆT ĐỐI KHÔNG từ chối xếp hạng với lý do thiếu thông tin hay không có khả năng so sánh khi người dùng yêu cầu xếp hạng.
                """, userName, role) + "\n" + buildRoleDomainOntology(role);
    }

    private String buildRoleDomainOntology(String role) {
        if (role == null) return "";
        return switch (role.toUpperCase()) {
            case "HEAD_TRAINER", "TRAINER" -> """
                16. SƠ ĐỒ THỰC THỂ NGHIỆP VỤ & QUY TẮC SUY LUẬN CHO HUẤN LUYỆN VIÊN (HEAD_TRAINER DOMAIN ONTOLOGY):
                   - QUAN HỆ THỰC THỂ:
                     + Huấn luyện viên trưởng [Trainer] quản lý Khu vực [Area] gồm nhiều Cụm chuồng [StableLot] và Ô chuồng [Stall].
                     + Mỗi Ô chuồng [Stall] được phân công cho 1 Groom chăm sóc và chứa 1 Chiến mã [Horse].
                     + Chiến mã [Horse] được ghi danh vào Khóa huấn luyện [Training Plan] -> gồm nhiều Buổi tập [Workout Session] -> chia thành các Lượt tập [Training Lot].
                     + Chiến mã có thành tích tốt được Huấn luyện viên đăng ký tham gia các Giải đua [Race Registration].
                   - QUY TẮC SUY LUẬN & CHUỖI CÔNG CỤ (TOOL CHAINING):
                     + Tra cứu lịch tập & ca tập: Dùng `getTrainerSchedule` (đọc `totalSessions` và `sessionList`).
                     + Tra cứu ngựa thuộc quyền huấn luyện: Dùng `getTrainerHorses` -> để lấy danh sách tên và ID ngựa.
                     + Tra cứu kế hoạch huấn luyện: Dùng `getTrainerPlansSummary`.
                     + Đăng ký giải đua: Dùng `getMyHorseRaceRegistrations` (đọc `totalRaceRegistrations`).
                     + Đơn tuyển nhận nhập đàn chờ thẩm định: Dùng `getAssignedAdmissionReviews` (đọc `pendingReviewCount`).
                      + Phân tích & Xếp hạng sở trường: Dùng `getHorseSpecialtyAnalysis` (đọc `rankedHorseList` và `specialtyCount`).
                """;
            case "VETERINARIAN", "VET" -> """
                16. SƠ ĐỒ THỰC THỂ NGHIỆP VỤ & QUY TẮC SUY LUẬN CHO BÁC SĨ THÚ Y (VETERINARIAN DOMAIN ONTOLOGY):
                   - QUAN HỆ THỰC THỂ:
                     + Bác sĩ thú y [Vet] theo dõi sức khỏe và điều trị cho toàn bộ Đàn ngựa [Horses].
                     + Khi ngựa gặp vấn đề sức khỏe: Bác sĩ ghi nhận Hồ sơ bệnh án [HealthRecord], Chỉ số sinh hiệu [HorseHealthMetric] và Lịch chăm sóc/điều trị [CareSchedule].
                     + Ngựa bị chấn thương/truyền nhiễm hoặc mới nhập đàn -> Chuyển vào Khu cách ly [Quarantine Area] và BAN HÀNH LỆNH CẤM TẬP (BLOCKED/RESTRICTED).
                     + Tiếp nhận ngựa mới vào đàn: Bác sĩ thực hiện Khám sàng lọc nhập đàn [Admission Vet Exam].
                   - QUY TẮC SUY LUẬN & CHUỖI CÔNG CỤ (TOOL CHAINING):
                     + Cảnh báo cấp cứu khẩn cấp: Dùng `getUrgentCareAlerts` (đọc `totalUrgentAlerts`).
                     + Đơn khám sàng lọc nhập đàn chờ xử lý: Dùng `getPendingAdmissionExams` (đọc `pendingExamCount`).
                     + Đơn thuốc và phác đồ điều trị đang thực hiện: Dùng `getActivePrescriptions` (đọc `totalPrescriptions`).
                     + Kiểm tra tình trạng cách ly: Dùng `getQuarantineStatus`.
                """;
            case "GROOM", "STABLE_STAFF" -> """
                16. SƠ ĐỒ THỰC THỂ NGHIỆP VỤ & QUY TẮC SUY LUẬN CHO GROOM (GROOM DOMAIN ONTOLOGY):
                   - QUAN HỆ THỰC THỂ:
                     + Nhân viên chăm sóc [Groom] được phân công phụ trách một nhóm Ô chuồng [Stall] thuộc Cụm chuồng [StableLot].
                     + Groom chịu trách nhiệm trực tiếp cho ăn, dọn dẹp, kiểm tra sức khỏe hàng ngày cho các Chiến mã trong ô chuồng của mình.
                     + Nếu phát hiện ngựa có dấu hiệu bất thường (bỏ ăn, cào đất, sốt, đau bụng Colic, trầy xước) -> Groom tạo Báo cáo sự cố [Incident Report].
                     + Khi có xe chở ngựa mới đến cổng -> Groom tiến hành Kiểm tra sơ bộ thể trạng [Admission Groom Review].
                   - QUY TẮC SUY LUẬN & CHUỖI CÔNG CỤ (TOOL CHAINING):
                     + Chuồng và ngựa phụ trách: Dùng `getMyAssignedHorses` (đọc `assignedStallsCount`, `stallsWithHorsesCount`).
                     + Nhiệm vụ trong ngày: Dùng `getDailyGroomTasks` hoặc `getTodayAggregatedTasks`.
                     + Sự cố đã báo cáo: Dùng `getMyReportedIncidents` (đọc `totalReportedIncidents`).
                     + Tra cứu vị trí chuồng ngựa: Dùng `getHorseStallLocation`.
                """;
            case "HORSE_OWNER", "OWNER" -> """
                16. SƠ ĐỒ THỰC THỂ NGHIỆP VỤ & QUY TẮC SUY LUẬN CHO CHỦ SỞ HỮU NGỰA (HORSE_OWNER DOMAIN ONTOLOGY):
                   - QUAN HỆ THỰC THỂ:
                     + Chủ ngựa [Horse Owner] nộp Đơn đăng ký nhập đàn [Admission Application] để đưa ngựa vào câu lạc bộ RTMS.
                     + Sau khi được duyệt, Chiến mã [Horse] lưu trú tại Ô chuồng [Stall] của câu lạc bộ, được chăm sóc bởi Groom và huấn luyện bởi HLV trưởng.
                     + Chủ ngựa có quyền tra cứu minh bạch: Tiến độ tập luyện, Lịch chăm sóc thú y, Lịch thi đấu và Lịch sử sự cố của chính các chiến mã mình sở hữu.
                   - QUY TẮC SUY LUẬN & CHUỖI CÔNG CỤ (TOOL CHAINING):
                     + Ngựa thuộc sở hữu: Dùng `getMyHorses` (đọc `totalOwnedHorses`, `allowedTrainingHorses`, `restrictedHorses`).
                     + Đơn đăng ký nhập chuồng: Dùng `getMyHorseAdmissions` (đọc `totalSubmittedApplications`, `approvedApplicationCount`, `pendingApplicationCount`).
                     + Lịch tập sắp tới: Dùng `getMyHorseUpcomingSchedule` (đọc `totalUpcomingWorkouts`).
                     + Phả hệ / huyết thống: Dùng `getMyHorsePedigreeInfo`.
                """;
            case "CLUB_MANAGER", "ADMIN" -> """
                16. SƠ ĐỒ THỰC THỂ NGHIỆP VỤ & QUY TẮC SUY LUẬN CHO QUẢN LÝ (CLUB_MANAGER DOMAIN ONTOLOGY):
                   - QUAN HỆ THỰC THỂ:
                     + Quản lý câu lạc bộ [Club Manager] giám sát toàn diện mọi mặt hoạt động: Quy mô đàn ngựa, Công suất chuồng trại, Đội ngũ nhân sự, Tiến trình tuyển nhận nhập đàn và Nhật ký kiểm toán bảo mật [Audit Trail].
                   - QUY TẮC SUY LUẬN & CHUỖI CÔNG CỤ (TOOL CHAINING):
                     + Quy mô đàn ngựa & Sức khỏe toàn đàn: BẮT BUỘC dùng `getClubHealthOverview` (đọc `totalClubHorses`) hoặc `getClubHorsesDirectory` (đọc `totalClubHorses`).
                     + Công suất chuồng trại: Dùng `getStableOccupancyMetrics` (đọc `totalStalls`, `occupiedStalls`, `occupancyRate`).
                     + Đội ngũ nhân sự: Dùng `getSystemStaffOverview` (đọc `totalStaff` và `roleBreakdown`).
                     + Tiến trình tuyển nhận nhập đàn: Dùng `getAdmissionRequestsOverview` (đọc `totalApplications`) và `getAdmissionPipelineDetails`.
                     + Nhật ký kiểm toán: Dùng `getRecentAuditLogs` (đọc `totalAuditLogs`).
                      + Xếp hạng sở trường chiến mã toàn đàn: Dùng `getHorseSpecialtyAnalysis` (đọc `rankedHorseList` và `specialtyCount`) hoặc `getClubHorsesDirectory`.
                """;
            default -> "";
        };
    }

    private String formatFallbackToolSummary(List<Map<String, Object>> executedTools) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dưới đây là thông tin chi tiết được ghi nhận từ hệ thống:\n\n");
        for (Map<String, Object> entry : executedTools) {
            String tool = (String) entry.get("tool");
            Object res = entry.get("result");
            if (res instanceof Map<?, ?> map) {
                if ("ERROR".equals(map.get("status"))) {
                    sb.append("⚠️ **Thông báo**: ").append(map.get("error")).append("\n\n");
                    continue;
                }
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    String key = String.valueOf(e.getKey());
                    Object val = e.getValue();
                    if (val instanceof List<?> list && !list.isEmpty()) {
                        sb.append("### ").append(humanizeKey(key)).append("\n");
                        appendListAsMarkdown(sb, list);
                        sb.append("\n");
                    } else if (val != null && !(val instanceof Map)) {
                        sb.append("- **").append(humanizeKey(key)).append("**: ").append(val).append("\n");
                    }
                }
                sb.append("\n");
            } else if (res instanceof List<?> list && !list.isEmpty()) {
                appendListAsMarkdown(sb, list);
                sb.append("\n");
            }
        }
        return sb.toString().trim();
    }

    private void appendListAsMarkdown(StringBuilder sb, List<?> list) {
        if (list.isEmpty()) return;
        Object first = list.get(0);
        if (first instanceof Map<?, ?> firstMap) {
            List<String> headers = new ArrayList<>();
            for (Object k : firstMap.keySet()) {
                headers.add(String.valueOf(k));
            }
            sb.append("| ").append(String.join(" | ", headers.stream().map(this::humanizeKey).toList())).append(" |\n");
            sb.append("| ").append(String.join(" | ", headers.stream().map(h -> "---").toList())).append(" |\n");
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    List<String> values = new ArrayList<>();
                    for (String h : headers) {
                        Object v = m.get(h);
                        values.add(v != null ? String.valueOf(v).replace("\n", " ") : "-");
                    }
                    sb.append("| ").append(String.join(" | ", values)).append(" |\n");
                }
            }
        } else {
            for (Object item : list) {
                sb.append("- ").append(item).append("\n");
            }
        }
    }

    String humanizeKey(String key) {
        if (key == null || key.isBlank()) return "";
        return switch (key) {
            // Ngựa & Phả hệ
            case "horseName", "tenNgua" -> "Tên ngựa";
            case "registrationNumber", "horseId", "maDangKy", "maSoNgua" -> "Mã đăng ký";
            case "breed" -> "Giống ngựa";
            case "sire" -> "Ngựa cha (Sire)";
            case "dam" -> "Ngựa mẹ (Dam)";
            case "origin" -> "Xuất xứ";
            case "pedigreeGeneration" -> "Phả hệ chi tiết";
            case "joinedAt" -> "Thời gian gia nhập";
            case "horseList", "myHorseList", "danhSachNgua" -> "Danh sách ngựa";
            case "caredHorseList", "danhSachNguaDangChamSoc" -> "Ngựa đang chăm sóc";
            case "horseInStall" -> "Ngựa trong chuồng";
            case "totalOwnedHorses", "totalClubHorses", "horseCount", "tongSoNguaQuanLy" -> "Tổng số ngựa";

            // Sinh hiệu & Y tế
            case "weight", "canNang" -> "Cân nặng";
            case "temperature", "thanNhiet" -> "Thân nhiệt";
            case "heartRate", "avgHeartRate", "maxHeartRate", "nhipTim" -> "Nhịp tim";
            case "respiratoryRate", "nhipTho" -> "Nhịp thở";
            case "clinicalNotes", "clinicalObservations", "notes", "nhanXet" -> "Ghi chú lâm sàng";
            case "diagnosis", "latestDiagnosis" -> "Chẩn đoán bệnh lý";
            case "treatmentPlan" -> "Phác đồ điều trị";
            case "medicalRestrictionNotes", "restrictionDetails" -> "Chỉ định y tế";
            case "symptoms", "intakeSymptoms" -> "Triệu chứng";
            case "severity" -> "Mức độ nghiêm trọng";
            case "measurementDate", "ngayDo" -> "Ngày đo";
            case "examDate", "lastExamDate" -> "Ngày khám";
            case "examType" -> "Loại hình khám";
            case "totalUrgentAlerts" -> "Tổng số ca cấp cứu";
            case "alertList" -> "Danh sách ca cấp cứu";
            case "totalPrescriptions" -> "Số đơn thuốc đang dùng";
            case "treatmentList" -> "Danh sách điều trị";
            case "treatmentType" -> "Loại điều trị";
            case "careType" -> "Loại chăm sóc";
            case "purpose" -> "Mục đích chăm sóc";
            case "upcomingCareSchedule" -> "Lịch chăm sóc sắp tới";
            case "inQuarantineArea" -> "Đang ở khu cách ly";
            case "abnormalSigns" -> "Dấu hiệu bất thường";
            case "alertLevel" -> "Mức độ cảnh báo";
            case "immediateActionRequired" -> "Hành động cần làm ngay";
            case "commonConditions" -> "Bệnh lý phổ biến";
            case "recurrenceRisk" -> "Nguy cơ tái phát";
            case "preventiveRecommendations" -> "Khuyến nghị phòng ngừa";
            case "recommendedIntervention" -> "Phác đồ can thiệp đề xuất";
            case "requiredTests" -> "Xét nghiệm cận lâm sàng";
            case "minObservationTime" -> "Thời gian theo dõi tối thiểu";
            case "referenceGuidelines" -> "Tài liệu tham chiếu";
            case "readinessScore" -> "Điểm sẵn sàng";
            case "readinessStatus" -> "Trạng thái sẵn sàng";
            case "vetRecommendations" -> "Khuyến nghị của Bác sĩ thú y";
            case "proposedTrainingRestriction" -> "Giới hạn tập luyện đề xuất";

            // Chuồng trại & Vị trí
            case "stallLocation", "stall", "stallCode", "stallNumber", "viTriChuong" -> "Vị trí ô chuồng";
            case "areaName", "areaCode", "areaType", "khuVuc" -> "Khu vực chuồng trại";
            case "totalStalls" -> "Tổng số ô chuồng";
            case "occupiedStalls" -> "Số chuồng đang sử dụng";
            case "vacantStalls" -> "Số chuồng trống";
            case "quarantineCasesCount" -> "Số ca cách ly";
            case "occupancyRate" -> "Tỷ lệ lấp đầy";
            case "bottleneckForecast" -> "Dự báo điểm nghẽn chuồng";
            case "managementRecommendations" -> "Khuyến nghị quản lý";
            case "stallStatus" -> "Trạng thái chuồng";
            case "stallCodeList", "danhSachMaChuong" -> "Các chuồng phụ trách";

            // Nhân sự & Phân công
            case "fullName" -> "Họ và tên";
            case "email" -> "Email";
            case "phone", "groomPhone", "soDienThoai" -> "Số điện thoại";
            case "role", "roleTitle", "vaiTro" -> "Vai trò";
            case "activeStatus" -> "Trạng thái hoạt động";
            case "specialtyInfo" -> "Thông tin chuyên môn";
            case "totalStaff", "tongSoNhanSu" -> "Tổng số nhân sự";
            case "roleBreakdown", "phanLoaiTheoVaiTro" -> "Phân loại theo vai trò";
            case "staffList", "danhSachNhanSu" -> "Danh sách nhân sự";
            case "assignedGroomName", "groomName", "assignedGroom", "tenGroom" -> "Groom phụ trách";
            case "assignedGroomCount" -> "Số Groom phụ trách";
            case "assignedTrainer", "tenHuanLuyenVien" -> "Huấn luyện viên phụ trách";
            case "assignedVet" -> "Bác sĩ thú y phụ trách";
            case "designatedTrainer" -> "Huấn luyện viên chỉ định";
            case "assignedStallCount", "soChuongPhuTrach" -> "Số chuồng phụ trách";
            case "totalGrooms", "tongSoGroomLamViecCung" -> "Số Groom làm việc cùng";
            case "tasksTodayCount" -> "Số việc hôm nay";
            case "workloadStatus" -> "Trạng thái tải trọng";
            case "groomWorkloadList" -> "Tải trọng nhân sự Groom";
            case "overloadedStaff" -> "Nhân sự quá tải";
            case "proposedStallTransfer" -> "Đề xuất điều chuyển chuồng";

            // Tập luyện & Kế hoạch
            case "trainingDecision", "isTrainingAllowed", "quyetDinhTapLuyen" -> "Quyết định tập luyện";
            case "decisionReason" -> "Lý do quyết định";
            case "totalWorkouts" -> "Tổng số buổi tập";
            case "completedSessions" -> "Số buổi đã hoàn thành";
            case "totalSessions" -> "Tổng số buổi theo giáo trình";
            case "planName" -> "Tên kế hoạch";
            case "courseName" -> "Tên khóa học";
            case "subjectName" -> "Bài tập / Môn học";
            case "targetGoal" -> "Mục tiêu huấn luyện";
            case "progress" -> "Tiến độ";
            case "startDate" -> "Ngày bắt đầu";
            case "endDate" -> "Ngày kết thúc";
            case "startTime" -> "Giờ bắt đầu";
            case "endTime" -> "Giờ kết thúc";
            case "sessionTime" -> "Ca tập (Lot)";
            case "surface" -> "Mặt sân";
            case "preferredSurface" -> "Mặt sân ưa thích";
            case "intensity" -> "Cường độ";
            case "durationMinutes" -> "Thời lượng (phút)";
            case "targetDistance" -> "Cự ly mục tiêu";
            case "actualDistanceMeters" -> "Cự ly thực tế (m)";
            case "topSpeedKmh", "optimalSpeed" -> "Tốc độ tối ưu";
            case "fitnessProgressionRate" -> "Tiến độ thể lực";
            case "racingPotentialEvaluation" -> "Tiềm năng thi đấu";
            case "totalDistance14Days" -> "Tổng cự ly 14 ngày";
            case "workoutCount" -> "Số buổi tập";
            case "highIntensitySessions" -> "Số buổi cường độ cao";
            case "riskAssessment" -> "Đánh giá nguy cơ";
            case "trainingRecommendations" -> "Khuyến nghị huấn luyện";
            case "lastWorkout" -> "Buổi tập gần nhất";
            case "fitnessReview" -> "Nhận xét thể lực";
            case "nextRecommendedWorkout" -> "Bài tập kế tiếp đề xuất";
            case "adjustmentReason" -> "Lý do điều chỉnh";
            case "optimalDistance", "recommendedDistance" -> "Cự ly sở trường";
            case "recommendationReason" -> "Lý do đề xuất";
            case "specialties" -> "Sở trường";
            case "specialtyCount" -> "Số lượng sở trường";
            case "averageRating", "diemTrungBinh" -> "Điểm trung bình sở trường";
            case "bestRating", "diemCaoNhat" -> "Điểm cự ly xuất sắc nhất";
            case "specialtyBreakdown" -> "Chi tiết điểm số từng cự ly";
            case "sortBy" -> "Tiêu chí xếp hạng";
            case "strongDistances" -> "Các cự ly sở trường";
            case "rankedHorseList" -> "Bảng xếp hạng chiến mã";
            case "rank" -> "Thứ hạng";
            case "rankingCriterion" -> "Tiêu chí xếp hạng";
            case "horsesWithSpecialties" -> "Số ngựa có sở trường";

            // Nhiệm vụ Groom & Sự cố
            case "taskType" -> "Loại công việc";
            case "estimatedTime" -> "Thời gian dự kiến";
            case "priorityOrder" -> "Thứ tự ưu tiên";
            case "priorityOrderList" -> "Danh sách ưu tiên";
            case "completionRate" -> "Tỷ lệ hoàn thành";
            case "completedTasksCount" -> "Số việc hoàn thành";
            case "totalTasksCount" -> "Tổng số công việc";
            case "totalWorkItemsCount" -> "Tổng số đầu việc";
            case "biosecurityNotes" -> "Quy tắc an toàn sinh học";
            case "incidentTitle", "title" -> "Tiêu đề sự cố";
            case "incidentStatus" -> "Trạng thái sự cố";
            case "reportedAt", "timestamp" -> "Thời gian ghi nhận";
            case "reporterName" -> "Người báo cáo";
            case "totalReportedIncidents", "totalIncidents" -> "Tổng số sự cố";
            case "incidentList" -> "Danh sách sự cố";

            // Đơn đăng ký nhập đàn
            case "totalApplications", "tongSoDon" -> "Tổng số đơn nhập đàn";
            case "pendingAdmissionsCount" -> "Số đơn chờ xử lý";
            case "pendingManagerReviewCount" -> "Số đơn chờ Quản lý duyệt";
            case "inProgressApplications" -> "Số đơn đang xử lý liên phòng ban";
            case "approvedApplications", "approvedApplicationCount", "fullyApproved" -> "Số đơn đã duyệt nhập đàn";
            case "rejectedOrExpiredApplications", "rejected", "arrivalExpired" -> "Số đơn từ chối / quá hạn";
            case "applicationList" -> "Danh sách đơn đăng ký";
            case "applicationStatus" -> "Trạng thái đơn";
            case "currentStage" -> "Giai đoạn hiện tại";
            case "submittedDate" -> "Ngày nộp đơn";
            case "ownerName" -> "Chủ sở hữu";
            case "groomCheckResult" -> "Kiểm tra sơ bộ Groom";
            case "vetExamResult" -> "Khám sàng lọc Thú y";
            case "managerApprovalResult" -> "Quyết định Quản lý";
            case "pendingExamCount" -> "Số đơn chờ khám thú y";
            case "examList" -> "Danh sách đơn chờ khám";
            case "pendingReviewCount" -> "Số đơn chờ HLV thẩm định";
            case "stageBreakdown" -> "Chi tiết theo giai đoạn";

            // Giải đua
            case "raceName" -> "Tên giải đua";
            case "registrationDate" -> "Ngày đăng ký đua";
            case "totalRaceRegistrations" -> "Tổng số giải đăng ký";
            case "raceList" -> "Danh sách giải đua";

            // Nhật ký kiểm toán
            case "action" -> "Hành động thao tác";
            case "performedBy" -> "Người thực hiện";
            case "changeDetails" -> "Chi tiết thay đổi";
            case "totalAuditLogs" -> "Tổng số nhật ký";
            case "auditLogList" -> "Danh sách nhật ký kiểm toán";

            // Chung & Điều hướng
            case "status", "currentStatus", "horseStatus", "trangThai" -> "Trạng thái";
            case "systemStatus" -> "Tình trạng hệ thống";
            case "description", "detailedDescription" -> "Mô tả";
            case "actionUrl", "navigationUrl" -> "Đường dẫn chi tiết";

            case "chienLuocCuLyVaSanDua" -> "Chiến lược cự ly và sân đua";
            case "dieuChinhBaiTapKeTiep" -> "Điều chỉnh bài tập kế tiếp";
            case "caTrucCapCuu" -> "Ca trực cấp cứu";
            case "suCoChuongTrai48h" -> "Sự cố chuồng trại 48h";
            case "tongQuanSoLuong" -> "Tổng quan số lượng";
            case "chiTietCacDon" -> "Chi tiết các đơn";
            default -> {
                String spaced = key.replaceAll("([a-z])([A-Z])", "$1 $2");
                yield Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
            }
        };
    }

    private Map<String, Object> callAiChatCompletions(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", aiModel);
            body.put("messages", messages);

            if (aiModel != null && (aiModel.contains("luna") || aiModel.startsWith("o1") || aiModel.startsWith("o3"))) {
                body.put("reasoning_effort", "none");
            } else {
                body.put("temperature", 0.2);
            }

            if (tools != null && !tools.isEmpty()) {
                body.put("tools", tools);
                body.put("tool_choice", "auto");
            }

            String jsonPayload = objectMapper.writeValueAsString(body);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(aiBaseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + aiApiKey)
                    .timeout(Duration.ofSeconds(25))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() >= 200 && httpResponse.statusCode() < 300) {
                return objectMapper.readValue(httpResponse.body(), new TypeReference<>() {});
            } else {
                log.error("AI API error HTTP [{}]: {}", httpResponse.statusCode(), httpResponse.body());
                return Map.of();
            }
        } catch (Exception e) {
            log.error("Ngoại lệ khi gọi AI API: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<String, Object> parseArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(argumentsJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Không thể parse arguments JSON: {}", argumentsJson);
            return Collections.emptyMap();
        }
    }
}
