package com.kortex.backend.controller;

import com.kortex.backend.security.UserDetailsImpl;
import com.kortex.backend.service.ClusteringService;
import com.kortex.backend.service.DocumentService;
import com.kortex.backend.service.TaggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for document tagging and clustering.
 */
@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
@Slf4j
public class TaggingController {

    private final TaggingService taggingService;
    private final ClusteringService clusteringService;
    private final DocumentService documentService;

    /**
     * Regenerate tags for a document.
     * POST /tags/{documentId}/regenerate
     */
    @PostMapping("/{documentId}/regenerate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> regenerateTags(
            @PathVariable Long documentId,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        // Verify document ownership
        documentService.getDocument(documentId, userDetails.getId());
        
        // Regenerate tags
        String tags = taggingService.regenerateTags(documentId);

        if (tags == null) {
            return ResponseEntity.ok(Map.of("message", "No tags generated"));
        }

        return ResponseEntity.ok(Map.of("tags", tags));
    }

    /**
     * Get all tags for current user's documents.
     * GET /tags
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Integer>> getUserTags(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        Map<String, Integer> tags = clusteringService.getUserTags(userDetails.getId());
        return ResponseEntity.ok(tags);
    }

    /**
     * Find similar documents to a given document.
     * GET /tags/{documentId}/similar
     */
    @GetMapping("/{documentId}/similar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClusteringService.SimilarDocument>> findSimilarDocuments(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "5") int limit,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        List<ClusteringService.SimilarDocument> similarDocuments = 
                clusteringService.findSimilarDocuments(userDetails.getId(), documentId, limit);

        return ResponseEntity.ok(similarDocuments);
    }

    /**
     * Get document clusters/topics for current user.
     * GET /tags/clusters
     */
    @GetMapping("/clusters")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClusteringService.DocumentCluster>> getDocumentClusters(
            @RequestParam(defaultValue = "2") int minClusterSize,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        List<ClusteringService.DocumentCluster> clusters = 
                clusteringService.getDocumentClusters(userDetails.getId(), minClusterSize);

        return ResponseEntity.ok(clusters);
    }
}
