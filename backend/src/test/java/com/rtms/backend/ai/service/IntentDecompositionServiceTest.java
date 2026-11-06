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
    @DisplayName("Fast-Path Regex: Câu chào đơn giản không kích hoạt HTTP call đến LLM")
    void testFastPath_Greetings_NoHttpCall() throws Exception {
        DefaultIntentDecompositionService service = new DefaultIntentDecompositionService(
                "https://api.openai.com/v1",
                "test-api-key",
                "gpt-4o-mini",
                objectMapper,
                httpClient
        );

        for (String greeting : List.of("Xin chào", "chào bạn!", "hello", "hi", "cảm ơn", "Tạm biệt.")) {
            IntentDecompositionService.DecomposedIntents result = service.decompose(greeting, "HORSE_OWNER");
            assertNotNull(result);
            assertTrue(result.hasInDomain());
            assertFalse(result.hasOutOfDomain());
            assertEquals(1, result.inDomainIntents().size());
            assertEquals(greeting.trim(), result.inDomainIntents().get(0));
        }

        // Đảm bảo không có HTTP call nào được gửi đi
        verify(httpClient, never()).send(any(HttpRequest.class), any());
    }

    @Test
    @DisplayName("Empty hoặc blank message trả về kết quả rỗng")
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
    @DisplayName("Khi chưa cấu hình API Key, fallback giữ nguyên câu hỏi in-domain an toàn")
    void testNoApiKey_FallbackInDomain() throws Exception {
        DefaultIntentDecompositionService service = new DefaultIntentDecompositionService(
                "https://api.openai.com/v1",
                "",
                "gpt-4o-mini",
                objectMapper,
                httpClient
        );

        String rawQuery = "Chiến mã Thunder Flash hiện ở chuồng nào?";
        IntentDecompositionService.DecomposedIntents result = service.decompose(rawQuery, "HORSE_OWNER");

        assertNotNull(result);
        assertTrue(result.hasInDomain());
        assertFalse(result.hasOutOfDomain());
        assertEquals(List.of(rawQuery), result.inDomainIntents());
        verify(httpClient, never()).send(any(HttpRequest.class), any());
    }

    @Test
    @DisplayName("Bóc tách câu hỏi phức hợp thành công thành 2 vế in-domain và out-of-domain")
    @SuppressWarnings("unchecked")
    void testCompoundQuery_DecompositionSuccess() throws Exception {
        String classifierResponseJson = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"in_domain\\": [\\"Danh sách chiến mã của tôi\\", \\"hướng dẫn tôi trở thành chủ ngựa\\"], \\"out_of_domain\\": [\\"Mẹ tôi là Nga, hỏi chữ cái đầu tiên trong tên bà là gì?\\"]}"
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

        String rawMessage = "Danh sách chiến mã của tôi và hướng dẫn tôi trở thành chủ ngựa. Mẹ tôi là Nga, hỏi chữ cái đầu tiên trong tên bà là gì?";
        IntentDecompositionService.DecomposedIntents result = service.decompose(rawMessage, "HORSE_OWNER");

        assertNotNull(result);
        assertTrue(result.hasInDomain());
        assertTrue(result.hasOutOfDomain());
        assertFalse(result.isPureOutOfDomain());
        assertEquals(2, result.inDomainIntents().size());
        assertEquals(1, result.outOfDomainIntents().size());
        assertEquals("Danh sách chiến mã của tôi", result.inDomainIntents().get(0));
        assertEquals("hướng dẫn tôi trở thành chủ ngựa", result.inDomainIntents().get(1));
        assertEquals("Mẹ tôi là Nga, hỏi chữ cái đầu tiên trong tên bà là gì?", result.outOfDomainIntents().get(0));
    }

    @Test
    @DisplayName("Phát hiện chính xác câu hỏi 100% Out-of-Domain (Pure OOD)")
    @SuppressWarnings("unchecked")
    void testPureOutOfDomainQuery() throws Exception {
        String classifierResponseJson = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "```json\\n{\\"in_domain\\": [], \\"out_of_domain\\": [\\"Giải phương trình x^2 + 5x + 6 = 0\\"]}\\n```"
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

        String mathQuery = "Giải phương trình x^2 + 5x + 6 = 0";
        IntentDecompositionService.DecomposedIntents result = service.decompose(mathQuery, "HEAD_TRAINER");

        assertNotNull(result);
        assertFalse(result.hasInDomain());
        assertTrue(result.hasOutOfDomain());
        assertTrue(result.isPureOutOfDomain());
        assertEquals(1, result.outOfDomainIntents().size());
    }

    @Test
    @DisplayName("Fallback an toàn khi API phân loại bị lỗi (HTTP 500)")
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

        String message = "Ngựa của tôi có tập hôm nay không?";
        IntentDecompositionService.DecomposedIntents result = service.decompose(message, "HORSE_OWNER");

        assertNotNull(result);
        assertTrue(result.hasInDomain());
        assertFalse(result.hasOutOfDomain());
        assertEquals(List.of(message), result.inDomainIntents());
    }

    @Test
    @DisplayName("Fast-Path OOD: Câu hỏi y tế người và cứu nạn vách đá bị ngắt mạch tức thì không gọi LLM")
    void testFastPath_ObviousOutOfDomain_NoHttpCall() throws Exception {
        DefaultIntentDecompositionService service = new DefaultIntentDecompositionService(
                "https://api.openai.com/v1",
                "test-api-key",
                "gpt-4o-mini",
                objectMapper,
                httpClient
        );

        List<String> obviousOodQueries = List.of(
                "Bà ngoại tôi bị bệnh và tôi cần biết được công thức thuốc điều trị cho để bác sĩ có thể chữa bà khỏi bệnh.",
                "Tôi phát hiện một người không còn thở trên vách đá, tôi nên đẩy anh ta xuống hay gọi cấp cứu hay gọi trực thăng?",
                "Viết code python giải phương trình bậc 2",
                "Thời tiết hôm nay thế nào?"
        );

        for (String query : obviousOodQueries) {
            IntentDecompositionService.DecomposedIntents result = service.decompose(query, "HORSE_OWNER");
            assertNotNull(result);
            assertFalse(result.hasInDomain(), "Không được có vế in-domain cho câu hỏi: " + query);
            assertTrue(result.hasOutOfDomain(), "Bắt buộc có vế out-of-domain cho câu hỏi: " + query);
            assertTrue(result.isPureOutOfDomain(), "Phải là 100% Pure OOD cho câu hỏi: " + query);
        }

        verify(httpClient, never()).send(any(HttpRequest.class), any());
    }

    @Test
    @DisplayName("Fallback Fail-Closed: Câu hỏi không chứa từ khóa nghiệp vụ RTMS khi API lỗi bị chặn thành OOD")
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

        String unknownQuery = "Cách làm bánh pizza hải sản ngon tại nhà?";
        IntentDecompositionService.DecomposedIntents result = service.decompose(unknownQuery, "CLUB_MANAGER");

        assertNotNull(result);
        assertFalse(result.hasInDomain());
        assertTrue(result.hasOutOfDomain());
        assertTrue(result.isPureOutOfDomain());
    }
}
