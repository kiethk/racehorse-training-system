package com.rtms.backend.ai.conversation.service;

import com.rtms.backend.ai.conversation.dto.AiConversationDto;
import com.rtms.backend.ai.conversation.dto.AiChatMessageDetailDto;
import com.rtms.backend.ai.conversation.entity.AiConversation;
import com.rtms.backend.ai.conversation.entity.AiChatMessage;
import com.rtms.backend.ai.conversation.repository.AiConversationRepository;
import com.rtms.backend.ai.conversation.repository.AiChatMessageRepository;
import com.rtms.backend.config.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiConversationServiceTest {

    private AiConversationRepository conversationRepository;
    private AiChatMessageRepository messageRepository;
    private AiConversationService conversationService;

    @BeforeEach
    void setUp() {
        conversationRepository = Mockito.mock(AiConversationRepository.class);
        messageRepository = Mockito.mock(AiChatMessageRepository.class);
        conversationService = new AiConversationService(conversationRepository, messageRepository);
    }

    @Test
    @DisplayName("Lấy danh sách các cuộc hội thoại của user")
    void testGetUserConversations() {
        Long userId = 5L;
        AiConversation c1 = new AiConversation(userId, "Lịch tập hôm nay");
        c1.setId(101L);
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId)).thenReturn(List.of(c1));

        List<AiConversationDto> result = conversationService.getUserConversations(userId);

        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).id());
        assertEquals("Lịch tập hôm nay", result.get(0).title());
    }

    @Test
    @DisplayName("Lấy lịch sử tin nhắn ném lỗi nếu không tìm thấy conversation hoặc sai userId")
    void testGetConversationMessages_NotFound() {
        when(conversationRepository.findByIdAndUserId(999L, 5L)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> conversationService.getConversationMessages(999L, 5L));
    }

    @Test
    @DisplayName("Lấy lịch sử tin nhắn thành công khi hợp lệ")
    void testGetConversationMessages_Success() {
        Long userId = 5L;
        Long convId = 101L;
        AiConversation conv = new AiConversation(userId, "Tra cứu y tế");
        conv.setId(convId);

        AiChatMessage m1 = new AiChatMessage(convId, "user", "Ngựa #1 sao rồi?", null);
        m1.setId(1L);
        AiChatMessage m2 = new AiChatMessage(convId, "assistant", "Ngựa #1 hoàn toàn khỏe mạnh.", null);
        m2.setId(2L);

        when(conversationRepository.findByIdAndUserId(convId, userId)).thenReturn(Optional.of(conv));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(convId)).thenReturn(List.of(m1, m2));

        List<AiChatMessageDetailDto> messages = conversationService.getConversationMessages(convId, userId);

        assertEquals(2, messages.size());
        assertEquals("user", messages.get(0).role());
        assertEquals("assistant", messages.get(1).role());
    }

    @Test
    @DisplayName("Tự động tạo cuộc trò chuyện mới từ tin nhắn đầu tiên khi conversationId là null")
    void testGetOrCreateConversation_NewThread() {
        Long userId = 5L;
        when(conversationRepository.save(any(AiConversation.class))).thenAnswer(inv -> {
            AiConversation c = inv.getArgument(0);
            c.setId(201L);
            return c;
        });

        AiConversation created = conversationService.getOrCreateConversation(null, userId, "Cho tôi xem danh sách ngựa đang bị chấn thương");

        assertNotNull(created);
        assertEquals(201L, created.getId());
        assertTrue(created.getTitle().startsWith("Cho tôi xem danh sách"));
    }

    @Test
    @DisplayName("Lưu tin nhắn và cập nhật thời gian updatedAt của hội thoại")
    void testSaveMessage() {
        Long convId = 101L;
        AiConversation conv = new AiConversation(5L, "Test");
        conv.setId(convId);

        when(messageRepository.save(any(AiChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conv));

        AiChatMessage saved = conversationService.saveMessage(convId, "user", "Tin nhắn test", null);

        assertNotNull(saved);
        assertEquals(convId, saved.getConversationId());
        verify(conversationRepository).save(conv);
    }

    @Test
    @DisplayName("Xóa cuộc trò chuyện và toàn bộ tin nhắn bên trong")
    void testDeleteConversation() {
        Long convId = 101L;
        Long userId = 5L;
        AiConversation conv = new AiConversation(userId, "Test");
        conv.setId(convId);

        when(conversationRepository.findByIdAndUserId(convId, userId)).thenReturn(Optional.of(conv));

        conversationService.deleteConversation(convId, userId);

        verify(messageRepository).deleteByConversationId(convId);
        verify(conversationRepository).delete(conv);
    }
}
