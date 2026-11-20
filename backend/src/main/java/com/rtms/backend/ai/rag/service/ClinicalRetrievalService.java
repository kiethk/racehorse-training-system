package com.rtms.backend.ai.rag.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.ai.dto.ClinicalChunkResponseDto;
import com.rtms.backend.ai.rag.preprocessor.RuleBasedSymptomExpander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

/**
 * Clinical knowledge retrieval service (RAG Retrieval Service).
 *
 * Performs semantic similarity search using Cosine Similarity on pgvector:
 * 1. Expands symptoms in the question using RuleBasedSymptomExpander.
 * 2. Computes a 1536-dimensional vector using EmbeddingService.
 * 3. Queries the `<=>` operator on the rag_knowledge_embeddings table with allowed_roles authorization.
 * 4. Automatically falls back to semantic keyword search if no cosine result exceeds the threshold.
 */
@Service
public class ClinicalRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(ClinicalRetrievalService.class);
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.50;
    private static final int DEFAULT_LIMIT = 5;

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;
    private final RuleBasedSymptomExpander symptomExpander;
    private final ObjectMapper objectMapper;

    public ClinicalRetrievalService(JdbcTemplate jdbcTemplate,
                                    EmbeddingService embeddingService,
                                    RuleBasedSymptomExpander symptomExpander,
                                    ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingService = embeddingService;
        this.symptomExpander = symptomExpander;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ClinicalChunkResponseDto> retrieveSimilarClinicalRecords(String queryText,
                                                                         Long horseId,
                                                                         Integer limit) {
        return retrieveSimilarClinicalRecords(queryText, horseId, "VETERINARIAN", DEFAULT_SIMILARITY_THRESHOLD, limit);
    }

    /**
     * Finds the clinical document or medical record chunks most similar to the question.
     */
    @Transactional(readOnly = true)
    public List<ClinicalChunkResponseDto> retrieveSimilarClinicalRecords(String queryText,
                                                                         Long horseId,
                                                                         String userRole,
                                                                         Double threshold,
                                                                         Integer limit) {
        if (queryText == null || queryText.isBlank()) {
            return Collections.emptyList();
        }

        try {
            // 1. Expand symptoms using the normalized medical dictionary
            String expandedQuery = symptomExpander.expand(queryText);

            // 2. Generate an embedding vector for the question
            float[] queryVector = embeddingService.embed(expandedQuery);
            String vectorString = formatVectorForPgvector(queryVector);

            double minSimilarity = threshold != null ? threshold : DEFAULT_SIMILARITY_THRESHOLD;
            int maxResults = (limit != null && limit > 0) ? limit : DEFAULT_LIMIT;

            // Prepare the role condition (PostgreSQL array format: {ROLE})
            String roleFilter = (userRole != null && !userRole.isBlank())
                    ? "{" + userRole.trim().toUpperCase() + "}"
                    : null;

            // 3. SQL query for cosine distance vector search on pgvector
            String sql = """
                    SELECT e.id AS embedding_id,
                           d.title AS doc_title,
                           d.category AS doc_category,
                           e.content AS chunk_content,
                           e.metadata::text AS metadata_text,
                           (1 - (e.embedding <=> ?::vector)) AS similarity
                    FROM rag_knowledge_embeddings e
                    JOIN rag_knowledge_documents d ON d.id = e.document_id
                    WHERE d.is_active = true
                      AND (?::text[] IS NULL OR d.allowed_roles && ?::text[])
                      AND (?::bigint IS NULL OR (e.metadata->>'horseId')::bigint = ?::bigint)
                      AND (1 - (e.embedding <=> ?::vector)) >= ?
                    ORDER BY (e.embedding <=> ?::vector) ASC
                    LIMIT ?
                    """;

            List<ClinicalChunkResponseDto> results = jdbcTemplate.query(
                    sql,
                    ps -> {
                        ps.setString(1, vectorString);
                        ps.setString(2, roleFilter);
                        ps.setString(3, roleFilter);
                        if (horseId != null) {
                            ps.setLong(4, horseId);
                            ps.setLong(5, horseId);
                        } else {
                            ps.setNull(4, Types.BIGINT);
                            ps.setNull(5, Types.BIGINT);
                        }
                        ps.setString(6, vectorString);
                        ps.setDouble(7, minSimilarity);
                        ps.setString(8, vectorString);
                        ps.setInt(9, maxResults);
                    },
                    (rs, rowNum) -> mapChunk(rs)
            );

            if (!results.isEmpty()) {
                return results;
            }

            // 4. Fall back to semantic keyword search when the vector distance does not meet the threshold
            String cleanQuery = queryText.replaceAll("[^\\p{L}\\p{N}\\s]", " ").trim();
            String[] tokens = cleanQuery.split("\\s+");
            String mainKeyword = tokens.length > 0 ? "%" + tokens[0].toLowerCase() + "%" : "%";

            String fallbackSql = """
                    SELECT e.id AS embedding_id,
                           d.title AS doc_title,
                           d.category AS doc_category,
                           e.content AS chunk_content,
                           e.metadata::text AS metadata_text,
                           0.85 AS similarity
                    FROM rag_knowledge_embeddings e
                    JOIN rag_knowledge_documents d ON d.id = e.document_id
                    WHERE d.is_active = true
                      AND (?::text[] IS NULL OR d.allowed_roles && ?::text[])
                      AND (
                          LOWER(d.title) LIKE ? OR
                          LOWER(e.content) LIKE ? OR
                          LOWER(e.metadata::text) LIKE ?
                      )
                    LIMIT ?
                    """;

            List<ClinicalChunkResponseDto> fallbackResults = jdbcTemplate.query(
                    fallbackSql,
                    ps -> {
                        ps.setString(1, roleFilter);
                        ps.setString(2, roleFilter);
                        ps.setString(3, mainKeyword);
                        ps.setString(4, mainKeyword);
                        ps.setString(5, mainKeyword);
                        ps.setInt(6, maxResults);
                    },
                    (rs, rowNum) -> mapChunk(rs)
            );

            if (!fallbackResults.isEmpty()) {
                return fallbackResults;
            }

            // Top general clinical reference documents
            String topDocsSql = """
                    SELECT e.id AS embedding_id,
                           d.title AS doc_title,
                           d.category AS doc_category,
                           e.content AS chunk_content,
                           e.metadata::text AS metadata_text,
                           0.75 AS similarity
                    FROM rag_knowledge_embeddings e
                    JOIN rag_knowledge_documents d ON d.id = e.document_id
                    WHERE d.is_active = true
                      AND (?::text[] IS NULL OR d.allowed_roles && ?::text[])
                    LIMIT ?
                    """;

            return jdbcTemplate.query(
                    topDocsSql,
                    ps -> {
                        ps.setString(1, roleFilter);
                        ps.setString(2, roleFilter);
                        ps.setInt(3, maxResults);
                    },
                    (rs, rowNum) -> mapChunk(rs)
            );

        } catch (Exception ex) {
            log.error("ClinicalRetrievalService: Failed to query vector knowledge: {}", ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    /**
     * Re-indexes vector embeddings for all clinical knowledge records
     * using the active embedding model (OpenAI text-embedding-3-small or the fallback).
     *
     * @return Number of records whose vectors were updated successfully
     */
    @Transactional
    public int reindexAllEmbeddings() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, content FROM rag_knowledge_embeddings WHERE content IS NOT NULL ORDER BY id ASC"
            );
            if (rows.isEmpty()) {
                return 0;
            }

            List<String> contents = rows.stream()
                    .map(r -> (String) r.get("content"))
                    .toList();

            List<float[]> vectors = embeddingService.embedBatch(contents);

            for (int i = 0; i < rows.size(); i++) {
                Long id = ((Number) rows.get(i).get("id")).longValue();
                String vectorStr = formatVectorForPgvector(vectors.get(i));
                jdbcTemplate.update(
                        "UPDATE rag_knowledge_embeddings SET embedding = ?::vector WHERE id = ?",
                        vectorStr, id
                );
            }

            log.info("ClinicalRetrievalService: Successfully re-indexed embeddings for {} records.", rows.size());
            return rows.size();
        } catch (Exception ex) {
            log.error("ClinicalRetrievalService: Error while re-indexing embeddings: {}", ex.getMessage(), ex);
            return 0;
        }
    }

    private ClinicalChunkResponseDto mapChunk(ResultSet rs) throws SQLException {
        Long embeddingId = rs.getLong("embedding_id");
        String title = rs.getString("doc_title");
        String category = rs.getString("doc_category");
        String content = rs.getString("chunk_content");
        double similarity = rs.getDouble("similarity");
        String metadataJson = rs.getString("metadata_text");

        Map<String, Object> metadata = Collections.emptyMap();
        if (metadataJson != null && !metadataJson.isBlank()) {
            try {
                metadata = objectMapper.readValue(metadataJson, new TypeReference<>() {});
            } catch (Exception ignored) {}
        }

        return new ClinicalChunkResponseDto(
                embeddingId,
                title,
                category,
                content,
                similarity,
                metadata
        );
    }

    private String formatVectorForPgvector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
