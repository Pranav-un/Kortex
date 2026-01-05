package com.kortex.backend.controller;

import com.kortex.backend.dto.NotificationMessage;
import com.kortex.backend.model.NotificationType;
import com.kortex.backend.security.UserDetailsImpl;
import com.kortex.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for WebSocket notifications and testing endpoints.
 * Provides endpoints for testing notification delivery.
 */
@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * REST endpoint to send a test notification to the authenticated user.
     * Useful for testing WebSocket connectivity.
     */
    @PostMapping("/api/notifications/test")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    public ResponseEntity<Map<String, String>> sendTestNotification(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        notificationService.sendSystemNotification(
            userDetails.getId(),
            "Test Notification",
            "This is a test notification from Kortex."
        );
        
        return ResponseEntity.ok(Map.of(
            "message", "Test notification sent",
            "userId", userDetails.getId().toString()
        ));
    }

    /**
     * STOMP message mapping for receiving client messages.
     * Clients can send messages to /app/notify, and this will broadcast to all.
     * 
     * Example: useful for admin broadcasting messages.
     */
    @MessageMapping("/notify")
    @SendTo("/topic/notifications")
    public NotificationMessage handleNotification(NotificationMessage message) {
        // Process and broadcast the notification
        return message;
    }

    /**
     * REST endpoint for admin to broadcast a notification to all users.
     */
    @PostMapping("/api/notifications/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, String>> broadcastNotification(
            @RequestBody Map<String, String> request) {
        
        String title = request.getOrDefault("title", "System Announcement");
        String message = request.getOrDefault("message", "");
        
        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationType.SYSTEM)
                .title(title)
                .message(message)
                .read(false)
                .build();
        
        notificationService.broadcastToAll(notification);
        
        return ResponseEntity.ok(Map.of(
            "message", "Notification broadcasted to all users"
        ));
    }

    /**
     * REST endpoint to send a custom notification to a specific user (admin only).
     */
    @PostMapping("/api/notifications/send/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, String>> sendToUser(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        
        String title = request.getOrDefault("title", "Notification");
        String message = request.getOrDefault("message", "");
        
        notificationService.sendSystemNotification(userId, title, message);
        
        return ResponseEntity.ok(Map.of(
            "message", "Notification sent to user " + userId
        ));
    }
}
