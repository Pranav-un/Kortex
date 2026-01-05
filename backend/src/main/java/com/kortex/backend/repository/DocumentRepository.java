package com.kortex.backend.repository;

import com.kortex.backend.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Document entity.
 * Provides database access methods for document management.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * Find all documents owned by a specific user.
     *
     * @param ownerId the owner's user ID
     * @return list of documents owned by the user
     */
    List<Document> findByOwnerId(Long ownerId);

    /**
     * Find a document by ID and owner ID.
     *
     * @param id the document ID
     * @param ownerId the owner's user ID
     * @return Optional containing the document if found and owned by user
     */
    Optional<Document> findByIdAndOwnerId(Long id, Long ownerId);

    /**
     * Find a document by stored filename.
     *
     * @param storedFilename the stored filename
     * @return Optional containing the document if found
     */
    Optional<Document> findByStoredFilename(String storedFilename);

    /**
     * Find the latest version of a document by filename and owner.
     *
     * @param filename the original filename
     * @param ownerId the owner's user ID
     * @return Optional containing the latest version if found
     */
    Optional<Document> findFirstByFilenameAndOwnerIdOrderByVersionDesc(String filename, Long ownerId);

    /**
     * Find all versions of a document by filename and owner.
     *
     * @param filename the original filename
     * @param ownerId the owner's user ID
     * @return list of all versions ordered by version descending
     */
    List<Document> findByFilenameAndOwnerIdOrderByVersionDesc(String filename, Long ownerId);

    /**
     * Count documents with the same filename for a user.
     *
     * @param filename the filename
     * @param ownerId the owner's user ID
     * @return count of documents with that filename
     */
    long countByFilenameAndOwnerId(String filename, Long ownerId);
}
