# Kortex Architecture Documentation

## System Overview
Kortex is an AI-powered document management system built with Spring Boot (backend) and React (frontend). The system provides intelligent document processing with semantic search and RAG (Retrieval-Augmented Generation) capabilities.

## Technology Stack

### Backend
- **Framework**: Spring Boot 4.0.1, Java 21
- **Database**: PostgreSQL with JPA/Hibernate
- **Authentication**: JWT (JJWT 0.12.6) with 24-hour token expiration
- **Text Processing**: 
  - Apache PDFBox 3.0.3 (PDF extraction)
  - Apache POI 5.3.0 (DOCX/DOC extraction)
- **Embeddings**: HuggingFace Inference API
  - Model: sentence-transformers/all-MiniLM-L6-v2
  - Dimensions: 384
  - Batch size: 32
- **Vector Database**: Qdrant 1.9.1 (cosine similarity)
- **Storage**: Local filesystem (./uploads)
- **Security**: BCrypt password hashing, stateless sessions

### Frontend
- React with TypeScript
- Vite build tool

## Core Services

### 1. Authentication Services
- **AuthService**: User registration, login, password management
- **JwtUtil**: JWT token generation and validation
- **UserDetailsServiceImpl**: Spring Security user details loading
- **JwtAuthenticationFilter**: Token-based request authentication

### 2. Document Management Services
- **DocumentService**: Document upload, retrieval, deletion with duplicate detection
- **TextExtractionService**: Extracts text from PDF, DOCX, DOC, TXT files with cleaning
- **TextChunkingService**: Intelligent chunking (150-300 words) with sentence boundary detection

### 3. Embedding & Vector Search Services
- **EmbeddingService**: Interface for embedding generation (abstraction layer)
- **HuggingFaceEmbeddingService**: Batch embedding generation using HuggingFace API
- **QdrantService**: Vector database operations (CRUD, similarity search)
- **SemanticSearchService**: High-level semantic search combining embeddings and vector search

### 4. RAG (Retrieval-Augmented Generation) Services
- **RAGService**: Retrieves relevant context and prepares structured prompts for LLMs
  - **retrieveContext()**: Retrieves chunks within token limit, ordered by relevance
  - **retrieveContextFromDocument()**: Retrieves context from specific document
  - **Token limiting**: Configurable max tokens (default 3000)
  - **Prompt formatting**: Structured prompts with system instructions, context, and query

### 5. Analytics Services
- **AnalyticsService**: Provides read-only statistics and insights for dashboard
  - **getOverviewStatistics()**: Total documents, words, reading time (200 WPM), storage, feature adoption
  - **getKeywordFrequency()**: Extracts and ranks tags/keywords by frequency
  - **getUploadStatistics()**: Upload trends by date/month, file type distribution, size analysis
  - **getRecentActivity()**: Recent document uploads with processing metadata

### 6. Admin Services
- **AdminService**: Admin operations for user management and system monitoring
  - **getAllUsers()**: List all users with document counts and storage usage
  - **activateUser() / deactivateUser()**: Control user account access
  - **deleteUser()**: Remove user and all associated documents
  - **getSystemHealth()**: Check database, Qdrant, storage health with response times
  - **getSystemStats()**: System-wide statistics (users, documents, processing rates)
  - **getEmbeddingStatus()**: Monitor embedding generation and identify failed documents
  - **retryEmbedding()**: Reset embedding flags for retry attempts

## Data Models

### User
- Fields: id, email, password (BCrypt), fullName, role (USER/ADMIN), createdAt, updatedAt
- Authentication: JWT tokens with userId and role claims
- Security: Owner-based access control (@PreAuthorize)

### Document
- Fields: id, originalFilename, storedFilename, fileSize, fileType, mimeType, uploadPath
- Relationships: Many-to-One with User (ownerId)
- Features: SHA-256 hash for duplicate detection, soft delete support

### DocumentChunk
- Fields: id, documentId, chunkText (TEXT), chunkOrder, wordCount, startPosition, endPosition
- Embeddings: embedding (double[] array), embeddingGenerated flag
- Relationships: Many-to-One with Document
- Purpose: Stores text chunks with embeddings for semantic search

## Vector Database Schema (Qdrant)

### Collection Naming
- Pattern: `kortex_user_{userId}`
- Isolation: Per-user collections for security and multi-tenancy

### Point Structure
```json
{
  "id": "{chunkId}",
  "vector": [384 float values],
  "payload": {
    "documentId": 123,
    "chunkId": 456,
    "chunkOrder": 0,
    "wordCount": 200
  }
}
```

### Search Configuration
- **Metric**: Cosine similarity
- **Vector Dimensions**: 384 (matches embedding model)
- **Filtering**: By documentId for document-specific search

## RAG Architecture

### Context Retrieval Pipeline
1. **Query Embedding**: Convert user query to 384-dim vector
2. **Vector Search**: Search Qdrant with cosine similarity
3. **Token Limiting**: Select top chunks within token budget
4. **Ordering**: Chunks ordered by similarity score (descending)
5. **Prompt Formatting**: Structure context for LLM consumption

### Token Estimation
- **Method**: Word count × tokens-per-word ratio
- **Default Ratio**: 1.3 tokens/word (English text)
- **Max Context Tokens**: 3000 (configurable)
- **Max Chunks Retrieved**: 10 (configurable)

