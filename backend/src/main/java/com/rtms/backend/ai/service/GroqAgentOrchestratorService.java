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
 * Điều phối viên chính cho AI Chat Agent sử dụng mô hình LLM từ Groq Cloud API.
 * 1. Tiếp nhận câu hỏi và quản lý ngữ cảnh hội thoại lưu trữ bền vững (Database Persistence).
 * 2. Cung cấp danh mục công cụ động (Dynamic Tool Scoping) theo vai trò.
 * 3. Tiếp nhận Tool Calls từ Groq, ủy quyền cho AiToolExecutionService thực thi.
 * 4. Bơm kết quả Tool Result ngược lại cho Groq để tổng hợp câu trả lời tự nhiên.
 * 5. Lưu vết hội thoại và công cụ thực thi vào DB phục vụ Audit Trail.
 */
@Service
public class GroqAgentOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(GroqAgentOrchestratorService.class);
    private static final int MAX_REACT_ITERATIONS = 4;

    private final String groqBaseUrl;
    private final String groqApiKey;
    private final String groqModel;
    private final DynamicToolRegistryService toolRegistryService;
    private final AiToolExecutionService toolExecutionService;
    private final AiConversationService conversationService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public GroqAgentOrchestratorService(
            @Value("${groq.api.base-url:https://api.groq.com/openai/v1}") String groqBaseUrl,
            @Value("${groq.api.key:}") String groqApiKey,
            @Value("${groq.api.model:qwen/qwen3.8-27b}") String groqModel,
            DynamicToolRegistryService toolRegistryService,
            AiToolExecutionService toolExecutionService,
            @Autowired(required = false) AiConversationService conversationService,
            ObjectMapper objectMapper) {
        this.groqBaseUrl = groqBaseUrl != null && !groqBaseUrl.isBlank() ? groqBaseUrl.trim() : "https://api.groq.com/openai/v1";
        this.groqApiKey = groqApiKey != null ? groqApiKey.trim() : "";
        this.groqModel = groqModel != null && !groqModel.isBlank() ? groqModel.trim() : "qwen/qwen3.8-27b";
        this.toolRegistryService = toolRegistryService;
        this.toolExecutionService = toolExecutionService;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public GroqAgentOrchestratorService(
            String groqBaseUrl,
            String groqApiKey,
            String groqModel,
            DynamicToolRegistryService toolRegistryService,
            AiToolExecutionService toolExecutionService,
            ObjectMapper objectMapper) {
        this(groqBaseUrl, groqApiKey, groqModel, toolRegistryService, toolExecutionService, null, objectMapper);
    }

    @PostConstruct
    public void init() {
        if (this.groqApiKey.isBlank()) {
            log.warn("GroqAgentOrchestratorService: GROQ_API_KEY chưa được cấu hình.");
        } else {
            String masked = this.groqApiKey.length() > 8
                    ? this.groqApiKey.substring(0, 7) + "..." + this.groqApiKey.substring(this.groqApiKey.length() - 4)
                    : "***";
            log.info("GroqAgentOrchestratorService: Đã nạp thành công GROQ_API_KEY [{}] với model [{}]", masked, groqModel);
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

        if (groqApiKey == null || groqApiKey.isBlank()) {
            log.warn("GroqAgentOrchestratorService: GROQ_API_KEY chưa được cấu hình.");
            String fallbackReply = "Chưa cấu hình GROQ_API_KEY. Vui lòng thiết lập biến môi trường GROQ_API_KEY trong Run Configuration của IDE hoặc application.properties để sử dụng trợ lý AI.";
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
            Map<String, Object> groqResponse = callGroqChatCompletions(messages, tools);

            if (groqResponse == null || !groqResponse.containsKey("choices")) {
                finalAnswer = "Xin lỗi, hiện tại không thể kết nối đến máy chủ AI Groq. Vui lòng thử lại sau.";
                break;
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) groqResponse.get("choices");
            if (choices == null || choices.isEmpty()) {
                finalAnswer = "Không nhận được phản hồi hợp lệ từ Groq AI.";
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
                Object result = toolExecutionService.executeTool(toolName, arguments, currentUser);

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
                    log.error("Lỗi khi serialize kết quả tool result cho Groq: {}", e.getMessage());
                    messages.add(Map.of(
                            "role", "tool",
                            "tool_call_id", toolCallId != null ? toolCallId : "",
                            "name", toolName,
                            "content", "{\"error\": \"Lỗi serialize kết quả\"}"
                    ));
                }
            }
        }

        if (finalAnswer == null || finalAnswer.isBlank()) {
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
                6. ĐỊNH DẠNG TRẢ LỜI BẰNG MARKDOWN RÕ RÀNG: Dùng bảng Markdown cho danh sách nhiều cột (ví dụ: Ca tập, Bài tập, Danh sách ngựa; hoặc Tên kế hoạch, Mục tiêu, Trạng thái, Tiến độ), bullet points cho các tiêu chí phân tích, in đậm các thông số quan trọng.
                7. Luôn trả lời bằng tiếng Việt lịch sự, gãy gọn, chuyên nghiệp và súc tích.
                8. CHỦ ĐỘNG ĐIỀU HƯỚNG BẰNG ACTION LINK NỘI BỘ: Khi phản hồi về các đơn chờ duyệt, lịch khám, buổi tập, sự cố hoặc nhân sự, hãy chủ động kèm theo đường dẫn Markdown nội bộ dạng `[Xem chi tiết đơn](/manager/admissions/...)` hoặc `[Xem danh sách](/manager/admissions)` để người dùng có thể nhấp chuột điều hướng ngay lập tức.
                9. TRỰC QUAN HÓA BẰNG BIỂU ĐỒ (CHART GENERATION): Khi người dùng yêu cầu "vẽ biểu đồ", "trực quan hóa", "thống kê tỷ lệ", AI hãy đóng gói dữ liệu số liệu thành khối mã:
                   ```chart
                   {
                     "type": "donut",
                     "title": "Tiêu đề biểu đồ",
                     "data": [
                       { "label": "Nhóm A", "value": 30, "color": "#10B981" },
                       { "label": "Nhóm B", "value": 70, "color": "#9CA3AF" }
                     ]
                   }
                   ```
                   (Hỗ trợ các type: "donut", "bar").
                """, userName, role);
    }

    private Map<String, Object> callGroqChatCompletions(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", groqModel);
            body.put("messages", messages);

            if (groqModel != null && (groqModel.contains("luna") || groqModel.startsWith("o1") || groqModel.startsWith("o3"))) {
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
                    .uri(URI.create(groqBaseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + groqApiKey)
                    .timeout(Duration.ofSeconds(25))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() >= 200 && httpResponse.statusCode() < 300) {
                return objectMapper.readValue(httpResponse.body(), new TypeReference<>() {});
            } else {
                log.error("Groq API error HTTP [{}]: {}", httpResponse.statusCode(), httpResponse.body());
                return Map.of();
            }
        } catch (Exception e) {
            log.error("Ngoại lệ khi gọi Groq API: {}", e.getMessage());
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
