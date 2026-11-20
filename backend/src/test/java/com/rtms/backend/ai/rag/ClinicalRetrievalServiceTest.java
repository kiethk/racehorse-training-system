package com.rtms.backend.ai.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.ai.dto.ClinicalChunkResponseDto;
import com.rtms.backend.ai.rag.preprocessor.RuleBasedSymptomExpander;
import com.rtms.backend.ai.rag.service.ClinicalRetrievalService;
import com.rtms.backend.ai.rag.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ClinicalRetrievalServiceTest {

    private JdbcTemplate jdbcTemplate;
    private EmbeddingService embeddingService;
    private RuleBasedSymptomExpander symptomExpander;
    private ObjectMapper objectMapper;
    private ClinicalRetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        embeddingService = Mockito.mock(EmbeddingService.class);
        symptomExpander = Mockito.mock(RuleBasedSymptomExpander.class);
        objectMapper = new ObjectMapper();

        retrievalService = new ClinicalRetrievalService(
                jdbcTemplate,
                embeddingService,
                symptomExpander,
                objectMapper
        );
    }

    @Test
    @DisplayName("RAG retrieval succeeds: expand symptoms, generate an embedding, and return the chunks")
    @SuppressWarnings("unchecked")
    void testRetrieveSimilarClinicalRecords_Success() {
        String query = "The horse is limping on a hind leg";
        when(symptomExpander.expand(query)).thenReturn("The horse is limping [Equine lameness] on a hind leg");

        float[] mockVector = new float[1536];
        mockVector[0] = 0.5f;
        when(embeddingService.embed(anyString())).thenReturn(mockVector);

        ClinicalChunkResponseDto mockChunk = new ClinicalChunkResponseDto(
                10L,
                "Medical record: tendonitis",
                "CLINICAL_RECORD",
                "The horse shows signs of tendonitis and mild limping...",
                0.88,
                Map.of("horseId", 12L)
        );

        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of(mockChunk));

        List<ClinicalChunkResponseDto> results = retrievalService.retrieveSimilarClinicalRecords(
                query,
                12L,
                "VETERINARIAN",
                0.70,
                3
        );

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Medical record: tendonitis", results.get(0).title());
        assertEquals(0.88, results.get(0).similarity());
    }

    @Test
    @DisplayName("Retrieval returns an empty list when the query is null or blank")
    void testRetrieveSimilarClinicalRecords_EmptyQuery() {
        List<ClinicalChunkResponseDto> results = retrievalService.retrieveSimilarClinicalRecords(
                "   ",
                12L,
                "VETERINARIAN",
                0.70,
                3
        );

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Successfully reindex all embeddings in batches")
    void testReindexAllEmbeddings_Success() {
        List<Map<String, Object>> mockRows = List.of(
                Map.of("id", 1L, "content", "Medical record content 1"),
                Map.of("id", 2L, "content", "Medical record content 2")
        );

        when(jdbcTemplate.queryForList(anyString())).thenReturn(mockRows);

        float[] v1 = new float[1536];
        float[] v2 = new float[1536];
        v1[0] = 0.1f;
        v2[0] = 0.2f;
        when(embeddingService.embedBatch(anyList())).thenReturn(List.of(v1, v2));

        int updatedCount = retrievalService.reindexAllEmbeddings();

        assertEquals(2, updatedCount);
        verify(jdbcTemplate, times(2)).update(anyString(), anyString(), anyLong());
    }
}
