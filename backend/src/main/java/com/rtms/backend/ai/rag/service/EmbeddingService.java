package com.rtms.backend.ai.rag.service;

/**
 * Dịch vụ sinh vector embeddings kích thước 1536 chiều cho mô hình RAG.
 */
public interface EmbeddingService {

    /**
     * Sinh vector embedding 1536 chiều từ văn bản đầu vào.
     *
     * @param text Văn bản cần nhúng vector
     * @return Mảng float 1536 phần tử đã được chuẩn hóa (L2-normalized)
     */
    float[] embed(String text);
}
