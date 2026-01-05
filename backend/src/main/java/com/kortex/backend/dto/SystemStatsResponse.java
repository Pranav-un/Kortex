package com.kortex.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for system-wide statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatsResponse {
    // User statistics
    private Integer totalUsers;
    private Integer activeUsers;
    private Integer inactiveUsers;
    private Integer adminUsers;
    
    // Document statistics
    private Integer totalDocuments;
    private Long totalStorageBytes;
    private String totalStorageFormatted;
    private Map<String, Integer> documentsByFileType;
    
    // Processing statistics
    private Integer documentsWithText;
    private Integer documentsWithEmbeddings;
    private Integer documentsWithSummaries;
    private Integer documentsWithTags;
    private Double processingSuccessRate;
    
    // Embedding statistics
    private Integer totalChunks;
    private Integer chunksWithEmbeddings;
    private Double embeddingCoverage;
    
    // Recent activity
    private Integer uploadsLast24Hours;
    private Integer uploadsLast7Days;
    private Integer uploadsLast30Days;
}
