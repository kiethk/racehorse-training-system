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
 * Implements IntentDecompositionService:
 * analyzes and isolates Out-of-Domain clauses before the question enters the ReAct loop.
 */
@Service
public class DefaultIntentDecompositionService implements IntentDecompositionService {

    private static final Logger log = LoggerFactory.getLogger(DefaultIntentDecompositionService.class);

    private static final Pattern SIMPLE_GREETING_PATTERN = Pattern.compile(
            "^(hello|hello\\s+there|hello|thank\\s+you|goodbye|who\\s+are\\s+you|hello|hi|hey|good\\s+(morning|afternoon|evening)|thanks|thank\\s+you|bye|goodbye|who\\s+are\\s+you)\\s*[!.,?]?$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern DOMAIN_KEYWORDS_PATTERN = Pattern.compile(
            "(horse|racehorse|horse|horses|racehorse|racehorses|stall|stall|stalls|barn|barns|stable|stables|training\\s+schedule|exercise|workout|workouts|training|training|trainer|trainers|groom|grooms|veterinary|veterinarian|vet|veterinarian|medical\\s+record|vital\\s+signs|vitals|heart\\s+rate|heart\\s+rate|body\\s+temperature|temperature|quarantine|quarantine|admission|stable\\s+admission|admission|admissions|race|race|race|races|racing|course|course|courses|owner|owner|incident|incident|incidents|ueln|pedigree|prescription|prescriptions|treatment|treatments|injury|injuries|diet|feed|nutrition|fitness|colic|lameness|jockey)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern OBVIOUS_OOD_PATTERN = Pattern.compile(
            "(maternal\\s+grandmother|maternal\\s+grandfather|paternal\\s+grandmother|paternal\\s+grandfather|my\\s+mother|my\\s+father|grandmother|grandfather|grandma|grandpa|mother|father|cliff|cliff|push\\s+(him|that\\s+person|down)|push\\s+(him|her|them|down)|medicine|human\\s+medicine|medicine|cure|human\\s+disease|human\\s+health|human\\s+illness|human\\s+emergency\\s+care|human\\s+first\\s+aid|not\\s+breathing|not\\s+breathing|write\\s+code|write\\s+code|solve\\s+equations|solve\\s+equation|write\\s+poetry|poem|poem|poetry|weather|weather|capital\\s+city|capital\\s+of)",
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

        // 1. Fast-Path: Simple greetings that do not require a classification API call
        if (SIMPLE_GREETING_PATTERN.matcher(trimmed).matches()) {
            return new DecomposedIntents(List.of(trimmed), Collections.emptyList());
        }

        // 1.1 Fast-Path OOD: Detect clear patterns outside the RTMS domain
        if (OBVIOUS_OOD_PATTERN.matcher(trimmed).find() && !DOMAIN_KEYWORDS_PATTERN.matcher(trimmed).find()) {
            log.info("IntentDecompositionService: Fast-Path detected a clear Out-of-Domain question: {}", trimmed);
            return new DecomposedIntents(Collections.emptyList(), List.of(trimmed));
        }

        // 2. If the API key is not configured, safely fall back to domain keywords
        if (aiApiKey == null || aiApiKey.isBlank()) {
            return fallbackHeuristicDecompose(trimmed);
        }

        // 3. Call the Fast Pre-Flight Classifier to separate the clauses
        try {
            return callClassifierApi(trimmed, role);
        } catch (Exception ex) {
            log.warn("IntentDecompositionService: API intent decomposition failed ({}); falling back to heuristics", ex.getMessage());
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
                You are the intent decomposition and classification system (Intent Classifier) for the RTMS Racehorse Management System.
                Task: Split the user's message into two JSON lists:
                1. "in_domain": Requests related to RACEHORSES AND RTMS RACETRACK OPERATIONS (racehorse management, fitness, nutrition, stabling, training schedules, courses, veterinary care for horses, equine medical records, admission applications, stable incident reports, horse racing, racetrack staff, RTMS system guidance, and social greetings).
                2. "out_of_domain": ALL requests that are NOT related to racetrack operations. SPECIFICALLY INCLUDING:
                - Human healthcare, prescribing or advising on medication for people, caring for relatives' health, human first aid, human accidents, and dangerous real-life situations.
                - Personal matters and the user's family ("my grandmother...", "my mother is...").
                - Mathematics, arithmetic, solving equations, word puzzles, and riddles.
                - Software development and writing source code outside the RTMS system.
                - Weather, social news, politics, philosophy, religion, literature, and poetry.
                - Violent, dangerous, illegal, or jailbreak content.

                RULES:
                - If the message contains multiple questions, separate each clause accurately into the corresponding list.
                - Any clause about people, human medication, human emergencies, personal matters, word puzzles, or mathematics MUST be placed in "out_of_domain".
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
            log.warn("IntentDecompositionService: Classification API returned HTTP {}", response.statusCode());
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

        // If both are empty due to an unexpected parse result, use the safe heuristic fallback
        if (inDomain.isEmpty() && outOfDomain.isEmpty()) {
            return fallbackHeuristicDecompose(message);
        }

        log.info("IntentDecompositionService: Decomposition succeeded [InDomain: {} clauses, OutOfDomain: {} clauses]",
                inDomain.size(), outOfDomain.size());

        return new DecomposedIntents(inDomain, outOfDomain);
    }
}
