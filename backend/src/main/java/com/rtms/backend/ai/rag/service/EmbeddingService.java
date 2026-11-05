package com.rtms.backend.ai.rag.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    /**
     * Sinh danh sách vector embeddings cho nhiều văn bản theo lô (batching).
     *
     * @param texts Danh sách các chuỗi văn bản cần nhúng
     * @return Danh sách mảng float tương ứng
     */
    default List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }
        List<float[]> results = new ArrayList<>(texts.size());
        for (String t : texts) {
            results.add(embed(t));
        }
        return results;
    }
}
