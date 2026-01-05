# Admin Management API Documentation

## Overview
The Admin Management APIs provide comprehensive tools for system administrators to manage users, monitor system health, and track embedding generation status. All endpoints require ADMIN role.

## Base URL
```
/admin
```

## Authentication & Authorization
All endpoints require:
1. Valid JWT token in Authorization header: `Authorization: Bearer <jwt_token>`
2. User must have ADMIN role

**Non-admin users will receive 403 Forbidden response.**

---

## User Management APIs

### 1. Get All Users

**GET** `/admin/users`

Returns list of all users with their statistics.

#### Request
```http
GET /admin/users
Authorization: Bearer <admin_jwt_token>
```

#### Response
```json
[
  {
    "id": 1,
    "email": "user@example.com",
    "name": "John Doe",
    "role": "USER",
    "active": true,
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-15T14:30:00",
    "documentCount": 25,
    "totalStorageBytes": 10485760,
    "totalStorageFormatted": "10.00 MB",
    "lastLoginAt": null
  }
]
```

---

### 2. Get User By ID

**GET** `/admin/users/{userId}`

Returns detailed information about a specific user.

#### Request
```http
GET /admin/users/123
Authorization: Bearer <admin_jwt_token>
```

#### Response
```json
{
  "id": 123,
  "email": "user@example.com",
  "name": "John Doe",
  "role": "USER",
  "active": true,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-15T14:30:00",
  "documentCount": 25,
  "totalStorageBytes": 10485760,
  "totalStorageFormatted": "10.00 MB",
  "lastLoginAt": null
}
```

---

### 3. Activate User

**PUT** `/admin/users/{userId}/activate`

Activates a deactivated user account.

#### Request
```http
PUT /admin/users/123/activate
Authorization: Bearer <admin_jwt_token>
```

#### Response
```json
{
  "id": 123,
  "email": "user@example.com",
  "name": "John Doe",
  "role": "USER",
  "active": true,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-15T14:30:00",
  "documentCount": 25,
  "totalStorageBytes": 10485760,
  "totalStorageFormatted": "10.00 MB",
  "lastLoginAt": null
}
```

---

### 4. Deactivate User

**PUT** `/admin/users/{userId}/deactivate`

Deactivates a user account (soft delete).

#### Request
```http
PUT /admin/users/123/deactivate
Authorization: Bearer <admin_jwt_token>
```

#### Response
```json
{
  "id": 123,
  "email": "user@example.com",
  "name": "John Doe",
  "role": "USER",
  "active": false,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-15T14:35:00",
  "documentCount": 25,
  "totalStorageBytes": 10485760,
  "totalStorageFormatted": "10.00 MB",
  "lastLoginAt": null
}
```

---

### 5. Delete User

**DELETE** `/admin/users/{userId}`

Permanently deletes a user and all their documents.

**WARNING: This operation is irreversible.**

#### Request
```http
DELETE /admin/users/123
Authorization: Bearer <admin_jwt_token>
```

#### Response
```
204 No Content
```

#### Side Effects
- All user documents are deleted from database
- All document files are deleted from storage
- All Qdrant vector embeddings are deleted
- All document chunks are deleted (cascade)

---

## System Health & Statistics APIs

### 6. Get System Health

**GET** `/admin/system/health`

Returns comprehensive system health status including database, Qdrant, storage, and resource usage.

#### Request
```http
GET /admin/system/health
Authorization: Bearer <admin_jwt_token>
```

#### Response
```json
{
  "status": "healthy",
  "timestamp": "2024-01-15T14:30:00",
  "uptimeSeconds": 86400,
  "database": {
    "status": "up",
    "message": "Database connection successful",
    "responseTimeMs": 15
  },
  "vectorDatabase": {
    "status": "up",
    "message": "Vector database accessible",
    "responseTimeMs": 25
  },
  "storage": {
    "status": "up",
    "message": "Storage accessible",
    "responseTimeMs": 0
  },
  "resources": {
    "usedMemoryMB": 512,
    "maxMemoryMB": 2048,
    "memoryUsagePercentage": 25.0,
    "activeThreads": 42,
    "diskUsedBytes": 10737418240,
    "diskTotalBytes": 107374182400,
    "diskUsedFormatted": "10.00 GB",
    "diskTotalFormatted": "100.00 GB"
  }
}
```

#### Status Values
- **overall status**: `"healthy"`, `"degraded"`, `"unhealthy"`
- **component status**: `"up"`, `"down"`, `"unknown"`

---

### 7. Get System Statistics

**GET** `/admin/system/stats`

Returns comprehensive system-wide statistics.

#### Request
```http
GET /admin/system/stats
Authorization: Bearer <admin_jwt_token>
```

#### Response
```json
{
  "totalUsers": 150,
  "activeUsers": 142,
  "inactiveUsers": 8,
  "adminUsers": 3,
  "totalDocuments": 3542,
  "totalStorageBytes": 5368709120,
  "totalStorageFormatted": "5.00 GB",
  "documentsByFileType": {
    "PDF": 2100,
    "DOCX": 980,
    "TXT": 462
  },
  "documentsWithText": 3542,
  "documentsWithEmbeddings": 3420,
  "documentsWithSummaries": 3100,
  "documentsWithTags": 3350,
  "processingSuccessRate": 100.0,
  "totalChunks": 42500,
  "chunksWithEmbeddings": 41000,
  "embeddingCoverage": 96.47,
  "uploadsLast24Hours": 45,
  "uploadsLast7Days": 320,
  "uploadsLast30Days": 1250
}
```

