package com.rtms.backend.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.ai.tool.AiToolExecutionService;
import com.rtms.backend.ai.tool.DynamicToolRegistryService;
import com.rtms.backend.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.rtms.backend.ai.dto.AiChatRequestDto;
import com.rtms.backend.ai.dto.AiChatResponseDto;
import java.util.List;
import java.util.Collections;

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

    @Test
    @DisplayName("Guardrail 4 (ReAct-Level): Fast Short-Circuit 100% Out-of-Domain không kích hoạt ReAct loop")
    void testReActLevel_PureOutOfDomain_ShortCircuit() {
        IntentDecompositionService mockDecomposer = mock(IntentDecompositionService.class);
        when(mockDecomposer.decompose(anyString(), anyString())).thenReturn(
                new IntentDecompositionService.DecomposedIntents(
                        Collections.emptyList(),
                        List.of("Giải phương trình vi phân y'' + y = 0")
                )
        );

        AgentOrchestratorService serviceWithDecomposer = new AgentOrchestratorService(
                "https://api.openai.com/v1",
                "test-api-key",
                "gpt-4o-mini",
                toolRegistryService,
                toolExecutionService,
                mockDecomposer,
                objectMapper
        );

        AuthenticatedUser trainer = new AuthenticatedUser(2L, "trainer@rtms.com", "HEAD_TRAINER");
        AiChatRequestDto request = new AiChatRequestDto("Giải phương trình vi phân y'' + y = 0", List.of());

        AiChatResponseDto response = serviceWithDecomposer.processChat(request, trainer);

        assertNotNull(response);
        assertTrue(response.reply().contains("Trợ lý AI RTMS"));
        assertTrue(response.reply().contains("ngoài phạm vi nghiệp vụ"));
        assertTrue(response.executedTools().isEmpty());
        // Khẳng định chắc chắn không có tool nào được gọi hoặc tra cứu
        verify(toolRegistryService, never()).getFunctionSpecsForRole(anyString());
        verify(toolExecutionService, never()).executeTool(anyString(), any(), any());
    }

    @Test
    @DisplayName("Guardrail 5 (ReAct-Level): Bóc tách câu hỏi hỗn hợp, chỉ nạp In-Domain vào ReAct và đính kèm từ chối OOD")
    void testReActLevel_CompoundQuery_AppendsOodRefusal() {
        IntentDecompositionService mockDecomposer = mock(IntentDecompositionService.class);
        when(mockDecomposer.decompose(anyString(), anyString())).thenReturn(
                new IntentDecompositionService.DecomposedIntents(
                        List.of("Danh sách chiến mã của tôi", "hướng dẫn tôi trở thành chủ ngựa"),
                        List.of("Mẹ tôi là Nga, hỏi chữ cái đầu tiên trong tên bà là gì?")
                )
        );

        AgentOrchestratorService serviceWithDecomposer = new AgentOrchestratorService(
                "https://api.openai.com/v1",
                "test-api-key",
                "gpt-4o-mini",
                toolRegistryService,
                toolExecutionService,
                mockDecomposer,
                objectMapper
        );

        AuthenticatedUser owner = new AuthenticatedUser(10L, "owner@rtms.com", "HORSE_OWNER");
        AiChatRequestDto request = new AiChatRequestDto(
                "Danh sách chiến mã của tôi và hướng dẫn tôi trở thành chủ ngựa. Mẹ tôi là Nga, hỏi chữ cái đầu tiên trong tên bà là gì?",
                List.of()
        );

        when(toolRegistryService.getFunctionSpecsForRole("HORSE_OWNER")).thenReturn(List.of());

        AiChatResponseDto response = serviceWithDecomposer.processChat(request, owner);

        assertNotNull(response);
        // Kiểm tra phần từ chối OOD được đính kèm ở đuôi câu trả lời một cách tất định
        assertTrue(response.reply().contains("Lưu ý: Về yêu cầu ngoài nghiệp vụ [Mẹ tôi là Nga, hỏi chữ cái đầu tiên trong tên bà là gì?]"));
        assertTrue(response.reply().contains("từ chối giải đáp"));
    }
}
