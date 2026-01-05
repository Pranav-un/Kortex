package com.kortex.backend.service;

import com.kortex.backend.model.DocumentChunk;
import com.kortex.backend.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for semantic search using vector embeddings.
 * Performs similarity search in Qdrant and retrieves matching chunks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SemanticSearchService {

    private final QdrantService qdrantService;
    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository chunkRepository;

    /**
     * Search for semantically similar document chunks.
     *
     * @param userId the user ID (for collection namespace)
     * @param query the search query text
     * @param limit maximum number of results
     * @return list of matching chunks with similarity scores
     */
    public List<SearchResultWithChunk> search(Long userId, String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        try {
            // Generate embedding for query
            log.debug("Generating embedding for query: {}", query);
            double[] queryEmbedding = embeddingService.generateEmbedding(query);

            // Search in Qdrant
            List<QdrantService.SearchResult> searchResults = qdrantService.search(userId, queryEmbedding, limit);
            log.info("Found {} similar chunks for query", searchResults.size());

            // Fetch full chunk details from database
            List<Long> chunkIds = searchResults.stream()
                    .map(QdrantService.SearchResult::getChunkId)
                    .collect(Collectors.toList());

            if (chunkIds.isEmpty()) {
                return List.of();
            }

            List<DocumentChunk> chunks = chunkRepository.findAllById(chunkIds);

            // Combine search results with chunk data
            return searchResults.stream()
                    .map(result -> {
                        DocumentChunk chunk = chunks.stream()
                                .filter(c -> c.getId().equals(result.getChunkId()))
                                .findFirst()
                                .orElse(null);

                        return new SearchResultWithChunk(
                                result.getChunkId(),
                                result.getDocumentId(),
                                result.getScore(),
                                chunk
                        );
                    })
                    .filter(result -> result.getChunk() != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error performing semantic search", e);
            throw new RuntimeException("Semantic search failed", e);
        }
    }

    /**
     * Search within a specific document.
     *
     * @param userId the user ID
     * @param documentId the document ID to search within
     * @param query the search query
     * @param limit maximum number of results
     * @return list of matching chunks from the specified document
     */
    public List<SearchResultWithChunk> searchInDocument(Long userId, Long documentId, String query, int limit) {
        List<SearchResultWithChunk> allResults = search(userId, query, limit * 2); // Get more results to filter

        return allResults.stream()
                .filter(result -> result.getDocumentId().equals(documentId))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Data class combining search result with full chunk data.
     */
    public static class SearchResultWithChunk {
        private final Long chunkId;
        private final Long documentId;
        private final float score;
        private final DocumentChunk chunk;

        public SearchResultWithChunk(Long chunkId, Long documentId, float score, DocumentChunk chunk) {
            this.chunkId = chunkId;
            this.documentId = documentId;
            this.score = score;
            this.chunk = chunk;
        }

        public Long getChunkId() {
            return chunkId;
        }

        public Long getDocumentId() {
            return documentId;
        }

        public float getScore() {
            return score;
        }

        public DocumentChunk getChunk() {
            return chunk;
        }
    }
}
