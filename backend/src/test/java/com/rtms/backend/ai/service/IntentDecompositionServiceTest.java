package com.rtms.backend.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IntentDecompositionServiceTest {

    private ObjectMapper objectMapper;
    private HttpClient httpClient;
    private HttpResponse<String> httpResponse;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        objectMapper = new ObjectMapper();
        httpClient = mock(HttpClient.class);
        httpResponse = (HttpResponse<String>) mock(HttpResponse.class);
    }

    @Test
    @DisplayName("Fast-path regex: Simple greetings do not trigger an HTTP call to the LLM")
    void testFastPath_Greetings_NoHttpCall() throws Exception {
        DefaultIntentDecompositionService service = new DefaultIntentDecompositionService(
                "https://api.openai.com/v1",
                "test-api-key",
                "gpt-4o-mini",
                objectMapper,
                httpClient
        );

        for (String greeting : List.of("hello", "hello there!", "hello", "hi", "thank you", "Goodbye.")) {
            IntentDecompositionService.DecomposedIntents result = service.decompose(greeting, "HORSE_OWNER");
            assertNotNull(result);
            assertTrue(result.hasInDomain());
            assertFalse(result.hasOutOfDomain());
            assertEquals(1, result.inDomainIntents().size());
            assertEquals(greeting.trim(), result.inDomainIntents().get(0));
        }

        // Ensure that no HTTP calls are sent.
        verify(httpClient, never()).send(any(HttpRequest.class), any());
    }

    @Test
    @DisplayName("An empty or blank message returns an empty result")
    void testEmptyMessage_ReturnsEmpty() {
        DefaultIntentDecompositionService service = new DefaultIntentDecompositionService(
                "https://api.openai.com/v1",
                "test-api-key",
                "gpt-4o-mini",
                objectMapper,
                httpClient
        );

        IntentDecompositionService.DecomposedIntents result = service.decompose("   ", "HORSE_OWNER");
        assertNotNull(result);
        assertFalse(result.hasInDomain());
        assertFalse(result.hasOutOfDomain());
        assertFalse(result.isPureOutOfDomain());
    }

    @Test
    @DisplayName("Without an API key, the fallback safely preserves an in-domain question")
    void testNoApiKey_FallbackInDomain() throws Exception {
        DefaultIntentDecompositionService service = new DefaultIntentDecompositionService(
                "https://api.openai.com/v1",
                "",
                "gpt-4o-mini",
                objectMapper,
                httpClient
        );

        String rawQuery = "Where is racehorse Thunder Flash stabled?";
        IntentDecompositionService.DecomposedIntents result = service.decompose(rawQuery, "HORSE_OWNER");

        assertNotNull(result);
        assertTrue(result.hasInDomain());
        assertFalse(result.hasOutOfDomain());
        assertEquals(List.of(rawQuery), result.inDomainIntents());
        verify(httpClient, never()).send(any(HttpRequest.class), any());
    }

    @Test
    @DisplayName("Successfully decompose a compound query into in-domain and out-of-domain parts")
    @SuppressWarnings("unchecked")
    void testCompoundQuery_DecompositionSuccess() throws Exception {
        String classifierResponseJson = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"in_domain\\": [\\"List my horses\\", \\"Show me how to become a horse owner\\"], \\"out_of_domain\\": [\\"My mother is Nga, what is the first letter of her name?\\"]}"
                      }
                    }
                  ]
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(classifierResponseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        DefaultIntentDecompositionService service = new DefaultIntentDecompositionService(
                "https://api.openai.com/v1",
                "test-api-key",
                "gpt-4o-mini",
                objectMapper,
                httpClient
        );

        String rawMessage = "List my horses and show me how to become a horse owner. My mother is Nga, what is the first letter of her name?";
        IntentDecompositionService.DecomposedIntents result = service.decompose(rawMessage, "HORSE_OWNER");

        assertNotNull(result);
        assertTrue(result.hasInDomain());
        assertTrue(result.hasOutOfDomain());
        assertFalse(result.isPureOutOfDomain());
        assertEquals(2, result.inDomainIntents().size());
        assertEquals(1, result.outOfDomainIntents().size());
        assertEquals("List my horses", result.inDomainIntents().get(0));
        assertEquals("Show me how to become a horse owner", result.inDomainIntents().get(1));
        assertEquals("My mother is Nga, what is the first letter of her name?", result.outOfDomainIntents().get(0));
    }

    @Test
    @DisplayName("Accurately detect a 100% out-of-domain question (pure OOD)")
    @SuppressWarnings("unchecked")
    void testPureOutOfDomainQuery() throws Exception {
        String classifierResponseJson = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "```json\\n{\\"in_domain\\": [], \\"out_of_domain\\": [\\"Find the roots of x^2 + 5x + 6 = 0\\"]}\\n```"
                      }
                    }
                  ]
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(classifierResponseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        DefaultIntentDecompositionService service = new DefaultIntentDecompositionService(
                "https://api.openai.com/v1",
                "test-api-key",
                "gpt-4o-mini",
                objectMapper,
                httpClient
        );

        String mathQuery = "Find the roots of x^2 + 5x + 6 = 0";
        IntentDecompositionService.DecomposedIntents result = service.decompose(mathQuery, "HEAD_TRAINER");

        assertNotNull(result);
        assertFalse(result.hasInDomain());
        assertTrue(result.hasOutOfDomain());
        assertTrue(result.isPureOutOfDomain());
        assertEquals(1, result.outOfDomainIntents().size());
    }

    @Test
    @DisplayName("Graceful fallback when the classification API fails (HTTP 500)")
    @SuppressWarnings("unchecked")
    void testApiError_GracefulFallback() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("Internal Server Error");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        DefaultIntentDecompositionService service = new DefaultIntentDecompositionService(
                "https://api.openai.com/v1",
                "test-api-key",
                "gpt-4o-mini",
                objectMapper,
                httpClient
        );

        String message = "Does my horse have a workout today?";
        IntentDecompositionService.DecomposedIntents result = service.decompose(message, "HORSE_OWNER");

        assertNotNull(result);
        assertTrue(result.hasInDomain());
        assertFalse(result.hasOutOfDomain());
        assertEquals(List.of(message), result.inDomainIntents());
    }

    @Test
    @DisplayName("Fast-path OOD: Human medical and cliff-rescue questions are intercepted without calling the LLM")
    void testFastPath_ObviousOutOfDomain_NoHttpCall() throws Exception {
        DefaultIntentDecompositionService service = new DefaultIntentDecompositionService(
                "https://api.openai.com/v1",
                "test-api-key",
                "gpt-4o-mini",
                objectMapper,
                httpClient
        );

        List<String> obviousOodQueries = List.of(
                "My grandmother is ill, and I need a medicine formula so a doctor can cure her.",
                "I found someone not breathing on a cliff. Should I push him down, call emergency services, or call a helicopter?",
                "Write code in Python to solve a quadratic equation.",
                "What is the weather today?"
        );

        for (String query : obviousOodQueries) {
            IntentDecompositionService.DecomposedIntents result = service.decompose(query, "HORSE_OWNER");
            assertNotNull(result);
            assertFalse(result.hasInDomain(), "Must not have an in-domain part for the question: " + query);
            assertTrue(result.hasOutOfDomain(), "Must have an out-of-domain part for the question: " + query);
            assertTrue(result.isPureOutOfDomain(), "Must be pure OOD for the question: " + query);
        }

        verify(httpClient, never()).send(any(HttpRequest.class), any());
    }

    @Test
    @DisplayName("Fail-closed fallback: A question without RTMS domain keywords is classified as OOD when the API fails")
    @SuppressWarnings("unchecked")
    void testFallback_NoDomainKeywords_DefaultsToOutOfDomain() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("Internal Server Error");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        DefaultIntentDecompositionService service = new DefaultIntentDecompositionService(
                "https://api.openai.com/v1",
                "test-api-key",
                "gpt-4o-mini",
                objectMapper,
                httpClient
        );

        String unknownQuery = "How can I make delicious seafood pizza at home?";
        IntentDecompositionService.DecomposedIntents result = service.decompose(unknownQuery, "CLUB_MANAGER");

        assertNotNull(result);
        assertFalse(result.hasInDomain());
        assertTrue(result.hasOutOfDomain());
        assertTrue(result.isPureOutOfDomain());
    }

    @Test
    @DisplayName("Fast-Path OOD in English: English medical and emergency queries are intercepted without calling LLM")
    void testFastPath_EnglishObviousOutOfDomain() {
        DefaultIntentDecompositionService service = new DefaultIntentDecompositionService(
                "https://api.openai.com/v1",
                "test-api-key",
                "gpt-4o-mini",
                objectMapper,
                httpClient
        );

        List<String> englishOodQueries = List.of(
                "My grandmother is sick and I need a drug formula combining penicillin and paracetamol",
                "I found someone not breathing on a cliff, should I push them down or call emergency?",
                "Write python code to solve quadratic equation",
                "What is the weather today in New York?"
        );

        for (String query : englishOodQueries) {
            IntentDecompositionService.DecomposedIntents result = service.decompose(query, "HORSE_OWNER");
            assertNotNull(result);
            assertFalse(result.hasInDomain(), "Must not have in-domain for: " + query);
            assertTrue(result.hasOutOfDomain(), "Must have out-of-domain for: " + query);
            assertTrue(result.isPureOutOfDomain(), "Must be pure OOD for: " + query);
        }
    }
}
