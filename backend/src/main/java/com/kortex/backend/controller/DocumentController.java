package com.kortex.backend.controller;

import com.kortex.backend.dto.DocumentResponse;
import com.kortex.backend.dto.DocumentUploadResponse;
import com.kortex.backend.security.UserDetailsImpl;
import com.kortex.backend.service.DocumentService;
import com.kortex.backend.service.SummarizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST controller for document management.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final SummarizationService summarizationService;

    /**
     * Upload a document.
     *
     * @param file the file to upload
     * @param authentication the authenticated user
     * @return document upload response
     */
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        DocumentUploadResponse response = documentService.uploadDocument(file, userDetails.getId());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all documents for current user.
     *
     * @param authentication the authenticated user
     * @return list of documents
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DocumentResponse>> getUserDocuments(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<DocumentResponse> documents = documentService.getUserDocuments(userDetails.getId());

        return ResponseEntity.ok(documents);
    }

    /**
     * Get a specific document by ID.
     *
     * @param documentId the document ID
     * @param authentication the authenticated user
     * @return document response
     */
    @GetMapping("/{documentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentResponse> getDocument(
            @PathVariable Long documentId,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        DocumentResponse document = documentService.getDocument(documentId, userDetails.getId());

        return ResponseEntity.ok(document);
    }

    /**
     * Delete a document.
     *
     * @param documentId the document ID
     * @param authentication the authenticated user
     * @return no content response
     */
    @DeleteMapping("/{documentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long documentId,
            Authentication authentication) throws IOException {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        documentService.deleteDocument(documentId, userDetails.getId());

        return ResponseEntity.noContent().build();

    /**
     * Regenerate summary for a document.
     * Useful after document updates or to refresh with improved LLM.
     * POST /documents/{documentId}/regenerate-summary
     *
     * @param documentId the document ID
     * @param authentication the authenticated user
     * @return summary text
     */
    @PostMapping("/{documentId}/regenerate-summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> regenerateSummary(
            @PathVariable Long documentId,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        // Verify document ownership
        documentService.getDocument(documentId, userDetails.getId());
        
        // Regenerate summary
        String summary = summarizationService.regenerateSummary(documentId);

        if (summary == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate summary"));
        }

        return ResponseEntity.ok(Map.of("summary", summary));
    }
    }
}
