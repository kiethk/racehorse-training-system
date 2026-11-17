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
    @DisplayName("Successfully sync a clinical record to rag_knowledge_documents and rag_knowledge_embeddings")
    void testOnClinicalRecordCreatedSuccess() {
        ClinicalRecordCreatedEvent event = new ClinicalRecordCreatedEvent(
                101L,
                42L,
                "VN-2024-00123",
                "pawing at the ground, off feed",
                "Suspected mild equine colic",
                List.of("VETERINARIAN", "HEAD_TRAINER")
        );

        when(symptomExpander.expand("pawing at the ground, off feed"))
                .thenReturn("pawing at the ground, off feed [Related medical terms: equine colic]");

        float[] mockVector = new float[]{0.1f, -0.2f, 0.5f};
        when(embeddingService.embed(anyString())).thenReturn(mockVector);

        // Simulate inserting a document and receiving documentId = 999L.
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(), any()))
                .thenReturn(999L);

        // Execute the listener.
        listener.onClinicalRecordCreated(event);

        // Verify that the expander was called.
        verify(symptomExpander).expand("pawing at the ground, off feed");

        // Verify that an embedding was generated.
        verify(embeddingService).embed(contains("VN-2024-00123"));

        // Verify that the document was saved.
        verify(jdbcTemplate).queryForObject(
                contains("INSERT INTO rag_knowledge_documents"),
                eq(Long.class),
                contains("VN-2024-00123"),
                contains("{VETERINARIAN,HEAD_TRAINER}")
        );

        // Verify that the embedding was saved.
        verify(jdbcTemplate).update(
                contains("INSERT INTO rag_knowledge_embeddings"),
                eq(999L),
                anyString(),
                contains("101"),
                contains("[0.1,-0.2,0.5]")
        );
    }
}
