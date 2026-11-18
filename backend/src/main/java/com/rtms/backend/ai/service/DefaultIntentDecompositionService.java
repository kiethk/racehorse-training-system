package com.rtms.backend.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.ai.dto.AiChatMessageDto;
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
 * Implements IntentDecompositionService:
 * analyzes and isolates Out-of-Domain clauses before the question enters the ReAct loop.
 */
@Service
public class DefaultIntentDecompositionService implements IntentDecompositionService {

    private static final Logger log = LoggerFactory.getLogger(DefaultIntentDecompositionService.class);

    private static final Pattern SIMPLE_GREETING_PATTERN = Pattern.compile(
            "^\\s*(?:"
            + "xin\\s+chào|chào\\s+bạn|chào|chào\\s+bot|chào\\s+em|chào\\s+anh|chào\\s+chị|chào\\s+ad|chào\\s+admin"
            + "|chào\\s+buổi\\s+(?:sáng|chiều|tối)"
            + "|cảm\\s+ơn(?:\\s+(?:bạn|nhiều|nhé|nha|ạ))?|cám\\s+ơn(?:\\s+(?:bạn|nhiều|nhé|nha|ạ))?"
            + "|tạm\\s+biệt|bye(\\s+bạn)?"
            + "|bạn\\s+là\\s+ai|bạn\\s+có\\s+thể\\s+(?:làm|giúp)(?:\\s+gì)?(?:\\s+(?:cho\\s+tôi|được))?|bạn\\s+(?:làm|giúp)\\s+được\\s+gì(?:\\s+cho\\s+tôi)?"
            + "|hướng\\s+dẫn(?:\\s+tôi|\\s+sử\\s+dụng)?|giúp\\s+tôi(?:\\s+với)?"
            + "|hello(\\s+there)?|hi|hey|good\\s+(morning|afternoon|evening)|thanks|thank\\s+you(?:\\s+very\\s+much)?"
            + "|bye|goodbye|who\\s+are\\s+you|what\\s+can\\s+you\\s+do|how\\s+can\\s+you\\s+help"
            + ")(?:\\s+(?:nhé|nha|ạ|nhá|bạn\\s+nhé|bạn\\s+nha))?\\s*[!.,?]?$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern CONVERSATIONAL_DIRECTIVE_PATTERN = Pattern.compile(
            "^\\s*(?:(?:xin\\s+chào|chào\\s+bạn|chào|hello|hi|hey)[,.]?\\s+)??(?:"
            + "(?:hãy\\s+)?(?:trả\\s+lời|nói|dùng|viết|dịch|chuyển|đổi|hỗ\\s+trợ|giao\\s+tiếp|sử\\s+dụng|chat)(?:\\s+(?:bằng|sang|với|bằng\\s+cả))?\\s+tiếng\\s+(?:việt|anh|kinh)(?:\\s+(?:nhé|đi|giúp|hộ|nha|ạ|với|nhá|được\\s+không|cho\\s+tôi|giúp\\s+tôi))?"
            + "|(?:bằng|sang)\\s+tiếng\\s+(?:việt|anh)(?:\\s+(?:nhé|đi|giúp|hộ|nha|ạ|với))?"
            + "|tiếng\\s+(?:việt|anh)(?:\\s+(?:đi|nhé|nha|ạ|thôi|nhá|giúp|hộ))?"
            + "|(?:please\\s+)?(?:can\\s+you\\s+)?(?:reply|answer|speak|respond|write|switch|translate|talk|use)(?:\\s+(?:in|to|with|into))?\\s+(?:vietnamese|english)(?:\\s+please)?"
            + "|(?:in|to|into)\\s+(?:vietnamese|english)(?:\\s+please)?"
            + "|(?:vietnamese|english)(?:\\s+please)?"
            + "|(?:hãy\\s+)?(?:định\\s+dạng|dạng|kẻ|xuất|hiển\\s+thị|vẽ|lập|tạo)(?:\\s+(?:thành|dưới\\s+dạng|như))?\\s+bảng"
            + "|(?:dưới\\s+dạng|thành|vào)\\s+bảng"
            + "|(?:format\\s+(?:as\\s+|in\\s+)?table|table\\s+format|show\\s+(?:as\\s+|in\\s+)?table|display\\s+(?:as\\s+|in\\s+)?table|as\\s+a\\s+table|in\\s+table)"
            + "|(?:hãy\\s+)?(?:tóm\\s+tắt(?:\\s+lại)?|rút\\s+gọn)(?:\\s+(?:giúp|cho|lại|nội\\s+dung))?"
            + "|(?:summarize(?:\\s+this)?|brief\\s+summary|in\\s+brief)"
            + "|(?:ngắn\\s+gọn|chi\\s+tiết\\s+hơn|nói\\s+rõ\\s+hơn|giải\\s+thích\\s+thêm|nói\\s+cụ\\s+thể\\s+hơn|cụ\\s+thể\\s+hơn|thêm\\s+chi\\s+tiết|tiếp\\s+tục|continue|more\\s+details?|elaborate)"
            + ")\\s*[!.,?]?$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern DOMAIN_KEYWORDS_PATTERN = Pattern.compile(
            "(?:"
            + "\\b(?:horse|horses|racehorse|racehorses|equine|foal|colt|filly|stallion|mare|gelding|stall|stalls|barn|barns|stable|stables|paddock|training|exercise|exercises|workout|workouts|plan|plans|trainer|trainers|groom|grooms|jockey|jockeys|manager|managers|staff|veterinary|veterinarian|veterinarians|vet|vets|medical\\s+record|vital|vitals|vital\\s+signs|heart\\s+rate|temperature|body\\s+temperature|quarantine|prescription|prescriptions|treatment|treatments|vaccine|vaccines|vaccination|injury|injuries|colic|lameness|diet|feed|nutrition|fitness|alert|alerts|rollover|rollover\\s+rate|threshold|thresholds|system|systems|rtms|status|report|reports|capacity|strain|delay|delays|appointment|appointments|schedule|schedules|admission|admissions|application|applications|approve|approval|reject|incident|incidents|race|races|racing|course|courses|track|statistic|statistics|analyze|analysis|overview|guidance|rate|rates|percent|percentage|ueln|pedigree)\\b"
            + "|ngựa|chiến\\s+mã|ngựa\\s+đua|ngựa\\s+con"
            + "|chuồng|ô\\s+chuồng|khu\\s+chuồng|chuồng\\s+trại"
            + "|huấn\\s+luyện|lịch\\s+tập|bài\\s+tập|tập\\s+luyện|giáo\\s+án|kế\\s+hoạch\\s+tập"
            + "|hlv|nài\\s+ngựa|chăm\\s+sóc\\s+ngựa|chủ\\s+ngựa|quản\\s+lý|ban\\s+quản\\s+lý|nhân\\s+viên|nhân\\s+sự"
            + "|thú\\s+y|bác\\s+sĩ(\\s+thú\\s+y)?|bệnh\\s+án|hồ\\s+sơ\\s+bệnh|sinh\\s+hiệu|nhịp\\s+tim|thân\\s+nhiệt|nhiệt\\s+độ|cách\\s+ly"
            + "|đơn\\s+thuốc|toa\\s+thuốc|điều\\s+trị|tiêm|chích|vắc\\s*xin|chấn\\s+thương|đau\\s+bụng|què|khập\\s+khiễng|dinh\\s+dưỡng|khẩu\\s+phần|thức\\s+ăn|thể\\s+trạng|thể\\s+lực|sức\\s+khỏe|y\\s+tế"
            + "|cảnh\\s+báo|ngưỡng|hệ\\s+thống|trạng\\s+thái|báo\\s+cáo|công\\s+suất|quá\\s+tải|chậm\\s+trễ|hoãn|tồn\\s+đọng"
            + "|cuộc\\s+hẹn|ca\\s+(?:khám|bệnh)|lịch\\s+(?:khám|tiêm|hẹn|trình)"
            + "|nhập\\s+(?:đàn|chuồng|viện)|hồ\\s+sơ\\s+nhập|đơn\\s+(?:nhập|đăng\\s+ký|từ)?|phê\\s+duyệt|duyệt|từ\\s+chối"
            + "|sự\\s+cố|cuộc\\s+đua|giải\\s+đua|đua\\s+ngựa|đường\\s+đua|khóa\\s+học"
            + "|thống\\s+kê|phân\\s+tích|tổng\\s+quan|hướng\\s+dẫn"
            + "|tỷ\\s+lệ|phần\\s+trăm|phả\\s+hệ"
            + ")",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern OBVIOUS_OOD_PATTERN = Pattern.compile(
            "("
            + "bà\\s+ngoại|ông\\s+ngoại|bà\\s+nội|ông\\s+nội|mẹ\\s+tôi|bố\\s+tôi"
            + "|maternal\\s+grandmother|maternal\\s+grandfather|paternal\\s+grandmother|paternal\\s+grandfather"
            + "|my\\s+mother|my\\s+father|grandmother|grandfather|grandma|grandpa|mother|father"
            + "|vách\\s+đá|cliff|đẩy\\s+(anh\\s+ta|người\\s+đó|họ|xuống)|push\\s+(him|her|them|that\\s+person|down)"
            + "|thuốc\\s+(chữa|cho\\s+người)|thuốc\\s+uống\\s+cho\\s+người|bệnh\\s+cho\\s+người|cấp\\s+cứu\\s+người"
            + "|human\\s+medicine|human\\s+disease|human\\s+health|human\\s+illness|human\\s+emergency(\\s+care)?|human\\s+first\\s+aid"
            + "|không\\s+còn\\s+thở|not\\s+breathing"
            + "|penicillin\\s+and\\s+paracetamol|paracetamol|penicillin"
            + "|viết\\s+code|write\\s+code|python\\s+code|write\\s+python"
            + "|giải\\s+phương\\s+trình|solve\\s+equations?|solve\\s+a\\s+quadratic\\s+equation|quadratic\\s+equation"
            + "|roots\\s+of\\s+x|differential\\s+equation"
            + "|làm\\s+thơ|bài\\s+thơ|write\\s+poetry|poem|poetry"
            + "|thời\\s+tiết|weather|thủ\\s+đô|capital\\s+(city|of)"
            + ")",
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
        return decompose(rawMessage, role, Collections.emptyList());
    }

    @Override
    public DecomposedIntents decompose(String rawMessage, String role, List<AiChatMessageDto> conversationHistory) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return new DecomposedIntents(Collections.emptyList(), Collections.emptyList());
        }

