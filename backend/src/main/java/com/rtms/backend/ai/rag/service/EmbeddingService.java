package com.rtms.backend.ai.rag.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service for generating 1536-dimensional embedding vectors for the RAG model.
 */
public interface EmbeddingService {

    /**
     * Generates a 1536-dimensional embedding vector from the input text.
     *
     * @param text Text to embed
     * @return L2-normalized array of 1536 floats
     */
    float[] embed(String text);

    /**
     * Generates embedding vectors for multiple text inputs in a batch.
     *
     * @param texts List of text strings to embed
     * @return Corresponding list of float arrays
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
