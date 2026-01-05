package com.kortex.backend.model;

/**
 * Enum representing different types of notifications in the system.
 */
public enum NotificationType {
    /**
     * Document has been successfully uploaded.
     */
    DOCUMENT_UPLOADED,
    
    /**
     * Document has been deleted.
     */
    DOCUMENT_DELETED,
    
    /**
     * Document text extraction has completed.
     */
    TEXT_EXTRACTION_COMPLETE,
    
    /**
     * Document text extraction has failed.
     */
    TEXT_EXTRACTION_FAILED,
    
    /**
     * Document embeddings have been generated.
     */
    EMBEDDINGS_GENERATED,
    
    /**
     * Document summary has been generated.
     */
    SUMMARY_GENERATED,
    
    /**
     * Document summary regeneration has completed.
     */
    SUMMARY_REGENERATED,
    
    /**
     * Document tags have been generated.
     */
    TAGS_GENERATED,
    
    /**
     * Document tags regeneration has completed.
     */
    TAGS_REGENERATED,
    
    /**
     * Question answering completed (RAG + LLM).
     */
    QUESTION_ANSWERED,
    
    /**
     * General system notification.
     */
    SYSTEM
}
