package com.kortex.backend.repository;

import com.kortex.backend.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for DocumentChunk entity.
 * Handles database operations for document chunks.
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    /**
     * Find all chunks for a given document, ordered by chunk order.
     *
     * @param documentId the document ID
     * @return list of chunks in sequential order
     */
    List<DocumentChunk> findByDocumentIdOrderByChunkOrderAsc(Long documentId);

    /**
     * Delete all chunks for a given document.
     * Used when re-chunking or deleting a document.
     *
     * @param documentId the document ID
     */
    void deleteByDocumentId(Long documentId);

    /**
     * Count the number of chunks for a given document.
     *
     * @param documentId the document ID
     * @return number of chunks
     */
    long countByDocumentId(Long documentId);
}
