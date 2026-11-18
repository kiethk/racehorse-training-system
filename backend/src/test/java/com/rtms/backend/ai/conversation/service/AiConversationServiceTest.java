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
    @DisplayName("Get the user's conversation list")
    void testGetUserConversations() {
        Long userId = 5L;
        AiConversation c1 = new AiConversation(userId, "Today's training schedule");
        c1.setId(101L);
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId)).thenReturn(List.of(c1));

        List<AiConversationDto> result = conversationService.getUserConversations(userId);

        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).id());
        assertEquals("Today's training schedule", result.get(0).title());
    }

    @Test
    @DisplayName("Getting message history throws an error when the conversation is not found or the user ID is incorrect")
    void testGetConversationMessages_NotFound() {
        when(conversationRepository.findByIdAndUserId(999L, 5L)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> conversationService.getConversationMessages(999L, 5L));
    }

    @Test
    @DisplayName("Get message history successfully when the request is valid")
    void testGetConversationMessages_Success() {
        Long userId = 5L;
        Long convId = 101L;
        AiConversation conv = new AiConversation(userId, "Medical lookup");
        conv.setId(convId);

        AiChatMessage m1 = new AiChatMessage(convId, "user", "How is horse #1 doing?", null);
        m1.setId(1L);
        AiChatMessage m2 = new AiChatMessage(convId, "assistant", "Horse #1 is completely healthy.", null);
        m2.setId(2L);

        when(conversationRepository.findByIdAndUserId(convId, userId)).thenReturn(Optional.of(conv));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(convId)).thenReturn(List.of(m1, m2));

        List<AiChatMessageDetailDto> messages = conversationService.getConversationMessages(convId, userId);

        assertEquals(2, messages.size());
        assertEquals("user", messages.get(0).role());
        assertEquals("assistant", messages.get(1).role());
    }

    @Test
    @DisplayName("Automatically create a new conversation from the first message when conversationId is null")
    void testGetOrCreateConversation_NewThread() {
        Long userId = 5L;
        when(conversationRepository.save(any(AiConversation.class))).thenAnswer(inv -> {
            AiConversation c = inv.getArgument(0);
            c.setId(201L);
            return c;
        });

        AiConversation created = conversationService.getOrCreateConversation(null, userId, "Show me the list of injured horses");

        assertNotNull(created);
        assertEquals(201L, created.getId());
        assertTrue(created.getTitle().startsWith("Show me the list"));
    }

    @Test
    @DisplayName("Save a message and update the conversation's updatedAt timestamp")
    void testSaveMessage() {
        Long convId = 101L;
        AiConversation conv = new AiConversation(5L, "Test");
        conv.setId(convId);

        when(messageRepository.save(any(AiChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conv));

        AiChatMessage saved = conversationService.saveMessage(convId, "user", "Test message", null);

        assertNotNull(saved);
        assertEquals(convId, saved.getConversationId());
        verify(conversationRepository).save(conv);
    }

    @Test
    @DisplayName("Delete a conversation and all of its messages")
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
