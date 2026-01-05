package com.kortex.backend.service;

import com.kortex.backend.model.DocumentChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for RAG (Retrieval-Augmented Generation) operations.
 * Handles retrieval of relevant context and prompt preparation for LLM.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RAGService {

    private final SemanticSearchService semanticSearchService;

    @Value("${rag.max.context.tokens:3000}")
    private int maxContextTokens;

    @Value("${rag.context.chunk.limit:10}")
    private int contextChunkLimit;

    @Value("${rag.tokens.per.word:1.3}")
    private double tokensPerWord;

    /**
     * Retrieve relevant context for a query and prepare structured prompt.
     *
     * @param userId the user ID
     * @param query the user's question
     * @param systemPrompt optional system prompt for the LLM
     * @return structured RAG context with prompt
     */
    public RAGContext retrieveContext(Long userId, String query, String systemPrompt) {
        log.info("Retrieving RAG context for user {} with query: {}", userId, query);

        // Search for relevant chunks
        List<SemanticSearchService.SearchResultWithChunk> searchResults = 
                semanticSearchService.search(userId, query, contextChunkLimit);

        log.debug("Found {} potential chunks for query", searchResults.size());

        // Filter chunks within token limit
        List<RelevantChunk> relevantChunks = selectChunksWithinTokenLimit(searchResults);

        log.info("Selected {} chunks within token limit ({} tokens)", 
                relevantChunks.size(), maxContextTokens);

        // Build structured prompt
        String formattedPrompt = buildPrompt(query, relevantChunks, systemPrompt);

        return RAGContext.builder()
                .query(query)
                .relevantChunks(relevantChunks)
                .formattedPrompt(formattedPrompt)
                .totalChunks(relevantChunks.size())
                .estimatedTokens(calculateTotalTokens(relevantChunks))
                .build();
    }

    /**
     * Retrieve context for a query within a specific document.
     *
     * @param userId the user ID
     * @param documentId the document ID to search within
     * @param query the user's question
     * @param systemPrompt optional system prompt
     * @return structured RAG context
     */
    public RAGContext retrieveContextFromDocument(Long userId, Long documentId, String query, String systemPrompt) {
        log.info("Retrieving RAG context from document {} for user {}", documentId, userId);

        // Search within specific document
        List<SemanticSearchService.SearchResultWithChunk> searchResults = 
                semanticSearchService.searchInDocument(userId, documentId, query, contextChunkLimit);

        log.debug("Found {} chunks in document {}", searchResults.size(), documentId);

        // Filter chunks within token limit
        List<RelevantChunk> relevantChunks = selectChunksWithinTokenLimit(searchResults);

        // Build structured prompt
        String formattedPrompt = buildPrompt(query, relevantChunks, systemPrompt);

        return RAGContext.builder()
                .query(query)
                .relevantChunks(relevantChunks)
                .formattedPrompt(formattedPrompt)
                .totalChunks(relevantChunks.size())
                .estimatedTokens(calculateTotalTokens(relevantChunks))
                .documentId(documentId)
                .build();
    }

    /**
     * Select chunks that fit within the token limit.
     * Prioritizes highest relevance scores.
     *
     * @param searchResults search results ordered by relevance
     * @return list of chunks within token limit
     */
    private List<RelevantChunk> selectChunksWithinTokenLimit(
            List<SemanticSearchService.SearchResultWithChunk> searchResults) {
        
        List<RelevantChunk> selectedChunks = new ArrayList<>();
        int cumulativeTokens = 0;

        for (SemanticSearchService.SearchResultWithChunk result : searchResults) {
            DocumentChunk chunk = result.getChunk();
            int chunkTokens = estimateTokens(chunk.getWordCount());

            // Check if adding this chunk would exceed limit
            if (cumulativeTokens + chunkTokens > maxContextTokens) {
                log.debug("Stopping chunk selection at token limit: {} tokens", cumulativeTokens);
                break;
            }

            selectedChunks.add(RelevantChunk.builder()
                    .chunkId(chunk.getId())
                    .documentId(chunk.getDocumentId())
                    .chunkText(chunk.getChunkText())
                    .chunkOrder(chunk.getChunkOrder())
                    .similarityScore(result.getScore())
                    .wordCount(chunk.getWordCount())
                    .estimatedTokens(chunkTokens)
                    .build());

            cumulativeTokens += chunkTokens;
        }

        return selectedChunks;
    }

    /**
     * Build structured prompt for LLM with retrieved context.
     *
     * @param query user's question
     * @param chunks relevant chunks ordered by relevance
     * @param systemPrompt optional system prompt
     * @return formatted prompt string
     */
    private String buildPrompt(String query, List<RelevantChunk> chunks, String systemPrompt) {
        StringBuilder prompt = new StringBuilder();

        // Add system prompt if provided
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            prompt.append("SYSTEM: ").append(systemPrompt).append("\n\n");
        } else {
            // Default system prompt
            prompt.append("SYSTEM: You are a helpful assistant that answers questions based on the provided context. ")
                  .append("Use the context below to answer the user's question accurately. ")
                  .append("If the answer cannot be found in the context, say so.\n\n");
        }

        // Add retrieved context
        prompt.append("CONTEXT:\n");
        prompt.append("---\n");

        for (int i = 0; i < chunks.size(); i++) {
            RelevantChunk chunk = chunks.get(i);
            prompt.append(String.format("[Chunk %d - Relevance: %.3f]\n", i + 1, chunk.getSimilarityScore()));
            prompt.append(chunk.getChunkText());
            prompt.append("\n\n");
        }

        prompt.append("---\n\n");

        // Add user query
        prompt.append("QUESTION: ").append(query).append("\n\n");
        prompt.append("ANSWER: ");

        return prompt.toString();
    }

    /**
     * Estimate tokens from word count.
     * Uses configurable ratio (default ~1.3 tokens per word for English).
     *
     * @param wordCount number of words
     * @return estimated token count
     */
    private int estimateTokens(int wordCount) {
        return (int) Math.ceil(wordCount * tokensPerWord);
    }

    /**
     * Calculate total tokens for a list of chunks.
     *
     * @param chunks list of chunks
     * @return total estimated tokens
     */
    private int calculateTotalTokens(List<RelevantChunk> chunks) {
        return chunks.stream()
                .mapToInt(RelevantChunk::getEstimatedTokens)
                .sum();
    }

    /**
     * Data class representing RAG context with retrieved chunks and formatted prompt.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RAGContext {
        private String query;
        private List<RelevantChunk> relevantChunks;
        private String formattedPrompt;
        private Integer totalChunks;
        private Integer estimatedTokens;
        private Long documentId; // Optional: if context from specific document
    }

    /**
     * Data class representing a relevant chunk with metadata.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RelevantChunk {
        private Long chunkId;
        private Long documentId;
        private String chunkText;
        private Integer chunkOrder;
        private Float similarityScore;
        private Integer wordCount;
        private Integer estimatedTokens;
    }
}
