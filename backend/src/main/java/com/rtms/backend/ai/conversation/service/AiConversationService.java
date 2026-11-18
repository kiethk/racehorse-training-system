package com.rtms.backend.ai.conversation.service;

import com.rtms.backend.ai.conversation.dto.AiConversationDto;
import com.rtms.backend.ai.conversation.dto.AiChatMessageDetailDto;
import com.rtms.backend.ai.conversation.entity.AiConversation;
import com.rtms.backend.ai.conversation.entity.AiChatMessage;
import com.rtms.backend.ai.conversation.repository.AiConversationRepository;
import com.rtms.backend.ai.conversation.repository.AiChatMessageRepository;
import com.rtms.backend.config.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiConversationService {

    private static final Logger log = LoggerFactory.getLogger(AiConversationService.class);

    private final AiConversationRepository conversationRepository;
    private final AiChatMessageRepository messageRepository;

    public AiConversationService(
            AiConversationRepository conversationRepository,
            AiChatMessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * Gets the user's conversations, sorted by most recently updated.
     */
    @Transactional(readOnly = true)
    public List<AiConversationDto> getUserConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(c -> new AiConversationDto(c.getId(), c.getTitle(), c.getCreatedAt(), c.getUpdatedAt()))
                .toList();
    }

    /**
     * Gets a conversation's message history (after verifying ownership).
     */
    @Transactional(readOnly = true)
    public List<AiChatMessageDetailDto> getConversationMessages(Long conversationId, Long userId) {
        AiConversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND",
                        "Conversation not found or you do not have access."));

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId())
                .stream()
                .map(m -> new AiChatMessageDetailDto(
                        m.getId(),
                        m.getConversationId(),
                        m.getRole(),
                        m.getContent(),
                        m.getToolCalls(),
                        m.getCreatedAt()
                ))
                .toList();
    }

    /**
     * Creates a new conversation or retrieves an existing one.
     */
    @Transactional
    public AiConversation getOrCreateConversation(Long conversationId, Long userId, String initialMessage) {
        if (conversationId != null) {
            return conversationRepository.findByIdAndUserId(conversationId, userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND",
                            "Conversation not found or you do not have access."));
        }

        // Generate a title from the first message (up to 40 characters)
        String title = generateTitle(initialMessage);
        AiConversation newConversation = new AiConversation(userId, title);
        return conversationRepository.save(newConversation);
    }

    /**
     * Explicitly creates a new conversation (for example, when the user clicks "New Chat").
     */
    @Transactional
    public AiConversationDto createNewConversation(Long userId, String customTitle) {
        String title = (customTitle != null && !customTitle.isBlank()) ? customTitle.trim() : "New conversation";
        AiConversation conversation = new AiConversation(userId, title);
        AiConversation saved = conversationRepository.save(conversation);
        return new AiConversationDto(saved.getId(), saved.getTitle(), saved.getCreatedAt(), saved.getUpdatedAt());
    }

    /**
     * Saves a user or AI message to the database.
     */
    @Transactional
    public AiChatMessage saveMessage(Long conversationId, String role, String content, String toolCallsJson) {
        AiChatMessage message = new AiChatMessage(conversationId, role, content, toolCallsJson);
        AiChatMessage saved = messageRepository.save(message);

        // Update the conversation timestamp
        conversationRepository.findById(conversationId).ifPresent(c -> {
            c.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(c);
        });

        return saved;
    }

    /**
     * Renames a conversation.
     */
    @Transactional
    public AiConversationDto renameConversation(Long conversationId, Long userId, String newTitle) {
        AiConversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND",
                        "Conversation not found."));

        conversation.setTitle(newTitle.trim());
        conversation.setUpdatedAt(LocalDateTime.now());
        AiConversation saved = conversationRepository.save(conversation);
        return new AiConversationDto(saved.getId(), saved.getTitle(), saved.getCreatedAt(), saved.getUpdatedAt());
    }

    /**
     * Deletes a conversation and all related messages.
     */
    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        AiConversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND",
                        "Conversation not found."));

        messageRepository.deleteByConversationId(conversation.getId());
        conversationRepository.delete(conversation);
        log.info("Deleted conversation ID [{}] for user ID [{}]", conversationId, userId);
    }

    private String generateTitle(String message) {
        if (message == null || message.isBlank()) {
            return "New conversation";
        }
        String clean = message.replaceAll("\\r?\\n", " ").trim();
        if (clean.length() <= 40) {
            return clean;
        }
        return clean.substring(0, 37) + "...";
    }
}
