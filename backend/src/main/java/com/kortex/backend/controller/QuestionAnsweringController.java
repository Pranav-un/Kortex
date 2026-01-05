package com.kortex.backend.controller;

import com.kortex.backend.dto.QuestionRequest;
import com.kortex.backend.dto.QuestionResponse;
import com.kortex.backend.security.UserDetailsImpl;
import com.kortex.backend.service.NotificationService;
import com.kortex.backend.service.QuestionAnsweringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * REST controller for Question Answering using RAG.
 */
@RestController
@RequestMapping("/qa")
@RequiredArgsConstructor
@Slf4j
public class QuestionAnsweringController {

    private final QuestionAnsweringService questionAnsweringService;
    private final NotificationService notificationService;

    /**
     * Answer a question using RAG across all user documents.
     * POST /qa
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<QuestionResponse> answerQuestion(
            @Valid @RequestBody QuestionRequest request,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();
        log.info("Question answering request from user {}", userId);

        QuestionAnsweringService.AnswerResponse response;

        if (request.getDocumentId() != null) {
            // Answer question within specific document
            response = questionAnsweringService.answerQuestionInDocument(
                    userId,
                    request.getDocumentId(),
                    request.getQuestion()
            );
        } else {
            // Answer question across all documents
            response = questionAnsweringService.answerQuestion(
                    userId,
                    request.getQuestion()
            );
        }

        // Send real-time notification
        try {
            String answerPreview = response.getAnswer().length() > 100 
                ? response.getAnswer().substring(0, 100) + "..." 
                : response.getAnswer();
            notificationService.sendQuestionAnsweredNotification(
                userId, 
                request.getQuestion(), 
                answerPreview, 
                response.getCitations().size()
            );
        } catch (Exception e) {
            log.error("Failed to send question answered notification: {}", e.getMessage());
        }

        return ResponseEntity.ok(convertToDto(response));
    }

    /**
     * Quick question answering using GET with query parameter.
     * GET /qa?question=...
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<QuestionResponse> answerQuestionGet(
            @RequestParam String question,
            @RequestParam(required = false) Long documentId,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();
        log.info("Question answering GET request from user {}", userId);

        QuestionAnsweringService.AnswerResponse response;

        if (documentId != null) {
            response = questionAnsweringService.answerQuestionInDocument(
                    userId,
                    documentId,
                    question
            );
        } else {
            response = questionAnsweringService.answerQuestion(
                    userId,
                    question
            );
        }

        // Send real-time notification
        try {
            String answerPreview = response.getAnswer().length() > 100 
                ? response.getAnswer().substring(0, 100) + "..." 
                : response.getAnswer();
            notificationService.sendQuestionAnsweredNotification(
                userId, 
                question, 
                answerPreview, 
                response.getCitations().size()
            );
        } catch (Exception e) {
            log.error("Failed to send question answered notification: {}", e.getMessage());
        }

        return ResponseEntity.ok(convertToDto(response));
    }

    /**
     * Convert service response to DTO.
     */
    private QuestionResponse convertToDto(QuestionAnsweringService.AnswerResponse response) {
        return QuestionResponse.builder()
                .question(response.getQuestion())
                .answer(response.getAnswer())
                .citations(response.getCitations().stream()
                        .map(citation -> QuestionResponse.CitationDto.builder()
                                .citationNumber(citation.getCitationNumber())
                                .documentId(citation.getDocumentId())
                                .documentName(citation.getDocumentName())
                                .chunkId(citation.getChunkId())
                                .chunkOrder(citation.getChunkOrder())
                                .relevanceScore(citation.getRelevanceScore())
                                .excerpt(citation.getExcerpt())
                                .build())
                        .collect(Collectors.toList()))
                .contextChunksUsed(response.getContextChunksUsed())
                .estimatedTokens(response.getEstimatedTokens())
                .llmProvider(response.getLlmProvider())
                .llmModel(response.getLlmModel())
                .documentId(response.getDocumentId())
                .build();
    }
}
