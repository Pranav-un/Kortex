package com.kortex.backend.service;

import com.kortex.backend.model.Document;
import com.kortex.backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for AI-powered document summarization.
 * Uses LLM to generate concise summaries of document content.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SummarizationService {

    private final LLMService llmService;
    private final DocumentRepository documentRepository;

    @Value("${summarization.max.input.length:10000}")
    private int maxInputLength;

    @Value("${summarization.max.tokens:300}")
    private int maxTokens;

    @Value("${summarization.temperature:0.3}")
    private double temperature;

    /**
     * Generate a summary for a document.
     *
     * @param document the document to summarize
     * @return generated summary text
     */
    public String generateSummary(Document document) {
        if (document.getExtractedText() == null || document.getExtractedText().isBlank()) {
            log.warn("Cannot generate summary for document {} - no extracted text", document.getId());
            return null;
        }

        try {
            log.info("Generating summary for document {} ({})", document.getId(), document.getFilename());

            // Truncate text if too long
            String textToSummarize = truncateText(document.getExtractedText(), maxInputLength);

            // Build summarization prompt
            String prompt = buildSummarizationPrompt(textToSummarize, document.getFilename());

            // Generate summary using LLM
            String summary = llmService.generateCompletion(prompt, maxTokens, temperature);

            log.info("Successfully generated summary for document {}: {} characters",
                    document.getId(), summary.length());

            return summary;

        } catch (Exception e) {
            log.error("Failed to generate summary for document {}: {}",
                    document.getId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generate and save summary for a document.
     *
     * @param documentId the document ID
     * @return the generated summary, or null if failed
     */
    @Transactional
    public String generateAndSaveSummary(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        String summary = generateSummary(document);

        if (summary != null) {
            document.setSummary(summary);
            document.setSummaryGeneratedAt(LocalDateTime.now());
            documentRepository.save(document);
            log.info("Saved summary for document {}", documentId);
        }

        return summary;
    }

    /**
     * Regenerate summary for a document (e.g., after document update).
     *
     * @param documentId the document ID
     * @return the newly generated summary
     */
    @Transactional
    public String regenerateSummary(Long documentId) {
        log.info("Regenerating summary for document {}", documentId);
        return generateAndSaveSummary(documentId);
    }

    /**
     * Check if a document needs summary regeneration.
     * Returns true if document has no summary or if extracted text was updated after summary.
     *
     * @param document the document
     * @return true if summary should be regenerated
     */
    public boolean needsSummaryRegeneration(Document document) {
        if (document.getSummary() == null) {
            return true;
        }

        if (document.getSummaryGeneratedAt() == null) {
            return true;
        }

        // If document was updated after summary generation
        if (document.getUploadTime().isAfter(document.getSummaryGeneratedAt())) {
            return true;
        }

        return false;
    }

    /**
     * Build prompt for summarization.
     *
     * @param text the text to summarize
     * @param filename the document filename for context
     * @return formatted prompt
     */
    private String buildSummarizationPrompt(String text, String filename) {
        return String.format(
                "You are a helpful AI assistant that creates concise, accurate summaries of documents.\n\n" +
                "Document: %s\n\n" +
                "Please provide a clear and concise summary of the following text. " +
                "Focus on the main topics, key points, and important information. " +
                "Keep the summary between 3-5 sentences.\n\n" +
                "TEXT:\n%s\n\n" +
                "SUMMARY:",
                filename,
                text
        );
    }

    /**
     * Truncate text to maximum length while preserving sentence boundaries.
     *
     * @param text the full text
     * @param maxLength maximum length in characters
     * @return truncated text
     */
    private String truncateText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }

        // Try to truncate at sentence boundary
        String truncated = text.substring(0, maxLength);
        int lastPeriod = truncated.lastIndexOf('.');
        int lastQuestion = truncated.lastIndexOf('?');
        int lastExclamation = truncated.lastIndexOf('!');

        int lastSentenceEnd = Math.max(lastPeriod, Math.max(lastQuestion, lastExclamation));

        if (lastSentenceEnd > maxLength / 2) {
            // Good sentence boundary found
            return truncated.substring(0, lastSentenceEnd + 1);
        }

        // No good boundary, truncate at word boundary
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > 0) {
            return truncated.substring(0, lastSpace) + "...";
        }

        // Fallback: hard truncate
        return truncated + "...";
    }
}
