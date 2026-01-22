import { apiClient } from './apiClient';
import type { Document, DocumentUploadResponse } from '../types';

export const documentService = {
  async uploadDocument(file: File, onProgress?: (progress: number) => void): Promise<DocumentUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.upload<DocumentUploadResponse>('/documents/upload', formData, onProgress);
  },

  async getDocuments(): Promise<Document[]> {
    return apiClient.get<Document[]>('/documents');
  },

  async getDocument(id: number): Promise<Document> {
    return apiClient.get<Document>(`/documents/${id}`);
  },

  async getDocumentById(id: number): Promise<Document> {
    return apiClient.get<Document>(`/documents/${id}`);
  },

  async deleteDocument(id: number): Promise<void> {
    await apiClient.delete(`/documents/${id}`);
  },

  async regenerateSummary(id: number): Promise<{ summary: string }> {
    return apiClient.post<{ summary: string }>(`/documents/${id}/regenerate-summary`);
  },
};