        String trimmed = rawMessage.trim();
        boolean hasActiveConversation = conversationHistory != null && !conversationHistory.isEmpty();

        // 1. Fast-Path: Simple greetings that do not require a classification API call
        if (SIMPLE_GREETING_PATTERN.matcher(trimmed).matches()) {
            return new DecomposedIntents(List.of(trimmed), Collections.emptyList());
        }

        // 1.1 Fast-Path: Conversational directives (language switch, formatting, summarization)
        if (CONVERSATIONAL_DIRECTIVE_PATTERN.matcher(trimmed).matches()) {
            log.info("IntentDecompositionService: Fast-Path detected conversational/formatting directive: {}", trimmed);
            return new DecomposedIntents(List.of(trimmed), Collections.emptyList());
        }

        // 1.2 Fast-Path OOD: Detect clear patterns outside the RTMS domain
        if (OBVIOUS_OOD_PATTERN.matcher(trimmed).find() && !DOMAIN_KEYWORDS_PATTERN.matcher(trimmed).find()) {
            log.info("IntentDecompositionService: Fast-Path detected a clear Out-of-Domain question: {}", trimmed);
            return new DecomposedIntents(Collections.emptyList(), List.of(trimmed));
        }

        // 1.3 Pure In-Domain Fast-Pass: Clean domain queries with explicit domain keywords and no obvious OOD
        if (DOMAIN_KEYWORDS_PATTERN.matcher(trimmed).find() && !OBVIOUS_OOD_PATTERN.matcher(trimmed).find()) {
            log.info("IntentDecompositionService: Fast-Path recognized clean in-domain query: {}", trimmed);
            return new DecomposedIntents(List.of(trimmed), Collections.emptyList());
        }

