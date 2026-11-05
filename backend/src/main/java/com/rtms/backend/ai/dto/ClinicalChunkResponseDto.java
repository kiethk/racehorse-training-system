package com.rtms.backend.ai.dto;

import java.util.Map;

/**
 * DTO biểu diễn một đoạn tri thức y khoa lâm sàng tìm kiếm được qua RAG Retrieval.
 */
public record ClinicalChunkResponseDto(
        Long embeddingId,
        String title,
        String category,
        String content,
        double similarity,
        Map<String, Object> metadata
) {}
