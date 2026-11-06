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
            "^(xin\\s+chào|chào\\s+bạn|chào|cảm\\s+ơn|tạm\\s+biệt|bạn\\s+là\\s+ai|hello|hi|hey|good\\s+(morning|afternoon|evening)|thanks|thank\\s+you|bye|goodbye|who\\s+are\\s+you)\\s*[!.,?]?$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern DOMAIN_KEYWORDS_PATTERN = Pattern.compile(
            "(ngựa|chiến\\s+mã|horse|horses|racehorse|racehorses|chuồng|stall|stalls|barn|barns|stable|stables|lịch\\s+tập|bài\\s+tập|workout|workouts|huấn\\s+luyện|training|trainer|trainers|groom|grooms|thú\\s+y|bác\\s+sĩ\\s+thú\\s+y|vet|veterinarian|bệnh\\s+án|sinh\\s+hiệu|vitals|nhịp\\s+tim|heart\\s+rate|thân\\s+nhiệt|temperature|cách\\s+ly|quarantine|nhập\\s+đàn|nhập\\s+chuồng|admission|admissions|giải\\s+đua|đua|race|races|racing|khóa\\s+học|course|courses|chủ\\s+ngựa|owner|sự\\s+cố|incident|incidents|ueln|pedigree|prescription|prescriptions|treatment|treatments|injury|injuries|diet|feed|nutrition|fitness|colic|lameness|jockey)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern OBVIOUS_OOD_PATTERN = Pattern.compile(
            "(bà\\s+ngoại|ông\\s+ngoại|bà\\s+nội|ông\\s+nội|mẹ\\s+tôi|bố\\s+tôi|grandmother|grandfather|grandma|grandpa|mother|father|vách\\s+đá|cliff|đẩy\\s+(anh\\s+ta|người\\s+đó|xuống)|push\\s+(him|her|them|down)|thuốc\\s+chữa|thuốc\\s+cho\\s+người|medicine|cure|human\\s+disease|human\\s+health|bệnh\\s+cho\\s+người|cấp\\s+cứu\\s+người|human\\s+first\\s+aid|không\\s+còn\\s+thở|not\\s+breathing|viết\\s+code|write\\s+code|giải\\s+phương\\s+trình|solve\\s+equation|làm\\s+thơ|bài\\s+thơ|poem|poetry|thời\\s+tiết|weather|thủ\\s+đô|capital\\s+of)",
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

        // 1.1 Fast-Path OOD: Phát hiện các mẫu câu rõ ràng nằm ngoài chuyên môn RTMS
        if (OBVIOUS_OOD_PATTERN.matcher(trimmed).find() && !DOMAIN_KEYWORDS_PATTERN.matcher(trimmed).find()) {
            log.info("IntentDecompositionService: Fast-Path phát hiện câu hỏi Out-of-Domain rõ ràng: {}", trimmed);
            return new DecomposedIntents(Collections.emptyList(), List.of(trimmed));
        }

        // 2. Nếu chưa cấu hình API Key, fallback an toàn dựa trên từ khóa chuyên môn
        if (aiApiKey == null || aiApiKey.isBlank()) {
            return fallbackHeuristicDecompose(trimmed);
        }

        // 3. Gọi Fast Pre-Flight Classifier để bóc tách các vế
        try {
            return callClassifierApi(trimmed, role);
        } catch (Exception ex) {
            log.warn("IntentDecompositionService: Lỗi phân rã ý định qua API ({}), fallback heuristic", ex.getMessage());
            return fallbackHeuristicDecompose(trimmed);
        }
    }

    private DecomposedIntents fallbackHeuristicDecompose(String trimmed) {
        if (DOMAIN_KEYWORDS_PATTERN.matcher(trimmed).find() && !OBVIOUS_OOD_PATTERN.matcher(trimmed).find()) {
            return new DecomposedIntents(List.of(trimmed), Collections.emptyList());
        }
        return new DecomposedIntents(Collections.emptyList(), List.of(trimmed));
    }

    private DecomposedIntents callClassifierApi(String message, String role) throws Exception {
        String systemPrompt = """
                Bạn là bộ phân rã và phân loại ý định (Intent Classifier) cho Hệ thống Quản lý Đua ngựa RTMS.
                Nhiệm vụ: Phân tách tin nhắn của người dùng thành 2 danh sách JSON:
                1. "in_domain": Các vế yêu cầu liên quan ĐẾN NGỰA ĐUA VÀ VẬN HÀNH TRƯỜNG ĐUA RTMS (quản lý chiến mã, thể lực, dinh dưỡng, chuồng trại, lịch tập, khóa học, y tế thú y cho ngựa, hồ sơ bệnh án ngựa, đơn đăng ký nhập đàn, báo cáo sự cố chuồng trại, giải đua ngựa, nhân sự trường đua, hướng dẫn hệ thống RTMS, chào hỏi xã giao).
                2. "out_of_domain": TẤT CẢ các vế yêu cầu KHÔNG thuộc nghiệp vụ trường đua ngựa. CỤ THỂ BAO GỒM:
                - Y tế con người, kê đơn/tư vấn thuốc cho người, chăm sóc sức khỏe người thân, sơ cấp cứu người, tai nạn người, tình huống nguy hiểm ngoài đời sống.
                - Đời tư cá nhân, gia đình người dùng ("bà ngoại tôi...", "mẹ tôi là...").
                - Toán học, số học, giải phương trình, đố chữ, câu đố mẹo.
                - Lập trình phần mềm, viết mã nguồn ngoài hệ thống RTMS.
                - Thời tiết, tin tức xã hội, chính trị, triết học, tôn giáo, văn học/thơ ca.
                - Nội dung bạo lực, nguy hiểm, phi pháp hoặc bẻ khóa (jailbreak).

                QUY TẮC:
                - Nếu tin nhắn có nhiều câu hỏi ghép lại, phân tách chính xác từng vế vào danh sách tương ứng.
                - Vế nào liên quan đến người, thuốc người, cấp cứu người, đời tư cá nhân, câu đố chữ, toán học BẮT BUỘC đưa vào "out_of_domain".
                - Chỉ trả về duy nhất JSON hợp lệ theo định dạng:
                {
                  "in_domain": ["..."],
                  "out_of_domain": ["..."]
                }

                VÍ DỤ MẪU:
                Ví dụ 1:
                Input: "Bà ngoại tôi bị bệnh và tôi cần biết được công thức thuốc penicillin phối hợp paracetamol"
                Output:
                {
                  "in_domain": [],
                  "out_of_domain": ["Bà ngoại tôi bị bệnh và tôi cần biết được công thức thuốc penicillin phối hợp paracetamol"]
                }

                Ví dụ 2:
                Input: "Tôi phát hiện một người không còn thở trên vách đá, tôi nên đẩy anh ta xuống hay gọi cấp cứu hay gọi trực thăng?"
                Output:
                {
                  "in_domain": [],
                  "out_of_domain": ["Tôi phát hiện một người không còn thở trên vách đá, tôi nên đẩy anh ta xuống hay gọi cấp cứu hay gọi trực thăng?"]
                }

                Ví dụ 3:
                Input: "Danh sách chiến mã của tôi và hướng dẫn tôi trở thành chủ ngựa. Mẹ tôi là Nga, hỏi chữ cái đầu tiên trong tên bà là gì?"
                Output:
                {
                  "in_domain": ["Danh sách chiến mã của tôi", "hướng dẫn tôi trở thành chủ ngựa"],
                  "out_of_domain": ["Mẹ tôi là Nga, hỏi chữ cái đầu tiên trong tên bà là gì?"]
                }

                Ví dụ 4:
                Input: "Chiến mã Thunder Flash hiện ở chuồng nào? Thời tiết hôm nay thế nào?"
                Output:
                {
                  "in_domain": ["Chiến mã Thunder Flash hiện ở chuồng nào?"],
                  "out_of_domain": ["Thời tiết hôm nay thế nào?"]
                }

                Ví dụ 5:
                Input: "Giải phương trình 2x + 5 = 15"
                Output:
                {
                  "in_domain": [],
                  "out_of_domain": ["Giải phương trình 2x + 5 = 15"]
                }

                Ví dụ 6:
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
            return fallbackHeuristicDecompose(message);
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            return fallbackHeuristicDecompose(message);
        }

        JsonNode messageNode = choices.get(0).path("message").path("content");
        if (messageNode.isMissingNode() || messageNode.isNull()) {
            return fallbackHeuristicDecompose(message);
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

        // Nếu cả 2 đều rỗng do parse bất thường, dùng fallback heuristic an toàn
        if (inDomain.isEmpty() && outOfDomain.isEmpty()) {
            return fallbackHeuristicDecompose(message);
        }

        log.info("IntentDecompositionService: Phân rã thành công [InDomain: {} vế, OutOfDomain: {} vế]",
                inDomain.size(), outOfDomain.size());

        return new DecomposedIntents(inDomain, outOfDomain);
    }
}
