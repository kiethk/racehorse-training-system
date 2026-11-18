package com.rtms.backend.ai.service;

import com.rtms.backend.ai.conversation.dto.AiChatMessageDetailDto;
import com.rtms.backend.ai.conversation.entity.AiConversation;
import com.rtms.backend.ai.conversation.service.AiConversationService;
import com.rtms.backend.ai.dto.AiChatMessageDto;
import com.rtms.backend.security.AuthenticatedUser;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Loads and persists the conversation thread used by the ReAct loop.
 */
final class ConversationThreadManager {

    private ConversationThreadManager() {
    }

    /**
     * Creates or reuses the conversation, saves the incoming user message, and returns the conversation ID.
     */
    static Long begin(AiConversationService conversationService,
                      Long conversationId,
                      String message,
                      AuthenticatedUser currentUser,
                      Logger log) {
        if (conversationService == null || currentUser == null || currentUser.getUserId() == null) {
            return conversationId;
        }
        try {
            AiConversation conv = conversationService.getOrCreateConversation(
                    conversationId,
                    currentUser.getUserId(),
                    message
            );
            Long resolvedId = conv.getId();
            conversationService.saveMessage(resolvedId, "user", message, null);
            return resolvedId;
        } catch (Exception e) {
            log.error("Failed to store the conversation session in the database: {}", e.getMessage());
            return conversationId;
        }
    }

    /**
     * Appends the request history, or the last stored turns from the database, to the LLM message list.
     */
    static void appendHistory(List<Map<String, Object>> messages,
                              List<AiChatMessageDto> requestHistory,
                              AiConversationService conversationService,
                              Long conversationId,
                              AuthenticatedUser currentUser,
                              Logger log) {
        if (requestHistory != null && !requestHistory.isEmpty()) {
            for (AiChatMessageDto msg : requestHistory) {
                if (msg != null && msg.content() != null && !msg.content().isBlank()) {
                    messages.add(Map.of(
                            "role", msg.role() != null ? msg.role() : "user",
                            "content", msg.content()
                    ));
                }
            }
            return;
        }

        if (conversationService == null || conversationId == null
                || currentUser == null || currentUser.getUserId() == null) {
            return;
        }

        try {
            List<AiChatMessageDetailDto> dbHistory =
                    conversationService.getConversationMessages(conversationId, currentUser.getUserId());
            int totalMsgs = dbHistory.size();
            int startIndex = Math.max(0, totalMsgs - 11);
            int endIndex = Math.max(0, totalMsgs - 1); // Skip the last message, which was just saved
            for (int i = startIndex; i < endIndex; i++) {
                AiChatMessageDetailDto m = dbHistory.get(i);
                messages.add(Map.of(
                        "role", m.role() != null ? m.role() : "user",
                        "content", m.content()
                ));
            }
        } catch (Exception e) {
            log.warn("Unable to load conversation history from the database for conversation #{}: {}",
                    conversationId, e.getMessage());
        }
    }
}
