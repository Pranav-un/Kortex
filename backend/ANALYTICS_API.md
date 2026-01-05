# Analytics Dashboard API Documentation

## Overview
The Analytics Dashboard provides read-only statistics and insights about user documents. All endpoints require authentication and return data specific to the authenticated user.

## Base URL
```
/api/analytics
```

## Authentication
All endpoints require a valid JWT token in the Authorization header:
```
Authorization: Bearer <jwt_token>
```

---

## Endpoints

### 1. Get Overview Statistics

**GET** `/analytics/overview`

Returns comprehensive overview statistics including document counts, word counts, reading time, storage, and feature adoption metrics.

#### Request
```http
GET /api/analytics/overview
Authorization: Bearer <jwt_token>
```

#### Response
```json
{
  "totalDocuments": 42,
  "totalWords": 125430,
  "averageWordsPerDocument": 2986,
  "totalReadingTimeMinutes": 627,
  "averageReadingTimeMinutes": 15,
  "totalStorageBytes": 15728640,
  "totalStorageFormatted": "15.00 MB",
  "documentsWithText": 42,
  "documentsWithEmbeddings": 38,
  "documentsWithSummaries": 35,
  "documentsWithTags": 40,
  "fileTypeDistribution": {
    "PDF": 25,
    "DOCX": 12,
    "TXT": 5
  }
}
```

#### Fields
- `totalDocuments`: Total number of documents uploaded by user
- `totalWords`: Total word count across all documents
- `averageWordsPerDocument`: Mean words per document
- `totalReadingTimeMinutes`: Total estimated reading time (200 words per minute)
- `averageReadingTimeMinutes`: Average reading time per document
- `totalStorageBytes`: Total file storage in bytes
- `totalStorageFormatted`: Human-readable storage size (KB/MB/GB)
- `documentsWithText`: Count of documents with extracted text
- `documentsWithEmbeddings`: Count of documents with generated embeddings
- `documentsWithSummaries`: Count of documents with AI summaries
- `documentsWithTags`: Count of documents with auto-generated tags
- `fileTypeDistribution`: Map of file types to document counts

---

### 2. Get Keyword Frequency

**GET** `/analytics/keywords?limit=20`

Returns top keywords/tags ranked by frequency across all user documents.

#### Request Parameters
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| limit | Integer | No | 20 | Maximum number of keywords to return |

#### Request
```http
GET /api/analytics/keywords?limit=20
Authorization: Bearer <jwt_token>
```

#### Response
```json
{
  "keywords": [
    {
      "keyword": "machine learning",
      "documentCount": 15,
      "frequencyPercentage": 35.71
    },
    {
      "keyword": "artificial intelligence",
      "documentCount": 12,
      "frequencyPercentage": 28.57
    },
    {
      "keyword": "data science",
      "documentCount": 8,
      "frequencyPercentage": 19.05
    }
  ],
  "totalUniqueKeywords": 125
}
```

#### Fields
- `keywords`: Array of keyword statistics
  - `keyword`: The keyword/tag text
  - `documentCount`: Number of documents containing this keyword
  - `frequencyPercentage`: Percentage of documents with this keyword
- `totalUniqueKeywords`: Total number of unique keywords across all documents

---

### 3. Get Upload Statistics

**GET** `/analytics/uploads`

Returns upload trends, file type distribution, and size statistics.

#### Request
```http
GET /api/analytics/uploads
Authorization: Bearer <jwt_token>
```

#### Response
```json
{
  "uploadsByDate": [
    {
      "date": "2024-01-15",
      "count": 5
    },
    {
      "date": "2024-01-14",
      "count": 3
    }
  ],
  "uploadsByMonth": [
    {
      "month": "2024-01",
      "count": 42
    },
    {
      "month": "2023-12",
      "count": 38
    }
  ],
  "fileTypeStats": [
    {
      "fileType": "PDF",
      "count": 25,
      "percentage": 59.52
    },
    {
      "fileType": "DOCX",
      "count": 12,
      "percentage": 28.57
    },
    {
      "fileType": "TXT",
      "count": 5,
      "percentage": 11.90
    }
  ],
  "averageSizeBytes": 524288,
  "averageSizeFormatted": "512.00 KB",
  "largestDocumentBytes": 5242880,
  "largestDocumentName": "Annual_Report_2023.pdf",
  "uploadsLast7Days": 8,
  "uploadsLast30Days": 42
}
```

#### Fields
- `uploadsByDate`: Daily upload counts for last 30 days (sorted desc)
  - `date`: ISO date string (YYYY-MM-DD)
  - `count`: Number of uploads on this date
- `uploadsByMonth`: Monthly upload counts (sorted desc)
  - `month`: Month string (YYYY-MM)
  - `count`: Number of uploads in this month
- `fileTypeStats`: File type distribution with percentages
  - `fileType`: File type (PDF, DOCX, DOC, TXT)
  - `count`: Number of documents of this type
  - `percentage`: Percentage of total documents
