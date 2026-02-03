import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import type { AnalyticsOverview, UploadStatistics, RecentActivity } from '../types';
import { analyticsService } from '../services/analyticsService';
import { Card, Badge, LoadingSpinner } from '../components/ui';
import { BarChart3, FileText, TrendingUp, Activity, MessageSquare, Search as SearchIcon, LayoutDashboard, User as UserIcon, LogOut } from 'lucide-react';
import Dock from '../components/magicui/Dock';
import { ROUTES } from '../config/constants';
import './Analytics.css';
import { useAuth } from '../contexts/AuthContext';
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  Legend,
} from 'recharts';

const AnalyticsPage: React.FC = () => {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const dockItems = useMemo(() => ([
    { icon: <LayoutDashboard size={20} />, label: 'Dashboard', onClick: () => navigate(ROUTES.DASHBOARD) },
    { icon: <FileText size={20} />, label: 'Documents', onClick: () => navigate(ROUTES.DOCUMENTS) },
    { icon: <SearchIcon size={20} />, label: 'Search', onClick: () => navigate(ROUTES.SEARCH) },
    { icon: <MessageSquare size={20} />, label: 'AI Chat', onClick: () => navigate(ROUTES.CHAT) },
    { icon: <BarChart3 size={20} />, label: 'Analytics', onClick: () => navigate(ROUTES.ANALYTICS) },
    { icon: <UserIcon size={20} />, label: 'Profile', onClick: () => navigate(ROUTES.PROFILE) },
    { icon: <LogOut size={20} />, label: 'Logout', onClick: () => logout() },
  ]), [navigate, logout]);
  const [overview, setOverview] = useState<AnalyticsOverview | null>(null);
  const [keywords, setKeywords] = useState<{ keyword: string; documentCount: number; frequencyPercentage: number }[]>([]);
  const [uploads, setUploads] = useState<UploadStatistics | null>(null);
  const [activity, setActivity] = useState<RecentActivity | null>(null);
  const [loading, setLoading] = useState(true);
  const chartColors = {
    accent: '#CF9EFF',
    accent2: '#A98BFF',
    grid: '#2A1B45',
    text: '#EDE3FF',
    secondaryText: '#CF9EFF',
  };

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
      <div className="flex items-center justify-center min-h-screen bg-[#0D0620]">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#0D0620] pt-28 px-6">
      <Dock items={dockItems} panelHeight={68} baseItemSize={50} magnification={70} />
      <div className="max-w-7xl mx-auto">
        <div className="analytics-header mb-6">
          <div>
            <h1 className="page-title">Analytics</h1>
            <p className="page-sub">Insights into your document collection and usage</p>
          </div>
        </div>

        {overview && (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
            <Card className="page-card">
              <div className="flex items-center gap-4">
                <div className="p-3 bg-[#120A24] rounded-lg">
                  <FileText size={24} className="text-[#CF9EFF]" />
                </div>
                <div>
                  <p className="text-sm text-[#CF9EFF]">Total Documents</p>
                  <p className="text-2xl font-bold text-[#EDE3FF]">{overview.totalDocuments}</p>
                </div>
              </div>
            </Card>
            <Card className="page-card">
              <div className="flex items-center gap-4">
                <div className="p-3 bg-[#120A24] rounded-lg">
                  <BarChart3 size={24} className="text-[#CF9EFF]" />
                </div>
                <div>
                  <p className="text-sm text-[#CF9EFF]">Total Words</p>
                  <p className="text-2xl font-bold text-[#EDE3FF]">{overview.totalWords.toLocaleString()}</p>
                </div>
              </div>
            </Card>
            <Card className="page-card">
              <div className="flex items-center gap-4">
                <div className="p-3 bg-[#120A24] rounded-lg">
                  <TrendingUp size={24} className="text-[#CF9EFF]" />
                </div>
                <div>
                  <p className="text-sm text-[#CF9EFF]">With Embeddings</p>
                  <p className="text-2xl font-bold text-[#EDE3FF]">{overview.documentsWithEmbeddings}</p>
                </div>
              </div>
            </Card>
            <Card className="page-card">
              <div className="flex items-center gap-4">
                <div className="p-3 bg-[#120A24] rounded-lg">
                  <Activity size={24} className="text-[#CF9EFF]" />
                </div>
                <div>
                  <p className="text-sm text-[#CF9EFF]">With Summaries</p>
                  <p className="text-2xl font-bold text-[#EDE3FF]">{overview.documentsWithSummaries}</p>
                </div>
              </div>
            </Card>
          </div>
        )}

        {/* Charts Top */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
          <Card className="page-card">
            <h2 className="page-section-title mb-4">Uploads Over Time</h2>
            {uploads?.uploadsByDate?.length ? (
              <div className="h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart
                    data={uploads.uploadsByDate.map(d => ({
                      date: new Date(d.date).toLocaleDateString(),
                      count: d.count,
                    }))}
                    margin={{ top: 10, right: 20, left: 0, bottom: 0 }}
                  >
                    <CartesianGrid stroke={chartColors.grid} strokeDasharray="3 3" />
                    <XAxis dataKey="date" tick={{ fill: chartColors.secondaryText }} />
                    <YAxis tick={{ fill: chartColors.secondaryText }} />
                    <Tooltip contentStyle={{ background: '#120A24', border: '1px solid #2A1B45' }}
                             labelStyle={{ color: chartColors.text }}
                             itemStyle={{ color: chartColors.secondaryText }} />
                    <Line type="monotone" dataKey="count" stroke={chartColors.accent} strokeWidth={2} dot={{ r: 2 }} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            ) : (
              <p className="text-[#CF9EFF] text-center py-8">No time-series upload data</p>
            )}
          </Card>

          <Card className="page-card">
            <h2 className="page-section-title mb-4">File Types</h2>
            {(uploads?.fileTypeStats?.length || overview?.fileTypeDistribution) ? (
              <div className="h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart
                    data={(uploads?.fileTypeStats?.length ? uploads.fileTypeStats.map(s => ({
                      name: s.fileType,
                      count: s.count,
                    })) : Object.entries(overview?.fileTypeDistribution || {}).map(([name, count]) => ({ name, count })))}
                    margin={{ top: 10, right: 20, left: 0, bottom: 0 }}
                  >
                    <CartesianGrid stroke={chartColors.grid} strokeDasharray="3 3" />
                    <XAxis dataKey="name" tick={{ fill: chartColors.secondaryText }} />
                    <YAxis tick={{ fill: chartColors.secondaryText }} />
                    <Tooltip contentStyle={{ background: '#120A24', border: '1px solid #2A1B45' }}
                             labelStyle={{ color: chartColors.text }}
                             itemStyle={{ color: chartColors.secondaryText }} />
                    <Bar dataKey="count" fill={chartColors.accent} radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            ) : (
              <p className="text-[#CF9EFF] text-center py-8">No file type stats</p>
            )}
          </Card>
        </div>

        {overview?.fileTypeDistribution && (
          <div className="grid grid-cols-1 gap-6 mb-8">
            <Card className="page-card">
              <h2 className="page-section-title mb-4">File Type Distribution</h2>
              <div className="h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={Object.entries(overview.fileTypeDistribution).map(([name, value]) => ({ name, value }))}
                      dataKey="value"
                      nameKey="name"
                      innerRadius={50}
                      outerRadius={90}
                      paddingAngle={3}
                    >
                      {Object.entries(overview.fileTypeDistribution).map(([name], index) => (
                        <Cell key={`cell-${name}`}
                              fill={index % 2 === 0 ? chartColors.accent : chartColors.accent2} />
                      ))}
                    </Pie>
                    <Legend formatter={(value) => (
                      <span style={{ color: chartColors.secondaryText }}>{value}</span>
                    )} />
                    <Tooltip contentStyle={{ background: '#120A24', border: '1px solid #2A1B45' }}
                             labelStyle={{ color: chartColors.text }}
                             itemStyle={{ color: chartColors.secondaryText }} />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </Card>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card className="page-card">
            <h2 className="page-section-title mb-4">Top Keywords</h2>
            {keywords.length ? (
              <div className="space-y-2 max-h-96 overflow-y-auto">
                {keywords.map((k) => (
                  <div key={k.keyword} className="flex items-center justify-between p-2 rounded border border-[#2A1B45] hover:bg-[#120A24]">
                    <span className="font-medium text-[#EDE3FF]">{k.keyword}</span>
                    <div className="flex items-center gap-2">
                      <Badge variant="default" size="sm" className="bg-[#120A24] text-[#EDE3FF] border border-[#2A1B45]">{k.documentCount} docs</Badge>
                      <Badge variant="info" size="sm" className="bg-[#120A24] text-[#EDE3FF] border border-[#2A1B45]">{k.frequencyPercentage.toFixed(1)}%</Badge>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-[#CF9EFF] text-center py-8">No keyword data yet</p>
            )}
          </Card>

          <div className="space-y-6">
            <Card className="page-card">
              <h2 className="page-section-title mb-4">Upload Statistics</h2>
              {uploads ? (
                <div className="space-y-3">
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-[#CF9EFF]">Average File Size</span>
                    <span className="font-medium text-[#EDE3FF]">{uploads.averageSizeFormatted}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-[#CF9EFF]">Last 7 Days</span>
                    <Badge variant="success" className="bg-[#120A24] text-[#EDE3FF] border border-[#2A1B45]">{uploads.uploadsLast7Days} uploads</Badge>
                  </div>
                </div>
              ) : (
                <p className="text-[#CF9EFF] text-center py-8">No upload statistics available</p>
              )}
            </Card>

            <Card className="page-card">
              <h2 className="page-section-title mb-4">Recent Activity</h2>
              {activity?.recentUploads?.length ? (
                <div className="space-y-2 max-h-64 overflow-y-auto">
                  {activity.recentUploads.map((a, idx) => (
                    <div key={`${a.activityType}-${a.documentId}-${a.timestamp}-${idx}`} className="p-3 rounded border border-[#2A1B45]">
                      <div className="flex items-center justify-between mb-1">
                        <Badge variant="info" size="sm" className="bg-[#120A24] text-[#EDE3FF] border border-[#2A1B45]">{a.activityType}</Badge>
                        <span className="text-xs text-[#CF9EFF]">{new Date(a.timestamp).toLocaleString()}</span>
                      </div>
                      <p className="text-sm font-medium text-[#EDE3FF]">{a.documentName}</p>
                      <p className="text-xs text-[#CF9EFF] mt-1">{a.description}</p>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-[#CF9EFF] text-center py-8">No recent activity</p>
              )}
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AnalyticsPage;
