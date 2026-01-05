package com.kortex.backend.service;

import com.kortex.backend.model.Document;
import com.kortex.backend.model.DocumentChunk;
import com.kortex.backend.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for intelligent text chunking.
 * Splits extracted text into manageable chunks for embedding generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TextChunkingService {

    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;

    private static final int MIN_CHUNK_WORDS = 150;
    private static final int MAX_CHUNK_WORDS = 300;
    private static final int TARGET_CHUNK_WORDS = 225; // Middle of range

    /**
     * Chunk the extracted text from a document and generate embeddings.
     * Creates chunks of 150-300 words while maintaining order and document metadata links.
     * Stores vectors in Qdrant for semantic search.
     *
     * @param document the document to chunk
     * @param ownerId the owner ID for Qdrant collection namespace
     * @return list of created chunks with embeddings
     */
    @Transactional
    public List<DocumentChunk> chunkDocument(Document document, Long ownerId) {
        if (document.getExtractedText() == null || document.getExtractedText().trim().isEmpty()) {
            log.warn("Document {} has no extracted text to chunk", document.getId());
            return List.of();
        }

        // Delete existing chunks if re-chunking
        chunkRepository.deleteByDocumentId(document.getId());
        
        // Delete existing vectors from Qdrant
        try {
            qdrantService.deleteVectorsByDocumentId(ownerId, document.getId());
        } catch (Exception e) {
            log.warn("Failed to delete existing vectors for document {}", document.getId(), e);
        }

        String text = document.getExtractedText();
        List<DocumentChunk> chunks = createChunks(text, document.getId());

        // Generate embeddings for all chunks in batch
        generateEmbeddingsForChunks(chunks);

        // Save all chunks with embeddings
        chunks = chunkRepository.saveAll(chunks);
        log.info("Created {} chunks with embeddings for document {}", chunks.size(), document.getId());

        // Store vectors in Qdrant
        storeVectorsInQdrant(chunks, ownerId);

        return chunks;
    }

    /**
     * Create chunks from text using intelligent word-based splitting.
     * Maintains sentence boundaries where possible.
     *
     * @param text the text to chunk
     * @param documentId the document ID to link chunks to
     * @return list of chunk entities (not yet persisted)
     */
    private List<DocumentChunk> createChunks(String text, Long documentId) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");
        
        if (words.length == 0) {
            return chunks;
        }

        int chunkOrder = 0;
        int currentPosition = 0;
        int wordIndex = 0;

        while (wordIndex < words.length) {
            int chunkStartPosition = currentPosition;
            StringBuilder chunkText = new StringBuilder();
            int wordCount = 0;

            // Build chunk up to target size
            while (wordIndex < words.length && wordCount < MAX_CHUNK_WORDS) {
                if (chunkText.length() > 0) {
                    chunkText.append(" ");
                    currentPosition++; // Account for space
                }
                
                String word = words[wordIndex];
                chunkText.append(word);
                currentPosition += word.length();
                wordCount++;
                wordIndex++;

                // If we've reached minimum size, look for sentence boundary
                if (wordCount >= MIN_CHUNK_WORDS && wordCount < MAX_CHUNK_WORDS) {
                    if (isSentenceEnd(word)) {
                        break; // Good breaking point
                    }
                }
            }

            // Create chunk if we have content
            if (chunkText.length() > 0) {
                DocumentChunk chunk = DocumentChunk.builder()
                        .documentId(documentId)
                        .chunkText(chunkText.toString().trim())
                        .chunkOrder(chunkOrder)
                        .wordCount(wordCount)
                        .startPosition(chunkStartPosition)
                        .endPosition(currentPosition)
                        .build();
                
                chunks.add(chunk);
                chunkOrder++;
            }
        }

        return chunks;
    }

    /**
     * Check if a word marks the end of a sentence.
     * Used to find natural breaking points for chunks.
     *
     * @param word the word to check
     * @return true if word ends with sentence-ending punctuation
     */
    private boolean isSentenceEnd(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }
        char lastChar = word.charAt(word.length() - 1);
        return lastChar == '.' || lastChar == '!' || lastChar == '?';
    }

    /**
     * Get all chunks for a document in order.
     *
     * @param documentId the document ID
     * @return list of chunks in sequential order
     */
    public List<DocumentChunk> getDocumentChunks(Long documentId) {
        return chunkRepository.findByDocumentIdOrderByChunkOrderAsc(documentId);
    }

    /**
     * Delete all chunks for a document.
     * Called when document is deleted.
     *
     * @param documentId the document ID
     * @param ownerId the owner ID for Qdrant collection namespace
     */
    @Transactional
    public void deleteDocumentChunks(Long documentId, Long ownerId) {
        chunkRepository.deleteByDocumentId(documentId);
        
        // Delete vectors from Qdrant
        try {
            qdrantService.deleteVectorsByDocumentId(ownerId, documentId);
        } catch (Exception e) {
            log.warn("Failed to delete vectors for document {} from Qdrant", documentId, e);
        }
        
        log.info("Deleted all chunks for document {}", documentId);
    }

    /**
     * Generate embeddings for a list of chunks using batch processing.
     * Updates each chunk with its embedding vector.
     *
     * @param chunks the chunks to generate embeddings for
     */
    private void generateEmbeddingsForChunks(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        try {
            // Extract chunk texts for batch processing
            List<String> chunkTexts = chunks.stream()
                    .map(DocumentChunk::getChunkText)
                    .toList();

            // Generate embeddings in batch
            log.debug("Generating embeddings for {} chunks", chunks.size());
            List<double[]> embeddings = embeddingService.generateEmbeddingsBatch(chunkTexts);

            // Assign embeddings to chunks
            for (int i = 0; i < chunks.size() && i < embeddings.size(); i++) {
                chunks.get(i).setEmbedding(embeddings.get(i));
                chunks.get(i).setEmbeddingGenerated(true);
            }

            log.info("Successfully generated embeddings for {} chunks", chunks.size());
        } catch (Exception e) {
            log.error("Error generating embeddings for chunks", e);
            // Mark all chunks as failed embedding generation
            chunks.forEach(chunk -> chunk.setEmbeddingGenerated(false));
            // Don't throw exception - allow chunks to be saved without embeddings
        }
    }

    /**
     * Store vectors in Qdrant for semantic search.
     * Uses batch processing for efficiency.
     *
     * @param chunks the chunks with embeddings to store
     * @param ownerId the owner ID for collection namespace
     */
    private void storeVectorsInQdrant(List<DocumentChunk> chunks, Long ownerId) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        try {
            List<QdrantService.VectorData> vectorDataList = chunks.stream()
                    .filter(chunk -> chunk.getEmbedding() != null && chunk.getEmbeddingGenerated())
                    .map(chunk -> {
                        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
                        metadata.put("chunkOrder", chunk.getChunkOrder());
                        metadata.put("wordCount", chunk.getWordCount());
                        
                        return new QdrantService.VectorData(
                                chunk.getId(),
                                chunk.getDocumentId(),
                                chunk.getEmbedding(),
                                metadata
                        );
                    })
                    .toList();

            if (!vectorDataList.isEmpty()) {
                qdrantService.storeVectorsBatch(ownerId, vectorDataList);
                log.info("Stored {} vectors in Qdrant for owner {}", vectorDataList.size(), ownerId);
            }
        } catch (Exception e) {
            log.error("Failed to store vectors in Qdrant", e);
            // Don't throw - chunks are already saved in database
        }
    }
}