- `averageSizeBytes`: Mean document size in bytes
- `averageSizeFormatted`: Human-readable average size
- `largestDocumentBytes`: Size of largest document
- `largestDocumentName`: Filename of largest document
- `uploadsLast7Days`: Upload count in last 7 days
- `uploadsLast30Days`: Upload count in last 30 days

---

### 4. Get Recent Activity

**GET** `/analytics/activity?limit=20`

Returns recent document upload activity with processing metadata.

#### Request Parameters
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| limit | Integer | No | 20 | Maximum number of activities to return |

#### Request
```http
GET /api/analytics/activity?limit=20
Authorization: Bearer <jwt_token>
```

#### Response
```json
{
  "recentUploads": [
    {
      "activityType": "DOCUMENT_UPLOAD",
      "documentId": 123,
      "documentName": "Research_Paper.pdf",
      "fileType": "PDF",
      "timestamp": "2024-01-15T14:30:00Z",
      "description": "Uploaded Research_Paper.pdf",
      "metadata": "3,245 words, 22 chunks, AI features: Summary, Tags"
    },
    {
      "activityType": "DOCUMENT_UPLOAD",
      "documentId": 122,
      "documentName": "Meeting_Notes.docx",
      "fileType": "DOCX",
      "timestamp": "2024-01-15T10:15:00Z",
      "description": "Uploaded Meeting_Notes.docx",
      "metadata": "1,523 words, 11 chunks, AI features: Tags"
    }
  ],
  "totalItems": 42
}
```

#### Fields
- `recentUploads`: Array of recent activity items (sorted by timestamp desc)
  - `activityType`: Type of activity (currently only DOCUMENT_UPLOAD)
  - `documentId`: ID of the document
  - `documentName`: Original filename
  - `fileType`: File type (PDF, DOCX, DOC, TXT)
  - `timestamp`: ISO 8601 timestamp of upload
  - `description`: Human-readable activity description
  - `metadata`: Processing details (word count, chunk count, AI features)
- `totalItems`: Total number of documents (all-time)

---

## Usage Examples

### Fetch Overview Statistics
```typescript
const response = await fetch('/api/analytics/overview', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});
const data = await response.json();
console.log(`Total documents: ${data.totalDocuments}`);
console.log(`Reading time: ${data.totalReadingTimeMinutes} minutes`);
```

### Fetch Top 10 Keywords
```typescript
const response = await fetch('/api/analytics/keywords?limit=10', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});
const data = await response.json();
data.keywords.forEach(kw => {
  console.log(`${kw.keyword}: ${kw.documentCount} docs (${kw.frequencyPercentage}%)`);
});
```

### Fetch Upload Trends
```typescript
const response = await fetch('/api/analytics/uploads', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});
const data = await response.json();
console.log(`Uploads in last 7 days: ${data.uploadsLast7Days}`);
console.log(`Average size: ${data.averageSizeFormatted}`);
```

### Fetch Recent Activity
```typescript
const response = await fetch('/api/analytics/activity?limit=20', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});
const data = await response.json();
data.recentUploads.forEach(activity => {
  console.log(`${activity.timestamp}: ${activity.description}`);
  console.log(`  ${activity.metadata}`);
});
```

---

## Reading Time Calculation

Reading time is calculated using the standard average reading speed:
- **Reading Speed**: 200 words per minute (WPM)
- **Formula**: `totalWords / 200 = readingTimeMinutes`

This is a widely accepted standard for estimating reading time in digital content.

---

## File Type Detection

File types are detected from the document's `contentType` field:
- **PDF**: `application/pdf`
- **DOCX**: `application/vnd.openxmlformats-officedocument.wordprocessingml.document`
- **DOC**: `application/msword`
- **TXT**: `text/plain`

---

## Storage Size Formatting

Storage sizes are formatted to human-readable strings:
- **< 1 KB**: `XXX B` (bytes)
- **< 1 MB**: `XXX.XX KB` (kilobytes)
- **< 1 GB**: `XXX.XX MB` (megabytes)
- **≥ 1 GB**: `XXX.XX GB` (gigabytes)

---

## Performance Considerations

All analytics endpoints are optimized for performance:
- **Read-Only Transactions**: All queries use `@Transactional(readOnly = true)`
- **Efficient Queries**: Minimal database roundtrips
- **In-Memory Aggregation**: Statistics calculated in-memory after fetching documents
- **No Caching**: Real-time data (no stale statistics)

---

## Error Responses

All endpoints return standard error responses:

### 401 Unauthorized
```json
{
  "message": "JWT token is missing or invalid"
}
```

### 500 Internal Server Error
```json
{
  "message": "An error occurred while processing your request"
}
```

---

## Security

- **Authentication Required**: All endpoints require valid JWT tokens
- **User Isolation**: Analytics only include documents owned by the authenticated user
- **Read-Only**: No write operations available (safe for dashboard polling)
- **No Side Effects**: Endpoints are idempotent and safe to call repeatedly
