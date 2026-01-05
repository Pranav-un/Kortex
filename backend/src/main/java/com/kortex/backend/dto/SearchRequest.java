package com.kortex.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for semantic search requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequest {

    @NotBlank(message = "Query is required")
    private String query;

    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit cannot exceed 100")
    @Builder.Default
    private Integer limit = 10;

    /**
     * Optional: Filter search to specific document.
     * If null, searches across all user's documents.
     */
    private Long documentId;
}
