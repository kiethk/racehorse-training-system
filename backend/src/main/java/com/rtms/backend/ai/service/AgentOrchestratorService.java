package com.rtms.backend.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.ai.conversation.dto.AiChatMessageDetailDto;
import com.rtms.backend.ai.conversation.entity.AiConversation;
import com.rtms.backend.ai.conversation.service.AiConversationService;
import com.rtms.backend.ai.dto.ActionProposalDto;
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
        List<ActionProposalDto> pendingProposals = new ArrayList<>();

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

        return new AiChatResponseDto(finalAnswer, conversationId, executedTools, pendingProposals);
    }

    private String buildSystemPrompt(AuthenticatedUser user) {
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
                5. KHI TRẢ LỜI CẦN DỮ LIỆU THỰC TẾ: Hãy chủ động gọi công cụ (tool_calls) được cung cấp theo đúng vai trò.

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

                10. BẢNG TRA CỨU ROUTE FRONTEND CHÍNH XÁC (TUYỆT ĐỐI TRÁNH LỖI 404):
                   - Khi tạo action link điều hướng nội bộ Markdown `[Xem chi tiết](route)`, BẮT BUỘC dùng đúng route frontend sau:
                     * Head Trainer:
                       - Lịch tập: `/trainer/schedule`
                       - Kế hoạch huấn luyện: `/trainer/plans`
                       - Dashboard & Cảnh báo rủi ro: `/trainer/dashboard` (LƯU Ý: KHÔNG CÓ ROUTE `/trainer/risk-alerts`, chỉ dùng `/trainer/dashboard`)
                       - Đăng ký đua & Thành tích: `/trainer/racing`
                       - Thẩm định nhập đàn: `/trainer/admissions`
                       - Chuồng trại & Phân công: `/trainer/stable`
                       - Giáo trình khóa học: `/trainer/courses`
                     * Veterinarian:
                       - Đơn khám nhập đàn: `/veterinarian/admissions`
                       - Chăm sóc định kỳ: `/veterinarian/preventive-care`
                       - Cấp cứu khẩn cấp: `/veterinarian/urgent` (LƯU Ý: KHÔNG DÙNG `/vet/quarantine` hay `/vet/urgent-care`, luôn dùng `/veterinarian/admissions`, `/veterinarian/preventive-care`, `/veterinarian/urgent`)
                     * Club Manager:
                       - Chuồng trại: `/manager/stable` (LƯU Ý: KHÔNG CÓ CHỮ S `/manager/stables`, đường dẫn đúng là `/manager/stable`)
                       - Quản lý nhân sự: `/manager/staff`
                       - Quản lý đàn ngựa: `/manager/horses`
                       - Tuyển nhận nhập đàn: `/manager/admissions`
                       - Nhật ký kiểm toán: `/manager/audit-log`
                       - Phân quyền & Tài khoản: `/manager/access-control`
                     * Groom:
                       - Nhiệm vụ hôm nay: `/groom/tasks`
                       - Báo cáo sự cố: `/groom/incidents`
                       - Chuồng trại phụ trách: `/groom/stable`
                       - Tuyển nhận: `/groom/admissions`
                     * Horse Owner:
                       - Danh sách ngựa sở hữu: `/owner/horses`
                       - Chi tiết ngựa & Sức khỏe / Sự cố: `/owner/horses`
                       - Đơn đăng ký nhập chuồng: `/owner/admissions` (LƯU Ý: KHÔNG DÙNG `/owner/health` hay `/owner/incidents` vì hệ thống quản lý tập trung tại `/owner/horses` và `/owner/admissions`)

                11. THÔNG TIN LIÊN HỆ GROOM CHO HUẤN LUYỆN VIÊN:
                   - Khi Huấn luyện viên hỏi thông tin Groom làm việc cùng, luôn hiển thị rõ ràng Tên Groom, Số điện thoại liên hệ, Email, Số lượng chuồng phụ trách, Mã chuồng và Danh sách ngựa đang chăm sóc.
                """, userName, role);
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

    private String humanizeKey(String key) {
        if (key == null) return "";
        return switch (key) {
            case "tenNgua" -> "Tên ngựa";
            case "maDangKy", "maSoNgua" -> "Mã đăng ký";
            case "ngayDo" -> "Ngày đo";
            case "canNang" -> "Cân nặng";
            case "thanNhiet" -> "Thân nhiệt";
            case "nhipTim" -> "Nhịp tim";
            case "nhipTho" -> "Nhịp thở";
            case "nhanXet" -> "Nhận xét";
            case "viTriChuong" -> "Vị trí chuồng";
            case "khuVuc" -> "Khu vực";
            case "tenGroom" -> "Groom phụ trách";
            case "soDienThoai" -> "Số điện thoại";
            case "email" -> "Email";
            case "soChuongPhuTrach" -> "Số chuồng phụ trách";
            case "danhSachMaChuong" -> "Các chuồng phụ trách";
            case "danhSachNguaDangChamSoc" -> "Ngựa đang chăm sóc";
            case "danhSachNgua" -> "Danh sách ngựa";
            case "tongSoNguaQuanLy" -> "Tổng số ngựa";
            case "tongSoGroomLamViecCung" -> "Số Groom làm việc cùng";
            case "tenHuanLuyenVien" -> "Huấn luyện viên";
            case "trangThai" -> "Trạng thái";
            case "quyetDinhTapLuyen" -> "Quyết định tập luyện";
            case "chienLuocCuLyVaSanDua" -> "Chiến lược cự ly và sân đua";
            case "dieuChinhBaiTapKeTiep" -> "Điều chỉnh bài tập kế tiếp";
            case "caTrucCapCuu" -> "Ca trực cấp cứu";
            case "suCoChuongTrai48h" -> "Sự cố chuồng trại 48h";
            case "tongQuanSoLuong" -> "Tổng quan số lượng";
            case "chiTietCacDon" -> "Chi tiết các đơn";
            default -> key;
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
