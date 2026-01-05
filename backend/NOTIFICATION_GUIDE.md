# Real-Time Notification System - Complete Guide

## Overview
Kortex implements a comprehensive real-time notification system using WebSocket (STOMP) with JWT authentication. All async operations trigger frontend-ready notifications.

## Notification Triggers

### 1. Document Upload Flow
```
User uploads document
  ↓
✓ DOCUMENT_UPLOADED notification sent
  ↓
Text extraction runs
  ↓
✓ TEXT_EXTRACTION_COMPLETE notification sent (includes word count)
  ↓
Chunking & embeddings generated
  ↓
✓ EMBEDDINGS_GENERATED notification sent (includes chunk count)
  ↓
AI summary generated
  ↓
✓ SUMMARY_GENERATED notification sent
  ↓
AI tags extracted
  ↓
✓ TAGS_GENERATED notification sent (includes tag count)
```

### 2. Document Deletion
```
User deletes document
  ↓
✓ DOCUMENT_DELETED notification sent
```

### 3. Question Answering (RAG)
```
User asks question
  ↓
RAG retrieval + LLM generation
  ↓
✓ QUESTION_ANSWERED notification sent
   - Includes question text
   - Includes answer preview (first 100 chars)
   - Includes citation count
```

### 4. Manual Operations
```
User regenerates summary
  ↓
✓ SUMMARY_REGENERATED notification sent

User regenerates tags
  ↓
✓ TAGS_REGENERATED notification sent (includes tag count)
```

## Frontend-Ready Payload Structure

All notifications follow this consistent schema:

```typescript
interface NotificationMessage {
  id: string;                    // UUID
  type: NotificationType;        // Enum value
  title: string;                 // Display title
  message: string;               // User-friendly message
  documentId?: number;           // Related document (if applicable)
  documentName?: string;         // Document filename (if applicable)
  timestamp: string;             // ISO 8601 datetime
  read: boolean;                 // Read status
  data?: any;                    // Additional structured data
}

type NotificationType = 
  | 'DOCUMENT_UPLOADED'
  | 'DOCUMENT_DELETED'
  | 'TEXT_EXTRACTION_COMPLETE'
  | 'TEXT_EXTRACTION_FAILED'
  | 'EMBEDDINGS_GENERATED'
  | 'SUMMARY_GENERATED'
  | 'SUMMARY_REGENERATED'
  | 'TAGS_GENERATED'
  | 'TAGS_REGENERATED'
  | 'QUESTION_ANSWERED'
  | 'SYSTEM';
```

## Example Payloads

### Document Processing Complete
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "type": "EMBEDDINGS_GENERATED",
  "title": "Embeddings Generated",
  "message": "Generated embeddings for 'research-paper.pdf' (24 chunks).",
  "documentId": 123,
  "documentName": "research-paper.pdf",
  "timestamp": "2024-01-15T10:30:15Z",
  "read": false,
  "data": null
}
```

### Question Answered (with structured data)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "type": "QUESTION_ANSWERED",
  "title": "Question Answered",
  "message": "Your question has been answered with 5 citations.",
  "documentId": null,
  "documentName": null,
  "timestamp": "2024-01-15T10:35:00Z",
  "read": false,
  "data": {
    "question": "What are the main findings of the research?",
    "answerPreview": "The research identified three key findings: 1) Machine learning models improved...",
    "citationCount": 5
  }
}
```

## Frontend Integration Guide

### 1. Connect to WebSocket
```javascript
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const token = localStorage.getItem('jwt_token');
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

const headers = {
  'Authorization': `Bearer ${token}`
};

stompClient.connect(headers, (frame) => {
  console.log('Connected:', frame);
  
  // Subscribe to user-specific notifications
  stompClient.subscribe('/user/notifications', (message) => {
    const notification = JSON.parse(message.body);
    handleNotification(notification);
  });
});
```

### 2. Handle Notifications
```javascript
function handleNotification(notification) {
  // Show toast/banner
  showToast(notification.title, notification.message);
  
  // Update notification center
  addToNotificationList(notification);
  
  // Type-specific handling
  switch (notification.type) {
    case 'DOCUMENT_UPLOADED':
      // Refresh document list
      refreshDocumentList();
      break;
      
    case 'EMBEDDINGS_GENERATED':
      // Enable search/QA features for this document
      enableAIFeatures(notification.documentId);
      break;
      
    case 'QUESTION_ANSWERED':
      // Show answer preview with link to full response
      const { question, answerPreview, citationCount } = notification.data;
      showAnswerPreview(question, answerPreview, citationCount);
      break;
      
    case 'TAGS_GENERATED':
      // Refresh document tags in UI
      updateDocumentTags(notification.documentId);
      break;
  }
}
```

### 3. Notification Center UI
```javascript
// Display in notification dropdown/panel
function renderNotification(notification) {
  return `
    <div class="notification ${notification.read ? 'read' : 'unread'}">
      <div class="notification-icon">${getIcon(notification.type)}</div>
      <div class="notification-content">
        <h4>${notification.title}</h4>
        <p>${notification.message}</p>
        <span class="timestamp">${formatTime(notification.timestamp)}</span>
      </div>
      ${notification.documentId ? 
        `<a href="/documents/${notification.documentId}">View Document</a>` : 
        ''
      }
    </div>
  `;
}
```

## Backend Implementation

### Service Layer
All notifications are sent via `NotificationService`:

```java
// Automatically called during document upload
notificationService.sendDocumentUploadedNotification(userId, documentId, documentName);

// Automatically called after embeddings generation
notificationService.sendEmbeddingsGeneratedNotification(userId, documentId, documentName, chunkCount);

// Automatically called after QA completes
String answerPreview = answer.substring(0, 100) + "...";
notificationService.sendQuestionAnsweredNotification(userId, question, answerPreview, citationCount);
```

### Error Handling
All notification sending is wrapped in try-catch blocks to ensure core operations never fail due to notification errors:

```java
try {
    notificationService.sendSummaryGeneratedNotification(userId, documentId, documentName);
} catch (Exception e) {
    log.error("Failed to send notification: {}", e.getMessage());
}
```

## Testing

### Test Endpoint
```bash
# Send test notification to yourself
curl -X POST http://localhost:8080/api/notifications/test \
  -H "Authorization: Bearer <token>"
```

### Admin Broadcast (Admin only)
```bash
# Broadcast to all users
curl -X POST http://localhost:8080/api/notifications/broadcast \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "System Maintenance",
    "message": "The system will be down for maintenance at 2 AM UTC."
  }'
```

## Benefits

1. **Real-time Feedback**: Users know immediately when operations complete
2. **No Polling**: Eliminates need for frontend polling loops
3. **Background Processing**: Users can continue working while AI processes
4. **Rich Context**: Notifications include all relevant data (document IDs, counts, previews)
5. **Actionable**: Frontend can navigate to related resources
6. **Type-Safe**: Enum-based types prevent typos
7. **Extensible**: Easy to add new notification types
8. **Resilient**: Notification failures don't break core functionality

## Future Enhancements

- [ ] Persistent notification storage (database)
- [ ] Mark as read/unread API
- [ ] Notification preferences (email, push)
- [ ] Notification filtering by type
- [ ] Notification history pagination
- [ ] Badge counts for unread notifications
- [ ] Sound/vibration options
