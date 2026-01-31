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
import AnimatedList from '../components/magicui/AnimatedList';
import NotificationCard from '../components/magicui/NotificationCard';
import { useAuth } from '../contexts/AuthContext';

const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [documents, setDocuments] = useState<Document[]>([]);
  const [overview, setOverview] = useState<AnalyticsOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [showWelcome, setShowWelcome] = useState(true);

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

  useEffect(() => {
    // Show welcome notification for 10 seconds after entering dashboard
    setShowWelcome(true);
    const t = setTimeout(() => setShowWelcome(false), 10000);
    return () => clearTimeout(t);
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

  const lastDoc = recentDocuments[0];
  const formatTimeAgo = (d: Date) => {
    const diff = Date.now() - d.getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 60) return `${mins}m ago`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    return `${days}d ago`;
  };

  return (
    <div className="min-h-screen bg-[#0D0620] pt-28 px-6">
      <Dock items={dockItems} panelHeight={68} baseItemSize={50} magnification={70} />
      {showWelcome && (
        <div className="fixed top-24 left-0 right-0 z-50 flex justify-center px-4">
          <AnimatedList>
            <NotificationCard
              name={`Welcome, ${user?.name || 'User'}`}
              description="You’re all set — enjoy Kortex!"
              time="Just now"
              icon="👋"
              color="#7A5CF5"
            />
          </AnimatedList>
        </div>
      )}
      <div className="max-w-5xl mx-auto">
        <BentoGrid>
          {/* Overview metrics (standalone grid, no outer box) */}
          <div className="col-span-4 xl:col-span-3 metrics-grid-standalone">
              <div className="metric-tile">
                <div className="metric-header">
                  <div className="metric-icon p-3 rounded-lg bg-[#120A24] border border-[#2A1B45]">
                    <FileText size={20} className="text-[#CF9EFF]" />
                  </div>
                  <h4 className="metric-title">Total Documents</h4>
                  <span className="metric-value ml-auto">{totalDocs}</span>
                </div>
                <div className="metric-bar-wrap">
                  <div className="metric-bar" style={{width: `100%`}} />
                </div>
                <p className="metric-subtext">Avg words/doc: {avgWordsPerDoc}</p>
                {lastDoc ? (
                  <p className="metric-foot">Last upload: {lastDoc.filename} · {formatTimeAgo(new Date(lastDoc.uploadTime))}</p>
                ) : null}
              </div>
              
              <div className="metric-tile">
                <div className="metric-header">
                  <div className="metric-icon p-3 rounded-lg bg-[#120A24] border border-[#2A1B45]">
                    <CheckCircle size={20} className="text-[#7A5CF5]" />
                  </div>
                  <h4 className="metric-title">With Embeddings</h4>
                  <span className="metric-value ml-auto">{withEmbeddings}</span>
                </div>
                <div className="metric-bar-wrap">
                  <div className="metric-bar" style={{width: `${embeddingsPct}%`}} />
                </div>
                <p className="metric-subtext">{embeddingsPct}% of total</p>
                {lastDoc ? (
                  <p className="metric-foot">Latest embedded: {withEmbeddings}/{totalDocs}</p>
                ) : null}
              </div>

              <div className="metric-tile">
                <div className="metric-header">
                  <div className="metric-icon p-3 rounded-lg bg-[#120A24] border border-[#2A1B45]">
                    <MessageSquare size={20} className="text-[#A98BFF]" />
                  </div>
                  <h4 className="metric-title">With Summaries</h4>
                  <span className="metric-value ml-auto">{withSummaries}</span>
                </div>
                <div className="metric-bar-wrap">
                  <div className="metric-bar" style={{width: `${summariesPct}%`}} />
                </div>
                <p className="metric-subtext">{summariesPct}% of total</p>
                {lastDoc ? (
                  <p className="metric-foot">Latest summarized: {withSummaries}/{totalDocs}</p>
                ) : null}
              </div>

              <div className="metric-tile">
                <div className="metric-header">
                  <div className="metric-icon p-3 rounded-lg bg-[#120A24] border border-[#2A1B45]">
                    <BarChart3 size={20} className="text-[#CF9EFF]" />
                  </div>
                  <h4 className="metric-title">Total Words</h4>
                  <span className="metric-value ml-auto">{totalWords?.toLocaleString() || '0'}</span>
                </div>
                <div className="metric-bar-wrap">
                  <div className="metric-bar" style={{width: `${Math.min(100, (avgWordsPerDoc / 2000) * 100)}%`}} />
                </div>
                <p className="metric-subtext">Across {totalDocs} documents</p>
                {lastDoc ? (
                  <p className="metric-foot">Avg per doc: {avgWordsPerDoc}</p>
                ) : null}
              </div>
          </div>

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
        <div className="bento-grid actions mt-4 mb-8">
          <Card hover className="bento-card bento-action action-small cursor-pointer" onClick={() => navigate(ROUTES.DOCUMENTS)}>
            <div className="flex items-center gap-3 mb-2">
              <FileText size={20} className="text-[#CF9EFF]" />
              <h3 className="font-semibold text-[#EDE3FF]">Manage Documents</h3>
            </div>
            <p className="text-sm text-[#CF9EFF]">
              Upload, organize, and manage your document collection
            </p>
          </Card>

          <Card hover className="bento-card bento-action action-small cursor-pointer" onClick={() => navigate(ROUTES.SEARCH)}>
            <div className="flex items-center gap-3 mb-2">
              <Search size={20} className="text-[#CF9EFF]" />
              <h3 className="font-semibold text-[#EDE3FF]">Semantic Search</h3>
            </div>
            <p className="text-sm text-[#CF9EFF]">
              Find information using natural language queries
            </p>
          </Card>

          <Card hover className="bento-card bento-action action-small cursor-pointer" onClick={() => navigate(ROUTES.CHAT)}>
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
