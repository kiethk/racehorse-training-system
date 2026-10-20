package com.rtms.backend.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Tác tử AI Điều phối (Agent Orchestrator) tích hợp Groq LLM (llama-3.3-70b-versatile).
 *
 * Thực hiện vòng lặp suy luận ReAct và gọi công cụ (Function Calling):
 * 1. Nạp System Prompt theo vai trò người dùng (Dynamic System Prompt).
 * 2. Cung cấp danh mục công cụ động (Dynamic Tool Scoping) theo vai trò.
 * 3. Tiếp nhận Tool Calls từ Groq, ủy quyền cho AiToolExecutionService thực thi.
 * 4. Bơm kết quả Tool Result ngược lại cho Groq để tổng hợp câu trả lời tự nhiên.
 * 5. Nhận diện các thẻ đề xuất Two-Phase HITL trả về cho UI.
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
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GroqAgentOrchestratorService(
            @Value("${groq.api.base-url:https://api.groq.com/openai/v1}") String groqBaseUrl,
            @Value("${groq.api.key:}") String groqApiKey,
            @Value("${groq.api.model:llama-3.3-70b-versatile}") String groqModel,
            DynamicToolRegistryService toolRegistryService,
            AiToolExecutionService toolExecutionService,
            ObjectMapper objectMapper) {
        this.groqBaseUrl = groqBaseUrl != null && !groqBaseUrl.isBlank() ? groqBaseUrl.trim() : "https://api.groq.com/openai/v1";
        this.groqApiKey = resolveApiKey(groqApiKey);
        this.groqModel = groqModel != null && !groqModel.isBlank() ? groqModel.trim() : "llama-3.3-70b-versatile";
        this.toolRegistryService = toolRegistryService;
        this.toolExecutionService = toolExecutionService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    @PostConstruct
    public void init() {
        if (this.groqApiKey.isBlank()) {
            log.warn("GroqAgentOrchestratorService: GROQ_API_KEY chưa được cấu hình trong .env hoặc application.properties.");
        } else {
            String masked = this.groqApiKey.length() > 8
                    ? this.groqApiKey.substring(0, 7) + "..." + this.groqApiKey.substring(this.groqApiKey.length() - 4)
                    : "***";
            log.info("GroqAgentOrchestratorService: Đã nạp thành công GROQ_API_KEY [{}] với model [{}]", masked, groqModel);
        }
    }

    /**
     * Thuật toán nạp API Key đa tầng chống lỗi môi trường trong IDE:
     * Tầng 1: Spring @Value
     * Tầng 2: System Property
     * Tầng 3: OS Environment Variable
     * Tầng 4: Trực tiếp quét parse file .env từ đĩa cứng
     */
    private static String resolveApiKey(String injectedKey) {
        if (injectedKey != null && !injectedKey.isBlank()) {
            return injectedKey.trim();
        }
        String sysProp = System.getProperty("GROQ_API_KEY");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }
        String envVal = System.getenv("GROQ_API_KEY");
        if (envVal != null && !envVal.isBlank()) {
            return envVal.trim();
        }

        File[] candidatePaths = new File[] {
                new File(".env"),
                new File("../.env"),
                new File("backend/.env"),
                new File("../../.env")
        };
        for (File file : candidatePaths) {
            if (file.exists() && file.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("GROQ_API_KEY=")) {
                            String val = line.substring("GROQ_API_KEY=".length()).trim();
                            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                                if (val.length() >= 2) val = val.substring(1, val.length() - 1);
                            }
                            if (!val.isBlank()) {
                                return val;
                            }
                        }
                    }
                } catch (IOException ignored) {}
            }
        }
        return "";
    }

    /**
     * Xử lý hội thoại chat với người dùng.
     */
    public AiChatResponseDto processChat(AiChatRequestDto request, AuthenticatedUser currentUser) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            log.warn("GroqAgentOrchestratorService: GROQ_API_KEY chưa được cấu hình.");
            return new AiChatResponseDto(
                    "Chưa cấu hình GROQ_API_KEY. Vui lòng thiết lập biến môi trường GROQ_API_KEY trong file .env hoặc application.properties để sử dụng trợ lý AI.",
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        List<Map<String, Object>> executedTools = new ArrayList<>();
        List<ActionProposalDto> pendingProposals = new ArrayList<>();

        // 1. Xây dựng danh sách tin nhắn hội thoại
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", buildSystemPrompt(currentUser)
        ));

        if (request.conversationHistory() != null) {
            for (AiChatMessageDto msg : request.conversationHistory()) {
                if (msg != null && msg.content() != null && !msg.content().isBlank()) {
                    messages.add(Map.of(
                            "role", msg.role() != null ? msg.role() : "user",
                            "content", msg.content()
                    ));
                }
            }
        }

        messages.add(Map.of(
                "role", "user",
                "content", request.message()
        ));

        // 2. Lấy danh sách công cụ theo vai trò người dùng (Dynamic Tool Scoping)
        List<Map<String, Object>> tools = toolRegistryService.getFunctionSpecsForRole(currentUser.getRole());

        // 3. Vòng lặp suy luận và gọi công cụ (ReAct Loop)
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

            // Đưa tin nhắn của assistant vào hội thoại để giữ ngữ cảnh ReAct
            messages.add(assistantMessage);

            // Kiểm tra xem assistant có yêu cầu gọi công cụ (tool_calls) hay không
            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) assistantMessage.get("tool_calls");
            if (toolCalls == null || toolCalls.isEmpty()) {
                // LLM đã hoàn tất suy luận và trả lời tự nhiên
                finalAnswer = (String) assistantMessage.getOrDefault("content", "");
                break;
            }

            // Thực thi từng tool_call
            for (Map<String, Object> toolCall : toolCalls) {
                String toolCallId = (String) toolCall.get("id");
                Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                String toolName = (String) function.get("name");
                String argsString = (String) function.get("arguments");

                Map<String, Object> arguments = parseArguments(argsString);
                log.info("Agent ReAct: Thực thi tool [{}] với args {}", toolName, arguments);

                Object executionResult = toolExecutionService.executeTool(toolName, arguments, currentUser);

                // Nếu kết quả trả về là một đề xuất HITL thì thu thập lại cho UI
                if (executionResult instanceof ActionProposalDto proposal) {
                    pendingProposals.add(proposal);
                }

                // Ghi nhận tool đã thực thi
                executedTools.add(Map.of(
                        "tool", toolName,
                        "arguments", arguments,
                        "result", executionResult != null ? executionResult : "DONE"
                ));

                // Bơm kết quả tool_result ngược lại cho Groq dưới dạng role = tool
                try {
                    String resultJson = objectMapper.writeValueAsString(executionResult);
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
            if (!pendingProposals.isEmpty()) {
                finalAnswer = String.format("Tôi đã tạo %d đề xuất hành động theo yêu cầu của bạn. Vui lòng xem và phê duyệt trên màn hình.", pendingProposals.size());
            } else {
                finalAnswer = "Tôi đã xử lý xong các yêu cầu tra cứu của bạn.";
            }
        }

        return new AiChatResponseDto(finalAnswer, executedTools, pendingProposals);
    }

    private String buildSystemPrompt(AuthenticatedUser user) {
        String role = user != null && user.getRole() != null ? user.getRole() : "ANONYMOUS";
        Long userId = user != null ? user.getUserId() : null;

        return String.format("""
                Bạn là Trợ lý AI Thông minh RTMS (Racehorse Training Management System) chuyên nghiệp cho trường đua ngựa.
                Thông tin người dùng hiện tại: User ID #%s, Vai trò: %s.
                
                QUY TẮC BẮT BUỘC:
                1. Hãy hỗ trợ người dùng theo đúng chuyên môn và thẩm quyền của vai trò:
                   - Với HORSE_OWNER (Chủ ngựa): Tuyệt đối KHÔNG có quyền thay đổi dữ liệu (100%% Read-Only). Chỉ giải đáp và tra cứu thông tin ngựa của họ.
                   - Với HEAD_TRAINER: Hỗ trợ xem thông số bài tập, biểu đồ thể lực và đề xuất sửa đổi thông số qua công cụ proposeEditWorkoutMetric (HITL).
                   - Với VETERINARIAN: Hỗ trợ tra cứu bệnh án y khoa qua RAG Retrieval (searchClinicalKnowledge) và đề xuất hạn chế y tế (HITL).
                   - Với GROOM: Hỗ trợ xem nhiệm vụ chuồng và vị trí ngựa.
                   - Với CLUB_MANAGER: Cung cấp số liệu thống kê chuồng trại và quản lý chung.
                2. Khi người dùng hỏi thông tin cần dữ liệu thật (ngựa, bài tập, bệnh án), hãy chủ động GỌI CÔNG CỤ (tool_calls) được cung cấp. Tuyệt đối KHÔNG tự bịa đặt ID hoặc số liệu.
                3. Khi thực hiện thay đổi dữ liệu nhạy cảm (Tier 3 Mutation), bạn chỉ được phép tạo đề xuất (proposal) qua công cụ propose... và thông báo cho người dùng biết cần bấm Xác nhận trên giao diện.
                4. Luôn trả lời bằng tiếng Việt lịch sự, gãy gọn, chuyên nghiệp và súc tích.
                """, userId, role);
    }

    private Map<String, Object> callGroqChatCompletions(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", groqModel);
            body.put("messages", messages);
            body.put("temperature", 0.2);

            if (tools != null && !tools.isEmpty()) {
                body.put("tools", tools);
                body.put("tool_choice", "auto");
            }

            String jsonPayload = objectMapper.writeValueAsString(body);
            String endpoint = groqBaseUrl.endsWith("/")
                    ? groqBaseUrl + "chat/completions"
                    : groqBaseUrl + "/chat/completions";

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(45))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), new TypeReference<>() {});
            } else {
                log.error("Groq API trả về mã lỗi {}: {}", response.statusCode(), response.body());
                return null;
            }

        } catch (Exception e) {
            log.error("Lỗi khi gọi Groq API: {}", e.getMessage(), e);
            return null;
        }
    }

    private Map<String, Object> parseArguments(String argsString) {
        if (argsString == null || argsString.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(argsString, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Không thể parse arguments JSON: {}", argsString);
            return Collections.emptyMap();
        }
    }
}
