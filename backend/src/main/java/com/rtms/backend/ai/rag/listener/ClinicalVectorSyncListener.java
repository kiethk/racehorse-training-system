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

import java.util.HashMap;
import java.util.Map;

/**
 * Listener that asynchronously synchronizes clinical domain events to the pgvector database.
 *
 * Runs only after the original medical record transaction has successfully COMMITted (AFTER_COMMIT).
 * Executes asynchronously through @Async("applicationTaskExecutor") while preserving the security context.
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
        log.info("ClinicalVectorSyncListener: Received new medical record event #{} for horse UELN={}",
                event.healthRecordId(), event.registrationNumber());

        try {
            // 1. Preprocess and expand symptoms into specialized medical terminology
            String rawFindings = event.findings() != null ? event.findings() : "";
            String expandedFindings = symptomExpander.expand(rawFindings);
            String diagnosis = event.diagnosis() != null ? event.diagnosis() : "";

            // 2. Package semantic content for RAG vectorization
            String title = String.format("Clinical record for horse %s (Record #%d)",
                    event.registrationNumber() != null ? event.registrationNumber() : event.horseId(),
                    event.healthRecordId());

            String content = String.format("Horse UELN: %s (ID: %d). Clinical findings: %s. Medical diagnosis: %s.",
                    event.registrationNumber(), event.horseId(), expandedFindings, diagnosis);

            // 3. Generate a 1536-dimensional embedding vector
            float[] vector = embeddingService.embed(content);
            String vectorString = formatVector(vector);

            // 4. Create JSON metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("healthRecordId", event.healthRecordId());
            metadata.put("horseId", event.horseId());
            metadata.put("registrationNumber", event.registrationNumber());
            metadata.put("category", "CLINICAL_RECORD");
            metadata.put("allowedRoles", event.allowedRoles());
            String metadataJson = objectMapper.writeValueAsString(metadata);

            // 5. Prepare the allowed_roles array in PostgreSQL array format: {ROLE_1,ROLE_2,...}
            String[] rolesArray = event.allowedRoles() != null && !event.allowedRoles().isEmpty()
                    ? event.allowedRoles().toArray(new String[0])
                    : new String[]{"VETERINARIAN", "HEAD_TRAINER", "CLUB_MANAGER"};
            String pgRolesArray = "{" + String.join(",", rolesArray) + "}";

            // 6. Save the knowledge record to rag_knowledge_documents
            String insertDocSql = """
                    INSERT INTO rag_knowledge_documents (title, category, source_url, allowed_roles)
                    VALUES (?, 'CLINICAL_RECORD', NULL, ?::text[])
                    RETURNING id
                    """;

            Long documentId = jdbcTemplate.queryForObject(insertDocSql, Long.class, title, pgRolesArray);

            if (documentId == null) {
                log.error("ClinicalVectorSyncListener: Unable to create a documentId for medical record #{}", event.healthRecordId());
                return;
            }

            // 7. Save the embedding vector to rag_knowledge_embeddings
            String insertEmbeddingSql = """
                    INSERT INTO rag_knowledge_embeddings (document_id, chunk_index, content, metadata, embedding)
                    VALUES (?, 0, ?, ?::jsonb, ?::vector)
                    """;

            jdbcTemplate.update(insertEmbeddingSql, documentId, content, metadataJson, vectorString);

            log.info("ClinicalVectorSyncListener: Successfully synchronized pgvector for medical record #{} (Document ID: {})",
                    event.healthRecordId(), documentId);

        } catch (Exception ex) {
            log.error("ClinicalVectorSyncListener: Failed to synchronize the vector for medical record #{}: {}",
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
