package com.kortex.backend.controller;

import com.kortex.backend.dto.EmbeddingStatusResponse;
import com.kortex.backend.dto.SystemHealthResponse;
import com.kortex.backend.dto.SystemStatsResponse;
import com.kortex.backend.dto.UserManagementResponse;
import com.kortex.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for admin operations.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;

    // ==================== User Management ====================

    /**
     * Get all users with their statistics.
     *
     * @return list of all users
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserManagementResponse>> getAllUsers() {
        log.info("Admin requesting all users");
        List<UserManagementResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Get a specific user by ID.
     *
     * @param userId the user ID
     * @return user details with statistics
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserManagementResponse> getUserById(@PathVariable Long userId) {
        log.info("Admin requesting user details for userId: {}", userId);
        UserManagementResponse user = adminService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Activate a user account.
     *
     * @param userId the user ID
     * @return updated user details
     */
    @PutMapping("/users/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserManagementResponse> activateUser(@PathVariable Long userId) {
        log.info("Admin activating user: {}", userId);
        UserManagementResponse user = adminService.activateUser(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Deactivate a user account.
     *
     * @param userId the user ID
     * @return updated user details
     */
    @PutMapping("/users/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserManagementResponse> deactivateUser(@PathVariable Long userId) {
        log.info("Admin deactivating user: {}", userId);
        UserManagementResponse user = adminService.deactivateUser(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Delete a user and all their documents.
     *
     * @param userId the user ID
     * @return no content
     */
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        log.info("Admin deleting user: {}", userId);
        adminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    // ==================== System Health & Stats ====================

    /**
     * Get system health status.
     *
     * @return system health information
     */
    @GetMapping("/system/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemHealthResponse> getSystemHealth() {
        log.info("Admin requesting system health");
        SystemHealthResponse health = adminService.getSystemHealth();
        return ResponseEntity.ok(health);
    }

    /**
     * Get system-wide statistics.
     *
     * @return comprehensive system statistics
     */
    @GetMapping("/system/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemStatsResponse> getSystemStats() {
        log.info("Admin requesting system statistics");
        SystemStatsResponse stats = adminService.getSystemStats();
        return ResponseEntity.ok(stats);
    }

    // ==================== Embedding Monitoring ====================

    /**
     * Get embedding generation status.
     *
     * @return embedding status and failed documents
     */
    @GetMapping("/embeddings/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmbeddingStatusResponse> getEmbeddingStatus() {
        log.info("Admin requesting embedding status");
        EmbeddingStatusResponse status = adminService.getEmbeddingStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * Retry embedding generation for a failed document.
     *
     * @param documentId the document ID
     * @return success message
     */
    @PostMapping("/embeddings/retry/{documentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> retryEmbedding(@PathVariable Long documentId) {
        log.info("Admin retrying embedding generation for document: {}", documentId);
        adminService.retryEmbedding(documentId);
        return ResponseEntity.ok(Map.of(
                "message", "Embedding retry initiated for document " + documentId,
                "documentId", String.valueOf(documentId)
        ));
    }

    /**
     * Get list of documents with failed embeddings.
     *
     * @return list of failed documents
     */
    @GetMapping("/embeddings/failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EmbeddingStatusResponse.FailedEmbedding>> getFailedEmbeddings() {
        log.info("Admin requesting failed embeddings");
        EmbeddingStatusResponse status = adminService.getEmbeddingStatus();
        return ResponseEntity.ok(status.getFailedDocuments());
    }
}
