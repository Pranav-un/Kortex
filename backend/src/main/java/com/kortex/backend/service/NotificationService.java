package com.kortex.backend.service;

import com.kortex.backend.dto.NotificationMessage;
import com.kortex.backend.model.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for sending real-time notifications to users via WebSocket.
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Send notification to a specific user.
     * 
     * @param userId User ID to send notification to
     * @param notification Notification message to send
     */
    public void sendToUser(Long userId, NotificationMessage notification) {
        try {
            // Ensure notification has an ID and timestamp
            if (notification.getId() == null) {
                notification.setId(UUID.randomUUID().toString());
            }
            if (notification.getTimestamp() == null) {
                notification.setTimestamp(LocalDateTime.now());
            }
            
            // Send to user-specific queue
            messagingTemplate.convertAndSendToUser(
                userId.toString(), 
                "/notifications", 
                notification
            );
            
            logger.info("Sent notification to user {}: {}", userId, notification.getType());
        } catch (Exception e) {
            logger.error("Failed to send notification to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Send document uploaded notification.
     */
    public void sendDocumentUploadedNotification(Long userId, Long documentId, String documentName) {
        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationType.DOCUMENT_UPLOADED)
                .title("Document Uploaded")
                .message("Document '" + documentName + "' has been successfully uploaded.")
                .documentId(documentId)
                .documentName(documentName)
                .read(false)
                .build();
        
        sendToUser(userId, notification);
    }

    /**
     * Send document deleted notification.
     */
    public void sendDocumentDeletedNotification(Long userId, String documentName) {
        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationType.DOCUMENT_DELETED)
                .title("Document Deleted")
                .message("Document '" + documentName + "' has been deleted.")
                .documentName(documentName)
                .read(false)
                .build();
        
        sendToUser(userId, notification);
    }

    /**
     * Send text extraction complete notification.
     */
    public void sendTextExtractionCompleteNotification(Long userId, Long documentId, String documentName, int wordCount) {
        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationType.TEXT_EXTRACTION_COMPLETE)
                .title("Text Extraction Complete")
                .message("Text extraction completed for '" + documentName + "' (" + wordCount + " words).")
                .documentId(documentId)
                .documentName(documentName)
                .read(false)
                .build();
        
        sendToUser(userId, notification);
    }

    /**
     * Send text extraction failed notification.
     */
    public void sendTextExtractionFailedNotification(Long userId, Long documentId, String documentName) {
        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationType.TEXT_EXTRACTION_FAILED)
                .title("Text Extraction Failed")
                .message("Text extraction failed for '" + documentName + "'.")
                .documentId(documentId)
                .documentName(documentName)
                .read(false)
                .build();
        
        sendToUser(userId, notification);
    }

    /**
     * Send embeddings generated notification.
     */
    public void sendEmbeddingsGeneratedNotification(Long userId, Long documentId, String documentName, int chunkCount) {
        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationType.EMBEDDINGS_GENERATED)
                .title("Embeddings Generated")
                .message("Generated embeddings for '" + documentName + "' (" + chunkCount + " chunks).")
                .documentId(documentId)
                .documentName(documentName)
                .read(false)
                .build();
        
        sendToUser(userId, notification);
    }

    /**
     * Send summary generated notification.
     */
    public void sendSummaryGeneratedNotification(Long userId, Long documentId, String documentName) {
        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationType.SUMMARY_GENERATED)
                .title("Summary Generated")
                .message("AI summary has been generated for '" + documentName + "'.")
                .documentId(documentId)
                .documentName(documentName)
                .read(false)
                .build();
        
        sendToUser(userId, notification);
    }

    /**
     * Send summary regenerated notification.
     */
    public void sendSummaryRegeneratedNotification(Long userId, Long documentId, String documentName) {
        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationType.SUMMARY_REGENERATED)
                .title("Summary Regenerated")
                .message("AI summary has been regenerated for '" + documentName + "'.")
                .documentId(documentId)
                .documentName(documentName)
                .read(false)
                .build();
        
        sendToUser(userId, notification);
    }

    /**
     * Send tags generated notification.
     */
    public void sendTagsGeneratedNotification(Long userId, Long documentId, String documentName, int tagCount) {
        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationType.TAGS_GENERATED)
                .title("Tags Generated")
                .message("Generated " + tagCount + " tags for '" + documentName + "'.")
                .documentId(documentId)
                .documentName(documentName)
                .read(false)
                .build();
        
        sendToUser(userId, notification);
    }

    /**
     * Send tags regenerated notification.
     */
    public void sendTagsRegeneratedNotification(Long userId, Long documentId, String documentName, int tagCount) {
        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationType.TAGS_REGENERATED)
                .title("Tags Regenerated")
                .message("Regenerated " + tagCount + " tags for '" + documentName + "'.")
                .documentId(documentId)
                .documentName(documentName)
                .read(false)
                .build();
        
        sendToUser(userId, notification);
    }

    /**
     * Send question answered notification with answer preview.
     */
    public void sendQuestionAnsweredNotification(Long userId, String question, String answerPreview, int citationCount) {
        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationType.QUESTION_ANSWERED)
                .title("Question Answered")
                .message("Your question has been answered with " + citationCount + " citations.")
                .read(false)
                .data(new QuestionAnsweredData(question, answerPreview, citationCount))
                .build();
        
        sendToUser(userId, notification);
    }

    /**
     * Data class for question answered notification payload.
     */
    public static class QuestionAnsweredData {
        public String question;
        public String answerPreview;
        public int citationCount;

        public QuestionAnsweredData(String question, String answerPreview, int citationCount) {
            this.question = question;
            this.answerPreview = answerPreview;
            this.citationCount = citationCount;
        }
    }

    /**
     * Send generic system notification.
     */
    public void sendSystemNotification(Long userId, String title, String message) {
        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationType.SYSTEM)
                .title(title)
                .message(message)
                .read(false)
                .build();
        
        sendToUser(userId, notification);
    }

    /**
     * Broadcast notification to all connected users (admin feature).
     */
    public void broadcastToAll(NotificationMessage notification) {
        try {
            if (notification.getId() == null) {
                notification.setId(UUID.randomUUID().toString());
            }
            if (notification.getTimestamp() == null) {
                notification.setTimestamp(LocalDateTime.now());
            }
            
            messagingTemplate.convertAndSend("/topic/notifications", notification);
            logger.info("Broadcasted notification: {}", notification.getType());
        } catch (Exception e) {
            logger.error("Failed to broadcast notification: {}", e.getMessage());
        }
    }
}
