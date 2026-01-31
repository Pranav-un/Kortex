import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import type { QuestionResponse, Document } from '../types';
import { searchService } from '../services/searchService';
import { documentService } from '../services/documentService';
import { Card, Button, Input, LoadingSpinner, Badge } from '../components/ui';
import { MessageSquare, Send, FileText, AlertCircle, Info, Search as SearchIcon, BarChart3, LayoutDashboard } from 'lucide-react';
import Dock from '../components/magicui/Dock';
import { ROUTES } from '../config/constants';
import './Chat.css';

const ChatPage: React.FC = () => {
  const navigate = useNavigate();
  const dockItems = useMemo(() => ([
    { icon: <LayoutDashboard size={20} />, label: 'Dashboard', onClick: () => navigate(ROUTES.DASHBOARD) },
    { icon: <FileText size={20} />, label: 'Documents', onClick: () => navigate(ROUTES.DOCUMENTS) },
    { icon: <SearchIcon size={20} />, label: 'Search', onClick: () => navigate(ROUTES.SEARCH) },
    { icon: <MessageSquare size={20} />, label: 'AI Chat', onClick: () => navigate(ROUTES.CHAT) },
    { icon: <BarChart3 size={20} />, label: 'Analytics', onClick: () => navigate(ROUTES.ANALYTICS) },
  ]), [navigate]);
  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState<QuestionResponse | null>(null);
  const [documents, setDocuments] = useState<Document[]>([]);
  const [documentId, setDocumentId] = useState<number | undefined>(undefined);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    documentService.getDocuments().then(setDocuments).catch(() => {});
  }, []);

  const ask = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!question.trim()) return;
    
    setLoading(true);
    setError('');
    setAnswer(null);
    try {
      const resp = await searchService.askQuestion(question, documentId);
      setAnswer(resp);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to get answer');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#0D0620] pt-28 px-6">
      <Dock items={dockItems} panelHeight={68} baseItemSize={50} magnification={70} />
      <div className="max-w-4xl mx-auto">
        <div className="chat-header mb-6">
          <div>
            <h1 className="page-title">AI Chat</h1>
            <p className="page-sub">Ask questions and get answers with citations</p>
          </div>
        </div>

        {/* Question Form */}
        <Card className="page-card mb-6">
          <form onSubmit={ask} className="space-y-4">
            <div className="flex items-center gap-3">
              <MessageSquare size={20} className="text-[#CF9EFF] flex-shrink-0" />
              <Input
                value={question}
                onChange={(e) => setQuestion(e.target.value)}
                placeholder="Ask a question about your documents..."
                fullWidth
                variant="dark"
                required
              />
            </div>

            <div className="flex items-center gap-4">
              <div className="flex-1">
                <label className="block text-sm font-medium text-[#CF9EFF] mb-1">
                  Filter by document (optional)
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

              <div className="flex items-end">
                <Button type="submit" loading={loading}>
                  <Send size={16} className="mr-2" />
                  Ask
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

        {/* Loading State */}
        {loading && (
          <Card className="page-card">
            <div className="py-8">
              <LoadingSpinner size="lg" />
              <p className="text-center text-[#CF9EFF] mt-4">Analyzing your documents...</p>
            </div>
          </Card>
        )}

        {/* Answer */}
        {!loading && answer && (
          <div className="space-y-6">
            {/* Question Card */}
            <Card padding="sm" className="page-card">
              <div className="flex items-start gap-3">
                <div className="p-2 bg-[#120A24] rounded-lg">
                  <MessageSquare size={16} className="text-[#CF9EFF]" />
                </div>
                <div>
                  <p className="text-sm text-[#CF9EFF] mb-1">Your question</p>
                  <p className="text-[#EDE3FF]">{answer.question}</p>
                </div>
              </div>
            </Card>

            {/* Answer Card */}
            <Card className="page-card">
              <div className="mb-4">
                <div className="flex items-center gap-2 mb-2">
                  <MessageSquare size={20} className="text-[#CF9EFF]" />
                  <h2 className="page-section-title">Answer</h2>
                </div>
                <p className="text-[#CF9EFF] text-base leading-relaxed whitespace-pre-wrap">
                  {answer.answer}
                </p>
              </div>

              {/* Metadata */}
              <div className="flex flex-wrap items-center gap-3 py-3 border-t border-[#2A1B45] text-sm text-[#CF9EFF]">
                <Badge variant="info" size="sm" className="bg-[#120A24] text-[#EDE3FF] border border-[#2A1B45]">
                  <Info size={12} className="mr-1" />
                  {answer.llmProvider}: {answer.llmModel}
                </Badge>
                <span>·</span>
                <span>{answer.contextChunksUsed} context chunks</span>
                <span>·</span>
                <span>~{answer.estimatedTokens} tokens</span>
              </div>

              {/* Citations */}
              {answer.citations && answer.citations.length > 0 && (
                <div className="mt-6 pt-6 border-t border-[#2A1B45]">
                  <h3 className="text-sm font-semibold text-[#EDE3FF] mb-3">
                    Citations ({answer.citations.length})
                  </h3>
                  <div className="space-y-3">
                    {answer.citations.map((c, idx) => (
                      <div
                        key={`${c.documentId}-${c.chunkId}-${idx}`}
                        className="p-3 rounded-lg border border-[#2A1B45] bg-[#120A24]"
                      >
                        <div className="flex items-start justify-between gap-3 mb-2">
                          <div className="flex items-center gap-2 flex-1 min-w-0">
                            <Badge variant="default" size="sm" className="bg-[#120A24] text-[#EDE3FF] border border-[#2A1B45]">[{c.citationNumber}]</Badge>
                            <FileText size={14} className="text-[#CF9EFF] flex-shrink-0" />
                            <span className="text-sm font-medium text-[#EDE3FF] truncate">{c.documentName}</span>
                          </div>
                          <Badge 
                            variant={c.relevanceScore && c.relevanceScore > 0.8 ? 'success' : 'info'} 
                            size="sm"
                            className="bg-[#120A24] text-[#EDE3FF] border border-[#2A1B45]"
                          >
                            {(c.relevanceScore ? c.relevanceScore * 100 : 0).toFixed(0)}%
                          </Badge>
                        </div>
                        <p className="text-sm text-[#CF9EFF] leading-relaxed">
                          {c.excerpt}
                        </p>
                        <p className="text-xs text-[#CF9EFF] mt-1">
                          Chunk #{c.chunkOrder}
                        </p>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </Card>
          </div>
        )}

        {/* Empty State */}
        {!loading && !answer && !error && (
          <Card className="page-card">
            <div className="text-center py-12">
              <MessageSquare size={48} className="text-[#A98BFF] mx-auto mb-3" />
              <p className="text-[#CF9EFF] mb-2">Ask your first question</p>
              <p className="text-sm text-[#CF9EFF]">Get AI-powered answers with citations from your documents</p>
            </div>
          </Card>
        )}
      </div>
    </div>
  );
};

export default ChatPage;
