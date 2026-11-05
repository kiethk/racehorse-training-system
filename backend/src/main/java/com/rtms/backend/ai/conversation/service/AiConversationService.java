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
     * Lấy danh sách các cuộc hội thoại của người dùng, sắp xếp theo thời gian cập nhật mới nhất.
     */
    @Transactional(readOnly = true)
    public List<AiConversationDto> getUserConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(c -> new AiConversationDto(c.getId(), c.getTitle(), c.getCreatedAt(), c.getUpdatedAt()))
                .toList();
    }

    /**
     * Lấy lịch sử tin nhắn của một cuộc hội thoại (kiểm tra quyền sở hữu).
     */
    @Transactional(readOnly = true)
    public List<AiChatMessageDetailDto> getConversationMessages(Long conversationId, Long userId) {
        AiConversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND",
                        "Không tìm thấy cuộc hội thoại hoặc bạn không có quyền truy cập."));

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
     * Tạo mới hoặc lấy cuộc trò chuyện hiện có.
     */
    @Transactional
    public AiConversation getOrCreateConversation(Long conversationId, Long userId, String initialMessage) {
        if (conversationId != null) {
            return conversationRepository.findByIdAndUserId(conversationId, userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND",
                            "Không tìm thấy cuộc hội thoại hoặc bạn không có quyền truy cập."));
        }

        // Tạo tiêu đề từ tin nhắn đầu tiên (tối đa 40 ký tự)
        String title = generateTitle(initialMessage);
        AiConversation newConversation = new AiConversation(userId, title);
        return conversationRepository.save(newConversation);
    }

    /**
     * Tạo cuộc trò chuyện mới có chủ đích (ví dụ người dùng bấm nút "New Chat").
     */
    @Transactional
    public AiConversationDto createNewConversation(Long userId, String customTitle) {
        String title = (customTitle != null && !customTitle.isBlank()) ? customTitle.trim() : "Cuộc trò chuyện mới";
        AiConversation conversation = new AiConversation(userId, title);
        AiConversation saved = conversationRepository.save(conversation);
        return new AiConversationDto(saved.getId(), saved.getTitle(), saved.getCreatedAt(), saved.getUpdatedAt());
    }

    /**
     * Lưu tin nhắn của người dùng hoặc AI vào cơ sở dữ liệu.
     */
    @Transactional
    public AiChatMessage saveMessage(Long conversationId, String role, String content, String toolCallsJson) {
        AiChatMessage message = new AiChatMessage(conversationId, role, content, toolCallsJson);
        AiChatMessage saved = messageRepository.save(message);

        // Cập nhật timestamp cho cuộc hội thoại
        conversationRepository.findById(conversationId).ifPresent(c -> {
            c.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(c);
        });

        return saved;
    }

    /**
     * Đổi tên cuộc trò chuyện.
     */
    @Transactional
    public AiConversationDto renameConversation(Long conversationId, Long userId, String newTitle) {
        AiConversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND",
                        "Không tìm thấy cuộc hội thoại."));

        conversation.setTitle(newTitle.trim());
        conversation.setUpdatedAt(LocalDateTime.now());
        AiConversation saved = conversationRepository.save(conversation);
        return new AiConversationDto(saved.getId(), saved.getTitle(), saved.getCreatedAt(), saved.getUpdatedAt());
    }

    /**
     * Xóa cuộc trò chuyện và toàn bộ tin nhắn liên quan.
     */
    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        AiConversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND",
                        "Không tìm thấy cuộc hội thoại."));

        messageRepository.deleteByConversationId(conversation.getId());
        conversationRepository.delete(conversation);
        log.info("Deleted conversation ID [{}] for user ID [{}]", conversationId, userId);
    }

    private String generateTitle(String message) {
        if (message == null || message.isBlank()) {
            return "Cuộc trò chuyện mới";
        }
        String clean = message.replaceAll("\\r?\\n", " ").trim();
        if (clean.length() <= 40) {
            return clean;
        }
        return clean.substring(0, 37) + "...";
    }
}
