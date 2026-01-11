package com.kortex.backend.service;

import com.kortex.backend.dto.DocumentResponse;
import com.kortex.backend.dto.DocumentUploadResponse;
import com.kortex.backend.exception.ResourceNotFoundException;
import com.kortex.backend.model.Document;
import com.kortex.backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for document management operations.
 * Handles file upload, storage, deletion, listing, and text extraction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final TextExtractionService textExtractionService;
    private final TextChunkingService textChunkingService;
    private final SummarizationService summarizationService;
    private final TaggingService taggingService;
    private final NotificationService notificationService;

    @Value("${document.storage.path}")
    private String storageBasePath;

    private static final List<String> ALLOWED_FILE_TYPES = Arrays.asList(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "text/plain"
    );

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "docx", "doc", "txt");

    /**
     * Uploads a document file to the system storage and creates a database record.
     * Handles versioning: if a file with the same name exists, creates a new version.
     *
     * @param file the uploaded file
     * @param userId the owner's user ID
     * @return document upload response
     * @throws IOException if file storage fails
     * @throws IllegalArgumentException if file type is not allowed
     */
    @Transactional
    public DocumentUploadResponse uploadDocument(MultipartFile file, Long userId) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        // Validate file type
        String contentType = file.getContentType();
        String extension = getFileExtension(originalFilename);

        if (!ALLOWED_FILE_TYPES.contains(contentType) || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("File type not allowed. Only PDF, DOCX, DOC, and TXT files are supported.");
        }

        // Calculate content hash for duplicate detection
        String contentHash = calculateFileHash(file);

        // Check for existing versions of this file
        Optional<Document> latestVersion = documentRepository
                .findFirstByFilenameAndOwnerIdOrderByVersionDesc(originalFilename, userId);

        Integer version = 1;
        Long previousVersionId = null;
        String message = "File uploaded successfully";

        if (latestVersion.isPresent()) {
            Document existingDoc = latestVersion.get();
            
            // Check if content is identical (duplicate)
            if (contentHash.equals(existingDoc.getContentHash())) {
                throw new IllegalArgumentException("Duplicate file detected. This exact file version already exists.");
            }

            // Create new version
            version = existingDoc.getVersion() + 1;
            previousVersionId = existingDoc.getId();
            message = "New version created (v" + version + ")";
        }

        // Generate unique stored filename
        String storedFilename = UUID.randomUUID().toString() + "_" + originalFilename;

        // Ensure storage directory exists
        Path storagePath = Paths.get(storageBasePath);
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }

        // Store file
        Path filePath = storagePath.resolve(storedFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Extract text from document
        String extractedText = null;
        Integer pageCount = null;
        boolean textExtractionSuccess = false;

        try {
            extractedText = textExtractionService.extractText(filePath.toFile(), contentType);
            if (extractedText != null && !extractedText.isBlank()) {
                textExtractionSuccess = true;
                pageCount = textExtractionService.estimatePageCount(extractedText);
            }
        } catch (Exception e) {
            // Log but don't fail the upload if text extraction fails
            log.warn("Text extraction failed for file: {}", originalFilename, e);
        }

        // Save metadata to database
        Document document = Document.builder()
                .filename(originalFilename)
                .fileType(contentType)
                .size(file.getSize())
                .storedFilename(storedFilename)
                .ownerId(userId)
                .version(version)
                .previousVersionId(previousVersionId)
                .contentHash(contentHash)
                .extractedText(extractedText)
                .textExtractionSuccess(textExtractionSuccess)
                .pageCount(pageCount)
                .build();

        Document savedDocument = documentRepository.save(document);

        // Send document uploaded notification
        try {
            notificationService.sendDocumentUploadedNotification(userId, savedDocument.getId(), savedDocument.getFilename());
        } catch (Exception e) {
            log.error("Failed to send upload notification: {}", e.getMessage());
        }

        // Chunk the extracted text if extraction was successful
        if (textExtractionSuccess && extractedText != null) {
            // Send text extraction complete notification
            try {
                int wordCount = extractedText.split("\\s+").length;
                notificationService.sendTextExtractionCompleteNotification(userId, savedDocument.getId(), savedDocument.getFilename(), wordCount);
            } catch (Exception e) {
                log.error("Failed to send text extraction notification: {}", e.getMessage());
            }

            // Chunk and generate embeddings
            int chunkCount = 0;
            try {
                chunkCount = textChunkingService.chunkDocument(savedDocument, userId).size();
                // Send embeddings generated notification
                notificationService.sendEmbeddingsGeneratedNotification(userId, savedDocument.getId(), savedDocument.getFilename(), chunkCount);
            } catch (Exception e) {
                // Log but don't fail upload if chunking fails
                log.warn("Text chunking failed for document: {}", savedDocument.getId(), e);
            }

            // Generate AI summary
            try {
                summarizationService.generateAndSaveSummary(savedDocument.getId());
                notificationService.sendSummaryGeneratedNotification(userId, savedDocument.getId(), savedDocument.getFilename());
            } catch (Exception e) {
                // Log but don't fail upload if summarization fails
                log.warn("Summarization failed for document: {}", savedDocument.getId(), e);
            }

            // Extract tags
            try {
                String tags = taggingService.extractAndSaveTags(savedDocument.getId());
                int tagCount = tags != null ? tags.split(",").length : 0;
                notificationService.sendTagsGeneratedNotification(userId, savedDocument.getId(), savedDocument.getFilename(), tagCount);
            } catch (Exception e) {
                // Log but don't fail upload if tagging fails
                log.warn("Tagging failed for document: {}", savedDocument.getId(), e);
            }
        } else {
            // Send text extraction failed notification
            try {
                notificationService.sendTextExtractionFailedNotification(userId, savedDocument.getId(), savedDocument.getFilename());
            } catch (Exception e) {
                log.error("Failed to send extraction failed notification: {}", e.getMessage());
            }
        }

        return DocumentUploadResponse.builder()
                .id(savedDocument.getId())
                .filename(savedDocument.getFilename())
                .fileType(savedDocument.getFileType())
                .size(savedDocument.getSize())
                .uploadTime(savedDocument.getUploadTime())
                .version(savedDocument.getVersion())
                .previousVersionId(savedDocument.getPreviousVersionId())
                .message(message)
                .size(savedDocument.getSize())
                .uploadTime(savedDocument.getUploadTime())
                .message("File uploaded successfully")
                .build();
    }

    /**
     * Get all documents for a user.
     *
     * @param userId the owner's user ID
     * @return list of document responses
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getUserDocuments(Long userId) {
        List<Document> documents = documentRepository.findByOwnerId(userId);

        return documents.stream()
                .map(this::mapToDocumentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific document by ID for a user.
     *
     * @param documentId the document ID
     * @param userId the owner's user ID
     * @return document response
     * @throws ResourceNotFoundException if document not found or not owned by user
     */
    @Transactional(readOnly = true)
    public DocumentResponse getDocument(Long documentId, Long userId) {
        Document document = documentRepository.findByIdAndOwnerId(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found or access denied"));

        return mapToDocumentResponse(document);
    }

    /**
     * Delete a document.
     *
     * @param documentId the document ID
     * @param userId the owner's user ID
     * @throws ResourceNotFoundException if document not found or not owned by user
     * @throws IOException if file deletion fails
     */
    @Transactional
    public void deleteDocument(Long documentId, Long userId) throws IOException {
        Document document = documentRepository.findByIdAndOwnerId(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found or access denied"));

        String filename = document.getFilename();

        // Delete chunks first (also deletes from Qdrant)
        textChunkingService.deleteDocumentChunks(documentId, userId);

        // Delete file from filesystem
        Path filePath = Paths.get(storageBasePath).resolve(document.getStoredFilename());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        // Delete metadata from database
        documentRepository.delete(document);

        // Send document deleted notification
        try {
            notificationService.sendDocumentDeletedNotification(userId, filename);
        } catch (Exception e) {
            log.error("Failed to send delete notification: {}", e.getMessage());
        }
    }

    /**
     * Get file extension from filename.
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }

    /**
     * Map Document entity to DocumentResponse DTO.
     */
    private DocumentResponse mapToDocumentResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .fileType(document.getFileType())
                .size(document.getSize())
                .uploadTime(document.getUploadTime())
                .ownerId(document.getOwnerId())
                .version(document.getVersion())
                .previousVersionId(document.getPreviousVersionId())
                .pageCount(document.getPageCount())
                .contentHash(document.getContentHash())
                .summary(document.getSummary())
                .summaryGeneratedAt(document.getSummaryGeneratedAt())
                .tags(document.getTags())
                .tagsGeneratedAt(document.getTagsGeneratedAt())
                .build();
    }

    /**
     * Calculate SHA-256 hash of file content.
     *
     * @param file the file to hash
     * @return hex string of file hash
     * @throws IOException if file reading fails
     */
    private String calculateFileHash(MultipartFile file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(file.getBytes());
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
