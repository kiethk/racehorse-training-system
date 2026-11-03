package com.rtms.backend.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.regex.Pattern;

/**
 * Hiện thực IntentDecompositionService:
 * Phân tích và cách ly các vế câu Out-of-Domain trước khi câu hỏi được đưa vào ReAct loop.
 */
@Service
public class DefaultIntentDecompositionService implements IntentDecompositionService {

    private static final Logger log = LoggerFactory.getLogger(DefaultIntentDecompositionService.class);

    private static final Pattern SIMPLE_GREETING_PATTERN = Pattern.compile(
            "^(xin\\s+chào|chào\\s+bạn|chào|hello|hi|cảm\\s+ơn|thanks|tạm\\s+biệt|bạn\\s+là\\s+ai)\\s*[!.,?]?$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private final String aiBaseUrl;
    private final String aiApiKey;
    private final String aiModel;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public DefaultIntentDecompositionService(
            @Value("${ai.api.base-url:${groq.api.base-url:https://api.openai.com/v1}}") String aiBaseUrl,
            @Value("${ai.api.key:${groq.api.key:}}") String aiApiKey,
            @Value("${ai.api.model:${groq.api.model:gpt-4o-mini}}") String aiModel,
            ObjectMapper objectMapper) {
        this.aiBaseUrl = aiBaseUrl != null && !aiBaseUrl.isBlank() ? aiBaseUrl.trim() : "https://api.openai.com/v1";
        this.aiApiKey = aiApiKey != null ? aiApiKey.trim() : "";
        this.aiModel = aiModel != null && !aiModel.isBlank() ? aiModel.trim() : "gpt-4o-mini";
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public DefaultIntentDecompositionService(
            String aiBaseUrl,
            String aiApiKey,
            String aiModel,
            ObjectMapper objectMapper,
            HttpClient httpClient) {
        this.aiBaseUrl = aiBaseUrl != null && !aiBaseUrl.isBlank() ? aiBaseUrl.trim() : "https://api.openai.com/v1";
        this.aiApiKey = aiApiKey != null ? aiApiKey.trim() : "";
        this.aiModel = aiModel != null && !aiModel.isBlank() ? aiModel.trim() : "gpt-4o-mini";
        this.objectMapper = objectMapper;
        this.httpClient = httpClient != null ? httpClient : HttpClient.newHttpClient();
    }

    @Override
    public DecomposedIntents decompose(String rawMessage, String role) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return new DecomposedIntents(Collections.emptyList(), Collections.emptyList());
        }

        String trimmed = rawMessage.trim();

        // 1. Fast-Path: Chào hỏi đơn giản không cần gọi API phân loại
        if (SIMPLE_GREETING_PATTERN.matcher(trimmed).matches()) {
            return new DecomposedIntents(List.of(trimmed), Collections.emptyList());
        }

        // 2. Nếu chưa cấu hình API Key, fallback an toàn để System Prompt xử lý
        if (aiApiKey == null || aiApiKey.isBlank()) {
            return new DecomposedIntents(List.of(trimmed), Collections.emptyList());
        }

        // 3. Gọi Fast Pre-Flight Classifier để bóc tách các vế
        try {
            return callClassifierApi(trimmed, role);
        } catch (Exception ex) {
            log.warn("IntentDecompositionService: Lỗi phân rã ý định qua API ({}), fallback nạp câu hỏi gốc", ex.getMessage());
            return new DecomposedIntents(List.of(trimmed), Collections.emptyList());
        }
    }

