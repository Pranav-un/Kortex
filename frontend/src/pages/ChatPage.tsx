import React, { useEffect, useState } from 'react';
import type { QuestionResponse, Document } from '../types';
import { searchService } from '../services/searchService';
import { documentService } from '../services/documentService';
import { Card, Button, Input, LoadingSpinner, Badge } from '../components/ui';
import { MessageSquare, Send, FileText, AlertCircle, Info } from 'lucide-react';

const ChatPage: React.FC = () => {
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
    <div className="min-h-screen bg-slate-50 p-6">
      <div className="max-w-4xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-slate-900 mb-2">AI Chat</h1>
          <p className="text-slate-600">Ask questions and get answers with citations from your documents</p>
        </div>

        {/* Question Form */}
        <Card className="mb-6">
          <form onSubmit={ask} className="space-y-4">
            <div className="flex items-center gap-3">
              <MessageSquare size={20} className="text-slate-600 flex-shrink-0" />
              <Input
                value={question}
                onChange={(e) => setQuestion(e.target.value)}
                placeholder="Ask a question about your documents..."
                fullWidth
                required
              />
            </div>

            <div className="flex items-center gap-4">
              <div className="flex-1">
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Filter by document (optional)
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

              <div className="flex items-end">
                <Button type="submit" loading={loading}>
                  <Send size={16} className="mr-2" />
                  Ask
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

        {/* Loading State */}
        {loading && (
          <Card>
            <div className="py-8">
              <LoadingSpinner size="lg" />
              <p className="text-center text-slate-600 mt-4">Analyzing your documents...</p>
            </div>
          </Card>
        )}

        {/* Answer */}
        {!loading && answer && (
          <div className="space-y-6">
            {/* Question Card */}
            <Card padding="sm" className="bg-slate-100 border-slate-300">
              <div className="flex items-start gap-3">
                <div className="p-2 bg-white rounded-lg">
                  <MessageSquare size={16} className="text-slate-700" />
                </div>
                <div>
                  <p className="text-sm text-slate-600 mb-1">Your question</p>
                  <p className="text-slate-900">{answer.question}</p>
                </div>
              </div>
            </Card>

            {/* Answer Card */}
            <Card>
              <div className="mb-4">
                <div className="flex items-center gap-2 mb-2">
                  <MessageSquare size={20} className="text-slate-700" />
                  <h2 className="text-lg font-semibold text-slate-900">Answer</h2>
                </div>
                <p className="text-slate-700 text-base leading-relaxed whitespace-pre-wrap">
                  {answer.answer}
                </p>
              </div>

              {/* Metadata */}
              <div className="flex flex-wrap items-center gap-3 py-3 border-t border-slate-200 text-sm text-slate-600">
                <Badge variant="info" size="sm">
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
                <div className="mt-6 pt-6 border-t border-slate-200">
                  <h3 className="text-sm font-semibold text-slate-900 mb-3">
                    Citations ({answer.citations.length})
                  </h3>
                  <div className="space-y-3">
                    {answer.citations.map((c, idx) => (
                      <div
                        key={`${c.documentId}-${c.chunkId}-${idx}`}
                        className="p-3 rounded-lg border border-slate-200 bg-slate-50"
                      >
                        <div className="flex items-start justify-between gap-3 mb-2">
                          <div className="flex items-center gap-2 flex-1 min-w-0">
                            <Badge variant="default" size="sm">[{c.citationNumber}]</Badge>
                            <FileText size={14} className="text-slate-600 flex-shrink-0" />
                            <span className="text-sm font-medium text-slate-900 truncate">{c.documentName}</span>
                          </div>
                          <Badge 
                            variant={c.relevanceScore && c.relevanceScore > 0.8 ? 'success' : 'info'} 
                            size="sm"
                          >
                            {(c.relevanceScore ? c.relevanceScore * 100 : 0).toFixed(0)}%
                          </Badge>
                        </div>
                        <p className="text-sm text-slate-700 leading-relaxed">
                          {c.excerpt}
                        </p>
                        <p className="text-xs text-slate-500 mt-1">
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
          <Card>
            <div className="text-center py-12">
              <MessageSquare size={48} className="text-slate-300 mx-auto mb-3" />
              <p className="text-slate-600 mb-2">Ask your first question</p>
              <p className="text-sm text-slate-500">Get AI-powered answers with citations from your documents</p>
            </div>
          </Card>
        )}
      </div>
    </div>
  );
};

export default ChatPage;
