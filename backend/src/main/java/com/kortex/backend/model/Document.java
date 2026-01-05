package com.kortex.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Document entity representing uploaded files.
 * Stores metadata about documents while files are stored in filesystem.
 */
@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Filename is required")
    @Column(nullable = false)
    private String filename;

    @NotBlank(message = "File type is required")
    @Column(nullable = false, length = 50)
    private String fileType;

    @NotNull(message = "File size is required")
    @Column(nullable = false)
    private Long size;

    @Column(nullable = false, unique = true)
    private String storedFilename;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadTime;

    @NotNull(message = "Owner ID is required")
    @Column(nullable = false)
    private Long ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ownerId", insertable = false, updatable = false)
    private User owner;

    /**
     * Version number of the document.
     * Starts at 1 for initial upload, increments on each re-upload of same filename.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    /**
     * Reference to previous version's document ID (if this is a new version).
     */
    @Column
    private Long previousVersionId;

    /**
     * Number of pages in the document (for preview support).
     * Null if not yet processed or not applicable.
     */
    @Column
    private Integer pageCount;

    /**
     * Hash of the file content for duplicate detection.
     */
    @Column(length = 64)
    private String contentHash;

    /**
     * Extracted and preprocessed text content from the document.
     * Null if extraction hasn't been performed or failed.
     */
    @Column(columnDefinition = "TEXT")
    private String extractedText;

    /**
     * Indicates if text extraction was successful.
     */
    @Column
    @Builder.Default
    private Boolean textExtractionSuccess = false;

    /**
     * AI-generated summary of the document content.
     * Generated automatically on upload using LLM.
     */
    @Column(columnDefinition = "TEXT")
    private String summary;

    /**
     * Timestamp when the summary was last generated.
     */
    @Column
    private LocalDateTime summaryGeneratedAt;

    /**
     * Auto-generated tags/keywords extracted from document content.
     * Stored as comma-separated string.
     */
    @Column(columnDefinition = "TEXT")
    private String tags;

    /**
     * Timestamp when tags were last generated.
     */
    @Column
    private LocalDateTime tagsGeneratedAt;
}
