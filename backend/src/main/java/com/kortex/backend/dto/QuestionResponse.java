package com.kortex.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for question answering response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {

    private String question;
    private String answer;
    private List<CitationDto> citations;
    private Integer contextChunksUsed;
    private Integer estimatedTokens;
    private String llmProvider;
    private String llmModel;
    private Long documentId; // Present if question was document-specific

    /**
     * Citation DTO with source information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CitationDto {
        private Integer citationNumber;
        private Long documentId;
        private String documentName;
        private Long chunkId;
        private Integer chunkOrder;
        private Float relevanceScore;
        private String excerpt;
    }
}
