package com.rtms.backend.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.ai.tool.AiToolExecutionService;
import com.rtms.backend.ai.tool.DynamicToolRegistryService;
import com.rtms.backend.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiGuardrailOrchestratorTest {

    private DynamicToolRegistryService toolRegistryService;
    private AiToolExecutionService toolExecutionService;
    private ObjectMapper objectMapper;
    private AgentOrchestratorService service;

    @BeforeEach
    void setUp() {
        toolRegistryService = Mockito.mock(DynamicToolRegistryService.class);
        toolExecutionService = Mockito.mock(AiToolExecutionService.class);
        objectMapper = new ObjectMapper();
        service = new AgentOrchestratorService(
                "https://api.openai.com/v1",
                "test-api-key",
                "gpt-4o-mini",
                toolRegistryService,
                toolExecutionService,
                objectMapper
        );
    }

    @Test
    @DisplayName("Guardrail 1: System prompt chứa đầy đủ chính sách Domain Confinement và Out-of-Domain Refusal")
    void testSystemPrompt_DomainConfinementRulesPresent() {
        AuthenticatedUser owner = new AuthenticatedUser(10L, "owner@example.com", "HORSE_OWNER");
        String prompt = service.buildSystemPrompt(owner);

        assertNotNull(prompt);
        assertTrue(prompt.contains("16. NGUYÊN TẮC THẾ GIỚI ĐÓNG & GIỚI HẠN PHẠM VI NGHIỆP VỤ TUYỆT ĐỐI"));
        assertTrue(prompt.contains("IN-DOMAIN WHITELIST"));
        assertTrue(prompt.contains("CLOSED-WORLD ASSUMPTION"));
        assertTrue(prompt.contains("MULTI-INTENT & COMPOUND QUERIES"));
        assertTrue(prompt.contains("Mẹ tôi là Nga, hỏi chữ cái đầu tiên trong tên bà là gì?"));
        assertTrue(prompt.contains("TUYỆT ĐỐI KHÔNG trả lời đáp án dù chỉ là 1 chữ cái hay 1 từ đơn"));
    }

    @Test
    @DisplayName("Guardrail 2: Domain Ontology được nạp riêng biệt và chính xác cho cả 5 Vai trò (RBAC)")
    void testSystemPrompt_RoleBasedOntologyCoverage() {
        for (String role : new String[]{"HORSE_OWNER", "HEAD_TRAINER", "VETERINARIAN", "GROOM", "CLUB_MANAGER"}) {
            AuthenticatedUser user = new AuthenticatedUser(99L, role.toLowerCase() + "@rtms.com", role);
            String prompt = service.buildSystemPrompt(user);

            assertNotNull(prompt);
            assertTrue(prompt.contains("17. SƠ ĐỒ THỰC THỂ NGHIỆP VỤ & QUY TẮC SUY LUẬN CHO"),
                    "Role " + role + " phải có mục 17 Domain Ontology");
            assertTrue(prompt.contains(role) || prompt.contains(role.replace("HEAD_", "")),
                    "Prompt phải chứa từ khóa ontology của role " + role);
        }
    }

    @Test
    @DisplayName("Guardrail 3: Xác thực quy tắc phân tách nghiêm ngặt giữa Ngựa và Chuồng trại")
    void testSystemPrompt_StallsVsHorsesGuardrail() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");
        String prompt = service.buildSystemPrompt(manager);

        assertNotNull(prompt);
        assertTrue(prompt.contains("PHÂN BIỆT RÕ RÀNG GIỮA SỐ LƯỢNG NGỰA VÀ SỐ LƯỢNG CHUỒNG TRẠI (STALLS)"));
        assertTrue(prompt.contains("TUYỆT ĐỐI KHÔNG đánh đồng hoặc lấy số lượng chuồng trại để trả lời thay cho số lượng ngựa"));
        assertTrue(prompt.contains("getStableOccupancyMetrics"));
    }
}
