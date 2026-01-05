package com.kortex.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for embedding generation status monitoring.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingStatusResponse {
    private Integer totalDocuments;
    private Integer documentsWithEmbeddings;
    private Integer documentsPending;
    private Integer documentsFailed;
    private Double completionPercentage;
    
    // Chunk-level statistics
    private Integer totalChunks;
    private Integer chunksWithEmbeddings;
    private Integer chunksPending;
    
    // Failed documents
    private List<FailedEmbedding> failedDocuments;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedEmbedding {
        private Long documentId;
        private String documentName;
        private String ownerEmail;
        private Integer totalChunks;
        private Integer failedChunks;
        private LocalDateTime uploadTime;
        private String errorMessage;
    }
}