    private DecomposedIntents callClassifierApi(String message, String role) throws Exception {
        String systemPrompt = """
                Bạn là bộ phân rã và phân loại ý định (Intent Classifier) cho Hệ thống Quản lý Đua ngựa RTMS.
                Nhiệm vụ: Phân tách tin nhắn của người dùng thành 2 danh sách JSON:
                1. "in_domain": Các vế yêu cầu liên quan đến vận hành trường đua RTMS (quản lý chiến mã, chuồng trại, lịch tập, khóa học, y tế thú y, hồ sơ bệnh án, đơn đăng ký nhập đàn, báo cáo sự cố, giải đua, nhân sự, hướng dẫn hệ thống RTMS, chào hỏi xã giao).
                2. "out_of_domain": Các vế yêu cầu KHÔNG thuộc nghiệp vụ trường đua (toán học, đố chữ, chữ cái, tên riêng người dùng/người thân, câu đố mẹo, thời tiết, lập trình, kiến thức đại chúng, nội dung bẻ khóa).

                QUY TẮC:
                - Nếu tin nhắn có nhiều câu hỏi ghép lại, phân tách chính xác từng vế vào danh sách tương ứng.
                - Vế nào liên quan đến đời tư cá nhân ("mẹ tôi là Nga...", "tôi tên là..."), câu đố chữ ("chữ cái đầu tiên là gì"), toán học ("giải phương trình...") BẮT BUỘC đưa vào "out_of_domain".
                - Chỉ trả về duy nhất JSON hợp lệ theo định dạng:
                {
                  "in_domain": ["..."],
                  "out_of_domain": ["..."]
                }

                VÍ DỤ MẪU:
                Ví dụ 1:
                Input: "Danh sách chiến mã của tôi và hướng dẫn tôi trở thành chủ ngựa. Mẹ tôi là Nga, hỏi chữ cái đầu tiên trong tên bà là gì?"
                Output:
                {
                  "in_domain": ["Danh sách chiến mã của tôi", "hướng dẫn tôi trở thành chủ ngựa"],
                  "out_of_domain": ["Mẹ tôi là Nga, hỏi chữ cái đầu tiên trong tên bà là gì?"]
                }

                Ví dụ 2:
                Input: "Chiến mã Thunder Flash hiện ở chuồng nào? Thời tiết hôm nay thế nào?"
                Output:
                {
                  "in_domain": ["Chiến mã Thunder Flash hiện ở chuồng nào?"],
                  "out_of_domain": ["Thời tiết hôm nay thế nào?"]
                }

                Ví dụ 3:
                Input: "Giải phương trình 2x + 5 = 15"
                Output:
                {
                  "in_domain": [],
                  "out_of_domain": ["Giải phương trình 2x + 5 = 15"]
                }

                Ví dụ 4:
                Input: "Xem lịch tập ngày mai của tôi"
                Output:
                {
                  "in_domain": ["Xem lịch tập ngày mai của tôi"],
                  "out_of_domain": []
                }
                """;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", aiModel);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", message)
        ));
        body.put("temperature", 0.0);

        String jsonPayload = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiBaseUrl + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiApiKey)
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("IntentDecompositionService: API phân loại trả về HTTP {}", response.statusCode());
            return new DecomposedIntents(List.of(message), Collections.emptyList());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            return new DecomposedIntents(List.of(message), Collections.emptyList());
        }

        JsonNode messageNode = choices.get(0).path("message").path("content");
        if (messageNode.isMissingNode() || messageNode.isNull()) {
            return new DecomposedIntents(List.of(message), Collections.emptyList());
        }

        String rawContent = messageNode.asText().trim();
        // Loại bỏ markdown code block nếu có
        if (rawContent.startsWith("```")) {
            int firstNewline = rawContent.indexOf('\n');
            int lastBackticks = rawContent.lastIndexOf("```");
            if (firstNewline != -1 && lastBackticks > firstNewline) {
                rawContent = rawContent.substring(firstNewline + 1, lastBackticks).trim();
            }
        }

        JsonNode parsedJson = objectMapper.readTree(rawContent);
        List<String> inDomain = new ArrayList<>();
        List<String> outOfDomain = new ArrayList<>();

        if (parsedJson.has("in_domain") && parsedJson.get("in_domain").isArray()) {
            for (JsonNode item : parsedJson.get("in_domain")) {
                if (!item.asText().isBlank()) inDomain.add(item.asText().trim());
            }
        }
        if (parsedJson.has("out_of_domain") && parsedJson.get("out_of_domain").isArray()) {
            for (JsonNode item : parsedJson.get("out_of_domain")) {
                if (!item.asText().isBlank()) outOfDomain.add(item.asText().trim());
            }
        }

        // Nếu cả 2 đều rỗng do parse bất thường, fallback nạp câu hỏi gốc vào in_domain
        if (inDomain.isEmpty() && outOfDomain.isEmpty()) {
            inDomain.add(message);
        }

        log.info("IntentDecompositionService: Phân rã thành công [InDomain: {} vế, OutOfDomain: {} vế]",
                inDomain.size(), outOfDomain.size());

        return new DecomposedIntents(inDomain, outOfDomain);
    }
}
