import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import type { SearchResponse, SearchResult, Document } from '../types';
import { searchService } from '../services/searchService';
import { documentService } from '../services/documentService';
import { Card, Button, Input, LoadingSpinner, Badge } from '../components/ui';
import { Search as SearchIcon, FileText, AlertCircle, MessageSquare, BarChart3, LayoutDashboard, User as UserIcon, LogOut } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import Dock from '../components/magicui/Dock';
import { ROUTES } from '../config/constants';
import './Search.css';
import { useNotifications } from '../contexts/NotificationContext';

const SearchPage: React.FC = () => {
  const navigate = useNavigate();
  const { addToast } = useNotifications();
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
  const [query, setQuery] = useState('');
  const [limit, setLimit] = useState(10);
  const [documentId, setDocumentId] = useState<number | undefined>(undefined);
  const [results, setResults] = useState<SearchResult[]>([]);
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [hasSearched, setHasSearched] = useState(false);

  useEffect(() => {
    documentService.getDocuments().then(setDocuments).catch(() => {});
  }, []);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!query.trim()) return;
    
    setLoading(true);
    setError('');
    setHasSearched(true);
    try {
      const resp: SearchResponse = await searchService.search(query, limit, documentId);
      setResults(resp.results || []);
      addToast({ type: 'info', message: `Search complete: ${resp.results?.length || 0} result(s)` });
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Search failed');
      setResults([]);
      addToast({ type: 'error', message: 'Search failed. Check your connection or query.' });
    } finally {
      setLoading(false);
    }
  };

  const highlightMatch = (text: string) => {
    // Simple highlighting - you can improve this
    return text;
  };

  return (
    <div className="min-h-screen bg-[#0D0620] pt-28 px-6">
      <Dock items={dockItems} panelHeight={68} baseItemSize={50} magnification={70} />
      <div className="max-w-7xl mx-auto">
        <div className="search-header mb-6">
          <div>
            <h1 className="page-title">Semantic Search</h1>
            <p className="page-sub">Find information using natural language queries</p>
          </div>
        </div>

        {/* Search Form */}
        <Card className="page-card mb-6">
          <form onSubmit={handleSearch} className="space-y-4">
            <div className="flex items-center gap-3">
              <SearchIcon size={20} className="text-[#CF9EFF]" />
              <Input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Ask anything about your documents..."
                fullWidth
                variant="dark"
                required
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-[#CF9EFF] mb-1">
                  Filter by document
                </label>
                <select
                  value={documentId ?? ''}
                  onChange={(e) => setDocumentId(e.target.value ? Number(e.target.value) : undefined)}
                  className="page-select"
                >
                  <option value="">All documents</option>
                  {documents.map((d) => (
                    <option key={d.id} value={d.id}>{d.filename}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-[#CF9EFF] mb-1">
                  Max results
                </label>
                <input
                  type="number"
                  min={1}
                  max={100}
                  value={limit}
                  onChange={(e) => setLimit(Number(e.target.value))}
                  className="page-select"
                />
              </div>

              <div className="flex items-end">
                <Button type="submit" fullWidth loading={loading}>
                  Search
                </Button>
              </div>
            </div>

            {error && (
              <div className="page-error">
                <AlertCircle size={16} className="flex-shrink-0 mt-0.5" />
                {error}
              </div>
            )}
          </form>
        </Card>

        {/* Results */}
        <Card className="page-card">
          <div className="mb-4">
            <h2 className="page-section-title mb-1">Results</h2>
            {hasSearched && !loading && (
              <p className="text-sm text-[#CF9EFF]">
                Found {results.length} {results.length === 1 ? 'result' : 'results'}
                {query && ` for "${query}"`}
              </p>
            )}
          </div>

          {loading ? (
            <div className="py-12">
              <LoadingSpinner size="lg" />
            </div>
          ) : !hasSearched ? (
            <div className="text-center py-12">
              <SearchIcon size={48} className="text-[#A98BFF] mx-auto mb-3" />
              <p className="text-[#CF9EFF] mb-2">Start searching</p>
              <p className="text-sm text-[#CF9EFF]">Enter a natural language query to find relevant content</p>
            </div>
          ) : results.length === 0 ? (
            <div className="text-center py-12">
              <AlertCircle size={48} className="text-[#A98BFF] mx-auto mb-3" />
              <p className="text-[#CF9EFF] mb-2">No results found</p>
              <p className="text-sm text-[#CF9EFF]">Try adjusting your query or search all documents</p>
            </div>
          ) : (
            <div className="space-y-4">
              {results.map((r, idx) => (
                <div
                  key={`${r.documentId}-${r.chunkId}-${idx}`}
                  className="p-4 rounded-lg border border-[#2A1B45] hover:bg-[#120A24] transition-colors"
                >
                  <div className="flex items-start justify-between gap-3 mb-2">
                    <div className="flex items-center gap-2 flex-1 min-w-0">
                      <FileText size={16} className="text-[#CF9EFF] flex-shrink-0" />
                      <span className="font-medium text-[#EDE3FF] truncate">{r.documentName}</span>
                    </div>
                    <div className="flex items-center gap-2 flex-shrink-0">
                      <Badge variant="default" size="sm" className="bg-[#120A24] text-[#EDE3FF] border border-[#2A1B45]">Chunk #{r.chunkOrder}</Badge>
                      <Badge 
                        variant={r.similarityScore > 0.8 ? 'success' : r.similarityScore > 0.6 ? 'info' : 'default'} 
                        size="sm"
                        className="bg-[#120A24] text-[#EDE3FF] border border-[#2A1B45]"
                      >
                        {(r.similarityScore * 100).toFixed(0)}% match
                      </Badge>
                    </div>
                  </div>

                  <p className="text-[#CF9EFF] text-sm leading-relaxed whitespace-pre-wrap">
                    {highlightMatch(r.chunkText)}
                  </p>

                  <div className="mt-2 text-xs text-[#CF9EFF]">
                    {r.wordCount} words · Position {r.startPosition}-{r.endPosition}
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
};

export default SearchPage;
