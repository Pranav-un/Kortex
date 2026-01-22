import { apiClient } from './apiClient';
import type { SearchResponse, QuestionResponse } from '../types';

export const searchService = {
  async search(query: string, limit: number = 10, documentId?: number): Promise<SearchResponse> {
    return apiClient.post<SearchResponse>('/search', { query, limit, documentId });
  },

  async askQuestion(question: string, documentId?: number): Promise<QuestionResponse> {
    return apiClient.post<QuestionResponse>('/qa', { question, documentId });
  },
};
