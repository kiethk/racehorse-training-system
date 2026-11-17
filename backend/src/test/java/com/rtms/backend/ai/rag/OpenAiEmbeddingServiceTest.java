package com.rtms.backend.ai.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.ai.rag.service.DefaultEmbeddingService;
import com.rtms.backend.ai.rag.service.OpenAiEmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OpenAiEmbeddingServiceTest {

    private DefaultEmbeddingService fallbackService;
    private ObjectMapper objectMapper;
    private HttpClient httpClient;
    private HttpResponse<String> httpResponse;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        fallbackService = Mockito.mock(DefaultEmbeddingService.class);
        objectMapper = new ObjectMapper();
        httpClient = Mockito.mock(HttpClient.class);
        httpResponse = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    }

    @Test
    @DisplayName("Embedding succeeds when OpenAI returns a valid 1536-dimensional vector")
    @SuppressWarnings("unchecked")
    void testEmbed_Success() throws Exception {
        // Prepare a sample 1536-dimensional vector.
        StringBuilder jsonBuilder = new StringBuilder("{\"object\":\"list\",\"data\":[{\"object\":\"embedding\",\"index\":0,\"embedding\":[");
        for (int i = 0; i < 1536; i++) {
            jsonBuilder.append(i == 0 ? "0.05" : ",0.01");
        }
        jsonBuilder.append("]}],\"model\":\"text-embedding-3-small\"}");

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonBuilder.toString());
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        OpenAiEmbeddingService service = new OpenAiEmbeddingService(
                "https://api.openai.com/v1",
                "sk-test-key-1234567890",
                "text-embedding-3-small",
                true,
                fallbackService,
                objectMapper,
                httpClient
        );

        float[] result = service.embed("The horse has signs of colic");

        assertNotNull(result);
        assertEquals(1536, result.length);
        assertEquals(0.05f, result[0], 0.0001f);
        assertEquals(0.01f, result[1], 0.0001f);
        verifyNoInteractions(fallbackService);
    }

    @Test
    @DisplayName("An empty or null input returns an all-zero vector without calling the API")
    void testEmbed_BlankInput_ReturnsZeroVector() {
        OpenAiEmbeddingService service = new OpenAiEmbeddingService(
                "https://api.openai.com/v1",
                "sk-test-key-1234567890",
                "text-embedding-3-small",
                true,
                fallbackService,
                objectMapper,
                httpClient
        );

        float[] resultNull = service.embed(null);
        float[] resultBlank = service.embed("   ");

        assertNotNull(resultNull);
        assertEquals(1536, resultNull.length);
        assertEquals(0.0f, resultNull[0]);

        assertNotNull(resultBlank);
        assertEquals(1536, resultBlank.length);
        assertEquals(0.0f, resultBlank[0]);

        verifyNoInteractions(fallbackService);
        verifyNoInteractions(httpClient);
    }

    @Test
    @DisplayName("An empty or placeholder API key triggers fallback to DefaultEmbeddingService")
    void testEmbed_PlaceholderApiKey_Fallback() {
        float[] mockFallbackVector = new float[1536];
        mockFallbackVector[0] = 0.99f;
        when(fallbackService.embed(anyString())).thenReturn(mockFallbackVector);

        OpenAiEmbeddingService service = new OpenAiEmbeddingService(
                "https://api.openai.com/v1",
                "YOUR_OPENAI_API_KEY_HERE",
                "text-embedding-3-small",
                true,
                fallbackService,
                objectMapper,
                httpClient
        );

        float[] result = service.embed("The horse has abdominal pain");

        assertNotNull(result);
        assertEquals(0.99f, result[0]);
        verify(fallbackService).embed("The horse has abdominal pain");
        verifyNoInteractions(httpClient);
    }

    @Test
    @DisplayName("When OpenAI returns HTTP 500, the service safely falls back without crashing")
    @SuppressWarnings("unchecked")
    void testEmbed_Http500_Fallback() throws Exception {
        float[] mockFallbackVector = new float[1536];
        mockFallbackVector[0] = 0.77f;
        when(fallbackService.embed(anyString())).thenReturn(mockFallbackVector);

        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("{\"error\":{\"message\":\"Internal Server Error\"}}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        OpenAiEmbeddingService service = new OpenAiEmbeddingService(
                "https://api.openai.com/v1",
                "sk-valid-key",
                "text-embedding-3-small",
                true,
                fallbackService,
                objectMapper,
                httpClient
        );

        float[] result = service.embed("Symptoms of arthritis");

        assertNotNull(result);
        assertEquals(0.77f, result[0]);
        verify(fallbackService).embed("Symptoms of arthritis");
    }

    @Test
    @DisplayName("Batch embedding succeeds with a list of multiple texts")
    @SuppressWarnings("unchecked")
    void testEmbedBatch_Success() throws Exception {
        StringBuilder jsonBuilder = new StringBuilder("{\"object\":\"list\",\"data\":[");
        // Item 1.
        jsonBuilder.append("{\"object\":\"embedding\",\"index\":0,\"embedding\":[");
        for (int i = 0; i < 1536; i++) {
            jsonBuilder.append(i == 0 ? "0.1" : ",0.01");
        }
        jsonBuilder.append("]},");
        // Item 2.
        jsonBuilder.append("{\"object\":\"embedding\",\"index\":1,\"embedding\":[");
        for (int i = 0; i < 1536; i++) {
            jsonBuilder.append(i == 0 ? "0.2" : ",0.02");
        }
        jsonBuilder.append("]}],\"model\":\"text-embedding-3-small\"}");

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonBuilder.toString());
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        OpenAiEmbeddingService service = new OpenAiEmbeddingService(
                "https://api.openai.com/v1",
                "sk-test-key",
                "text-embedding-3-small",
                true,
                fallbackService,
                objectMapper,
                httpClient
        );

        List<float[]> results = service.embedBatch(List.of("Symptom 1", "Symptom 2"));

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(0.1f, results.get(0)[0], 0.0001f);
        assertEquals(0.2f, results.get(1)[0], 0.0001f);
    }
}
