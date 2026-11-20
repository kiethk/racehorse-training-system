package com.rtms.backend.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.ai.dto.AiChatMessageDto;
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
                "gpt-6-luna",
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
                "gpt-6-luna",
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
                "gpt-6-luna",
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
                "gpt-6-luna",
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
                "gpt-6-luna",
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
                "gpt-6-luna",
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
                "gpt-6-luna",
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
                "gpt-6-luna",
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
                "gpt-6-luna",
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

    @Test
    @DisplayName("Fast-path regex: Vietnamese greetings do not trigger an HTTP call to the LLM")
    void testFastPath_VietnameseGreetings_NoHttpCall() throws Exception {
        DefaultIntentDecompositionService service = new DefaultIntentDecompositionService(
                "https://api.openai.com/v1",
                "test-api-key",
                "gpt-6-luna",
                objectMapper,
                httpClient
        );

        for (String greeting : List.of("xin chào", "chào bạn", "chào", "chào bạn nhé", "chào buổi sáng", "cảm ơn", "cảm ơn bạn nhé", "tạm biệt", "bạn là ai", "bạn có thể giúp gì cho tôi", "what can you do")) {
            IntentDecompositionService.DecomposedIntents result = service.decompose(greeting, "HORSE_OWNER");
            assertNotNull(result);
            assertTrue(result.hasInDomain(), "Greeting must be in-domain: " + greeting);
            assertFalse(result.hasOutOfDomain(), "Greeting must not be OOD: " + greeting);
            assertEquals(greeting.trim(), result.inDomainIntents().get(0));
        }

        verify(httpClient, never()).send(any(HttpRequest.class), any());
    }

    @Test
    @DisplayName("Fast-path regex: Language switch and conversational directives are immediately in-domain without HTTP call")
    void testFastPath_ConversationalDirectives_NoHttpCall() throws Exception {
        DefaultIntentDecompositionService service = new DefaultIntentDecompositionService(
                "https://api.openai.com/v1",
                "test-api-key",
                "gpt-6-luna",
                objectMapper,
                httpClient
        );

        List<String> directives = List.of(
                "Trả lời tiếng Việt",
                "Trả lời bằng tiếng Việt",
                "Nói tiếng Việt",
                "tiếng Việt",
                "Reply in Vietnamese",
                "Please reply in Vietnamese",
                "switch to Vietnamese",
                "switch to English",
                "in Vietnamese",
                "bằng tiếng Việt",
                "Trả lời tiếng Việt nha",
                "Nói tiếng Việt giúp tôi",
                "Xin chào, hãy trả lời bằng tiếng Việt nhé",
                "Định dạng bảng",
                "dưới dạng bảng",
                "kẻ bảng",
                "lập bảng",
                "Format as table",
                "Table format",
                "Tóm tắt",
                "Chi tiết hơn"
        );

        for (String directive : directives) {
            IntentDecompositionService.DecomposedIntents result = service.decompose(directive, "CLUB_MANAGER");
            assertNotNull(result);
            assertTrue(result.hasInDomain(), "Directive must be in-domain: " + directive);
            assertFalse(result.hasOutOfDomain(), "Directive must not be OOD: " + directive);
            assertFalse(result.isPureOutOfDomain(), "Directive must not be pure OOD: " + directive);
            assertEquals(directive.trim(), result.inDomainIntents().get(0));
        }

        verify(httpClient, never()).send(any(HttpRequest.class), any());
    }

    @Test
    @DisplayName("Verification of the user 4-turn sequence: All four turns correctly decompose as in-domain")
    void testUserFourTurnFlow_DecomposesInDomain() {
        DefaultIntentDecompositionService service = new DefaultIntentDecompositionService(
                "https://api.openai.com/v1",
                "", // empty api key: test heuristic & domain keyword matching
                "gpt-6-luna",
                objectMapper,
                httpClient
        );

        // Turn 1: "Phân tích trạng thái Vet" (starts conversation)
        IntentDecompositionService.DecomposedIntents turn1 = service.decompose("Phân tích trạng thái Vet", "CLUB_MANAGER", List.of());
        assertNotNull(turn1);
        assertTrue(turn1.hasInDomain(), "Turn 1 must be in-domain");
        assertFalse(turn1.isPureOutOfDomain(), "Turn 1 must not be pure OOD");

        // History after Turn 1
        List<AiChatMessageDto> historyAfterTurn1 = List.of(
                new AiChatMessageDto("user", "Phân tích trạng thái Vet"),
                new AiChatMessageDto("assistant", "## Veterinarian Status Analysis\nThe system lists 3 veterinarians, rollover rate 22.1%...")
        );

        // Turn 2: "Trả lời tiếng Việt" (conversational directive)
        IntentDecompositionService.DecomposedIntents turn2 = service.decompose("Trả lời tiếng Việt", "CLUB_MANAGER", historyAfterTurn1);
        assertNotNull(turn2);
        assertTrue(turn2.hasInDomain(), "Turn 2 must be in-domain");
        assertFalse(turn2.isPureOutOfDomain(), "Turn 2 must not be pure OOD");

        // History after Turn 2
        List<AiChatMessageDto> historyAfterTurn2 = List.of(
                new AiChatMessageDto("user", "Phân tích trạng thái Vet"),
                new AiChatMessageDto("assistant", "## Veterinarian Status Analysis\nThe system lists 3 veterinarians, rollover rate 22.1%..."),
                new AiChatMessageDto("user", "Trả lời tiếng Việt"),
                new AiChatMessageDto("assistant", "## Phân tích trạng thái Bác sĩ Thú y\nHệ thống ghi nhận 3 bác sĩ thú y đang hoạt động...")
        );

        // Turn 3: "Ngưỡng rollover rate là bao nhiêu?" (contextual inquiry with domain keywords)
        IntentDecompositionService.DecomposedIntents turn3 = service.decompose("Ngưỡng rollover rate là bao nhiêu?", "CLUB_MANAGER", historyAfterTurn2);
        assertNotNull(turn3);
        assertTrue(turn3.hasInDomain(), "Turn 3 must be in-domain");
        assertFalse(turn3.isPureOutOfDomain(), "Turn 3 must not be pure OOD");

        // History after Turn 3
        List<AiChatMessageDto> historyAfterTurn3 = List.of(
                new AiChatMessageDto("user", "Phân tích trạng thái Vet"),
                new AiChatMessageDto("assistant", "## Veterinarian Status Analysis..."),
                new AiChatMessageDto("user", "Trả lời tiếng Việt"),
                new AiChatMessageDto("assistant", "## Phân tích trạng thái Bác sĩ Thú y..."),
                new AiChatMessageDto("user", "Ngưỡng rollover rate là bao nhiêu?"),
                new AiChatMessageDto("assistant", "Ngưỡng cảnh báo rollover rate thông thường được cấu hình...")
        );

        // Turn 4: "Có alert nào trong hệ thống" (contextual alert inquiry)
        IntentDecompositionService.DecomposedIntents turn4 = service.decompose("Có alert nào trong hệ thống", "CLUB_MANAGER", historyAfterTurn3);
        assertNotNull(turn4);
        assertTrue(turn4.hasInDomain(), "Turn 4 must be in-domain");
        assertFalse(turn4.isPureOutOfDomain(), "Turn 4 must not be pure OOD");
    }

    @Test
    @DisplayName("Active conversation context: Short contextual follow-up without horse nouns is preserved in-domain")
    void testActiveConversation_ContextualInquiry_PreservedInDomain() {
        DefaultIntentDecompositionService service = new DefaultIntentDecompositionService(
                "https://api.openai.com/v1",
                "",
                "gpt-6-luna",
                objectMapper,
                httpClient
        );

        List<AiChatMessageDto> history = List.of(
                new AiChatMessageDto("user", "Phân tích trạng thái Vet"),
                new AiChatMessageDto("assistant", "## Veterinarian Status Analysis\nHannah Reed had 33.3% rollover rate.")
        );

        // Follow-up without horse noun
        IntentDecompositionService.DecomposedIntents followUp = service.decompose("Tại sao lại như vậy?", "CLUB_MANAGER", history);
        assertNotNull(followUp);
        assertTrue(followUp.hasInDomain(), "Contextual follow-up in active conversation must be in-domain");
        assertFalse(followUp.isPureOutOfDomain());

        // BUT obvious out-of-domain query in active conversation MUST still be rejected
        IntentDecompositionService.DecomposedIntents oodInChat = service.decompose("Thời tiết hôm nay thế nào?", "CLUB_MANAGER", history);
        assertNotNull(oodInChat);
        assertFalse(oodInChat.hasInDomain(), "OOD query must not be in-domain even in active conversation");
        assertTrue(oodInChat.isPureOutOfDomain(), "OOD query must be pure OOD");
    }

    @Test
    @DisplayName("Safety Rescue: When classification API erroneously marks contextual follow-up as OOD, safety check rescues it to in-domain")
    @SuppressWarnings("unchecked")
    void testSafetyRescue_ContextualFollowUp_RescuedFromClassifierOod() throws Exception {
        String classifierPureOodJson = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"in_domain\\": [], \\"out_of_domain\\": [\\"Tại sao lại như vậy?\\"]}"
                      }
                    }
                  ]
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(classifierPureOodJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        DefaultIntentDecompositionService serviceWithKey = new DefaultIntentDecompositionService(
                "https://api.openai.com/v1",
                "test-api-key",
                "gpt-6-luna",
                objectMapper,
                httpClient
        );

        List<AiChatMessageDto> history = List.of(
                new AiChatMessageDto("user", "Phân tích trạng thái Vet"),
                new AiChatMessageDto("assistant", "Hannah Reed rollover rate is 33.3%.")
        );

        IntentDecompositionService.DecomposedIntents result = serviceWithKey.decompose("Tại sao lại như vậy?", "CLUB_MANAGER", history);
        assertNotNull(result);
        assertTrue(result.hasInDomain(), "Contextual follow-up in active conversation must be rescued into in-domain");
        assertFalse(result.isPureOutOfDomain(), "Contextual follow-up must not be pure OOD");
    }

    @Test
    @DisplayName("Domain Keywords: RTMS system, staff statistics, and management terms are recognized in-domain")
    void testDomainKeywords_SystemAndStaff_InDomain() {
        DefaultIntentDecompositionService service = new DefaultIntentDecompositionService(
                "https://api.openai.com/v1",
                "",
                "gpt-6-luna",
                objectMapper,
                httpClient
        );

        for (String query : List.of(
                "RTMS là gì?",
                "Thống kê nhân sự",
                "Có bao nhiêu nhân viên trong hệ thống?",
                "Quản lý có thể duyệt những đơn nào?"
        )) {
            IntentDecompositionService.DecomposedIntents res = service.decompose(query, "CLUB_MANAGER", List.of());
            assertNotNull(res);
            assertTrue(res.hasInDomain(), "Must be in-domain: " + query);
            assertFalse(res.isPureOutOfDomain(), "Must not be pure OOD: " + query);
        }
    }

    @Test
    @DisplayName("Fallback Heuristics: Compound query containing both horse keywords and OOD does NOT fail-closed to pure OOD")
    void testFallback_CompoundQuery_PreservesInDomain() {
        DefaultIntentDecompositionService service = new DefaultIntentDecompositionService(
                "https://api.openai.com/v1",
                "",
                "gpt-6-luna",
                objectMapper,
                httpClient
        );

        String compound = "Cho tôi xem danh sách ngựa và cho biết thời tiết hôm nay thế nào?";
        IntentDecompositionService.DecomposedIntents res = service.decompose(compound, "CLUB_MANAGER", List.of());
        assertNotNull(res);
        assertTrue(res.hasInDomain(), "Compound query must have in-domain intent");
        assertFalse(res.isPureOutOfDomain(), "Compound query must NOT fail-closed to pure OOD");
    }
}
