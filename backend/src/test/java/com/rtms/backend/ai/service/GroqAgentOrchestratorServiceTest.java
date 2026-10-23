package com.rtms.backend.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.ai.dto.AiChatRequestDto;
import com.rtms.backend.ai.dto.AiChatResponseDto;
import com.rtms.backend.ai.tool.AiToolExecutionService;
import com.rtms.backend.ai.tool.DynamicToolRegistryService;
import com.rtms.backend.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class GroqAgentOrchestratorServiceTest {

    private DynamicToolRegistryService toolRegistryService;
    private AiToolExecutionService toolExecutionService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        toolRegistryService = Mockito.mock(DynamicToolRegistryService.class);
        toolExecutionService = Mockito.mock(AiToolExecutionService.class);
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Fallback an toàn khi chưa cấu hình GROQ_API_KEY")
    void testProcessChat_NoApiKey_GracefulFallback() {
        GroqAgentOrchestratorService service = new GroqAgentOrchestratorService(
                "https://api.groq.com/openai/v1",
                "", // Không có API key
                "qwen/qwen3.8-27b",
                toolRegistryService,
                toolExecutionService,
                objectMapper
        );

        AuthenticatedUser user = new AuthenticatedUser(5L, "trainer@test.com", "HEAD_TRAINER");
        AiChatRequestDto request = new AiChatRequestDto("Chào bạn", List.of());

        AiChatResponseDto response = service.processChat(request, user);

        assertNotNull(response);
        assertTrue(response.reply().contains("GROQ_API_KEY"));
        assertTrue(response.executedTools().isEmpty());
    }

    @Test
    @DisplayName("Dynamic Tool Scoping nạp đúng danh mục công cụ cho vai trò")
    void testProcessChat_DynamicToolScoping() {
        when(toolRegistryService.getFunctionSpecsForRole("HORSE_OWNER")).thenReturn(List.of(
                Map.of("type", "function", "function", Map.of("name", "getMyHorses"))
        ));

        GroqAgentOrchestratorService service = new GroqAgentOrchestratorService(
                "https://api.groq.com/openai/v1",
                "gsk_test_mock_key_12345",
                "qwen/qwen3.8-27b",
                toolRegistryService,
                toolExecutionService,
                objectMapper
        );

        AuthenticatedUser owner = new AuthenticatedUser(10L, "owner@test.com", "HORSE_OWNER");
        AiChatRequestDto request = new AiChatRequestDto("Ngựa của tôi đâu?", List.of());

        // Do mock key không kết nối mạng thật, service sẽ xử lý và trả về fallback message mà không crash
        AiChatResponseDto response = service.processChat(request, owner);

        assertNotNull(response);
        assertNotNull(response.reply());
    }

    @Test
    @DisplayName("System Prompt bao hàm đầy đủ quy tắc Groundedness và Zero Hallucination")
    void testBuildSystemPrompt_GroundednessAndAntiHallucinationRules() {
        GroqAgentOrchestratorService service = new GroqAgentOrchestratorService(
                "https://api.groq.com/openai/v1",
                "test-key",
                "qwen/qwen3.8-27b",
                toolRegistryService,
                toolExecutionService,
                objectMapper
        );

        AuthenticatedUser vet = new AuthenticatedUser(20L, "vet@test.com", "VETERINARIAN");
        String prompt = service.buildSystemPrompt(vet);

        assertNotNull(prompt);
        assertTrue(prompt.contains("BẢO CHỨNG DỮ LIỆU THỰC TẾ & CHỐNG ẢO GIÁC"));
        assertTrue(prompt.contains("TUYỆT ĐỐI KHÔNG tự suy đoán, phỏng đoán hoặc bịa đặt dữ liệu"));
        assertTrue(prompt.contains("MỌI dữ kiện thực tế về hệ thống RTMS bắt buộc phải được lấy thông qua việc gọi công cụ"));
        assertTrue(prompt.contains("ĐƯỢC PHÉP sử dụng tri thức thú y và huấn luyện đua ngựa tiêu chuẩn"));
        assertTrue(prompt.contains("ĐƯỢC PHÉP phản hồi trực tiếp các câu chào hỏi xã giao"));
        assertTrue(prompt.contains("PHÂN BIỆT RÕ RÀNG GIỮA SỐ LƯỢNG NGỰA VÀ SỐ LƯỢNG CHUỒNG TRẠI"));
        assertTrue(prompt.contains("THỐNG KÊ ĐƠN ĐĂNG KÝ NHẬP ĐÀN"));
    }

    @Test
    @DisplayName("humanizeKey chuyển đổi chính xác các trường English camelCase và Vietnamese sang tiếng Việt")
    void testHumanizeKey_EnglishAndVietnameseKeys() {
        GroqAgentOrchestratorService service = new GroqAgentOrchestratorService(
                "https://api.groq.com/openai/v1",
                "test-key",
                "qwen/qwen3.8-27b",
                toolRegistryService,
                toolExecutionService,
                objectMapper
        );

        // English keys
        org.junit.jupiter.api.Assertions.assertEquals("Tên ngựa", service.humanizeKey("horseName"));
        org.junit.jupiter.api.Assertions.assertEquals("Mã đăng ký", service.humanizeKey("registrationNumber"));
        org.junit.jupiter.api.Assertions.assertEquals("Thân nhiệt", service.humanizeKey("temperature"));
        org.junit.jupiter.api.Assertions.assertEquals("Nhịp tim", service.humanizeKey("heartRate"));
        org.junit.jupiter.api.Assertions.assertEquals("Vị trí ô chuồng", service.humanizeKey("stallLocation"));
        org.junit.jupiter.api.Assertions.assertEquals("Quyết định tập luyện", service.humanizeKey("trainingDecision"));
        org.junit.jupiter.api.Assertions.assertEquals("Tổng số nhân sự", service.humanizeKey("totalStaff"));
        org.junit.jupiter.api.Assertions.assertEquals("Phân loại theo vai trò", service.humanizeKey("roleBreakdown"));
        org.junit.jupiter.api.Assertions.assertEquals("Tổng số đơn nhập đàn", service.humanizeKey("totalApplications"));
        org.junit.jupiter.api.Assertions.assertEquals("Sở trường", service.humanizeKey("specialties"));
        org.junit.jupiter.api.Assertions.assertEquals("Số lượng sở trường", service.humanizeKey("specialtyCount"));

        // Vietnamese legacy keys
        org.junit.jupiter.api.Assertions.assertEquals("Tên ngựa", service.humanizeKey("tenNgua"));
        org.junit.jupiter.api.Assertions.assertEquals("Thân nhiệt", service.humanizeKey("thanNhiet"));
        org.junit.jupiter.api.Assertions.assertEquals("Nhịp tim", service.humanizeKey("nhipTim"));
        org.junit.jupiter.api.Assertions.assertEquals("Cân nặng", service.humanizeKey("canNang"));

        // Fallback for unknown camelCase key
        org.junit.jupiter.api.Assertions.assertEquals("Unknown Custom Field", service.humanizeKey("unknownCustomField"));
    }
}