---

## Embedding Monitoring APIs

### 8. Get Embedding Status

**GET** `/admin/embeddings/status`

Returns embedding generation status and failed documents.

#### Request
```http
GET /admin/embeddings/status
Authorization: Bearer <admin_jwt_token>
```

#### Response
```json
{
  "totalDocuments": 3542,
  "documentsWithEmbeddings": 3420,
  "documentsPending": 100,
  "documentsFailed": 22,
  "completionPercentage": 96.55,
  "totalChunks": 42500,
  "chunksWithEmbeddings": 41000,
  "chunksPending": 1500,
  "failedDocuments": [
    {
      "documentId": 123,
      "documentName": "corrupted.pdf",
      "ownerEmail": "user@example.com",
      "totalChunks": 15,
      "failedChunks": 15,
      "uploadTime": "2024-01-14T10:00:00",
      "errorMessage": "Embedding generation failed or incomplete"
    }
  ]
}
```

---

### 9. Retry Embedding Generation

**POST** `/admin/embeddings/retry/{documentId}`

Resets embedding flags for a document to allow retry.

#### Request
```http
POST /admin/embeddings/retry/123
Authorization: Bearer <admin_jwt_token>
```

#### Response
```json
{
  "message": "Embedding retry initiated for document 123",
  "documentId": "123"
}
```

#### Notes
- Resets `embeddingGenerated` flag on all chunks
- Clears existing embedding vectors
- Document will be picked up by next embedding generation job
- Manual re-trigger of embedding service may be required

---

### 10. Get Failed Embeddings

**GET** `/admin/embeddings/failed`

Returns list of documents with failed embeddings.

#### Request
```http
GET /admin/embeddings/failed
Authorization: Bearer <admin_jwt_token>
```

#### Response
```json
[
  {
    "documentId": 123,
    "documentName": "corrupted.pdf",
    "ownerEmail": "user@example.com",
    "totalChunks": 15,
    "failedChunks": 15,
    "uploadTime": "2024-01-14T10:00:00",
    "errorMessage": "Embedding generation failed or incomplete"
  }
]
```

---

## Usage Examples

### Check System Health
```typescript
const response = await fetch('/admin/system/health', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${adminToken}`,
    'Content-Type': 'application/json'
  }
});
const health = await response.json();
console.log(`System status: ${health.status}`);
console.log(`Database: ${health.database.status} (${health.database.responseTimeMs}ms)`);
console.log(`Memory usage: ${health.resources.memoryUsagePercentage}%`);
```

### List All Users
```typescript
const response = await fetch('/admin/users', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${adminToken}`,
    'Content-Type': 'application/json'
  }
});
const users = await response.json();
users.forEach(user => {
  console.log(`${user.email}: ${user.documentCount} docs, ${user.totalStorageFormatted}`);
});
```

### Deactivate User
```typescript
const response = await fetch('/admin/users/123/deactivate', {
  method: 'PUT',
  headers: {
    'Authorization': `Bearer ${adminToken}`,
    'Content-Type': 'application/json'
  }
});
const updatedUser = await response.json();
console.log(`User ${updatedUser.email} deactivated`);
```

### Monitor Embedding Status
```typescript
const response = await fetch('/admin/embeddings/status', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${adminToken}`,
    'Content-Type': 'application/json'
  }
});
const status = await response.json();
console.log(`Completion: ${status.completionPercentage}%`);
console.log(`Failed: ${status.documentsFailed} documents`);

// Retry failed embeddings
for (const failed of status.failedDocuments) {
  await fetch(`/admin/embeddings/retry/${failed.documentId}`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${adminToken}` }
  });
}
```

---

## Error Responses

### 401 Unauthorized
```json
{
  "message": "JWT token is missing or invalid"
}
```

### 403 Forbidden
```json
{
  "message": "Access denied. Admin role required."
}
```

### 404 Not Found
```json
{
  "message": "User not found with id: 123"
}
```

### 500 Internal Server Error
```json
{
  "message": "An error occurred while processing your request"
}
```

---

## Security Considerations

1. **Role-Based Access**: All endpoints require ADMIN role
2. **Audit Logging**: All admin actions are logged with user ID and action details
3. **Destructive Operations**: User deletion is logged and includes cascade deletion warnings
4. **Health Monitoring**: Exposes system internals only to admins
5. **User Privacy**: User passwords are never included in responses

---

## Best Practices

1. **User Deactivation**: Use deactivation instead of deletion when possible for audit trails
2. **Health Monitoring**: Set up periodic health checks (every 1-5 minutes)
3. **Embedding Monitoring**: Check embedding status after bulk uploads
4. **Resource Alerts**: Alert on memory usage > 80% or disk usage > 90%
5. **Failed Embeddings**: Review failed embeddings regularly and retry or investigate

---

## Performance Notes

- All endpoints use read-only transactions where applicable
- User statistics calculated on-the-fly (no caching)
- Health checks have 5-second timeout for database validation
- System stats may be slow with large document counts (consider adding indexes)
- Embedding status queries all documents and chunks (may take time on large systems)
