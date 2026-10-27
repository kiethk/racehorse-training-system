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
import static org.mockito.Mockito.when;

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
    @DisplayName("Truy xuất RAG thành công: mở rộng triệu chứng, sinh vector và trả danh sách chunk")
    void testRetrieveSimilarClinicalRecords_Success() {
        String query = "Ngựa bị khập khiễng chân sau";
        when(symptomExpander.expand(query)).thenReturn("Ngựa bị khập khiễng [Lameness] chân sau");

        float[] mockVector = new float[1536];
        mockVector[0] = 0.5f;
        when(embeddingService.embed(anyString())).thenReturn(mockVector);

        ClinicalChunkResponseDto mockChunk = new ClinicalChunkResponseDto(
                10L,
                "Bệnh án viêm gân",
                "CLINICAL_RECORD",
                "Ngựa có dấu hiệu viêm gân và khập khiễng nhẹ...",
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
        assertEquals("Bệnh án viêm gân", results.get(0).title());
        assertEquals(0.88, results.get(0).similarity());
    }

    @Test
    @DisplayName("Truy xuất trả về danh sách rỗng khi câu hỏi null hoặc blank")
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
}
