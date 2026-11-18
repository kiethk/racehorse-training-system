package com.rtms.backend.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Client that connects to the LLM REST API (OpenAI / Groq) for Chat Completions.
 */
@Component
public class LlmApiClient {

    private static final Logger log = LoggerFactory.getLogger(LlmApiClient.class);

    private final String aiBaseUrl;
    private final String aiApiKey;
    private final String aiModel;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public LlmApiClient(
            @Value("${ai.api.base-url:${groq.api.base-url:https://api.openai.com/v1}}") String aiBaseUrl,
            @Value("${ai.api.key:${groq.api.key:}}") String aiApiKey,
            @Value("${ai.api.model:${groq.api.model:gpt-4o-mini}}") String aiModel,
            ObjectMapper objectMapper
    ) {
        this(aiBaseUrl, aiApiKey, aiModel, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    public LlmApiClient(
            String aiBaseUrl,
            String aiApiKey,
            String aiModel,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.aiBaseUrl = aiBaseUrl;
        this.aiApiKey = aiApiKey;
        this.aiModel = aiModel;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isApiKeyConfigured() {
        return aiApiKey != null && !aiApiKey.isBlank();
    }

    public String getAiModel() {
        return aiModel;
    }

    public Map<String, Object> callChatCompletions(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        if (!isApiKeyConfigured()) {
            return Collections.emptyMap();
        }
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
                return Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error("Exception while calling the AI API: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    public Map<String, Object> parseArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(argumentsJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Unable to parse the arguments JSON: {}", argumentsJson);
            return Collections.emptyMap();
        }
    }
}
