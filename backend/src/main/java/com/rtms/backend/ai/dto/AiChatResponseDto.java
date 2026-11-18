package com.rtms.backend.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * AI Agent response containing the answer, conversationId, list of called tools, and any pending HITL proposals.
 */
public record AiChatResponseDto(
        String reply,
        Long conversationId,
        List<Map<String, Object>> executedTools,
        List<ActionProposalDto> pendingProposals
) {
    public AiChatResponseDto(String reply, List<Map<String, Object>> executedTools, List<ActionProposalDto> pendingProposals) {
        this(reply, null, executedTools, pendingProposals);
    }
}
