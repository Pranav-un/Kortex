import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Document, AnalyticsOverview } from '../types';
import { documentService } from '../services/documentService';
import { analyticsService } from '../services/analyticsService';
import { Card, Badge, LoadingSpinner } from '../components/ui';
import { FileText, Search, MessageSquare, BarChart3, CheckCircle, AlertCircle } from 'lucide-react';
import { ROUTES } from '../config/constants';

const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const [documents, setDocuments] = useState<Document[]>([]);
  const [overview, setOverview] = useState<AnalyticsOverview | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [docsData, overviewData] = await Promise.all([
          documentService.getDocuments(),
          analyticsService.getOverview().catch(() => null),
        ]);
        setDocuments(docsData);
        setOverview(overviewData);
      } catch (error) {
        console.error('Failed to fetch dashboard data:', error);
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

  const recentDocuments = documents.slice(0, 5);

  return (
    <div className="min-h-screen bg-slate-50 p-6">
      <div className="max-w-7xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-slate-900 mb-2">Dashboard</h1>
          <p className="text-slate-600">Overview of your document management system</p>
        </div>

        {/* Overview Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          <Card>
            <div className="flex items-center gap-4">
              <div className="p-3 bg-slate-100 rounded-lg">
                <FileText size={24} className="text-slate-700" />
              </div>
              <div>
                <p className="text-sm text-slate-600">Total Documents</p>
                <p className="text-2xl font-bold text-slate-900">
                  {overview?.totalDocuments || documents.length}
                </p>
              </div>
            </div>
          </Card>

          <Card>
            <div className="flex items-center gap-4">
              <div className="p-3 bg-green-100 rounded-lg">
                <CheckCircle size={24} className="text-green-700" />
              </div>
              <div>
                <p className="text-sm text-slate-600">With Embeddings</p>
                <p className="text-2xl font-bold text-slate-900">
                  {overview?.documentsWithEmbeddings || 0}
                </p>
              </div>
            </div>
          </Card>

          <Card>
            <div className="flex items-center gap-4">
              <div className="p-3 bg-blue-100 rounded-lg">
                <MessageSquare size={24} className="text-blue-700" />
              </div>
              <div>
                <p className="text-sm text-slate-600">With Summaries</p>
                <p className="text-2xl font-bold text-slate-900">
                  {overview?.documentsWithSummaries || 0}
                </p>
              </div>
            </div>
          </Card>

          <Card>
            <div className="flex items-center gap-4">
              <div className="p-3 bg-purple-100 rounded-lg">
                <BarChart3 size={24} className="text-purple-700" />
              </div>
              <div>
                <p className="text-sm text-slate-600">Total Words</p>
                <p className="text-2xl font-bold text-slate-900">
                  {overview?.totalWords?.toLocaleString() || '0'}
                </p>
              </div>
            </div>
          </Card>
        </div>

        {/* Quick Actions */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <Card hover className="cursor-pointer" onClick={() => navigate(ROUTES.DOCUMENTS)}>
            <div className="flex items-center gap-3 mb-2">
              <FileText size={20} className="text-slate-700" />
              <h3 className="font-semibold text-slate-900">Manage Documents</h3>
            </div>
            <p className="text-sm text-slate-600">
              Upload, organize, and manage your document collection
            </p>
          </Card>

          <Card hover className="cursor-pointer" onClick={() => navigate(ROUTES.SEARCH)}>
            <div className="flex items-center gap-3 mb-2">
              <Search size={20} className="text-slate-700" />
              <h3 className="font-semibold text-slate-900">Semantic Search</h3>
            </div>
            <p className="text-sm text-slate-600">
              Find information using natural language queries
            </p>
          </Card>

          <Card hover className="cursor-pointer" onClick={() => navigate(ROUTES.CHAT)}>
            <div className="flex items-center gap-3 mb-2">
              <MessageSquare size={20} className="text-slate-700" />
              <h3 className="font-semibold text-slate-900">AI Chat</h3>
            </div>
            <p className="text-sm text-slate-600">
              Ask questions and get answers with citations
            </p>
          </Card>
        </div>

        {/* Recent Documents */}
        <Card>
          <div className="mb-4">
            <h2 className="text-xl font-semibold text-slate-900 mb-1">Recent Documents</h2>
            <p className="text-sm text-slate-600">Your most recently uploaded documents</p>
          </div>

          {recentDocuments.length > 0 ? (
            <div className="space-y-3">
              {recentDocuments.map((doc) => (
                <div
                  key={doc.id}
                  className="flex items-center justify-between p-3 rounded-lg border border-slate-200 hover:bg-slate-50 transition-colors"
                >
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    <FileText size={20} className="text-slate-600 flex-shrink-0" />
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-slate-900 truncate">{doc.filename}</p>
                      <p className="text-sm text-slate-600">
                        {new Date(doc.uploadTime).toLocaleDateString()} · {(doc.size / 1024).toFixed(1)} KB
                      </p>
                    </div>
                  </div>
                  <Badge variant="default" size="sm">
                    {doc.fileType}
                  </Badge>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8">
              <AlertCircle size={48} className="text-slate-300 mx-auto mb-3" />
              <p className="text-slate-600 mb-2">No documents yet</p>
              <p className="text-sm text-slate-500">
                Upload your first document to get started
              </p>
            </div>
          )}
        </Card>
      </div>
    </div>
  );
};

export default DashboardPage;
