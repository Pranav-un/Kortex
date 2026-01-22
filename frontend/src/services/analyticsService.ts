import { apiClient } from './apiClient';
import type {
  AnalyticsOverview,
  KeywordFrequency,
  UploadStatistics,
  RecentActivity,
} from '../types';

export const analyticsService = {
  async getOverview(): Promise<AnalyticsOverview> {
    return apiClient.get<AnalyticsOverview>('/analytics/overview');
  },

  async getKeywordFrequency(limit: number = 20): Promise<KeywordFrequency> {
    return apiClient.get<KeywordFrequency>(`/analytics/keywords?limit=${limit}`);
  },

  async getUploadStatistics(): Promise<UploadStatistics> {
    return apiClient.get<UploadStatistics>('/analytics/uploads');
  },

  async getRecentActivity(limit: number = 20): Promise<RecentActivity> {
    return apiClient.get<RecentActivity>(`/analytics/activity?limit=${limit}`);
  },
};
