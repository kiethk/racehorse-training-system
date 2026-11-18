package com.rtms.backend.ai.dto;

import java.util.Map;

/**
 * DTO representing a clinical knowledge chunk retrieved through RAG.
 */
public record ClinicalChunkResponseDto(
        Long embeddingId,
        String title,
        String category,
        String content,
        double similarity,
        Map<String, Object> metadata
) {}
