import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Document, AnalyticsOverview } from '../types';
import { documentService } from '../services/documentService';
import { analyticsService } from '../services/analyticsService';
import { Card, Badge, LoadingSpinner } from '../components/ui';
import { FileText, Search, MessageSquare, BarChart3, CheckCircle, AlertCircle, LayoutDashboard } from 'lucide-react';
import { ROUTES } from '../config/constants';
import Dock from '../components/magicui/Dock';
import './Dashboard.css';
import { BentoGrid, BentoCard } from '../components/magicui/BentoGrid';
import Globe from '../components/magicui/Globe';

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

  const dockItems = useMemo(() => ([
    { icon: <LayoutDashboard size={20} />, label: 'Dashboard', onClick: () => navigate(ROUTES.DASHBOARD) },
    { icon: <FileText size={20} />, label: 'Documents', onClick: () => navigate(ROUTES.DOCUMENTS) },
    { icon: <Search size={20} />, label: 'Search', onClick: () => navigate(ROUTES.SEARCH) },
    { icon: <MessageSquare size={20} />, label: 'AI Chat', onClick: () => navigate(ROUTES.CHAT) },
    { icon: <BarChart3 size={20} />, label: 'Analytics', onClick: () => navigate(ROUTES.ANALYTICS) },
  ]), [navigate]);

  if (loading) {
    return (
      <div className="min-h-screen bg-[#0D0620] pt-28 px-6">
        <Dock items={dockItems} panelHeight={68} baseItemSize={50} magnification={70} />
        <div className="flex items-center justify-center min-h-[60vh]">
          <LoadingSpinner size="lg" />
        </div>
      </div>
    );
  }

  const recentDocuments = documents.slice(0, 5);
  const totalDocs = overview?.totalDocuments || documents.length || 0;
  const withEmbeddings = overview?.documentsWithEmbeddings || 0;
  const withSummaries = overview?.documentsWithSummaries || 0;
  const totalWords = overview?.totalWords || 0;
  const embeddingsPct = totalDocs ? Math.round((withEmbeddings / totalDocs) * 100) : 0;
  const summariesPct = totalDocs ? Math.round((withSummaries / totalDocs) * 100) : 0;
  const avgWordsPerDoc = totalDocs ? Math.round(totalWords / totalDocs) : 0;

  return (
    <div className="min-h-screen bg-[#0D0620] pt-28 px-6">
      <Dock items={dockItems} panelHeight={68} baseItemSize={50} magnification={70} />
      <div className="max-w-7xl mx-auto">
        <BentoGrid>
          {/* Overview metrics (2x2) + Globe */}
          <BentoCard
            name="Overview"
            className="col-span-4 xl:col-span-3 metrics-card"
          >
            <div className="metrics-grid">
              <div className="metric-tile">
                <div className="flex items-center gap-4">
                  <div className="p-3 rounded-lg bg-[#120A24] border border-[#2A1B45]">
                    <FileText size={24} className="text-[#CF9EFF]" />
                  </div>
                  <div>
                    <p className="text-sm text-[#CF9EFF]">Total Documents</p>
                    <p className="text-2xl font-bold text-[#EDE3FF]">
                      {totalDocs}
                    </p>
                    <p className="text-xs text-[#CF9EFF] mt-1">Avg words/doc: {avgWordsPerDoc}</p>
                  </div>
                </div>
                <div className="metric-progress"><div className="metric-bar" style={{width: `${Math.min(100, avgWordsPerDoc / 1000 * 100)}%`}} /></div>
              </div>
              
              <div className="metric-tile">
                <div className="flex items-center gap-4">
                  <div className="p-3 rounded-lg bg-[#120A24] border border-[#2A1B45]">
                    <CheckCircle size={24} className="text-[#7A5CF5]" />
                  </div>
                  <div>
                    <p className="text-sm text-[#CF9EFF]">With Embeddings</p>
                    <p className="text-2xl font-bold text-[#EDE3FF]">
                      {withEmbeddings}
                    </p>
                    <p className="text-xs text-[#CF9EFF] mt-1">{embeddingsPct}% of total</p>
                  </div>
                </div>
                <div className="metric-progress"><div className="metric-bar" style={{width: `${embeddingsPct}%`}} /></div>
              </div>

              <div className="metric-tile">
                <div className="flex items-center gap-4">
                  <div className="p-3 rounded-lg bg-[#120A24] border border-[#2A1B45]">
                    <MessageSquare size={24} className="text-[#A98BFF]" />
                  </div>
                  <div>
                    <p className="text-sm text-[#CF9EFF]">With Summaries</p>
                    <p className="text-2xl font-bold text-[#EDE3FF]">
                      {withSummaries}
                    </p>
                    <p className="text-xs text-[#CF9EFF] mt-1">{summariesPct}% of total</p>
                  </div>
                </div>
                <div className="metric-progress"><div className="metric-bar" style={{width: `${summariesPct}%`}} /></div>
              </div>

              <div className="metric-tile">
                <div className="flex items-center gap-4">
                  <div className="p-3 rounded-lg bg-[#120A24] border border-[#2A1B45]">
                    <BarChart3 size={24} className="text-[#CF9EFF]" />
                  </div>
                  <div>
                    <p className="text-sm text-[#CF9EFF]">Total Words</p>
                    <p className="text-2xl font-bold text-[#EDE3FF]">
                      {totalWords?.toLocaleString() || '0'}
                    </p>
                    <p className="text-xs text-[#CF9EFF] mt-1">Across {totalDocs} documents</p>
                  </div>
                </div>
                <div className="metric-progress"><div className="metric-bar" style={{width: `${Math.min(100, (totalWords/ (totalDocs || 1)) / 2000 * 100)}%`}} /></div>
              </div>
            </div>
          </BentoCard>

          <BentoCard name="Globe" className="col-span-2 xl:col-span-3 globe-card">
            <Globe config={{
              dark: 1,
              // Lilac dots for visibility
              baseColor: [0.812, 0.619, 1.0],
              markerColor: [0.98, 0.5, 0.2],
              // Match atmosphere glow to card background to remove white ring
              glowColor: [0.039, 0.012, 0.086],
              diffuse: 1.1,
              mapBrightness: 1.2
            }} />
          </BentoCard>
          {/* Stats moved into the overview card above */}
        </BentoGrid>

        {/* Quick Actions Bento */}
        <div className="bento-grid actions mt-6 mb-8">
          <Card hover className="bento-card bento-action cursor-pointer" onClick={() => navigate(ROUTES.DOCUMENTS)}>
            <div className="flex items-center gap-3 mb-2">
              <FileText size={20} className="text-[#CF9EFF]" />
              <h3 className="font-semibold text-[#EDE3FF]">Manage Documents</h3>
            </div>
            <p className="text-sm text-[#CF9EFF]">
              Upload, organize, and manage your document collection
            </p>
          </Card>

          <Card hover className="bento-card bento-action cursor-pointer" onClick={() => navigate(ROUTES.SEARCH)}>
            <div className="flex items-center gap-3 mb-2">
              <Search size={20} className="text-[#CF9EFF]" />
              <h3 className="font-semibold text-[#EDE3FF]">Semantic Search</h3>
            </div>
            <p className="text-sm text-[#CF9EFF]">
              Find information using natural language queries
            </p>
          </Card>

          <Card hover className="bento-card bento-action cursor-pointer" onClick={() => navigate(ROUTES.CHAT)}>
            <div className="flex items-center gap-3 mb-2">
              <MessageSquare size={20} className="text-[#A98BFF]" />
              <h3 className="font-semibold text-[#EDE3FF]">AI Chat</h3>
            </div>
            <p className="text-sm text-[#CF9EFF]">
              Ask questions and get answers with citations
            </p>
          </Card>
        </div>

        {/* Recent Documents */}
        <Card className="bento-card">
          <div className="mb-4">
            <h2 className="text-xl font-semibold text-[#EDE3FF] mb-1">Recent Documents</h2>
            <p className="text-sm text-[#CF9EFF]">Your most recently uploaded documents</p>
          </div>

          {recentDocuments.length > 0 ? (
            <div className="space-y-3">
              {recentDocuments.map((doc) => (
                <div
                  key={doc.id}
                  className="flex items-center justify-between p-3 rounded-lg border border-[#2A1B45] hover:bg-[#120A24] transition-colors"
                >
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    <FileText size={20} className="text-[#CF9EFF] flex-shrink-0" />
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-[#EDE3FF] truncate">{doc.filename}</p>
                      <p className="text-sm text-[#CF9EFF]">
                        {new Date(doc.uploadTime).toLocaleDateString()} · {(doc.size / 1024).toFixed(1)} KB
                      </p>
                    </div>
                  </div>
                  <Badge variant="default" size="sm" className="bg-[#120A24] text-[#EDE3FF] border border-[#2A1B45]">
                    {doc.fileType}
                  </Badge>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8">
              <AlertCircle size={48} className="text-[#A98BFF] mx-auto mb-3" />
              <p className="text-[#CF9EFF] mb-2">No documents yet</p>
              <p className="text-sm text-[#CF9EFF]">
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
