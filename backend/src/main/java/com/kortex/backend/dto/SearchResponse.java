package com.kortex.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for semantic search response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponse {

    private String query;
    private Integer resultsCount;
    private List<SearchResultItem> results;

    /**
     * Individual search result item.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchResultItem {
        
        private Long chunkId;
        private Long documentId;
        private String documentName;
        private Float similarityScore;
        private String chunkText;
        private Integer chunkOrder;
        private Integer wordCount;
        private Integer startPosition;
        private Integer endPosition;
    }
}
