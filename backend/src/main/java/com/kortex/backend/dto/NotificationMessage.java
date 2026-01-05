package com.kortex.backend.dto;

import com.kortex.backend.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for real-time notifications sent via WebSocket.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationMessage {
    
    /**
     * Unique notification ID.
     */
    private String id;
    
    /**
     * Type of notification.
     */
    private NotificationType type;
    
    /**
     * Notification title.
     */
    private String title;
    
    /**
     * Notification message/description.
     */
    private String message;
    
    /**
     * Related document ID (if applicable).
     */
    private Long documentId;
    
    /**
     * Related document name (if applicable).
     */
    private String documentName;
    
    /**
     * Timestamp when notification was created.
     */
    private LocalDateTime timestamp;
    
    /**
     * Whether the notification has been read.
     */
    private boolean read;
    
    /**
     * Additional data (JSON format).
     */
    private Object data;
}
