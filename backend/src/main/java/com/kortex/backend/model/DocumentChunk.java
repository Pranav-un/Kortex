package com.kortex.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DocumentChunk entity representing a text chunk from a document.
 * Each document's extracted text is split into manageable chunks for embedding generation.
 */
@Entity
@Table(name = "document_chunks", indexes = {
        @Index(name = "idx_document_id", columnList = "documentId"),
        @Index(name = "idx_document_chunk_order", columnList = "documentId,chunkOrder")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Document ID is required")
    @Column(nullable = false)
    private Long documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documentId", insertable = false, updatable = false)
    private Document document;

    @NotBlank(message = "Chunk text is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    /**
     * Order of this chunk within the document (0-based).
     * Maintains the sequential position for proper context.
     */
    @NotNull(message = "Chunk order is required")
    @Column(nullable = false)
    private Integer chunkOrder;

    /**
     * Word count of this chunk.
     * Used for validation and analytics.
     */
    @NotNull(message = "Word count is required")
    @Column(nullable = false)
    private Integer wordCount;

    /**
     * Character start position in the original document text.
     * Enables mapping back to the source document.
     */
    @Column
    private Integer startPosition;

    /**
     * Character end position in the original document text.
     * Enables mapping back to the source document.
     */
    @Column
    private Integer endPosition;

    /**
     * Vector embedding representation of the chunk text.
     * Generated using sentence-transformer models (e.g., all-MiniLM-L6-v2).
     * Stored as PostgreSQL array of doubles for semantic search.
     */
    @Column(columnDefinition = "double precision[]")
    private double[] embedding;

    /**
     * Indicates if embedding generation was successful.
     * Used for monitoring and retry logic.
     */
    @Column
    @Builder.Default
    private Boolean embeddingGenerated = false;
}
