import { apiClient } from './apiClient';
import type {
  UserManagement,
  SystemHealth,
  SystemStats,
  EmbeddingStatus,
  FailedEmbedding,
} from '../types';

export const adminService = {
  // User Management
  async getAllUsers(): Promise<UserManagement[]> {
    return apiClient.get<UserManagement[]>('/admin/users');
  },

  async getUserById(userId: number): Promise<UserManagement> {
    return apiClient.get<UserManagement>(`/admin/users/${userId}`);
  },

  async activateUser(userId: number): Promise<UserManagement> {
    return apiClient.put<UserManagement>(`/admin/users/${userId}/activate`);
  },

  async deactivateUser(userId: number): Promise<UserManagement> {
    return apiClient.put<UserManagement>(`/admin/users/${userId}/deactivate`);
  },

  async deleteUser(userId: number): Promise<void> {
    await apiClient.delete(`/admin/users/${userId}`);
  },

  // System Health & Stats
  async getSystemHealth(): Promise<SystemHealth> {
    return apiClient.get<SystemHealth>('/admin/system/health');
  },

  async getSystemStats(): Promise<SystemStats> {
    return apiClient.get<SystemStats>('/admin/system/stats');
  },

  // Embedding Monitoring
  async getEmbeddingStatus(): Promise<EmbeddingStatus> {
    return apiClient.get<EmbeddingStatus>('/admin/embeddings/status');
  },

  async retryEmbedding(documentId: number): Promise<{ message: string; documentId: string }> {
    return apiClient.post<{ message: string; documentId: string }>(
      `/admin/embeddings/retry/${documentId}`
    );
  },

  async getFailedEmbeddings(): Promise<FailedEmbedding[]> {
    return apiClient.get<FailedEmbedding[]>('/admin/embeddings/failed');
  },
};
