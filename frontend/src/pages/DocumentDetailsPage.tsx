import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  FileText,
  ArrowLeft,
  Calendar,
  FileType,
  Hash,
  Tag,
  BarChart2,
  RefreshCw,
  Trash2,
  AlertCircle
} from 'lucide-react';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { Modal } from '../components/ui/Modal';
import { documentService } from '../services/documentService';
import { ROUTES } from '../config/constants';
import type { Document } from '../types';
import './DocumentDetails.css';

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
      <div className="min-h-screen bg-[#0D0620] flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (error || !document) {
    return (
      <div className="min-h-screen bg-[#0D0620] flex flex-col items-center justify-center text-center px-6">
        <AlertCircle size={48} className="text-red-500 mb-4" />
        <h2 className="text-xl font-semibold text-[#EDE3FF] mb-2">Error Loading Document</h2>
        <p className="text-[#CF9EFF] mb-4">{error || 'Document not found'}</p>
        <Button onClick={() => navigate(ROUTES.DOCUMENTS)} className="border-[#2A1B45] text-[#EDE3FF] hover:bg-[#1A0F2E]">
          <ArrowLeft size={16} />
          Back to Documents
        </Button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#0D0620] pt-10 px-6">
      <div className="max-w-6xl mx-auto space-y-6">
      {/* Header */}
      <div className="dd-header">
        <div className="flex items-center gap-4">
          <Button
            variant="outline"
            size="sm"
            onClick={() => navigate(ROUTES.DOCUMENTS)}
            className="border-[#2A1B45] text-[#EDE3FF] hover:bg-[#1A0F2E]"
          >
            <ArrowLeft size={16} />
            Back
          </Button>
          <div>
            <h1 className="dd-title">{document.filename}</h1>
            <p className="dd-sub">Document ID: {document.id}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Badge className="bg-[#120A24] text-[#EDE3FF] border border-[#2A1B45]">{getStatusBadge() || 'Uploaded'}</Badge>
        </div>
      </div>

      {/* Metadata Card */}
      <Card className="dd-card">
        <h2 className="dd-section-title mb-4">Document Information</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="dd-meta-item">
            <FileType size={20} className="text-[#A98BFF] mt-0.5" />
            <div>
              <p className="dd-meta-label">File Type</p>
              <p className="dd-meta-value">{document.fileType.toUpperCase()}</p>
            </div>
          </div>

          <div className="dd-meta-item">
            <BarChart2 size={20} className="text-[#A98BFF] mt-0.5" />
            <div>
              <p className="dd-meta-label">File Size</p>
              <p className="dd-meta-value">{formatFileSize(document.size)}</p>
            </div>
          </div>

          <div className="dd-meta-item">
            <Calendar size={20} className="text-[#A98BFF] mt-0.5" />
            <div>
              <p className="dd-meta-label">Uploaded</p>
              <p className="dd-meta-value">{formatDate(document.uploadTime)}</p>
            </div>
          </div>

          {document.pageCount && (
            <div className="dd-meta-item">
              <FileText size={20} className="text-[#A98BFF] mt-0.5" />
              <div>
                <p className="dd-meta-label">Pages</p>
                <p className="dd-meta-value">{document.pageCount}</p>
              </div>
            </div>
          )}

          <div className="dd-meta-item">
            <Hash size={20} className="text-[#A98BFF] mt-0.5" />
            <div>
              <p className="dd-meta-label">Embeddings</p>
              <p className="dd-meta-value">{document.embeddingsGenerated ? 'Yes' : 'No'}</p>
            </div>
          </div>

          <div className="dd-meta-item">
            <BarChart2 size={20} className="text-[#A98BFF] mt-0.5" />
            <div>
              <p className="dd-meta-label">Version</p>
              <p className="dd-meta-value">{document.version}</p>
            </div>
          </div>
        </div>
      </Card>

      {/* Tags */}
      {document.tags && document.tags.trim().length > 0 && (
        <Card className="dd-card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="dd-section-title">Tags</h2>
            {document.tagsGeneratedAt && (
              <p className="dd-muted">Generated {formatDate(document.tagsGeneratedAt)}</p>
            )}
          </div>
          <div className="flex flex-wrap gap-2">
            {document.tags.split(',').map((tag: string, index: number) => (
              <Badge key={index} className="bg-[#120A24] text-[#EDE3FF] border border-[#2A1B45]">
                <Tag size={14} className="mr-1" />
                {tag.trim()}
              </Badge>
            ))}
          </div>
        </Card>
      )}

      {/* Summary */}
      {document.summary && (
        <Card className="dd-card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="dd-section-title">AI-Generated Summary</h2>
            <div className="flex items-center gap-2">
              {document.summaryGeneratedAt && (
                <p className="dd-muted">{formatDate(document.summaryGeneratedAt)}</p>
              )}
              <Button
                variant="outline"
                size="sm"
                onClick={handleRegenerateSummary}
                loading={isRegenerating}
                className="border-[#2A1B45] text-[#EDE3FF] hover:bg-[#1A0F2E]"
              >
                <RefreshCw size={14} />
                Regenerate
              </Button>
            </div>
          </div>
          <p className="dd-body">
            {document.summary}
          </p>
        </Card>
      )}

      {/* Extracted Text Preview */}
      {document.extractedText && (
        <Card className="dd-card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="dd-section-title">Extracted Text</h2>
            <Button
              variant="outline"
              size="sm"
              className="border-[#2A1B45] text-[#EDE3FF] hover:bg-[#1A0F2E]"
              onClick={() => setShowExtractedText(!showExtractedText)}
            >
              {showExtractedText ? 'Hide' : 'Show'} Text
            </Button>
          </div>
          {showExtractedText && (
            <div className="mt-4 p-4 bg-[#120A24] rounded-lg border border-[#2A1B45] max-h-96 overflow-y-auto">
              <pre className="text-sm text-[#CF9EFF] whitespace-pre-wrap font-mono">
                {document.extractedText.substring(0, 5000)}
                {document.extractedText.length > 5000 && '\n\n... (truncated)'}
              </pre>
            </div>
          )}
        </Card>
      )}

      {/* Actions */}
      <Card className="dd-card">
        <h2 className="dd-section-title mb-4">Actions</h2>
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
          <p className="text-[#CF9EFF]">
            Are you sure you want to delete <strong>{document.filename}</strong>? This action cannot be undone.
          </p>
          <div className="flex justify-end space-x-3">
            <Button variant="outline" onClick={() => setShowDeleteModal(false)} className="border-[#2A1B45] text-[#EDE3FF] hover:bg-[#1A0F2E]">
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
    </div>
  );
};
