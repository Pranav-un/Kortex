import React, { useEffect, useState } from 'react';
import type { SearchResponse, SearchResult, Document } from '../types';
import { searchService } from '../services/searchService';
import { documentService } from '../services/documentService';
import { Card, Button, Input, LoadingSpinner, Badge } from '../components/ui';
import { Search, FileText, AlertCircle } from 'lucide-react';

const SearchPage: React.FC = () => {
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
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Search failed');
      setResults([]);
    } finally {
      setLoading(false);
    }
  };

  const highlightMatch = (text: string) => {
    // Simple highlighting - you can improve this
    return text;
  };

  return (
    <div className="min-h-screen bg-slate-50 p-6">
      <div className="max-w-7xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-slate-900 mb-2">Semantic Search</h1>
          <p className="text-slate-600">Find information using natural language queries</p>
        </div>

        {/* Search Form */}
        <Card className="mb-6">
          <form onSubmit={handleSearch} className="space-y-4">
            <div className="flex items-center gap-3">
              <Search size={20} className="text-slate-600" />
              <Input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Ask anything about your documents..."
                fullWidth
                required
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Filter by document
                </label>
                <select
                  value={documentId ?? ''}
                  onChange={(e) => setDocumentId(e.target.value ? Number(e.target.value) : undefined)}
                  className="block w-full px-3 py-2 border border-slate-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-slate-500 focus:border-slate-500 text-sm"
                >
                  <option value="">All documents</option>
                  {documents.map((d) => (
                    <option key={d.id} value={d.id}>{d.filename}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Max results
                </label>
                <input
                  type="number"
                  min={1}
                  max={100}
                  value={limit}
                  onChange={(e) => setLimit(Number(e.target.value))}
                  className="block w-full px-3 py-2 border border-slate-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-slate-500 focus:border-slate-500 text-sm"
                />
              </div>

              <div className="flex items-end">
                <Button type="submit" fullWidth loading={loading}>
                  Search
                </Button>
              </div>
            </div>

            {error && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm flex items-start gap-2">
                <AlertCircle size={16} className="flex-shrink-0 mt-0.5" />
                {error}
              </div>
            )}
          </form>
        </Card>

        {/* Results */}
        <Card>
          <div className="mb-4">
            <h2 className="text-lg font-semibold text-slate-900 mb-1">Results</h2>
            {hasSearched && !loading && (
              <p className="text-sm text-slate-600">
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
              <Search size={48} className="text-slate-300 mx-auto mb-3" />
              <p className="text-slate-600 mb-2">Start searching</p>
              <p className="text-sm text-slate-500">Enter a natural language query to find relevant content</p>
            </div>
          ) : results.length === 0 ? (
            <div className="text-center py-12">
              <AlertCircle size={48} className="text-slate-300 mx-auto mb-3" />
              <p className="text-slate-600 mb-2">No results found</p>
              <p className="text-sm text-slate-500">Try adjusting your query or search all documents</p>
            </div>
          ) : (
            <div className="space-y-4">
              {results.map((r, idx) => (
                <div
                  key={`${r.documentId}-${r.chunkId}-${idx}`}
                  className="p-4 rounded-lg border border-slate-200 hover:bg-slate-50 transition-colors"
                >
                  <div className="flex items-start justify-between gap-3 mb-2">
                    <div className="flex items-center gap-2 flex-1 min-w-0">
                      <FileText size={16} className="text-slate-600 flex-shrink-0" />
                      <span className="font-medium text-slate-900 truncate">{r.documentName}</span>
                    </div>
                    <div className="flex items-center gap-2 flex-shrink-0">
                      <Badge variant="default" size="sm">Chunk #{r.chunkOrder}</Badge>
                      <Badge 
                        variant={r.similarityScore > 0.8 ? 'success' : r.similarityScore > 0.6 ? 'info' : 'default'} 
                        size="sm"
                      >
                        {(r.similarityScore * 100).toFixed(0)}% match
                      </Badge>
                    </div>
                  </div>

                  <p className="text-slate-700 text-sm leading-relaxed whitespace-pre-wrap">
                    {highlightMatch(r.chunkText)}
                  </p>

                  <div className="mt-2 text-xs text-slate-500">
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
