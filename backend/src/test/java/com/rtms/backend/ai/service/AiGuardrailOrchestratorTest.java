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

import com.rtms.backend.ai.dto.AiChatMessageDto;
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
                "gpt-6-luna",
                toolRegistryService,
                toolExecutionService,
                objectMapper
        );
    }

    @Test
    @DisplayName("Guardrail 1: System prompt includes the complete Domain Confinement and Out-of-Domain Refusal policies")
    void testSystemPrompt_DomainConfinementRulesPresent() {
        AuthenticatedUser owner = new AuthenticatedUser(10L, "owner@example.com", "HORSE_OWNER");
        String prompt = service.buildSystemPrompt(owner);

        assertNotNull(prompt);
        assertTrue(prompt.contains("16. CLOSED-WORLD WHITELIST DOMAIN CONFINEMENT"));
        assertTrue(prompt.contains("IN-DOMAIN WHITELIST"));
        assertTrue(prompt.contains("CLOSED-WORLD ASSUMPTION"));
        assertTrue(prompt.contains("MULTI-INTENT & COMPOUND QUERIES"));
        assertTrue(prompt.contains("My mother is Nga, what is the first letter of her name?"));
        assertTrue(prompt.contains("ABSOLUTELY FORBIDDEN to provide the answer or reasoning (never answer even a single letter or single word)"));
    }

    @Test
    @DisplayName("Guardrail 2: Domain ontology is loaded separately and accurately for all five roles (RBAC)")
    void testSystemPrompt_RoleBasedOntologyCoverage() {
        for (String role : new String[]{"HORSE_OWNER", "HEAD_TRAINER", "VETERINARIAN", "GROOM", "CLUB_MANAGER"}) {
            AuthenticatedUser user = new AuthenticatedUser(99L, role.toLowerCase() + "@rtms.com", role);
            String prompt = service.buildSystemPrompt(user);

            assertNotNull(prompt);
            assertTrue(prompt.contains("17. BUSINESS ENTITY MODEL & REASONING RULES FOR"),
                    "Role " + role + " must have section 17, Domain Ontology");
            assertTrue(prompt.contains(role) || prompt.contains(role.replace("HEAD_", "")),
                    "Prompt must contain the ontology keyword for role " + role);
        }
    }

    @Test
    @DisplayName("Guardrail 3: Verify the strict distinction between horses and stalls")
    void testSystemPrompt_StallsVsHorsesGuardrail() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");
        String prompt = service.buildSystemPrompt(manager);

        assertNotNull(prompt);
        assertTrue(prompt.contains("CLEARLY DISTINGUISH HORSE COUNT FROM STALL COUNT"));
        assertTrue(prompt.contains("NEVER equate or use the stall count to answer a question about the horse count"));
        assertTrue(prompt.contains("getStableOccupancyMetrics"));
    }

    @Test
    @DisplayName("Guardrail 4 (ReAct-Level): A pure out-of-domain fast short-circuit does not start the ReAct loop")
    void testReActLevel_PureOutOfDomain_ShortCircuit() {
        IntentDecompositionService mockDecomposer = mock(IntentDecompositionService.class);
        when(mockDecomposer.decompose(anyString(), anyString())).thenReturn(
                new IntentDecompositionService.DecomposedIntents(
                        Collections.emptyList(),
                        List.of("Solve the differential equation y'' + y = 0")
                )
        );

        AgentOrchestratorService serviceWithDecomposer = new AgentOrchestratorService(
                "https://api.openai.com/v1",
                "test-api-key",
                "gpt-6-luna",
                toolRegistryService,
                toolExecutionService,
                mockDecomposer,
                objectMapper
        );

        AuthenticatedUser trainer = new AuthenticatedUser(2L, "trainer@rtms.com", "HEAD_TRAINER");
        AiChatRequestDto request = new AiChatRequestDto("Solve the differential equation y'' + y = 0", List.of());

        AiChatResponseDto response = serviceWithDecomposer.processChat(request, trainer);

        assertNotNull(response);
        assertTrue(response.reply().contains("I am the RTMS AI Assistant, specialized exclusively in racehorse training and management operations"));
        assertTrue(response.reply().contains("Your request is outside my assigned operational scope, so I cannot assist with it."));
        assertTrue(response.executedTools().isEmpty());
        // Confirm that no tools were called or queried.
        verify(toolRegistryService, never()).getFunctionSpecsForRole(anyString());
        verify(toolExecutionService, never()).executeTool(anyString(), any(), any());
    }

    @Test
    @DisplayName("Guardrail 5 (ReAct-Level): Decompose a compound query, send only in-domain content to ReAct, and append an OOD refusal")
    void testReActLevel_CompoundQuery_AppendsOodRefusal() {
        IntentDecompositionService mockDecomposer = mock(IntentDecompositionService.class);
        when(mockDecomposer.decompose(anyString(), anyString())).thenReturn(
                new IntentDecompositionService.DecomposedIntents(
                        List.of("List my horses", "Show me how to become a horse owner"),
                        List.of("My mother is Nga, what is the first letter of her name?")
                )
        );

        AgentOrchestratorService serviceWithDecomposer = new AgentOrchestratorService(
                "https://api.openai.com/v1",
                "test-api-key",
                "gpt-6-luna",
                toolRegistryService,
                toolExecutionService,
                mockDecomposer,
                objectMapper
        );

        AuthenticatedUser owner = new AuthenticatedUser(10L, "owner@rtms.com", "HORSE_OWNER");
        AiChatRequestDto request = new AiChatRequestDto(
                "List my horses and show me how to become a horse owner. My mother is Nga, what is the first letter of her name?",
                List.of()
        );

        when(toolRegistryService.getFunctionSpecsForRole("HORSE_OWNER")).thenReturn(List.of());

        AiChatResponseDto response = serviceWithDecomposer.processChat(request, owner);

        assertNotNull(response);
        // Confirm that the OOD refusal is deterministically appended to the end of the reply.
        assertTrue(response.reply().contains("*Note: Regarding the out-of-scope request [My mother is Nga, what is the first letter of her name?], the RTMS AI Assistant is specialized in racehorse operations and management, and cannot assist with external requests.*"));
        assertTrue(response.reply().contains("cannot assist with external requests"));
    }

    @Test
    @DisplayName("Guardrail 6 (End-to-End Multi-Turn): Vietnamese queries, language directives, rollover thresholds, and alerts bypass pre-flight OOD refusal")
    void testMultiTurnConversation_GuardrailAllowsBilingualAndContextualQueries() {
        // Instantiate a real DefaultIntentDecompositionService with empty API key (deterministic heuristic/regex router)
        DefaultIntentDecompositionService realDecomposer = new DefaultIntentDecompositionService(
                "https://api.openai.com/v1",
                "",
                "gpt-6-luna",
                objectMapper
        );

        AgentOrchestratorService orchestrator = new AgentOrchestratorService(
                "https://api.openai.com/v1",
                "test-api-key",
                "gpt-6-luna",
                toolRegistryService,
                toolExecutionService,
                realDecomposer,
                objectMapper
        );

        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");
        when(toolRegistryService.getFunctionSpecsForRole("CLUB_MANAGER")).thenReturn(List.of());

        String oodSnippet = "outside my assigned operational scope";

        // Turn 1: "Phân tích trạng thái Vet"
        AiChatRequestDto req1 = new AiChatRequestDto("Phân tích trạng thái Vet", List.of());
        AiChatResponseDto res1 = orchestrator.processChat(req1, manager);
        assertNotNull(res1);
        assertFalse(res1.reply().contains(oodSnippet), "Turn 1 must NOT be rejected as out-of-domain");

        // History after Turn 1
        List<AiChatMessageDto> history1 = List.of(
                new AiChatMessageDto("user", "Phân tích trạng thái Vet"),
                new AiChatMessageDto("assistant", "## Veterinarian Status Analysis\nRollover rate is 22.1%...")
        );

        // Turn 2: "Trả lời tiếng Việt"
        AiChatRequestDto req2 = new AiChatRequestDto("Trả lời tiếng Việt", history1);
        AiChatResponseDto res2 = orchestrator.processChat(req2, manager);
        assertNotNull(res2);
        assertFalse(res2.reply().contains(oodSnippet), "Turn 2 (Language directive) must NOT be rejected as out-of-domain");

        // History after Turn 2
        List<AiChatMessageDto> history2 = List.of(
                new AiChatMessageDto("user", "Phân tích trạng thái Vet"),
                new AiChatMessageDto("assistant", "## Veterinarian Status Analysis..."),
                new AiChatMessageDto("user", "Trả lời tiếng Việt"),
                new AiChatMessageDto("assistant", "## Phân tích trạng thái Bác sĩ Thú y...")
        );

        // Turn 3: "Ngưỡng rollover rate là bao nhiêu?"
        AiChatRequestDto req3 = new AiChatRequestDto("Ngưỡng rollover rate là bao nhiêu?", history2);
        AiChatResponseDto res3 = orchestrator.processChat(req3, manager);
        assertNotNull(res3);
        assertFalse(res3.reply().contains(oodSnippet), "Turn 3 (Rollover rate threshold) must NOT be rejected as out-of-domain");

        // History after Turn 3
        List<AiChatMessageDto> history3 = List.of(
                new AiChatMessageDto("user", "Phân tích trạng thái Vet"),
                new AiChatMessageDto("assistant", "## Veterinarian Status Analysis..."),
                new AiChatMessageDto("user", "Trả lời tiếng Việt"),
                new AiChatMessageDto("assistant", "## Phân tích trạng thái Bác sĩ Thú y..."),
                new AiChatMessageDto("user", "Ngưỡng rollover rate là bao nhiêu?"),
                new AiChatMessageDto("assistant", "Ngưỡng rollover rate thông thường...")
        );

        // Turn 4: "Có alert nào trong hệ thống"
        AiChatRequestDto req4 = new AiChatRequestDto("Có alert nào trong hệ thống", history3);
        AiChatResponseDto res4 = orchestrator.processChat(req4, manager);
        assertNotNull(res4);
        assertFalse(res4.reply().contains(oodSnippet), "Turn 4 (System alert inquiry) must NOT be rejected as out-of-domain");

        // Turn 5: Contextual follow-up without horse nouns in active conversation
        AiChatRequestDto req5 = new AiChatRequestDto("Tại sao lại như vậy?", history3);
        AiChatResponseDto res5 = orchestrator.processChat(req5, manager);
        assertNotNull(res5);
        assertFalse(res5.reply().contains(oodSnippet), "Turn 5 (Contextual follow-up 'Tại sao lại như vậy?') must NOT be rejected as out-of-domain");

        // Turn 6: Obvious OOD in active conversation must STILL be rejected by pre-flight guardrail
        AiChatRequestDto req6 = new AiChatRequestDto("Thời tiết hôm nay thế nào?", history3);
        AiChatResponseDto res6 = orchestrator.processChat(req6, manager);
        assertNotNull(res6);
        assertTrue(res6.reply().contains(oodSnippet), "Obvious OOD question in active conversation MUST still be rejected");

        // Turn 7: Greeting combined with language switch directive (Turn 1, no history)
        AiChatRequestDto req7 = new AiChatRequestDto("Xin chào, hãy trả lời bằng tiếng Việt nhé", List.of());
        AiChatResponseDto res7 = orchestrator.processChat(req7, manager);
        assertNotNull(res7);
        assertFalse(res7.reply().contains(oodSnippet), "Greeting with language switch directive must NOT be rejected as out-of-domain");

        // Turn 8: System identity & guidance inquiry
        AiChatRequestDto req8 = new AiChatRequestDto("RTMS là gì?", List.of());
        AiChatResponseDto res8 = orchestrator.processChat(req8, manager);
        assertNotNull(res8);
        assertFalse(res8.reply().contains(oodSnippet), "RTMS system guidance inquiry must NOT be rejected as out-of-domain");
    }
}
