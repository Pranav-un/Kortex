package com.kortex.backend.controller;

import com.kortex.backend.security.UserDetailsImpl;
import com.kortex.backend.service.QdrantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrative endpoints for managing Qdrant collections per user.
 * Temporary utility to reset a user's collection when changing embedding dimensions.
 */
@RestController
@RequestMapping("/admin/qdrant")
@RequiredArgsConstructor
@Slf4j
public class AdminQdrantController {

    private final QdrantService qdrantService;

    /**
     * Reset the current user's Qdrant collection.
     * Deletes the collection if present and immediately recreates it using the
     * current embedding dimension to avoid NOT_FOUND or size mismatch issues.
     */
    @DeleteMapping("/reset")
    public ResponseEntity<String> resetUserCollection(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        String collectionName = qdrantService.getCollectionName(userId);
        try {
            qdrantService.deleteCollection(userId);
            log.info("Deleted Qdrant collection {} for user {}", collectionName, userId);
        } catch (RuntimeException ex) {
            // If collection did not exist or delete failed, continue to recreate
            log.warn("Delete failed or collection missing for {} — proceeding to recreate: {}", collectionName, ex.getMessage());
        }

        // Recreate collection with the current embedding dimension
        qdrantService.ensureCollectionExists(userId);
        log.info("Recreated Qdrant collection {} for user {}", collectionName, userId);
        return ResponseEntity.ok("Reset collection: " + collectionName);
    }
}
