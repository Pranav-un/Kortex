package com.kortex.backend.service;

import java.util.List;

/**
 * Service abstraction for generating text embeddings.
 * Allows different embedding implementations (HuggingFace, OpenAI, etc.)
 */
public interface EmbeddingService {

    /**
     * Generate embedding for a single text.
     *
     * @param text the text to embed
     * @return embedding vector as double array
     * @throws RuntimeException if embedding generation fails
     */
    double[] generateEmbedding(String text);

    /**
     * Generate embeddings for multiple texts in batch.
     * More efficient than calling generateEmbedding multiple times.
     *
     * @param texts list of texts to embed
     * @return list of embedding vectors
     * @throws RuntimeException if batch embedding generation fails
     */
    List<double[]> generateEmbeddingsBatch(List<String> texts);

    /**
     * Get the dimension of embeddings produced by this service.
     *
     * @return embedding dimension (e.g., 384 for all-MiniLM-L6-v2)
     */
    int getEmbeddingDimension();

    /**
     * Get the name of the embedding model being used.
     *
     * @return model name (e.g., "sentence-transformers/all-MiniLM-L6-v2")
     */
    String getModelName();
}