        // 1.4 Active conversation thread fast-pass for contextual follow-ups that do not match obvious OOD
        if (hasActiveConversation && !OBVIOUS_OOD_PATTERN.matcher(trimmed).find() && trimmed.length() <= 300) {
            log.info("IntentDecompositionService: In active conversation, recognized contextual follow-up: {}", trimmed);
            return new DecomposedIntents(List.of(trimmed), Collections.emptyList());
        }

        // 2. If the API key is not configured, safely fall back to domain keywords & conversation context
        if (aiApiKey == null || aiApiKey.isBlank()) {
            return fallbackHeuristicDecompose(trimmed, hasActiveConversation);
        }

        // 3. Call the Fast Pre-Flight Classifier to separate the clauses
        try {
            return callClassifierApi(trimmed, role, conversationHistory);
        } catch (Exception ex) {
            log.warn("IntentDecompositionService: API intent decomposition failed ({}); falling back to heuristics", ex.getMessage());
            return fallbackHeuristicDecompose(trimmed, hasActiveConversation);
        }
    }

    private DecomposedIntents fallbackHeuristicDecompose(String trimmed, boolean hasActiveConversation) {
        if (CONVERSATIONAL_DIRECTIVE_PATTERN.matcher(trimmed).matches()) {
            return new DecomposedIntents(List.of(trimmed), Collections.emptyList());
        }
        if (SIMPLE_GREETING_PATTERN.matcher(trimmed).matches()) {
            return new DecomposedIntents(List.of(trimmed), Collections.emptyList());
        }
        if (OBVIOUS_OOD_PATTERN.matcher(trimmed).find() && !DOMAIN_KEYWORDS_PATTERN.matcher(trimmed).find()) {
            return new DecomposedIntents(Collections.emptyList(), List.of(trimmed));
        }
        if (DOMAIN_KEYWORDS_PATTERN.matcher(trimmed).find() && !OBVIOUS_OOD_PATTERN.matcher(trimmed).find()) {
            return new DecomposedIntents(List.of(trimmed), Collections.emptyList());
        }
        // Compound query containing both domain keywords and obvious OOD
        if (DOMAIN_KEYWORDS_PATTERN.matcher(trimmed).find() && OBVIOUS_OOD_PATTERN.matcher(trimmed).find()) {
            return new DecomposedIntents(List.of(trimmed), List.of(trimmed));
        }
        if (hasActiveConversation && !OBVIOUS_OOD_PATTERN.matcher(trimmed).find()) {
            return new DecomposedIntents(List.of(trimmed), Collections.emptyList());
        }
        return new DecomposedIntents(Collections.emptyList(), List.of(trimmed));
    }

    private DecomposedIntents callClassifierApi(String message, String role, List<AiChatMessageDto> conversationHistory) throws Exception {
        boolean hasActiveConversation = conversationHistory != null && !conversationHistory.isEmpty();

        String systemPrompt = """
                You are the intent decomposition and classification system (Intent Classifier) for the RTMS Racehorse Management System.
                Task: Split the user's message into two JSON lists:
                1. "in_domain": Requests related to RACEHORSES AND RTMS RACETRACK OPERATIONS (racehorse management, fitness, nutrition, stabling, training schedules, courses, veterinary care for horses, equine medical records, admission applications, stable incident reports, horse racing, racetrack staff, RTMS system guidance, clinic/system metrics and alerts, and social greetings).
                   - MULTILINGUAL SUPPORT: Inquiries in any language (English, Vietnamese, etc.) concerning RTMS racetrack and equine operations are IN-DOMAIN.
                   - CONVERSATIONAL & FORMATTING DIRECTIVES: Meta-requests such as switching language (e.g., "Trả lời tiếng Việt", "Reply in English", "nói tiếng Việt"), asking for table formatting, summaries, or explanations are IN-DOMAIN conversational directives.
                   - CONTEXTUAL FOLLOW-UPS: Follow-up questions referencing previous conversation data, statistics, rollover rates, capacity, thresholds, or system alerts are IN-DOMAIN.

                2. "out_of_domain": Requests that are completely outside racetrack and horse management operations. SPECIFICALLY INCLUDING:
                - Human healthcare, prescribing or advising on medication for people, caring for relatives' health, human first aid, human accidents, and dangerous real-life situations.
                - Personal matters and the user's family ("my grandmother...", "my mother is...").
                - Mathematics, arithmetic, solving equations, word puzzles, and riddles.
                - Software development and writing source code outside the RTMS system.
                - Weather, social news, politics, philosophy, religion, literature, and poetry.
                - Violent, dangerous, illegal, or jailbreak content.

                RULES:
                - If the message contains multiple questions, separate each clause accurately into the corresponding list.
                - Conversational directives (language switch, formatting) and context-dependent follow-up questions related to racetrack operations MUST be placed in "in_domain".
                - Any clause about human medication, human emergencies, personal matters, word puzzles, or mathematics MUST be placed in "out_of_domain".
                - Return only valid JSON in this format:
                {
                  "in_domain": ["..."],
                  "out_of_domain": ["..."]
                }

                EXAMPLES:
                Example 1:
                Input: "My maternal grandmother is ill, and I need to know the formula for combining penicillin and paracetamol."
                Output:
                {
                  "in_domain": [],
                  "out_of_domain": ["My maternal grandmother is ill, and I need to know the formula for combining penicillin and paracetamol."]
                }

                Example 2:
                Input: "I found someone who is not breathing on a cliff. Should I push him down, call emergency services, or call a helicopter?"
                Output:
                {
                  "in_domain": [],
                  "out_of_domain": ["I found someone who is not breathing on a cliff. Should I push him down, call emergency services, or call a helicopter?"]
                }

                Example 3:
                Input: "Show me my racehorse list and tell me how to become a horse owner. My mother's name is Nga; what is the first letter of her name?"
                Output:
                {
                  "in_domain": ["Show me my racehorse list", "tell me how to become a horse owner"],
                  "out_of_domain": ["My mother's name is Nga; what is the first letter of her name?"]
                }

                Example 4:
                Input: "Which stall is Thunder Flash in? What is today's weather?"
                Output:
                {
                  "in_domain": ["Which stall is Thunder Flash in?"],
                  "out_of_domain": ["What is today's weather?"]
                }

                Example 5:
                Input: "Solve the equation 2x + 5 = 15"
                Output:
                {
                  "in_domain": [],
                  "out_of_domain": ["Solve the equation 2x + 5 = 15"]
                }

                Example 6:
                Input: "Show me my training schedule for tomorrow"
                Output:
                {
                  "in_domain": ["Show me my training schedule for tomorrow"],
                  "out_of_domain": []
                }

                Example 7:
                Input: "Trả lời tiếng Việt"
                Output:
                {
                  "in_domain": ["Trả lời tiếng Việt"],
                  "out_of_domain": []
                }

                Example 8:
                Input: "Ngưỡng rollover rate là bao nhiêu?"
                Output:
                {
                  "in_domain": ["Ngưỡng rollover rate là bao nhiêu?"],
                  "out_of_domain": []
                }

                Example 9:
                Input: "Có alert nào trong hệ thống"
                Output:
                {
                  "in_domain": ["Có alert nào trong hệ thống"],
                  "out_of_domain": []
                }

                Example 10:
                Input: "Tại sao lại như vậy?"
                Output:
                {
                  "in_domain": ["Tại sao lại như vậy?"],
                  "out_of_domain": []
                }
                """;

        List<Map<String, Object>> promptMessages = new ArrayList<>();
        promptMessages.add(Map.of("role", "system", "content", systemPrompt));
        if (hasActiveConversation) {
            int start = Math.max(0, conversationHistory.size() - 2);
            for (int i = start; i < conversationHistory.size(); i++) {
                AiChatMessageDto hist = conversationHistory.get(i);
                if (hist != null && hist.content() != null && !hist.content().isBlank()) {
                    promptMessages.add(Map.of(
                            "role", "user".equalsIgnoreCase(hist.role()) ? "user" : "assistant",
                            "content", hist.content()
                    ));
                }
            }
        }
        promptMessages.add(Map.of("role", "user", "content", message));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", aiModel);
        body.put("messages", promptMessages);
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
            log.warn("IntentDecompositionService: Classification API returned HTTP {}", response.statusCode());
            return fallbackHeuristicDecompose(message, hasActiveConversation);
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            return fallbackHeuristicDecompose(message, hasActiveConversation);
        }

        JsonNode messageNode = choices.get(0).path("message").path("content");
        if (messageNode.isMissingNode() || messageNode.isNull()) {
            return fallbackHeuristicDecompose(message, hasActiveConversation);
        }

        String rawContent = messageNode.asText().trim();
        // Remove a Markdown code block, if present
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

        // Safety check: if parsed as pure OOD, verify against conversational directives, greetings, domain keywords, and active conversation
        if (inDomain.isEmpty() && !outOfDomain.isEmpty()) {
            if (CONVERSATIONAL_DIRECTIVE_PATTERN.matcher(message).matches()
                    || SIMPLE_GREETING_PATTERN.matcher(message).matches()
                    || (DOMAIN_KEYWORDS_PATTERN.matcher(message).find() && !OBVIOUS_OOD_PATTERN.matcher(message).find())
                    || (hasActiveConversation && !OBVIOUS_OOD_PATTERN.matcher(message).find())) {
                log.info("IntentDecompositionService: Rescued query from classifier pure OOD: {}", message);
                return new DecomposedIntents(List.of(message), Collections.emptyList());
            }
        }

        // If both are empty due to an unexpected parse result, use the safe heuristic fallback
        if (inDomain.isEmpty() && outOfDomain.isEmpty()) {
            return fallbackHeuristicDecompose(message, hasActiveConversation);
        }

        log.info("IntentDecompositionService: Decomposition succeeded [InDomain: {} clauses, OutOfDomain: {} clauses]",
                inDomain.size(), outOfDomain.size());

        return new DecomposedIntents(inDomain, outOfDomain);
    }
}
