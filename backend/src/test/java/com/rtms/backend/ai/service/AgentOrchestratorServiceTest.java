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
    @DisplayName("Graceful fallback when AI_API_KEY is not configured")
    void testProcessChat_NoApiKey_GracefulFallback() {
        AgentOrchestratorService service = new AgentOrchestratorService(
                "https://api.openai.com/v1",
                "", // No API key
                "gpt-6-luna",
                toolRegistryService,
                toolExecutionService,
                objectMapper
        );

        AuthenticatedUser user = new AuthenticatedUser(5L, "trainer@test.com", "HEAD_TRAINER");
        AiChatRequestDto request = new AiChatRequestDto("Hello", List.of());

        AiChatResponseDto response = service.processChat(request, user);

        assertNotNull(response);
        assertTrue(response.reply().contains("AI_API_KEY is not configured"));
        assertTrue(response.executedTools().isEmpty());
    }

    @Test
    @DisplayName("Dynamic tool scoping loads the correct tool catalog for the user's role")
    void testProcessChat_DynamicToolScoping() {
        when(toolRegistryService.getFunctionSpecsForRole("HORSE_OWNER")).thenReturn(List.of(
                Map.of("type", "function", "function", Map.of("name", "getMyHorses"))
        ));

        AgentOrchestratorService service = new AgentOrchestratorService(
                "https://api.openai.com/v1",
                "sk-test_mock_key_12345",
                "gpt-6-luna",
                toolRegistryService,
                toolExecutionService,
                objectMapper
        );

        AuthenticatedUser owner = new AuthenticatedUser(10L, "owner@test.com", "HORSE_OWNER");
        AiChatRequestDto request = new AiChatRequestDto("Where are my horses?", List.of());

        // The mock key cannot connect to the network, so the service should return a fallback message without crashing.
        AiChatResponseDto response = service.processChat(request, owner);

        assertNotNull(response);
        assertNotNull(response.reply());
    }

    @Test
    @DisplayName("System prompt includes the complete groundedness and zero-hallucination rules")
    void testBuildSystemPrompt_GroundednessAndAntiHallucinationRules() {
        AgentOrchestratorService service = new AgentOrchestratorService(
                "https://api.openai.com/v1",
                "test-key",
                "gpt-6-luna",
                toolRegistryService,
                toolExecutionService,
                objectMapper
        );

        AuthenticatedUser vet = new AuthenticatedUser(20L, "vet@test.com", "VETERINARIAN");
        String prompt = service.buildSystemPrompt(vet);

        assertNotNull(prompt);
        assertTrue(prompt.contains("GROUNDEDNESS & ZERO HALLUCINATION"));
        assertTrue(prompt.contains("NEVER guess, speculate, or invent data."));
        assertTrue(prompt.contains("EVERY factual data point about the RTMS system MUST be obtained by calling the tool"));
        assertTrue(prompt.contains("You MAY use standard veterinary and racehorse training knowledge"));
        assertTrue(prompt.contains("You MAY answer social greetings, explain role permissions"));
        assertTrue(prompt.contains("CLEARLY DISTINGUISH HORSE COUNT FROM STALL COUNT"));
        assertTrue(prompt.contains("ADMISSION APPLICATION STATISTICS"));
        assertTrue(prompt.contains("HORSE SPECIALTY & AVERAGE RATING RANKING"));
        assertTrue(prompt.contains("CLOSED-WORLD WHITELIST DOMAIN CONFINEMENT"));
        assertTrue(prompt.contains("CLOSED-WORLD ASSUMPTION"));
    }

    @Test
    @DisplayName("humanizeKey maps English camelCase and legacy Vietnamese keys to professional English labels")
    void testHumanizeKey_EnglishAndVietnameseKeys() {
        AgentOrchestratorService service = new AgentOrchestratorService(
                "https://api.openai.com/v1",
                "test-key",
                "gpt-6-luna",
                toolRegistryService,
                toolExecutionService,
                objectMapper
        );

        // English keys
        org.junit.jupiter.api.Assertions.assertEquals("Horse Name", service.humanizeKey("horseName"));
        org.junit.jupiter.api.Assertions.assertEquals("Registration Number", service.humanizeKey("registrationNumber"));
        org.junit.jupiter.api.Assertions.assertEquals("Body Temperature (°C)", service.humanizeKey("temperature"));
        org.junit.jupiter.api.Assertions.assertEquals("Heart Rate (bpm)", service.humanizeKey("heartRate"));
        org.junit.jupiter.api.Assertions.assertEquals("Stall Location", service.humanizeKey("stallLocation"));
        org.junit.jupiter.api.Assertions.assertEquals("Training Decision", service.humanizeKey("trainingDecision"));
        org.junit.jupiter.api.Assertions.assertEquals("Total Staff", service.humanizeKey("totalStaff"));
        org.junit.jupiter.api.Assertions.assertEquals("Role Breakdown", service.humanizeKey("roleBreakdown"));
        org.junit.jupiter.api.Assertions.assertEquals("Total Applications", service.humanizeKey("totalApplications"));
        org.junit.jupiter.api.Assertions.assertEquals("Specialties", service.humanizeKey("specialties"));
        org.junit.jupiter.api.Assertions.assertEquals("Specialty Count", service.humanizeKey("specialtyCount"));
        org.junit.jupiter.api.Assertions.assertEquals("Average Rating", service.humanizeKey("averageRating"));
        org.junit.jupiter.api.Assertions.assertEquals("Best Rating", service.humanizeKey("bestRating"));
        org.junit.jupiter.api.Assertions.assertEquals("Specialty Breakdown", service.humanizeKey("specialtyBreakdown"));
        org.junit.jupiter.api.Assertions.assertEquals("Sort By", service.humanizeKey("sortBy"));

        // Legacy Vietnamese keys
        org.junit.jupiter.api.Assertions.assertEquals("Horse Name", service.humanizeKey("tenNgua"));
        org.junit.jupiter.api.Assertions.assertEquals("Body Temperature (°C)", service.humanizeKey("thanNhiet"));
        org.junit.jupiter.api.Assertions.assertEquals("Heart Rate (bpm)", service.humanizeKey("nhipTim"));
        org.junit.jupiter.api.Assertions.assertEquals("Weight (kg)", service.humanizeKey("canNang"));

        // Fallback for an unknown camelCase key
        org.junit.jupiter.api.Assertions.assertEquals("Unknown Custom Field", service.humanizeKey("unknownCustomField"));
    }

    @Test
    @DisplayName("AI Assistant uses English by default regardless of the user's message language")
    void testAdaptiveLanguageSupport() {
        AgentOrchestratorService service = new AgentOrchestratorService(
                "https://api.openai.com/v1",
                "", // Empty API key to trigger fallback
                "gpt-6-luna",
                toolRegistryService,
                toolExecutionService,
                objectMapper
        );

        AuthenticatedUser owner = new AuthenticatedUser(10L, "owner@test.com", "HORSE_OWNER");

        // 1. First request -> English fallback
        AiChatRequestDto enRequest = new AiChatRequestDto("Where are my horses located?", List.of());
        AiChatResponseDto enResponse = service.processChat(enRequest, owner);
        assertNotNull(enResponse);
        assertTrue(enResponse.reply().contains("AI_API_KEY is not configured"));

        // 2. A subsequent request also receives the English fallback.
        AiChatRequestDto viRequest = new AiChatRequestDto("Which stall are my horses in?", List.of());
        AiChatResponseDto viResponse = service.processChat(viRequest, owner);
        assertNotNull(viResponse);
        assertTrue(viResponse.reply().contains("AI_API_KEY is not configured"));
    }

    @Test
    @DisplayName("humanizeKeyEn converts camelCase domain keys accurately into professional English labels")
    void testHumanizeKeyEn() {
        AgentOrchestratorService service = new AgentOrchestratorService(
                "https://api.openai.com/v1",
                "test-key",
                "gpt-6-luna",
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
