package com.kortex.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for analytics dashboard overview statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsOverviewResponse {
    
    /**
     * Total number of documents.
     */
    private long totalDocuments;
    
    /**
     * Total word count across all documents.
     */
    private long totalWords;
    
    /**
     * Average words per document.
     */
    private double averageWordsPerDocument;
    
    /**
     * Estimated total reading time in minutes.
     */
    private long totalReadingTimeMinutes;
    
    /**
     * Average reading time per document in minutes.
     */
    private double averageReadingTimeMinutes;
    
    /**
     * Total storage used in bytes.
     */
    private long totalStorageBytes;
    
    /**
     * Total storage used (human-readable).
     */
    private String totalStorageFormatted;
    
    /**
     * Number of documents with successful text extraction.
     */
    private long documentsWithText;
    
    /**
     * Number of documents with embeddings.
     */
    private long documentsWithEmbeddings;
    
    /**
     * Number of documents with AI summaries.
     */
    private long documentsWithSummaries;
    
    /**
     * Number of documents with tags.
     */
    private long documentsWithTags;
    
    /**
     * File type distribution (e.g., {"PDF": 45, "DOCX": 30}).
     */
    private Map<String, Long> fileTypeDistribution;
}
