package com.rtms.backend.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.ai.conversation.service.AiConversationService;
import com.rtms.backend.ai.dto.AiChatRequestDto;
import com.rtms.backend.ai.dto.AiChatResponseDto;
import com.rtms.backend.ai.tool.AiToolExecutionService;
import com.rtms.backend.ai.tool.DynamicToolRegistryService;
import com.rtms.backend.security.AuthenticatedUser;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Main coordinator for the AI Chat Agent (ReAct Loop + Intent Decomposition).
 */
@Service
public class AgentOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestratorService.class);
    private static final int MAX_REACT_ITERATIONS = 4;

    private final DynamicToolRegistryService toolRegistryService;
    private final AiToolExecutionService toolExecutionService;
    private final AiConversationService conversationService;
    private final IntentDecompositionService intentDecompositionService;
    private final ObjectMapper objectMapper;
    private final SystemPromptBuilder systemPromptBuilder;
    private final LlmApiClient llmApiClient;

    @Autowired
    public AgentOrchestratorService(
            @Value("${ai.api.base-url:${groq.api.base-url:https://api.openai.com/v1}}") String aiBaseUrl,
            @Value("${ai.api.key:${groq.api.key:}}") String aiApiKey,
            @Value("${ai.api.model:${groq.api.model:gpt-4o-mini}}") String aiModel,
            DynamicToolRegistryService toolRegistryService,
            AiToolExecutionService toolExecutionService,
            @Autowired(required = false) AiConversationService conversationService,
            @Autowired(required = false) IntentDecompositionService intentDecompositionService,
            ObjectMapper objectMapper,
            SystemPromptBuilder systemPromptBuilder,
            LlmApiClient llmApiClient) {
        this.toolRegistryService = toolRegistryService;
        this.toolExecutionService = toolExecutionService;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.systemPromptBuilder = systemPromptBuilder != null ? systemPromptBuilder : new SystemPromptBuilder();
        this.llmApiClient = llmApiClient != null ? llmApiClient : new LlmApiClient(aiBaseUrl, aiApiKey, aiModel, this.objectMapper);
        this.intentDecompositionService = intentDecompositionService != null
                ? intentDecompositionService
                : new DefaultIntentDecompositionService(aiBaseUrl, aiApiKey, aiModel, this.objectMapper);
    }

    /**
     * Backward-compatible constructor for testing without SystemPromptBuilder / LlmApiClient beans.
     */
    public AgentOrchestratorService(
            String aiBaseUrl,
            String aiApiKey,
            String aiModel,
            DynamicToolRegistryService toolRegistryService,
            AiToolExecutionService toolExecutionService,
            ObjectMapper objectMapper) {
        this(aiBaseUrl, aiApiKey, aiModel, toolRegistryService, toolExecutionService, null, null, objectMapper, null, null);
    }

    /**
     * Backward-compatible constructor for testing with mock IntentDecompositionService.
     */
    public AgentOrchestratorService(
            String aiBaseUrl,
            String aiApiKey,
            String aiModel,
            DynamicToolRegistryService toolRegistryService,
            AiToolExecutionService toolExecutionService,
            IntentDecompositionService intentDecompositionService,
            ObjectMapper objectMapper) {
        this(aiBaseUrl, aiApiKey, aiModel, toolRegistryService, toolExecutionService, null, intentDecompositionService, objectMapper, null, null);
    }

    @PostConstruct
    public void init() {
        if (!llmApiClient.isApiKeyConfigured()) {
            log.warn("AgentOrchestratorService: AI_API_KEY is not configured.");
        } else {
            log.info("AgentOrchestratorService: AI Client initialized successfully with model [{}]", llmApiClient.getAiModel());
        }
    }

    /**
     * Processes a chat conversation with the user and synchronizes it with the database.
     */
    @SuppressWarnings("unchecked")
    public AiChatResponseDto processChat(AiChatRequestDto request, AuthenticatedUser currentUser) {
        Long conversationId = request.conversationId();

        // 1. Manage the persistent conversation thread in the database
        conversationId = ConversationThreadManager.begin(
                conversationService, conversationId, request.message(), currentUser, log
        );

        if (!llmApiClient.isApiKeyConfigured()) {
            log.warn("AgentOrchestratorService: AI_API_KEY is not configured.");
            String fallbackReply = "AI_API_KEY is not configured. Please set the AI_API_KEY environment variable in your IDE Run Configuration or application.properties to enable the RTMS AI Assistant.";
            if (conversationService != null && conversationId != null) {
                conversationService.saveMessage(conversationId, "assistant", fallbackReply, null);
            }
            return new AiChatResponseDto(
                    fallbackReply,
                    conversationId,
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        // 2. Pre-Flight Intent Decomposition (intent decomposition and OOD isolation)
        IntentDecompositionService.DecomposedIntents intents = intentDecompositionService != null
                ? intentDecompositionService.decompose(request.message(), currentUser != null ? currentUser.getRole() : null)
                : new IntentDecompositionService.DecomposedIntents(List.of(request.message()), Collections.emptyList());

        // Fast Short-Circuit: If the entire question is outside the RTMS scope, decline immediately without starting the ReAct loop
        if (intents.isPureOutOfDomain()) {
            log.info("AgentOrchestratorService: Detected a fully Out-of-Domain question. Triggering Pre-Flight Refusal.");
            String cannedRefusal = "I am the RTMS AI Assistant, specialized exclusively in racehorse training and management operations (horses, training schedules, stabling, veterinary care, and racing). Your request is outside my assigned operational scope, so I cannot assist with it.";
            if (conversationService != null && conversationId != null) {
                try {
                    conversationService.saveMessage(conversationId, "assistant", cannedRefusal, null);
                } catch (Exception e) {
                    log.error("Failed to save the OOD refusal response to the database: {}", e.getMessage());
                }
            }
            return new AiChatResponseDto(cannedRefusal, conversationId, Collections.emptyList(), Collections.emptyList());
        }

        List<Map<String, Object>> executedTools = new ArrayList<>();

        // 3. Build the conversation message list
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", buildSystemPrompt(currentUser)
        ));

        // Load the conversation history
        ConversationThreadManager.appendHistory(
                messages, request.conversationHistory(), conversationService, conversationId, currentUser, log
        );

        // Strictly isolate context: include only in-domain items in the ReAct context to eliminate any chance of leaking OOD answers
        StringBuilder promptBuilder = new StringBuilder();
        if (intents.hasInDomain()) {
            promptBuilder.append(String.join("\n", intents.inDomainIntents()));
            if (intents.hasOutOfDomain()) {
                promptBuilder.append("\n\n[SYSTEM DIRECTIVE: The user also asked out-of-scope question(s): \"")
                        .append(String.join("; ", intents.outOfDomainIntents()))
                        .append("\". You MUST answer ONLY the in-domain operational inquiry above and explicitly decline the out-of-scope topic(s) per Rule 16. Do not answer or advise on out-of-scope topics.]");
            }
        } else {
            promptBuilder.append(request.message());
        }
        String sanitizedUserPrompt = promptBuilder.toString();

        messages.add(Map.of(
                "role", "user",
                "content", sanitizedUserPrompt
        ));

        // 4. Get the tools available for the user's role (Dynamic Tool Scoping)
        List<Map<String, Object>> tools = toolRegistryService.getFunctionSpecsForRole(currentUser.getRole());

        // 5. Reasoning and tool-calling loop (ReAct Loop)
        String finalAnswer = "";
        int iterations = 0;

        while (iterations < MAX_REACT_ITERATIONS) {
            iterations++;
            Map<String, Object> aiResponse = llmApiClient.callChatCompletions(messages, tools);

            if (aiResponse == null || !aiResponse.containsKey("choices")) {
                finalAnswer = "Sorry, cannot connect to the AI server at this moment. Please try again later.";
                break;
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) aiResponse.get("choices");
            if (choices == null || choices.isEmpty()) {
                finalAnswer = "Received invalid or empty response from the AI server.";
                break;
            }

            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> assistantMessage = (Map<String, Object>) firstChoice.get("message");
            if (assistantMessage == null) {
                break;
            }

            // Check whether the LLM called a tool
            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) assistantMessage.get("tool_calls");
            String content = (String) assistantMessage.get("content");

            if (toolCalls == null || toolCalls.isEmpty()) {
                finalAnswer = content != null ? content : "";
                break;
            }

            // The LLM requested a tool call: add the assistant message containing tool_calls to the context
            messages.add(assistantMessage);

            for (Map<String, Object> toolCall : toolCalls) {
                String toolCallId = (String) toolCall.get("id");
                Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                String toolName = (String) function.get("name");
                String argumentsJson = (String) function.get("arguments");

                log.info("Agent ReAct loop [{}]: Executing tool [{}] with args [{}]", iterations, toolName, argumentsJson);

                Map<String, Object> arguments = llmApiClient.parseArguments(argumentsJson);

                // Execute the tool through AiToolExecutionService
                Object result;
                try {
                    result = toolExecutionService.executeTool(toolName, arguments, currentUser);
                } catch (Exception e) {
                    log.warn("Error while the AI called tool [{}]: {}", toolName, e.getMessage());
                    result = Map.of(
                            "status", "ERROR",
                            "error", e.getMessage() != null ? e.getMessage() : "Tool execution failed",
                            "huongDanAI", "The tool encountered an error or is missing parameters. First, look up any missing parameters using the relevant directory or listing tool, or politely explain the issue to the user."
                    );
                }

                executedTools.add(Map.of(
                        "tool", toolName,
                        "arguments", arguments,
                        "result", result != null ? result : Map.of()
                ));

                // Add the tool result back to the context
                try {
                    String resultJson = objectMapper.writeValueAsString(result);
                    messages.add(Map.of(
                            "role", "tool",
                            "tool_call_id", toolCallId != null ? toolCallId : "",
                            "name", toolName,
                            "content", resultJson
                    ));
                } catch (Exception e) {
                    log.error("Failed to serialize the tool result for the AI model: {}", e.getMessage());
                    messages.add(Map.of(
                            "role", "tool",
                            "tool_call_id", toolCallId != null ? toolCallId : "",
                            "name", toolName,
                            "content", "{\"error\": \"Failed to serialize result\"}"
                    ));
                }
            }
        }

        boolean isMeaninglessAnswer = finalAnswer == null || finalAnswer.isBlank()
                || finalAnswer.trim().equals("I have completed the data retrieval according to your request.");

        if (isMeaninglessAnswer && !executedTools.isEmpty()) {
            // Make one final synthesis call, asking the LLM to summarize the tool results
            try {
                List<Map<String, Object>> summaryMessages = new ArrayList<>(messages);
                summaryMessages.add(Map.of(
                        "role", "user",
                        "content", "Based on the data retrieved from the executed tools above, synthesize a comprehensive and detailed response in English. Format using Markdown tables, structured bullet points with in-depth analysis, and relevant navigation links. Do not provide a brief or generic summary."
                ));
                Map<String, Object> summaryResponse = llmApiClient.callChatCompletions(summaryMessages, null);
                if (summaryResponse != null && summaryResponse.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) summaryResponse.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                        if (msg != null && msg.get("content") != null) {
                            String summaryContent = ((String) msg.get("content")).trim();
                            if (!summaryContent.isBlank()
                                    && !summaryContent.equals("I have completed the data retrieval according to your request.")) {
                                finalAnswer = summaryContent;
                                isMeaninglessAnswer = false;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Unable to call the LLM to synthesize a summary of the tool results: {}", e.getMessage());
            }

            // If the LLM still does not respond or returns meaningless text, format the data from executedTools automatically
            if (isMeaninglessAnswer) {
                finalAnswer = formatFallbackToolSummary(executedTools);
            }
        } else if (finalAnswer == null || finalAnswer.isBlank()) {
            finalAnswer = "I have completed the data retrieval according to your request.";
        }

        // Add a definitive refusal note for any out-of-domain items
        if (intents.hasOutOfDomain()) {
            String oodItems = String.join(", ", intents.outOfDomainIntents());
            finalAnswer += "\n\n*Note: Regarding the out-of-scope request [" + oodItems + "], the RTMS AI Assistant is specialized in racehorse operations and management, and cannot assist with external requests.*";
        }

        // 6. Save the assistant's response to the database
        if (conversationService != null && conversationId != null) {
            try {
                String toolCallsJson = executedTools.isEmpty() ? null : objectMapper.writeValueAsString(executedTools);
                conversationService.saveMessage(conversationId, "assistant", finalAnswer, toolCallsJson);
            } catch (Exception e) {
                log.error("Failed to save the AI response to the database: {}", e.getMessage());
            }
        }

        return new AiChatResponseDto(finalAnswer, conversationId, executedTools, Collections.emptyList());
    }

    String buildSystemPrompt(AuthenticatedUser user) {
        return systemPromptBuilder.buildSystemPrompt(user)
                + "\n\nRespond in English by default. Use another language only when the user explicitly requests it.";
    }

    String formatFallbackToolSummary(List<Map<String, Object>> executedTools) {
        return systemPromptBuilder.formatFallbackToolSummary(executedTools);
    }

    String formatFallbackToolSummary(List<Map<String, Object>> executedTools, boolean isVi) {
        return systemPromptBuilder.formatFallbackToolSummary(executedTools, isVi);
    }

    String humanizeKey(String key) {
        return systemPromptBuilder.humanizeKey(key);
    }

    String humanizeKeyEn(String key) {
        return systemPromptBuilder.humanizeKeyEn(key);
    }
}
