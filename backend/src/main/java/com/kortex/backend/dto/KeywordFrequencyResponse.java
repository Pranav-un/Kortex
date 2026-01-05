package com.kortex.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for keyword frequency analytics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeywordFrequencyResponse {
    
    /**
     * List of keywords with their frequencies.
     */
    private List<KeywordCount> keywords;
    
    /**
     * Total unique keywords.
     */
    private int totalUniqueKeywords;
    
    /**
     * DTO for individual keyword count.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KeywordCount {
        /**
         * The keyword/tag.
         */
        private String keyword;
        
        /**
         * Number of documents containing this keyword.
         */
        private long documentCount;
        
        /**
         * Frequency percentage (0-100).
         */
        private double frequencyPercentage;
    }
}
