import React, { useEffect, useState } from 'react';
import type { AnalyticsOverview, UploadStatistics, RecentActivity } from '../types';
import { analyticsService } from '../services/analyticsService';
import { Card, Badge, LoadingSpinner } from '../components/ui';
import { BarChart3, FileText, TrendingUp, Activity } from 'lucide-react';

const AnalyticsPage: React.FC = () => {
  const [overview, setOverview] = useState<AnalyticsOverview | null>(null);
  const [keywords, setKeywords] = useState<{ keyword: string; documentCount: number; frequencyPercentage: number }[]>([]);
  const [uploads, setUploads] = useState<UploadStatistics | null>(null);
  const [activity, setActivity] = useState<RecentActivity | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [overviewData, keywordsData, uploadsData, activityData] = await Promise.all([
          analyticsService.getOverview().catch(() => null),
          analyticsService.getKeywordFrequency(20).catch(() => ({ keywords: [] })),
          analyticsService.getUploadStatistics().catch(() => null),
          analyticsService.getRecentActivity(20).catch(() => null),
        ]);
        setOverview(overviewData);
        setKeywords(keywordsData.keywords);
        setUploads(uploadsData);
        setActivity(activityData);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 p-6">
      <div className="max-w-7xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-slate-900 mb-2">Analytics</h1>
          <p className="text-slate-600">Insights into your document collection and usage</p>
        </div>

        {/* Overview cards */}
        {overview && (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
            <Card>
              <div className="flex items-center gap-4">
                <div className="p-3 bg-slate-100 rounded-lg">
                  <FileText size={24} className="text-slate-700" />
                </div>
                <div>
                  <p className="text-sm text-slate-600">Total Documents</p>
                  <p className="text-2xl font-bold text-slate-900">{overview.totalDocuments}</p>
                </div>
              </div>
            </Card>
            <Card>
              <div className="flex items-center gap-4">
                <div className="p-3 bg-blue-100 rounded-lg">
                  <BarChart3 size={24} className="text-blue-700" />
                </div>
                <div>
                  <p className="text-sm text-slate-600">Total Words</p>
                  <p className="text-2xl font-bold text-slate-900">{overview.totalWords.toLocaleString()}</p>
                </div>
              </div>
            </Card>
            <Card>
              <div className="flex items-center gap-4">
                <div className="p-3 bg-green-100 rounded-lg">
                  <TrendingUp size={24} className="text-green-700" />
                </div>
                <div>
                  <p className="text-sm text-slate-600">With Embeddings</p>
                  <p className="text-2xl font-bold text-slate-900">{overview.documentsWithEmbeddings}</p>
                </div>
              </div>
            </Card>
            <Card>
              <div className="flex items-center gap-4">
                <div className="p-3 bg-purple-100 rounded-lg">
                  <Activity size={24} className="text-purple-700" />
                </div>
                <div>
                  <p className="text-sm text-slate-600">With Summaries</p>
                  <p className="text-2xl font-bold text-slate-900">{overview.documentsWithSummaries}</p>
                </div>
              </div>
            </Card>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Keywords */}
          <Card>
            <h2 className="text-lg font-semibold text-slate-900 mb-4">Top Keywords</h2>
            {keywords.length ? (
              <div className="space-y-2 max-h-96 overflow-y-auto">
                {keywords.map((k) => (
                  <div key={k.keyword} className="flex items-center justify-between p-2 rounded border border-slate-200 hover:bg-slate-50">
                    <span className="font-medium text-slate-700">{k.keyword}</span>
                    <div className="flex items-center gap-2">
                      <Badge variant="default" size="sm">{k.documentCount} docs</Badge>
                      <Badge variant="info" size="sm">{k.frequencyPercentage.toFixed(1)}%</Badge>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-slate-600 text-center py-8">No keyword data yet</p>
            )}
          </Card>

          {/* Upload Statistics */}
          <div className="space-y-6">
            <Card>
              <h2 className="text-lg font-semibold text-slate-900 mb-4">Upload Statistics</h2>
              {uploads ? (
                <div className="space-y-3">
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-slate-600">Average File Size</span>
                    <span className="font-medium text-slate-900">{uploads.averageSizeFormatted}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-slate-600">Last 7 Days</span>
                    <Badge variant="success">{uploads.uploadsLast7Days} uploads</Badge>
                  </div>
                </div>
              ) : (
                <p className="text-slate-600 text-center py-8">No upload statistics available</p>
              )}
            </Card>

            {/* Recent Activity */}
            <Card>
              <h2 className="text-lg font-semibold text-slate-900 mb-4">Recent Activity</h2>
              {activity?.recentUploads?.length ? (
                <div className="space-y-2 max-h-64 overflow-y-auto">
                  {activity.recentUploads.map((a, idx) => (
                    <div key={`${a.activityType}-${a.documentId}-${a.timestamp}-${idx}`} className="p-3 rounded border border-slate-200">
                      <div className="flex items-center justify-between mb-1">
                        <Badge variant="info" size="sm">{a.activityType}</Badge>
                        <span className="text-xs text-slate-500">{new Date(a.timestamp).toLocaleString()}</span>
                      </div>
                      <p className="text-sm font-medium text-slate-900">{a.documentName}</p>
                      <p className="text-xs text-slate-600 mt-1">{a.description}</p>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-slate-600 text-center py-8">No recent activity</p>
              )}
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AnalyticsPage;
