import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  FileText, ArrowLeft, Calendar, FileType, Hash, Tag, 
  BarChart2, RefreshCw, Trash2, AlertCircle
} from 'lucide-react';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { Modal } from '../components/ui/Modal';
import { documentService } from '../services/documentService';
import { ROUTES } from '../config/constants';
import type { Document } from '../types';

export const DocumentDetailsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [document, setDocument] = useState<Document | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isRegenerating, setIsRegenerating] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showExtractedText, setShowExtractedText] = useState(false);

  useEffect(() => {
    if (id) {
      loadDocument();
    }
  }, [id]);

  const loadDocument = async () => {
    if (!id) return;
    
    try {
      setLoading(true);
      setError(null);
      const data = await documentService.getDocumentById(parseInt(id, 10));
      setDocument(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load document');
    } finally {
      setLoading(false);
    }
  };

  const handleRegenerateSummary = async () => {
    if (!document) return;
    
    try {
      setIsRegenerating(true);
      await documentService.regenerateSummary(document.id);
      // Reload document to get updated summary
      await loadDocument();
    } catch (err: any) {
      setError(err.message || 'Failed to regenerate summary');
    } finally {
      setIsRegenerating(false);
    }
  };

  const handleDelete = async () => {
    if (!document) return;
    
    try {
      await documentService.deleteDocument(document.id);
      navigate(ROUTES.DOCUMENTS);
    } catch (err: any) {
      setError(err.message || 'Failed to delete document');
      setShowDeleteModal(false);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  };

  const getStatusBadge = () => {
    if (!document) return null;
    
    if (document.embeddingsGenerated) {
      return <Badge variant="success">Indexed</Badge>;
    } else if (document.textExtractionSuccess) {
      return <Badge variant="warning">Processing</Badge>;
    }
    return <Badge variant="default">Uploaded</Badge>;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (error || !document) {
    return (
      <div className="flex flex-col items-center justify-center h-64 text-center">
        <AlertCircle size={48} className="text-red-500 mb-4" />
        <h2 className="text-xl font-semibold text-slate-900 mb-2">Error Loading Document</h2>
        <p className="text-slate-600 mb-4">{error || 'Document not found'}</p>
        <Button onClick={() => navigate(ROUTES.DOCUMENTS)}>
          <ArrowLeft size={16} />
          Back to Documents
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <Button 
            variant="outline" 
            size="sm"
            onClick={() => navigate(ROUTES.DOCUMENTS)}
          >
            <ArrowLeft size={16} />
            Back
          </Button>
          <div>
            <h1 className="text-2xl font-bold text-slate-900">{document.filename}</h1>
            <p className="text-sm text-slate-600 mt-1">Document ID: {document.id}</p>
          </div>
        </div>
        <div className="flex items-center space-x-2">
          {getStatusBadge()}
        </div>
      </div>

      {/* Metadata Card */}
      <Card>
        <h2 className="text-lg font-semibold text-slate-900 mb-4">Document Information</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="flex items-start space-x-3">
            <FileType size={20} className="text-slate-400 mt-0.5" />
            <div>
              <p className="text-sm font-medium text-slate-700">File Type</p>
              <p className="text-sm text-slate-900">{document.fileType.toUpperCase()}</p>
            </div>
          </div>
          
          <div className="flex items-start space-x-3">
            <BarChart2 size={20} className="text-slate-400 mt-0.5" />
            <div>
              <p className="text-sm font-medium text-slate-700">File Size</p>
              <p className="text-sm text-slate-900">{formatFileSize(document.size)}</p>
            </div>
          </div>
          
          <div className="flex items-start space-x-3">
            <Calendar size={20} className="text-slate-400 mt-0.5" />
            <div>
              <p className="text-sm font-medium text-slate-700">Uploaded</p>
              <p className="text-sm text-slate-900">{formatDate(document.uploadTime)}</p>
            </div>
          </div>
          
          {document.pageCount && (
            <div className="flex items-start space-x-3">
              <FileText size={20} className="text-slate-400 mt-0.5" />
              <div>
                <p className="text-sm font-medium text-slate-700">Pages</p>
                <p className="text-sm text-slate-900">{document.pageCount}</p>
              </div>
            </div>
          )}
          
          <div className="flex items-start space-x-3">
            <Hash size={20} className="text-slate-400 mt-0.5" />
            <div>
              <p className="text-sm font-medium text-slate-700">Embeddings</p>
              <p className="text-sm text-slate-900">{document.embeddingsGenerated ? 'Yes' : 'No'}</p>
            </div>
          </div>
          
          <div className="flex items-start space-x-3">
            <BarChart2 size={20} className="text-slate-400 mt-0.5" />
            <div>
              <p className="text-sm font-medium text-slate-700">Version</p>
              <p className="text-sm text-slate-900">{document.version}</p>
            </div>
          </div>
        </div>
      </Card>

      {/* Tags */}
      {document.tags && document.tags.trim().length > 0 && (
        <Card>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-slate-900">Tags</h2>
            {document.tagsGeneratedAt && (
              <p className="text-xs text-slate-500">
                Generated {formatDate(document.tagsGeneratedAt)}
              </p>
            )}
          </div>
          <div className="flex flex-wrap gap-2">
            {document.tags.split(',').map((tag: string, index: number) => (
              <Badge key={index} variant="info">
                <Tag size={14} />
                {tag.trim()}
              </Badge>
            ))}
          </div>
        </Card>
      )}

      {/* Summary */}
      {document.summary && (
        <Card>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-slate-900">AI-Generated Summary</h2>
            <div className="flex items-center space-x-2">
              {document.summaryGeneratedAt && (
                <p className="text-xs text-slate-500">
                  {formatDate(document.summaryGeneratedAt)}
                </p>
              )}
              <Button
                variant="outline"
                size="sm"
                onClick={handleRegenerateSummary}
                loading={isRegenerating}
              >
                <RefreshCw size={14} />
                Regenerate
              </Button>
            </div>
          </div>
          <p className="text-slate-700 leading-relaxed whitespace-pre-wrap">
            {document.summary}
          </p>
        </Card>
      )}

      {/* Extracted Text Preview */}
      {document.extractedText && (
        <Card>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-slate-900">Extracted Text</h2>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setShowExtractedText(!showExtractedText)}
            >
              {showExtractedText ? 'Hide' : 'Show'} Text
            </Button>
          </div>
          {showExtractedText && (
            <div className="mt-4 p-4 bg-slate-50 rounded-lg border border-slate-200 max-h-96 overflow-y-auto">
              <pre className="text-sm text-slate-700 whitespace-pre-wrap font-mono">
                {document.extractedText.substring(0, 5000)}
                {document.extractedText.length > 5000 && '\n\n... (truncated)'}
              </pre>
            </div>
          )}
        </Card>
      )}

      {/* Actions */}
      <Card>
        <h2 className="text-lg font-semibold text-slate-900 mb-4">Actions</h2>
        <div className="flex flex-wrap gap-3">
          {!document.summary && document.textExtractionSuccess && (
            <Button
              variant="primary"
              onClick={handleRegenerateSummary}
              loading={isRegenerating}
            >
              <RefreshCw size={16} />
              Generate Summary
            </Button>
          )}
          
          <Button
            variant="danger"
            onClick={() => setShowDeleteModal(true)}
          >
            <Trash2 size={16} />
            Delete Document
          </Button>
        </div>
      </Card>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={showDeleteModal}
        onClose={() => setShowDeleteModal(false)}
        title="Delete Document"
      >
        <div className="space-y-4">
          <p className="text-slate-700">
            Are you sure you want to delete <strong>{document.filename}</strong>? This action cannot be undone.
          </p>
          <div className="flex justify-end space-x-3">
            <Button variant="outline" onClick={() => setShowDeleteModal(false)}>
              Cancel
            </Button>
            <Button variant="danger" onClick={handleDelete}>
              <Trash2 size={16} />
              Delete
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
