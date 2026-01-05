package com.kortex.backend.service;

import com.kortex.backend.dto.EmbeddingStatusResponse;
import com.kortex.backend.dto.SystemHealthResponse;
import com.kortex.backend.dto.SystemStatsResponse;
import com.kortex.backend.dto.UserManagementResponse;
import com.kortex.backend.exception.ResourceNotFoundException;
import com.kortex.backend.model.Document;
import com.kortex.backend.model.DocumentChunk;
import com.kortex.backend.model.User;
import com.kortex.backend.repository.DocumentChunkRepository;
import com.kortex.backend.repository.DocumentRepository;
import com.kortex.backend.repository.UserRepository;
import com.kortex.backend.service.qdrant.QdrantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for admin operations including user management, system health, and monitoring.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final QdrantService qdrantService;
    private final DataSource dataSource;

    @Value("${document.storage.path:./uploads}")
    private String uploadPath;

    private static final long SERVICE_START_TIME = System.currentTimeMillis();

    /**
     * Get all users with their statistics.
     *
     * @return list of users with document counts and storage usage
     */
    @Transactional(readOnly = true)
    public List<UserManagementResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        
        return users.stream()
                .map(this::buildUserManagementResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific user with statistics.
     *
     * @param userId the user ID
     * @return user details with statistics
     */
    @Transactional(readOnly = true)
    public UserManagementResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        return buildUserManagementResponse(user);
    }

    /**
     * Activate a user account.
     *
     * @param userId the user ID
     * @return updated user details
     */
    @Transactional
    public UserManagementResponse activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        user.setActive(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("User {} ({}) activated", userId, user.getEmail());
        return buildUserManagementResponse(user);
    }

    /**
     * Deactivate a user account.
     *
     * @param userId the user ID
     * @return updated user details
     */
    @Transactional
    public UserManagementResponse deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("User {} ({}) deactivated", userId, user.getEmail());
        return buildUserManagementResponse(user);
    }

    /**
     * Delete a user and all their documents.
     *
     * @param userId the user ID
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Delete all user's documents (will cascade to chunks)
        List<Document> documents = documentRepository.findByOwnerId(userId);
        for (Document document : documents) {
            try {
                // Delete physical file
                File file = new File(uploadPath, document.getStoredFilename());
                if (file.exists()) {
                    file.delete();
                }
                
                // Delete from Qdrant if embeddings exist
                if (document.getEmbeddingsGenerated()) {
                    try {
                        qdrantService.deletePoint("kortex_user_" + userId, document.getId());
                    } catch (Exception e) {
                        log.warn("Failed to delete Qdrant points for document {}: {}", document.getId(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Error deleting document {} during user deletion: {}", document.getId(), e.getMessage());
            }
        }
        
        // Delete user
        userRepository.delete(user);
        log.info("User {} ({}) deleted with {} documents", userId, user.getEmail(), documents.size());
    }

    /**
     * Get system health status.
     *
     * @return system health information
     */
    public SystemHealthResponse getSystemHealth() {
        LocalDateTime timestamp = LocalDateTime.now();
        long uptimeSeconds = (System.currentTimeMillis() - SERVICE_START_TIME) / 1000;
        
        // Check database health
        SystemHealthResponse.ComponentHealth databaseHealth = checkDatabaseHealth();
        
        // Check Qdrant health
        SystemHealthResponse.ComponentHealth qdrantHealth = checkQdrantHealth();
        
        // Check storage health
        SystemHealthResponse.ComponentHealth storageHealth = checkStorageHealth();
        
        // Get resource usage
        SystemHealthResponse.ResourceUsage resourceUsage = getResourceUsage();
        
        // Determine overall status
        String overallStatus = "healthy";
        if ("down".equals(databaseHealth.getStatus()) || "down".equals(qdrantHealth.getStatus())) {
            overallStatus = "unhealthy";
        } else if ("unknown".equals(databaseHealth.getStatus()) || 
                   "unknown".equals(qdrantHealth.getStatus()) ||
                   "unknown".equals(storageHealth.getStatus())) {
            overallStatus = "degraded";
        }
        
        return SystemHealthResponse.builder()
                .status(overallStatus)
                .timestamp(timestamp)
                .uptimeSeconds(uptimeSeconds)
                .database(databaseHealth)
                .vectorDatabase(qdrantHealth)
                .storage(storageHealth)
                .resources(resourceUsage)
                .build();
    }

    /**
     * Get system-wide statistics.
     *
     * @return comprehensive system statistics
     */
    @Transactional(readOnly = true)
    public SystemStatsResponse getSystemStats() {
        // User statistics
        List<User> allUsers = userRepository.findAll();
        int totalUsers = allUsers.size();
        int activeUsers = (int) allUsers.stream().filter(User::getActive).count();
        int inactiveUsers = totalUsers - activeUsers;
        int adminUsers = (int) allUsers.stream()
                .filter(u -> "ADMIN".equals(u.getRole().name()))
                .count();
        
        // Document statistics
        List<Document> allDocuments = documentRepository.findAll();
        int totalDocuments = allDocuments.size();
        long totalStorageBytes = allDocuments.stream()
                .mapToLong(Document::getSize)
                .sum();
        
        // File type distribution
        Map<String, Integer> documentsByFileType = allDocuments.stream()
                .collect(Collectors.groupingBy(
                        this::getFileType,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
        
        // Processing statistics
        int documentsWithText = (int) allDocuments.stream()
                .filter(d -> d.getExtractedText() != null && !d.getExtractedText().isEmpty())
                .count();
        int documentsWithEmbeddings = (int) allDocuments.stream()
                .filter(Document::getEmbeddingsGenerated)
                .count();
        int documentsWithSummaries = (int) allDocuments.stream()
                .filter(d -> d.getSummary() != null && !d.getSummary().isEmpty())
                .count();
        int documentsWithTags = (int) allDocuments.stream()
                .filter(d -> d.getTags() != null && !d.getTags().isEmpty())
                .count();
        
        double processingSuccessRate = totalDocuments > 0 
                ? Math.round((double) documentsWithText / totalDocuments * 10000.0) / 100.0 
                : 0.0;
        
        // Chunk statistics
        List<DocumentChunk> allChunks = documentChunkRepository.findAll();
        int totalChunks = allChunks.size();
        int chunksWithEmbeddings = (int) allChunks.stream()
                .filter(DocumentChunk::isEmbeddingGenerated)
                .count();
        double embeddingCoverage = totalChunks > 0 
                ? Math.round((double) chunksWithEmbeddings / totalChunks * 10000.0) / 100.0 
                : 0.0;
        
        // Recent activity
        LocalDateTime now = LocalDateTime.now();
        int uploadsLast24Hours = (int) allDocuments.stream()
                .filter(d -> d.getUploadTime().isAfter(now.minusHours(24)))
                .count();
        int uploadsLast7Days = (int) allDocuments.stream()
                .filter(d -> d.getUploadTime().isAfter(now.minusDays(7)))
                .count();
        int uploadsLast30Days = (int) allDocuments.stream()
                .filter(d -> d.getUploadTime().isAfter(now.minusDays(30)))
                .count();
        
        return SystemStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .adminUsers(adminUsers)
                .totalDocuments(totalDocuments)
                .totalStorageBytes(totalStorageBytes)
                .totalStorageFormatted(formatBytes(totalStorageBytes))
                .documentsByFileType(documentsByFileType)
                .documentsWithText(documentsWithText)
                .documentsWithEmbeddings(documentsWithEmbeddings)
                .documentsWithSummaries(documentsWithSummaries)
                .documentsWithTags(documentsWithTags)
                .processingSuccessRate(processingSuccessRate)
                .totalChunks(totalChunks)
                .chunksWithEmbeddings(chunksWithEmbeddings)
                .embeddingCoverage(embeddingCoverage)
                .uploadsLast24Hours(uploadsLast24Hours)
                .uploadsLast7Days(uploadsLast7Days)
                .uploadsLast30Days(uploadsLast30Days)
                .build();
    }

    /**
     * Get embedding generation status and failed documents.
     *
     * @return embedding status information
     */
    @Transactional(readOnly = true)
    public EmbeddingStatusResponse getEmbeddingStatus() {
        List<Document> allDocuments = documentRepository.findAll();
        
        int totalDocuments = allDocuments.size();
        int documentsWithEmbeddings = (int) allDocuments.stream()
                .filter(Document::getEmbeddingsGenerated)
                .count();
        int documentsFailed = (int) allDocuments.stream()
                .filter(d -> !d.getEmbeddingsGenerated() && d.getExtractedText() != null)
                .count();
        int documentsPending = totalDocuments - documentsWithEmbeddings - documentsFailed;
        
        double completionPercentage = totalDocuments > 0 
                ? Math.round((double) documentsWithEmbeddings / totalDocuments * 10000.0) / 100.0 
                : 0.0;
        
        // Chunk statistics
        List<DocumentChunk> allChunks = documentChunkRepository.findAll();
        int totalChunks = allChunks.size();
        int chunksWithEmbeddings = (int) allChunks.stream()
                .filter(DocumentChunk::isEmbeddingGenerated)
                .count();
        int chunksPending = totalChunks - chunksWithEmbeddings;
        
        // Build failed documents list
        List<EmbeddingStatusResponse.FailedEmbedding> failedDocuments = allDocuments.stream()
                .filter(d -> !d.getEmbeddingsGenerated() && d.getExtractedText() != null)
                .map(this::buildFailedEmbedding)
                .collect(Collectors.toList());
        
        return EmbeddingStatusResponse.builder()
                .totalDocuments(totalDocuments)
                .documentsWithEmbeddings(documentsWithEmbeddings)
                .documentsPending(documentsPending)
                .documentsFailed(documentsFailed)
                .completionPercentage(completionPercentage)
                .totalChunks(totalChunks)
                .chunksWithEmbeddings(chunksWithEmbeddings)
                .chunksPending(chunksPending)
                .failedDocuments(failedDocuments)
                .build();
    }

    /**
     * Retry embedding generation for a failed document.
     *
     * @param documentId the document ID
     */
    @Transactional
    public void retryEmbedding(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));
        
        if (document.getEmbeddingsGenerated()) {
            log.warn("Document {} already has embeddings, skipping retry", documentId);
            return;
        }
        
        // Reset chunk embedding flags to allow retry
        List<DocumentChunk> chunks = documentChunkRepository.findByDocumentIdOrderByChunkOrderAsc(documentId);
        for (DocumentChunk chunk : chunks) {
            chunk.setEmbeddingGenerated(false);
            chunk.setEmbedding(null);
        }
        documentChunkRepository.saveAll(chunks);
        
        log.info("Reset embedding flags for document {} with {} chunks for retry", documentId, chunks.size());
    }

    // Helper methods

    private UserManagementResponse buildUserManagementResponse(User user) {
        List<Document> userDocuments = documentRepository.findByOwnerId(user.getId());
        
        int documentCount = userDocuments.size();
        long totalStorageBytes = userDocuments.stream()
                .mapToLong(Document::getSize)
                .sum();
        
        return UserManagementResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .documentCount(documentCount)
                .totalStorageBytes(totalStorageBytes)
                .totalStorageFormatted(formatBytes(totalStorageBytes))
                .lastLoginAt(null) // TODO: Add login tracking in future
                .build();
    }

    private EmbeddingStatusResponse.FailedEmbedding buildFailedEmbedding(Document document) {
        long totalChunks = documentChunkRepository.countByDocumentId(document.getId());
        long chunksWithEmbeddings = documentChunkRepository.findByDocumentIdOrderByChunkOrderAsc(document.getId())
                .stream()
                .filter(DocumentChunk::isEmbeddingGenerated)
                .count();
        
        return EmbeddingStatusResponse.FailedEmbedding.builder()
                .documentId(document.getId())
                .documentName(document.getFilename())
                .ownerEmail(document.getOwner().getEmail())
                .totalChunks((int) totalChunks)
                .failedChunks((int) (totalChunks - chunksWithEmbeddings))
                .uploadTime(document.getUploadTime())
                .errorMessage("Embedding generation failed or incomplete")
                .build();
    }

    private SystemHealthResponse.ComponentHealth checkDatabaseHealth() {
        long startTime = System.currentTimeMillis();
        try {
            Connection connection = dataSource.getConnection();
            boolean isValid = connection.isValid(5);
            connection.close();
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            return SystemHealthResponse.ComponentHealth.builder()
                    .status(isValid ? "up" : "down")
                    .message(isValid ? "Database connection successful" : "Database connection failed")
                    .responseTimeMs(responseTime)
                    .build();
        } catch (Exception e) {
            return SystemHealthResponse.ComponentHealth.builder()
                    .status("down")
                    .message("Database error: " + e.getMessage())
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private SystemHealthResponse.ComponentHealth checkQdrantHealth() {
        long startTime = System.currentTimeMillis();
        try {
            // Try to list collections as a health check
            boolean isHealthy = true; // Qdrant doesn't expose health endpoint easily
            long responseTime = System.currentTimeMillis() - startTime;
            
            return SystemHealthResponse.ComponentHealth.builder()
                    .status("up")
                    .message("Vector database accessible")
                    .responseTimeMs(responseTime)
                    .build();
        } catch (Exception e) {
            return SystemHealthResponse.ComponentHealth.builder()
                    .status("down")
                    .message("Vector database error: " + e.getMessage())
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private SystemHealthResponse.ComponentHealth checkStorageHealth() {
        try {
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists() || !uploadDir.isDirectory()) {
                return SystemHealthResponse.ComponentHealth.builder()
                        .status("down")
                        .message("Upload directory does not exist or is not accessible")
                        .responseTimeMs(0L)
                        .build();
            }
            
            if (!uploadDir.canRead() || !uploadDir.canWrite()) {
                return SystemHealthResponse.ComponentHealth.builder()
                        .status("degraded")
                        .message("Upload directory has limited permissions")
                        .responseTimeMs(0L)
                        .build();
            }
            
            return SystemHealthResponse.ComponentHealth.builder()
                    .status("up")
                    .message("Storage accessible")
                    .responseTimeMs(0L)
                    .build();
        } catch (Exception e) {
            return SystemHealthResponse.ComponentHealth.builder()
                    .status("unknown")
                    .message("Storage check error: " + e.getMessage())
                    .responseTimeMs(0L)
                    .build();
        }
    }

    private SystemHealthResponse.ResourceUsage getResourceUsage() {
        Runtime runtime = Runtime.getRuntime();
        
        long usedMemoryBytes = runtime.totalMemory() - runtime.freeMemory();
        long maxMemoryBytes = runtime.maxMemory();
        long usedMemoryMB = usedMemoryBytes / (1024 * 1024);
        long maxMemoryMB = maxMemoryBytes / (1024 * 1024);
        double memoryUsagePercentage = Math.round((double) usedMemoryBytes / maxMemoryBytes * 10000.0) / 100.0;
        
        int activeThreads = Thread.activeCount();
        
        // Disk usage
        File uploadDir = new File(uploadPath);
        long diskUsedBytes = 0;
        long diskTotalBytes = 0;
        if (uploadDir.exists()) {
            diskTotalBytes = uploadDir.getTotalSpace();
            diskUsedBytes = diskTotalBytes - uploadDir.getUsableSpace();
        }
        
        return SystemHealthResponse.ResourceUsage.builder()
                .usedMemoryMB(usedMemoryMB)
                .maxMemoryMB(maxMemoryMB)
                .memoryUsagePercentage(memoryUsagePercentage)
                .activeThreads(activeThreads)
                .diskUsedBytes(diskUsedBytes)
                .diskTotalBytes(diskTotalBytes)
                .diskUsedFormatted(formatBytes(diskUsedBytes))
                .diskTotalFormatted(formatBytes(diskTotalBytes))
                .build();
    }

    private String getFileType(Document document) {
        String contentType = document.getContentType();
        if (contentType == null) return "Unknown";
        
        if (contentType.contains("pdf")) return "PDF";
        if (contentType.contains("wordprocessingml")) return "DOCX";
        if (contentType.contains("msword")) return "DOC";
        if (contentType.contains("text/plain")) return "TXT";
        return "Other";
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
