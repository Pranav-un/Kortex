export interface User {
  id: number;
  email: string;
  name: string;
  role: 'USER' | 'ADMIN';
  active: boolean;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  name: string;
  role: 'USER' | 'ADMIN';
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface Document {
  id: number;
  filename: string;
  fileType: string;
  size: number;
  uploadTime: string;
  version: number;
  pageCount: number | null;
  extractedText: string | null;
  textExtractionSuccess: boolean;
  summary: string | null;
  summaryGeneratedAt: string | null;
  tags: string | null;
  tagsGeneratedAt: string | null;
  embeddingsGenerated: boolean;
  previousVersionId: number | null;
}

export interface DocumentUploadResponse {
  id: number;
  filename: string;
  message: string;
  size: number;
  fileType: string;
  uploadTime: string;
  version: number;
  previousVersionId: number | null;
}

export interface SearchResult {
  chunkId: number;
  documentId: number;
  documentName: string;
  chunkText: string;
  similarityScore: number;
  chunkOrder: number;
  wordCount?: number;
  startPosition?: number;
  endPosition?: number;
  uploadTime?: string;
}

export interface SearchResponse {
  query: string;
  results: SearchResult[];
  totalResults: number;
}

export interface QuestionResponse {
  question: string;
  answer: string;
  citations: Citation[];
  contextUsed: string[];
  model: string;
  documentId: number | null;
  llmProvider?: string;
  llmModel?: string;
  contextChunksUsed?: number;
  estimatedTokens?: number;
}

export interface Citation {
  documentId: number;
  documentName: string;
  chunkOrder: number;
  similarityScore: number;
  chunkId?: number;
  citationNumber?: number;
  relevanceScore?: number;
  excerpt?: string;
}

export interface NotificationMessage {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  documentId: number | null;
  documentName: string | null;
  timestamp: string;
  read: boolean;
  data: any;
}

export type NotificationType =
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

export interface AnalyticsOverview {
  totalDocuments: number;
  totalWords: number;
  averageWordsPerDocument: number;
  totalReadingTimeMinutes: number;
  averageReadingTimeMinutes: number;
  totalStorageBytes: number;
  totalStorageFormatted: string;
  documentsWithText: number;
  documentsWithEmbeddings: number;
  documentsWithSummaries: number;
  documentsWithTags: number;
  fileTypeDistribution: Record<string, number>;
}

export interface KeywordFrequency {
  keywords: {
    keyword: string;
    documentCount: number;
    frequencyPercentage: number;
  }[];
  totalUniqueKeywords: number;
}

export interface UploadStatistics {
  uploadsByDate: { date: string; count: number }[];
  uploadsByMonth: { month: string; count: number }[];
  fileTypeStats: { fileType: string; count: number; percentage: number }[];
  averageSizeBytes: number;
  averageSizeFormatted: string;
  largestDocumentBytes: number;
  largestDocumentName: string;
  uploadsLast7Days: number;
  uploadsLast30Days: number;
}

export interface RecentActivity {
  recentUploads: {
    activityType: string;
    documentId: number;
    documentName: string;
    fileType: string;
    timestamp: string;
    description: string;
    metadata: string;
  }[];
  totalItems: number;
}

export interface UserManagement {
  id: number;
  email: string;
  name: string;
  role: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  documentCount: number;
  totalStorageBytes: number;
  totalStorageFormatted: string;
  lastLoginAt: string | null;
}

export interface SystemHealth {
  status: 'healthy' | 'degraded' | 'unhealthy';
  timestamp: string;
  uptimeSeconds: number;
  database: ComponentHealth;
  vectorDatabase: ComponentHealth;
  storage: ComponentHealth;
  resources: ResourceUsage;
}

export interface ComponentHealth {
  status: 'up' | 'down' | 'unknown';
  message: string;
  responseTimeMs: number;
}

export interface ResourceUsage {
  usedMemoryMB: number;
  maxMemoryMB: number;
  memoryUsagePercentage: number;
  activeThreads: number;
  diskUsedBytes: number;
  diskTotalBytes: number;
  diskUsedFormatted: string;
  diskTotalFormatted: string;
}

export interface SystemStats {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  adminUsers: number;
  totalDocuments: number;
  totalStorageBytes: number;
  totalStorageFormatted: string;
  documentsByFileType: Record<string, number>;
  documentsWithText: number;
  documentsWithEmbeddings: number;
  documentsWithSummaries: number;
  documentsWithTags: number;
  processingSuccessRate: number;
  totalChunks: number;
  chunksWithEmbeddings: number;
  embeddingCoverage: number;
  uploadsLast24Hours: number;
  uploadsLast7Days: number;
  uploadsLast30Days: number;
}

export interface EmbeddingStatus {
  totalDocuments: number;
  documentsWithEmbeddings: number;
  documentsPending: number;
  documentsFailed: number;
  completionPercentage: number;
  totalChunks: number;
  chunksWithEmbeddings: number;
  chunksPending: number;
  failedDocuments: FailedEmbedding[];
}

export interface FailedEmbedding {
  documentId: number;
  documentName: string;
  ownerEmail: string;
  totalChunks: number;
  failedChunks: number;
  uploadTime: string;
  errorMessage: string;
}

export interface TagInfo {
  tag: string;
  documentCount: number;
}

export interface SimilarDocument {
  documentId: number;
  documentName: string;
  similarityScore: number;
  tags: string | null;
  uploadTime: string;
}

export interface DocumentCluster {
  clusterName: string;
  documents: Document[];
  commonTags: string[];
  documentCount: number;
}
