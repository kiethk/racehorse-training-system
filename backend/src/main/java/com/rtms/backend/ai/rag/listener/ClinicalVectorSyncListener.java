package com.rtms.backend.ai.rag.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.ai.rag.preprocessor.RuleBasedSymptomExpander;
import com.rtms.backend.ai.rag.service.EmbeddingService;
import com.rtms.backend.event.ClinicalRecordCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Listener đồng bộ sự kiện miền Y tế bất đồng bộ vào cơ sở dữ liệu Vector pgvector.
 *
 * Chỉ kích hoạt khi giao dịch lưu bệnh án ban đầu đã COMMIT thành công 100% (AFTER_COMMIT).
 * Chạy bất đồng bộ qua @Async("applicationTaskExecutor") với ngữ cảnh bảo mật được bảo toàn.
 */
@Component
public class ClinicalVectorSyncListener {

    private static final Logger log = LoggerFactory.getLogger(ClinicalVectorSyncListener.class);

    private final RuleBasedSymptomExpander symptomExpander;
    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ClinicalVectorSyncListener(RuleBasedSymptomExpander symptomExpander,
                                      EmbeddingService embeddingService,
                                      JdbcTemplate jdbcTemplate,
                                      ObjectMapper objectMapper) {
        this.symptomExpander = symptomExpander;
        this.embeddingService = embeddingService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Async("applicationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClinicalRecordCreated(ClinicalRecordCreatedEvent event) {
        log.info("ClinicalVectorSyncListener: Nhận sự kiện bệnh án mới #{} cho ngựa UELN={}",
                event.healthRecordId(), event.registrationNumber());

        try {
            // 1. Tiền xử lý mở rộng triệu chứng sang thuật ngữ y khoa chuyên sâu
            String rawSymptoms = event.symptoms() != null ? event.symptoms() : "";
            String expandedSymptoms = symptomExpander.expand(rawSymptoms);
            String diagnosis = event.diagnosis() != null ? event.diagnosis() : "";

            // 2. Đóng gói nội dung ngữ nghĩa để RAG vector hóa
            String title = String.format("Bệnh án chiến mã %s (Record #%d)",
                    event.registrationNumber() != null ? event.registrationNumber() : event.horseId(),
                    event.healthRecordId());

            String content = String.format("Chiến mã UELN: %s (ID: %d). Triệu chứng lâm sàng: %s. Chẩn đoán y khoa: %s.",
                    event.registrationNumber(), event.horseId(), expandedSymptoms, diagnosis);

            // 3. Sinh vector embedding 1536 chiều
            float[] vector = embeddingService.embed(content);
            String vectorString = formatVector(vector);

            // 4. Tạo metadata JSON
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("healthRecordId", event.healthRecordId());
            metadata.put("horseId", event.horseId());
            metadata.put("registrationNumber", event.registrationNumber());
            metadata.put("category", "CLINICAL_RECORD");
            metadata.put("allowedRoles", event.allowedRoles());
            String metadataJson = objectMapper.writeValueAsString(metadata);

            // 5. Chuẩn bị mảng allowed_roles định dạng PostgreSQL Array: {ROLE_1,ROLE_2,...}
            String[] rolesArray = event.allowedRoles() != null && !event.allowedRoles().isEmpty()
                    ? event.allowedRoles().toArray(new String[0])
                    : new String[]{"VETERINARIAN", "HEAD_TRAINER", "CLUB_MANAGER"};
            String pgRolesArray = "{" + String.join(",", rolesArray) + "}";

            // 6. Lưu bản ghi tri thức vào rag_knowledge_documents
            String insertDocSql = """
                    INSERT INTO rag_knowledge_documents (title, category, source_url, allowed_roles)
                    VALUES (?, 'CLINICAL_RECORD', NULL, ?::text[])
                    RETURNING id
                    """;

            Long documentId = jdbcTemplate.queryForObject(insertDocSql, Long.class, title, pgRolesArray);

            if (documentId == null) {
                log.error("ClinicalVectorSyncListener: Không thể tạo documentId cho bệnh án #{}", event.healthRecordId());
                return;
            }

            // 7. Lưu vector embedding vào rag_knowledge_embeddings
            String insertEmbeddingSql = """
                    INSERT INTO rag_knowledge_embeddings (document_id, chunk_index, content, metadata, embedding)
                    VALUES (?, 0, ?, ?::jsonb, ?::vector)
                    """;

            jdbcTemplate.update(insertEmbeddingSql, documentId, content, metadataJson, vectorString);

            log.info("ClinicalVectorSyncListener: Đồng bộ vector pgvector thành công cho bệnh án #{} (Document ID: {})",
                    event.healthRecordId(), documentId);

        } catch (Exception ex) {
            log.error("ClinicalVectorSyncListener: Lỗi khi đồng bộ vector cho bệnh án #{}: {}",
                    event.healthRecordId(), ex.getMessage(), ex);
        }
    }

    private String formatVector(float[] vector) {
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
