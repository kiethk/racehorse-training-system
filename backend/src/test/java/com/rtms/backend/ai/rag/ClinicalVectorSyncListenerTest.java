package com.rtms.backend.ai.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.ai.rag.listener.ClinicalVectorSyncListener;
import com.rtms.backend.ai.rag.preprocessor.RuleBasedSymptomExpander;
import com.rtms.backend.ai.rag.service.EmbeddingService;
import com.rtms.backend.event.ClinicalRecordCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ClinicalVectorSyncListenerTest {

    private RuleBasedSymptomExpander symptomExpander;
    private EmbeddingService embeddingService;
    private JdbcTemplate jdbcTemplate;
    private ObjectMapper objectMapper;
    private ClinicalVectorSyncListener listener;

    @BeforeEach
    void setUp() {
        symptomExpander = Mockito.mock(RuleBasedSymptomExpander.class);
        embeddingService = Mockito.mock(EmbeddingService.class);
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        objectMapper = new ObjectMapper();

        listener = new ClinicalVectorSyncListener(symptomExpander, embeddingService, jdbcTemplate, objectMapper);
    }

    @Test
    @DisplayName("Đồng bộ bệnh án thành công vào rag_knowledge_documents và rag_knowledge_embeddings")
    void testOnClinicalRecordCreatedSuccess() {
        ClinicalRecordCreatedEvent event = new ClinicalRecordCreatedEvent(
                101L,
                42L,
                "VN-2024-00123",
                "cào đất, bỏ ăn",
                "Nghi ngờ đau bụng cấp tính thể nhẹ (mild colic)",
                List.of("VETERINARIAN", "HEAD_TRAINER")
        );

        when(symptomExpander.expand("cào đất, bỏ ăn"))
                .thenReturn("cào đất, bỏ ăn [Thuật ngữ y khoa liên quan: đau bụng cấp tính ở ngựa (equine colic)]");

        float[] mockVector = new float[]{0.1f, -0.2f, 0.5f};
        when(embeddingService.embed(anyString())).thenReturn(mockVector);

        // Giả lập insert document trả về documentId = 999L
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(), any()))
                .thenReturn(999L);

        // Thực thi listener
        listener.onClinicalRecordCreated(event);

        // Xác minh expander được gọi
        verify(symptomExpander).expand("cào đất, bỏ ăn");

        // Xác minh sinh vector
        verify(embeddingService).embed(contains("VN-2024-00123"));

        // Xác minh lưu document
        verify(jdbcTemplate).queryForObject(
                contains("INSERT INTO rag_knowledge_documents"),
                eq(Long.class),
                contains("VN-2024-00123"),
                contains("{VETERINARIAN,HEAD_TRAINER}")
        );

        // Xác minh lưu embedding
        verify(jdbcTemplate).update(
                contains("INSERT INTO rag_knowledge_embeddings"),
                eq(999L),
                anyString(),
                contains("101"),
                contains("[0.1,-0.2,0.5]")
        );
    }
}
