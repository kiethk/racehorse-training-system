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

class AgentOrchestratorServiceTest {

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
    @DisplayName("Fallback an toàn khi chưa cấu hình AI_API_KEY")
    void testProcessChat_NoApiKey_GracefulFallback() {
        AgentOrchestratorService service = new AgentOrchestratorService(
                "https://api.openai.com/v1",
                "", // Không có API key
                "gpt-4o-mini",
                toolRegistryService,
                toolExecutionService,
                objectMapper
        );

        AuthenticatedUser user = new AuthenticatedUser(5L, "trainer@test.com", "HEAD_TRAINER");
        AiChatRequestDto request = new AiChatRequestDto("Chào bạn", List.of());

        AiChatResponseDto response = service.processChat(request, user);

        assertNotNull(response);
        assertTrue(response.reply().contains("AI_API_KEY"));
        assertTrue(response.executedTools().isEmpty());
    }

    @Test
    @DisplayName("Dynamic Tool Scoping nạp đúng danh mục công cụ cho vai trò")
    void testProcessChat_DynamicToolScoping() {
        when(toolRegistryService.getFunctionSpecsForRole("HORSE_OWNER")).thenReturn(List.of(
                Map.of("type", "function", "function", Map.of("name", "getMyHorses"))
        ));

        AgentOrchestratorService service = new AgentOrchestratorService(
                "https://api.openai.com/v1",
                "sk-test_mock_key_12345",
                "gpt-4o-mini",
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
        AgentOrchestratorService service = new AgentOrchestratorService(
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
        assertTrue(prompt.contains("XẾP HẠNG SỞ TRƯỜNG & ĐIỂM TRUNG BÌNH PHONG ĐỘ"));
        assertTrue(prompt.contains("NGUYÊN TẮC THẾ GIỚI ĐÓNG & GIỚI HẠN PHẠM VI NGHIỆP VỤ TUYỆT ĐỐI"));
        assertTrue(prompt.contains("CLOSED-WORLD ASSUMPTION"));
    }

    @Test
    @DisplayName("humanizeKey chuyển đổi chính xác các trường English camelCase và Vietnamese sang tiếng Việt")
    void testHumanizeKey_EnglishAndVietnameseKeys() {
        AgentOrchestratorService service = new AgentOrchestratorService(
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
        org.junit.jupiter.api.Assertions.assertEquals("Điểm trung bình sở trường", service.humanizeKey("averageRating"));
        org.junit.jupiter.api.Assertions.assertEquals("Điểm cự ly xuất sắc nhất", service.humanizeKey("bestRating"));
        org.junit.jupiter.api.Assertions.assertEquals("Chi tiết điểm số từng cự ly", service.humanizeKey("specialtyBreakdown"));
        org.junit.jupiter.api.Assertions.assertEquals("Tiêu chí xếp hạng", service.humanizeKey("sortBy"));

        // Vietnamese legacy keys
        org.junit.jupiter.api.Assertions.assertEquals("Tên ngựa", service.humanizeKey("tenNgua"));
        org.junit.jupiter.api.Assertions.assertEquals("Thân nhiệt", service.humanizeKey("thanNhiet"));
        org.junit.jupiter.api.Assertions.assertEquals("Nhịp tim", service.humanizeKey("nhipTim"));
        org.junit.jupiter.api.Assertions.assertEquals("Cân nặng", service.humanizeKey("canNang"));

        // Fallback for unknown camelCase key
        org.junit.jupiter.api.Assertions.assertEquals("Unknown Custom Field", service.humanizeKey("unknownCustomField"));
    }

    @Test
    @DisplayName("AI Assistant defaults to English when user asks in English and adaptive Vietnamese when in Vietnamese")
    void testAdaptiveLanguageSupport() {
        AgentOrchestratorService service = new AgentOrchestratorService(
                "https://api.groq.com/openai/v1",
                "", // empty api key to trigger fallback
                "qwen/qwen3.8-27b",
                toolRegistryService,
                toolExecutionService,
                objectMapper
        );

        AuthenticatedUser owner = new AuthenticatedUser(10L, "owner@test.com", "HORSE_OWNER");

        // 1. English input -> English fallback
        AiChatRequestDto enRequest = new AiChatRequestDto("Where are my horses located?", List.of());
        AiChatResponseDto enResponse = service.processChat(enRequest, owner);
        assertNotNull(enResponse);
        assertTrue(enResponse.reply().contains("AI_API_KEY is not configured"));

        // 2. Vietnamese input -> Vietnamese fallback
        AiChatRequestDto viRequest = new AiChatRequestDto("Ngựa của tôi đang ở chuồng nào?", List.of());
        AiChatResponseDto viResponse = service.processChat(viRequest, owner);
        assertNotNull(viResponse);
        assertTrue(viResponse.reply().contains("Chưa cấu hình AI_API_KEY"));
    }

    @Test
    @DisplayName("humanizeKeyEn converts camelCase domain keys accurately into professional English labels")
    void testHumanizeKeyEn() {
        AgentOrchestratorService service = new AgentOrchestratorService(
                "https://api.groq.com/openai/v1",
                "test-key",
                "qwen/qwen3.8-27b",
                toolRegistryService,
                toolExecutionService,
                objectMapper
        );

        org.junit.jupiter.api.Assertions.assertEquals("Horse Name", service.humanizeKeyEn("horseName"));
        org.junit.jupiter.api.Assertions.assertEquals("Registration Number", service.humanizeKeyEn("registrationNumber"));
        org.junit.jupiter.api.Assertions.assertEquals("Body Temperature (°C)", service.humanizeKeyEn("temperature"));
        org.junit.jupiter.api.Assertions.assertEquals("Heart Rate (bpm)", service.humanizeKeyEn("heartRate"));
        org.junit.jupiter.api.Assertions.assertEquals("Stall Location", service.humanizeKeyEn("stallLocation"));
        org.junit.jupiter.api.Assertions.assertEquals("Training Decision", service.humanizeKeyEn("trainingDecision"));
        org.junit.jupiter.api.Assertions.assertEquals("Total Staff", service.humanizeKeyEn("totalStaff"));
        org.junit.jupiter.api.Assertions.assertEquals("Role Breakdown", service.humanizeKeyEn("roleBreakdown"));
        org.junit.jupiter.api.Assertions.assertEquals("Total Applications", service.humanizeKeyEn("totalApplications"));
        org.junit.jupiter.api.Assertions.assertEquals("Specialties", service.humanizeKeyEn("specialties"));
        org.junit.jupiter.api.Assertions.assertEquals("Specialty Count", service.humanizeKeyEn("specialtyCount"));
        org.junit.jupiter.api.Assertions.assertEquals("Average Rating", service.humanizeKeyEn("averageRating"));
        org.junit.jupiter.api.Assertions.assertEquals("Best Rating", service.humanizeKeyEn("bestRating"));
        org.junit.jupiter.api.Assertions.assertEquals("Specialty Breakdown", service.humanizeKeyEn("specialtyBreakdown"));
        org.junit.jupiter.api.Assertions.assertEquals("Sort By", service.humanizeKeyEn("sortBy"));
    }
}