### Prompt Structure
```
SYSTEM: [Instructions for LLM]

CONTEXT:
---
[Chunk 1 - Relevance: 0.XXX]
{chunk text}

[Chunk 2 - Relevance: 0.XXX]
{chunk text}
---

QUESTION: {user query}

ANSWER:
```

## Security Architecture

### Authentication Flow
1. User login → JWT token generation (24h expiration)
2. Token included in Authorization header: `Bearer {token}`
3. JwtAuthenticationFilter validates token on each request
4. SecurityContext populated with user details

### Authorization
- **Method Security**: @PreAuthorize annotations on service methods
- **Owner Checks**: Services validate userId matches document owner
- **Role-Based**: USER and ADMIN roles with hierarchical permissions

### Data Isolation
- **Database**: User-owned documents (ownerId foreign key)
- **Vector Store**: Per-user Qdrant collections
- **Filesystem**: UUID-prefixed filenames prevent conflicts

## API Endpoints

### Authentication (`/api/auth`)
- POST `/register` - User registration
- POST `/login` - User login (returns JWT)
- GET `/profile` - Get user profile (authenticated)
- PUT `/profile` - Update profile
- PUT `/password` - Update password

### Documents (`/api/documents`)
- POST `/upload` - Upload document
- GET `/` - List user's documents
- GET `/{id}` - Get document details
- DELETE `/{id}` - Delete document

### Search (`/api/search`)
- POST `/` - Semantic search (JSON body)
- GET `/` - Semantic search (query params)

### Analytics (`/api/analytics`)
- GET `/overview` - Dashboard overview statistics
- GET `/keywords` - Top keywords/tags by frequency (param: limit)
- GET `/uploads` - Upload trends and file type distribution
- GET `/activity` - Recent document activity (param: limit)

### Admin (`/admin`) - ADMIN ROLE REQUIRED
- GET `/users` - List all users with statistics
- GET `/users/{userId}` - Get specific user details
- PUT `/users/{userId}/activate` - Activate user account
- PUT `/users/{userId}/deactivate` - Deactivate user account
- DELETE `/users/{userId}` - Delete user and all documents
- GET `/system/health` - System health status
- GET `/system/stats` - System-wide statistics
- GET `/embeddings/status` - Embedding generation status
- POST `/embeddings/retry/{documentId}` - Retry failed embeddings
- GET `/embeddings/failed` - List documents with failed embeddings

## Configuration (application.properties)

### Database
- URL: `jdbc:postgresql://localhost:5432/kortex`
- DDL: `hibernate.ddl-auto=update`

### JWT
- Secret: Configurable (base64-encoded)
- Expiration: 86400000ms (24 hours)

### Document Storage
- Path: `./uploads` (relative to application root)

### HuggingFace
- API URL: `https://api-inference.huggingface.co/pipeline/feature-extraction`
- API Token: Environment variable `HUGGINGFACE_API_TOKEN`
- Model: `sentence-transformers/all-MiniLM-L6-v2`
- Batch Size: 32

### Qdrant
- Host: Environment variable `QDRANT_HOST` (default: localhost)
- Port: `QDRANT_PORT` (default: 6334)
- API Key: `QDRANT_API_KEY` (optional)
- TLS: `QDRANT_USE_TLS` (default: false)
- Collection Prefix: `kortex`

### RAG
- Max Context Tokens: 3000 (configurable via `rag.max.context.tokens`)
- Chunk Limit: 10 (configurable via `rag.context.chunk.limit`)
- Tokens/Word: 1.3 (configurable via `rag.tokens.per.word`)

## Module Status

### Completed Modules ✅
1. **User Authentication & Profile** - JWT auth, BCrypt, RBAC
2. **Document Management** - Upload, versioning, duplicate detection
3. **Text Extraction & Preprocessing** - PDF/DOCX/DOC/TXT, intelligent chunking
4. **Embedding & Vector Store** - HuggingFace embeddings, Qdrant integration
5. **Semantic Search** - Natural language queries, REST APIs
6. **RAG Question Answering** - OpenAI/Groq integration, streaming responses
7. **AI Summarization & Auto-Tagging** - Automatic content analysis
8. **Real-Time Notifications** - WebSocket with STOMP, JWT auth, 11 notification types
9. **Analytics Dashboard** - Word count, reading time, keyword frequency, upload trends, activity logs
10. **Admin Management** - User management, system health monitoring, embedding status tracking

### All Modules Complete! 🎉

## Design Decisions

### Why Dual Storage for Embeddings?
- **PostgreSQL**: Backup, audit trail, relational integrity
- **Qdrant**: High-performance vector similarity search
- **Trade-off**: Storage overhead for search performance

### Why Per-User Collections?
- **Security**: Multi-tenant isolation at vector DB level
- **Performance**: Smaller search spaces improve query speed
- **Scalability**: Independent collection management

### Why Batch Processing?
- **API Limits**: HuggingFace API has rate limits
- **Performance**: 32 chunks per request reduces API calls significantly
- **Cost**: Fewer API calls reduce potential usage costs

### Why Token Limiting in RAG?
- **LLM Constraints**: Most LLMs have fixed context windows (e.g., GPT-3.5: 4096 tokens)
- **Quality**: Focused context improves answer quality
- **Cost**: Fewer tokens reduce API costs for paid LLMs

### Why Chunking 150-300 Words?
- **Semantic Coherence**: Preserves meaningful context
- **Search Granularity**: Balances precision and recall
- **Embedding Quality**: Optimal size for sentence-transformer models
