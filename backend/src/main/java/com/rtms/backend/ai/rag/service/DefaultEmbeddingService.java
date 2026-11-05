package com.rtms.backend.ai.rag.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Triển khai mặc định của EmbeddingService phục vụ môi trường phát triển cục bộ và kiểm thử.
 *
 * Sinh vector 1536 chiều được chuẩn hóa L2 dựa trên mã băm phân rã văn bản (Deterministic Feature Hashing),
 * đảm bảo tương thích 100% với cột vector(1536) của pgvector mà không bắt buộc phải gọi API ngoài khi dev/test.
 */
@Service
public class DefaultEmbeddingService implements EmbeddingService {

    public static final int VECTOR_DIMENSION = 1536;

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[VECTOR_DIMENSION];
        }

        float[] vector = new float[VECTOR_DIMENSION];

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));

            // Dùng hash làm seed sinh phân phối giả ngẫu nhiên có tính xác định (deterministic)
            long seed = 0;
            for (int i = 0; i < 8; i++) {
                seed = (seed << 8) | (hash[i] & 0xFF);
            }

            Random random = new Random(seed);
            double sumSquares = 0.0;

            for (int i = 0; i < VECTOR_DIMENSION; i++) {
                vector[i] = (float) random.nextGaussian();
                sumSquares += vector[i] * vector[i];
            }

            // Chuẩn hóa L2 (Euclidean Norm) để phục vụ tìm kiếm Cosine Distance
            double norm = Math.sqrt(sumSquares);
            if (norm > 0) {
                for (int i = 0; i < VECTOR_DIMENSION; i++) {
                    vector[i] = (float) (vector[i] / norm);
                }
            }

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi thuật toán sinh hash SHA-256", e);
        }

        return vector;
    }
}
