package com.rtms.backend.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * Kết quả phản hồi từ AI Agent bao gồm câu trả lời, conversationId, danh sách công cụ đã gọi và các đề xuất HITL (nếu có).
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
