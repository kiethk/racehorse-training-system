package com.rtms.backend.ai.rag.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Default implementation of EmbeddingService for local development and testing.
 *
 * Generates a 1536-dimensional L2-normalized vector based on deterministic feature hashing of the text,
 * ensuring 100% compatibility with the pgvector vector(1536) column without requiring an external API during development or testing.
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

            // Use the hash as the seed for a deterministic pseudo-random distribution
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

            // Apply L2 normalization (Euclidean norm) for cosine distance search
            double norm = Math.sqrt(sumSquares);
            if (norm > 0) {
                for (int i = 0; i < VECTOR_DIMENSION; i++) {
                    vector[i] = (float) (vector[i] / norm);
                }
            }

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 hash generation algorithm failed", e);
        }

        return vector;
    }
}
