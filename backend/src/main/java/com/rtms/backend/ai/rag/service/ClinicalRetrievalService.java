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

import java.util.*;

/**
 * Dịch vụ truy xuất tri thức y khoa lâm sàng (RAG Retrieval Service).
 *
 * Thực hiện tìm kiếm tương đồng ngữ nghĩa bằng Cosine Similarity trên pgvector:
 * 1. Mở rộng từ điển triệu chứng trong câu hỏi bằng RuleBasedSymptomExpander.
 * 2. Tính toán vector 1536 chiều bằng EmbeddingService.
 * 3. Truy vấn toán tử `<=>` trên bảng rag_knowledge_embeddings kết hợp phân quyền allowed_roles.
 * 4. Tự động fallback tìm kiếm từ khóa ngữ nghĩa nếu không có kết quả cosine vượt ngưỡng.
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

    /**
     * Tìm kiếm các đoạn tài liệu/bệnh án lâm sàng tương đồng nhất với câu hỏi.
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
            // 1. Mở rộng triệu chứng bằng từ điển chuẩn hóa y khoa
            String expandedQuery = symptomExpander.expand(queryText);

            // 2. Sinh vector embedding cho câu hỏi
            float[] queryVector = embeddingService.embed(expandedQuery);
            String vectorString = formatVectorForPgvector(queryVector);

            double minSimilarity = threshold != null ? threshold : DEFAULT_SIMILARITY_THRESHOLD;
            int maxResults = (limit != null && limit > 0) ? limit : DEFAULT_LIMIT;

            // Chuẩn bị điều kiện vai trò (PostgreSQL array format: {ROLE})
            String roleFilter = (userRole != null && !userRole.isBlank())
                    ? "{" + userRole.trim().toUpperCase() + "}"
                    : null;

            // 3. Câu lệnh SQL tìm kiếm vector cosine distance trên pgvector
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
                            ps.setNull(4, java.sql.Types.BIGINT);
                            ps.setNull(5, java.sql.Types.BIGINT);
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

            // 4. Fallback tìm kiếm từ khóa ngữ nghĩa khi vector distance không đủ ngưỡng
            String cleanQuery = queryText.replaceAll("[^a-zA-Z0-9\\sàáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ]", " ").trim();
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

            // Top tài liệu lâm sàng tham khảo chung
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
            log.error("ClinicalRetrievalService: Lỗi khi truy vấn tri thức vector: {}", ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    private ClinicalChunkResponseDto mapChunk(java.sql.ResultSet rs) throws java.sql.SQLException {
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
