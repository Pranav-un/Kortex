package com.kortex.backend.service;

import com.kortex.backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Question Answering using RAG (Retrieval-Augmented Generation).
 * Combines context retrieval with LLM generation to provide grounded, cited answers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionAnsweringService {

    private final RAGService ragService;
    private final LLMService llmService;
    private final DocumentRepository documentRepository;

    /**
     * Answer a question using RAG with citations.
     * Retrieves relevant context, generates LLM answer, extracts citations.
     *
     * @param userId the user ID
     * @param question the user's question
     * @return answer with context and citations
     */
    public AnswerResponse answerQuestion(Long userId, String question) {
        log.info("Answering question for user {}: {}", userId, question);

        try {
            // Step 1: Retrieve relevant context using RAG
            RAGService.RAGContext context = ragService.retrieveContext(
                    userId,
                    question,
                    buildSystemPrompt()
            );

            log.debug("Retrieved {} chunks for context", context.getTotalChunks());

            // Step 2: Generate answer using LLM
            String llmAnswer = llmService.generateCompletion(context.getFormattedPrompt());

            // Step 3: Extract citations from context
            List<Citation> citations = extractCitations(context.getRelevantChunks());

            log.info("Generated answer with {} citations", citations.size());

            return AnswerResponse.builder()
                    .question(question)
                    .answer(llmAnswer)
                    .citations(citations)
                    .contextChunksUsed(context.getTotalChunks())
                    .estimatedTokens(context.getEstimatedTokens())
                    .llmProvider(llmService.getProviderName())
                    .llmModel(llmService.getModelName())
                    .build();

        } catch (Exception e) {
            log.error("Error answering question: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate answer: " + e.getMessage(), e);
        }
    }

    /**
     * Answer a question within a specific document.
     *
     * @param userId the user ID
     * @param documentId the document ID to search within
     * @param question the user's question
     * @return answer with context and citations
     */
    public AnswerResponse answerQuestionInDocument(Long userId, Long documentId, String question) {
        log.info("Answering question in document {} for user {}: {}", documentId, userId, question);

        try {
            // Step 1: Retrieve context from specific document
            RAGService.RAGContext context = ragService.retrieveContextFromDocument(
                    userId,
                    documentId,
                    question,
                    buildSystemPrompt()
            );

            log.debug("Retrieved {} chunks from document {}", context.getTotalChunks(), documentId);

            // Step 2: Generate answer using LLM
            String llmAnswer = llmService.generateCompletion(context.getFormattedPrompt());

            // Step 3: Extract citations
            List<Citation> citations = extractCitations(context.getRelevantChunks());

            log.info("Generated answer with {} citations from document {}", citations.size(), documentId);

            return AnswerResponse.builder()
                    .question(question)
                    .answer(llmAnswer)
                    .citations(citations)
                    .contextChunksUsed(context.getTotalChunks())
                    .estimatedTokens(context.getEstimatedTokens())
                    .llmProvider(llmService.getProviderName())
                    .llmModel(llmService.getModelName())
                    .documentId(documentId)
                    .build();

        } catch (Exception e) {
            log.error("Error answering question in document: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate answer: " + e.getMessage(), e);
        }
    }

    /**
     * Build system prompt for LLM to prevent hallucination.
     *
     * @return system prompt string
     */
    private String buildSystemPrompt() {
        return "You are a helpful AI assistant that answers questions based ONLY on the provided context. " +
               "Follow these rules strictly:\n" +
               "1. ONLY use information from the provided context to answer questions\n" +
               "2. If the answer is not in the context, say 'I cannot find that information in the provided documents'\n" +
               "3. Do NOT make up information or use external knowledge\n" +
               "4. Cite specific chunks when possible by referring to 'Chunk X'\n" +
               "5. Be concise and accurate\n" +
               "6. If multiple chunks contain relevant information, synthesize them coherently";
    }

    /**
     * Extract citations from relevant chunks.
     *
     * @param chunks relevant chunks with metadata
     * @return list of citations
     */
    private List<Citation> extractCitations(List<RAGService.RelevantChunk> chunks) {
        List<Citation> citations = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            RAGService.RelevantChunk chunk = chunks.get(i);
            
            // Get document name from repository
            String documentName = documentRepository.findById(chunk.getDocumentId())
                    .map(doc -> doc.getFilename())
                    .orElse("Unknown Document");

            citations.add(Citation.builder()
                    .citationNumber(i + 1)
                    .documentId(chunk.getDocumentId())
                    .documentName(documentName)
                    .chunkId(chunk.getChunkId())
                    .chunkOrder(chunk.getChunkOrder())
                    .relevanceScore(chunk.getSimilarityScore())
                    .excerpt(truncateExcerpt(chunk.getChunkText(), 200))
                    .build());
        }

        return citations;
    }

    /**
     * Truncate text excerpt for citation display.
     *
     * @param text full text
     * @param maxLength maximum length
     * @return truncated text
     */
    private String truncateExcerpt(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    /**
     * Response object for question answering.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AnswerResponse {
        private String question;
        private String answer;
        private List<Citation> citations;
        private Integer contextChunksUsed;
        private Integer estimatedTokens;
        private String llmProvider;
        private String llmModel;
        private Long documentId; // Optional: if question was document-specific
    }

    /**
     * Citation object with source information.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class Citation {
        private Integer citationNumber;
        private Long documentId;
        private String documentName;
        private Long chunkId;
        private Integer chunkOrder;
        private Float relevanceScore;
        private String excerpt;
    }
}
