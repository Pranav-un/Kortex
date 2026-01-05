package com.kortex.backend.service;

import com.kortex.backend.model.Document;
import com.kortex.backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for automatic keyword extraction and document tagging.
 * Uses LLM to extract relevant tags from document content.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaggingService {

    private final LLMService llmService;
    private final DocumentRepository documentRepository;

    @Value("${tagging.max.input.length:8000}")
    private int maxInputLength;

    @Value("${tagging.max.tags:10}")
    private int maxTags;

    @Value("${tagging.max.tokens:200}")
    private int maxTokens;

    @Value("${tagging.temperature:0.3}")
    private double temperature;

    /**
     * Extract tags/keywords from a document.
     *
     * @param document the document to tag
     * @return comma-separated list of tags
     */
    public String extractTags(Document document) {
        if (document.getExtractedText() == null || document.getExtractedText().isBlank()) {
            log.warn("Cannot extract tags for document {} - no extracted text", document.getId());
            return null;
        }

        try {
            log.info("Extracting tags for document {} ({})", document.getId(), document.getFilename());

            // Use summary if available, otherwise truncate extracted text
            String textToAnalyze = document.getSummary() != null && !document.getSummary().isBlank()
                    ? document.getSummary()
                    : truncateText(document.getExtractedText(), maxInputLength);

            // Build tagging prompt
            String prompt = buildTaggingPrompt(textToAnalyze, document.getFilename());

            // Generate tags using LLM
            String tagsResponse = llmService.generateCompletion(prompt, maxTokens, temperature);

            // Parse and clean tags
            List<String> tagList = parseTags(tagsResponse);

            if (tagList.isEmpty()) {
                log.warn("No tags extracted for document {}", document.getId());
                return null;
            }

            String tags = String.join(", ", tagList);
            log.info("Extracted {} tags for document {}: {}", tagList.size(), document.getId(), tags);

            return tags;

        } catch (Exception e) {
            log.error("Failed to extract tags for document {}: {}", document.getId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extract and save tags for a document.
     *
     * @param documentId the document ID
     * @return the extracted tags, or null if failed
     */
    @Transactional
    public String extractAndSaveTags(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        String tags = extractTags(document);

        if (tags != null) {
            document.setTags(tags);
            document.setTagsGeneratedAt(LocalDateTime.now());
            documentRepository.save(document);
            log.info("Saved tags for document {}", documentId);
        }

        return tags;
    }

    /**
     * Regenerate tags for a document.
     *
     * @param documentId the document ID
     * @return the newly generated tags
     */
    @Transactional
    public String regenerateTags(Long documentId) {
        log.info("Regenerating tags for document {}", documentId);
        return extractAndSaveTags(documentId);
    }

    /**
     * Get tag list from comma-separated tags string.
     *
     * @param tags comma-separated tags
     * @return list of individual tags
     */
    public List<String> getTagList(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Build prompt for tag extraction.
     *
     * @param text the text to analyze
     * @param filename the document filename
     * @return formatted prompt
     */
    private String buildTaggingPrompt(String text, String filename) {
        return String.format(
                "You are a helpful AI assistant that extracts relevant keywords and tags from documents.\n\n" +
                "Document: %s\n\n" +
                "Analyze the following text and extract up to %d relevant keywords or tags that best describe the content. " +
                "Focus on main topics, concepts, technologies, methodologies, or themes. " +
                "Return ONLY the tags as a comma-separated list, nothing else.\n\n" +
                "TEXT:\n%s\n\n" +
                "TAGS:",
                filename,
                maxTags,
                text
        );
    }

    /**
     * Parse tags from LLM response.
     * Handles various response formats and cleans up the tags.
     *
     * @param response LLM response containing tags
     * @return list of cleaned tags
     */
    private List<String> parseTags(String response) {
        if (response == null || response.isBlank()) {
            return List.of();
        }

        // Remove common prefixes/suffixes
        response = response.trim()
                .replaceAll("(?i)^(tags:|keywords:|tags are:|keywords are:)\\s*", "")
                .replaceAll("\\.$", "");

        // Split by comma and clean
        return Arrays.stream(response.split("[,;\\n]"))
                .map(String::trim)
                .map(tag -> tag.replaceAll("^[\"'\\[\\]()]+|[\"'\\[\\]()]+$", "")) // Remove quotes/brackets
                .filter(tag -> !tag.isEmpty())
                .filter(tag -> tag.length() >= 2) // Minimum tag length
                .filter(tag -> tag.length() <= 50) // Maximum tag length
                .limit(maxTags)
                .collect(Collectors.toList());
    }

    /**
     * Truncate text to maximum length.
     *
     * @param text the full text
     * @param maxLength maximum length in characters
     * @return truncated text
     */
    private String truncateText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
