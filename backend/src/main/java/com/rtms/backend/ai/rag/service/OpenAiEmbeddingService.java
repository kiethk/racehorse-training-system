package com.rtms.backend.ai.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Triển khai EmbeddingService kết nối với OpenAI API (text-embedding-3-small).
 * 
 * Tạo vector 1536 chiều từ mô hình OpenAI thế hệ mới.
 * Nếu không có API Key, gặp lỗi kết nối hoặc bị vô hiệu hóa, tự động fallback sang
 * DefaultEmbeddingService (Deterministic SHA-256 Hashing) để đảm bảo hệ thống không bị crash.
 */
@Service
@Primary
public class OpenAiEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingService.class);
    public static final int VECTOR_DIMENSION = 1536;

    private final String baseUrl;
    private final String apiKey;
    private final String modelName;
    private final boolean isEnabled;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DefaultEmbeddingService fallbackService;

    @Autowired
    public OpenAiEmbeddingService(
            @Value("${ai.api.base-url:${groq.api.base-url:https://api.openai.com/v1}}") String baseUrl,
            @Value("${ai.api.key:${groq.api.key:}}") String apiKey,
            @Value("${ai.embedding.model:text-embedding-3-small}") String modelName,
            @Value("${ai.embedding.enabled:true}") boolean isEnabled,
            DefaultEmbeddingService fallbackService,
            ObjectMapper objectMapper) {
        this(baseUrl, apiKey, modelName, isEnabled, fallbackService, objectMapper,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    public OpenAiEmbeddingService(
            String baseUrl,
            String apiKey,
            String modelName,
            boolean isEnabled,
            DefaultEmbeddingService fallbackService,
            ObjectMapper objectMapper,
            HttpClient httpClient) {
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank())
                ? (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl.trim())
                : "https://api.openai.com/v1";
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.modelName = (modelName != null && !modelName.isBlank()) ? modelName.trim() : "text-embedding-3-small";
        this.isEnabled = isEnabled;
        this.fallbackService = fallbackService;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient != null
                ? httpClient
                : HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[VECTOR_DIMENSION];
        }

        if (!isRemoteCallAvailable()) {
            return fallbackService.embed(text);
        }

        try {
            String requestUrl = baseUrl + "/embeddings";
            Map<String, Object> payload = Map.of(
                    "model", modelName,
                    "input", text.strip(),
                    "encoding_format", "float"
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode dataArray = root.path("data");
                if (dataArray.isArray() && !dataArray.isEmpty()) {
                    JsonNode embeddingArray = dataArray.get(0).path("embedding");
                    if (embeddingArray.isArray() && embeddingArray.size() == VECTOR_DIMENSION) {
                        float[] vector = new float[VECTOR_DIMENSION];
                        for (int i = 0; i < VECTOR_DIMENSION; i++) {
                            vector[i] = (float) embeddingArray.get(i).asDouble();
                        }
                        return vector;
                    }
                }
                log.warn("OpenAI Embedding phản hồi cấu trúc không hợp lệ: {}", response.body());
            } else {
                log.warn("Gọi OpenAI Embedding thất bại (HTTP {}): {}. Chuyển sang fallback.", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("Lỗi ngoại lệ khi kết nối OpenAI Embedding: {}. Chuyển sang fallback.", e.getMessage());
        }

        return fallbackService.embed(text);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        if (!isRemoteCallAvailable()) {
            return fallbackService.embedBatch(texts);
        }

        try {
            String requestUrl = baseUrl + "/embeddings";
            List<String> cleaned = texts.stream()
                    .map(t -> (t == null || t.isBlank()) ? " " : t.strip())
                    .toList();

            Map<String, Object> payload = Map.of(
                    "model", modelName,
                    "input", cleaned,
                    "encoding_format", "float"
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode dataArray = root.path("data");
                if (dataArray.isArray() && dataArray.size() == texts.size()) {
                    List<float[]> results = new ArrayList<>(texts.size());
                    for (int i = 0; i < dataArray.size(); i++) {
                        JsonNode embeddingArray = dataArray.get(i).path("embedding");
                        float[] vector = new float[VECTOR_DIMENSION];
                        for (int j = 0; j < VECTOR_DIMENSION; j++) {
                            vector[j] = (float) embeddingArray.get(j).asDouble();
                        }
                        results.add(vector);
                    }
                    return results;
                }
            }
            log.warn("Gọi OpenAI Batch Embedding thất bại (HTTP {}): {}. Chuyển sang fallback.", response.statusCode(), response.body());
        } catch (Exception e) {
            log.warn("Lỗi ngoại lệ khi kết nối OpenAI Batch Embedding: {}. Chuyển sang fallback.", e.getMessage());
        }

        return fallbackService.embedBatch(texts);
    }

    private boolean isRemoteCallAvailable() {
        if (!isEnabled || apiKey.isBlank()) {
            return false;
        }
        return !apiKey.startsWith("YOUR_OPENAI_API_KEY") && !apiKey.startsWith("sk-proj-YOUR_");
    }
}
