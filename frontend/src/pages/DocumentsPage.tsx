import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Document } from '../types';
import { documentService } from '../services/documentService';
import { Card, Button, Badge, LoadingSpinner, Modal } from '../components/ui';
import { FileText, Upload, Trash2, RefreshCw, CheckCircle, AlertCircle, X, Search, MessageSquare, BarChart3, LayoutDashboard, User as UserIcon, LogOut } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import Dock from '../components/magicui/Dock';
import { ROUTES } from '../config/constants';
import './Documents.css';
import { useNotifications } from '../contexts/NotificationContext';

const DocumentsPage: React.FC = () => {
  const navigate = useNavigate();
  const { addToast } = useNotifications();
  const { logout } = useAuth();
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string>('');
  const [file, setFile] = useState<File | null>(null);
  const [uploadProgress, setUploadProgress] = useState<number>(0);
  const [uploading, setUploading] = useState<boolean>(false);
  const [selectedDoc, setSelectedDoc] = useState<Document | null>(null);
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const dockItems = useMemo(() => ([
    { icon: <LayoutDashboard size={20} />, label: 'Dashboard', onClick: () => navigate(ROUTES.DASHBOARD) },
    { icon: <FileText size={20} />, label: 'Documents', onClick: () => navigate(ROUTES.DOCUMENTS) },
    { icon: <Search size={20} />, label: 'Search', onClick: () => navigate(ROUTES.SEARCH) },
    { icon: <MessageSquare size={20} />, label: 'AI Chat', onClick: () => navigate(ROUTES.CHAT) },
    { icon: <BarChart3 size={20} />, label: 'Analytics', onClick: () => navigate(ROUTES.ANALYTICS) },
    { icon: <UserIcon size={20} />, label: 'Profile', onClick: () => navigate(ROUTES.PROFILE) },
    { icon: <LogOut size={20} />, label: 'Logout', onClick: () => logout() },
  ]), [navigate, logout]);

  const loadDocuments = async () => {
    setLoading(true);
    setError('');
    try {
      const docs = await documentService.getDocuments();
      setDocuments(docs);
    } catch (e: any) {
      setError(e?.message || 'Failed to load documents');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDocuments();
  }, []);

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (selectedFile) {
      const validTypes = ['.pdf', '.docx', '.doc', '.txt'];
      const fileExt = '.' + selectedFile.name.split('.').pop()?.toLowerCase();
      if (!validTypes.includes(fileExt)) {
        setError('Invalid file type. Please upload PDF, DOCX, DOC, or TXT files.');
        return;
      }
      if (selectedFile.size > 50 * 1024 * 1024) {
        setError('File size must be less than 50MB');
        return;
      }
      setFile(selectedFile);
      setError('');
    }
  };

  const handleUpload = async () => {
    if (!file) return;
    setUploading(true);
    setError('');
    setUploadProgress(0);
    try {
      const resp = await documentService.uploadDocument(file, (p) => setUploadProgress(p));
      setFile(null);
      // Reset file input
      const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
      if (fileInput) fileInput.value = '';
      await loadDocuments();
      addToast({ type: 'success', message: `Upload complete: ${resp.filename}${resp.version && resp.version > 1 ? ` (v${resp.version})` : ''}` });
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Upload failed');
      addToast({ type: 'error', message: 'Upload failed. Please try again.' });
    } finally {
      setUploading(false);
      setUploadProgress(0);
    }
  };

  const confirmDelete = (doc: Document) => {
    setSelectedDoc(doc);
    setDeleteModalOpen(true);
  };

  const handleDelete = async () => {
    if (!selectedDoc) return;
    setDeletingId(selectedDoc.id);
    setError('');
    try {
      await documentService.deleteDocument(selectedDoc.id);
      setDeleteModalOpen(false);
      await loadDocuments();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Delete failed');
    } finally {
      setDeletingId(null);
      setSelectedDoc(null);
    }
  };

  const handleRegenerateSummary = async (id: number) => {
    setError('');
    try {
      await documentService.regenerateSummary(id);
      await loadDocuments();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Regenerate summary failed');
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  };

  const formatFileType = (type: string) => {
    if (!type) return '';
    const lower = type.toLowerCase();
    if (lower.includes('pdf')) return 'PDF';
    if (lower.includes('docx') || lower.includes('msword') || lower.includes('word') || lower.includes('doc')) return 'DOCX';
    if (lower.includes('text') || lower.includes('plain') || lower.includes('txt')) return 'TXT';
    const ext = lower.split('/').pop();
    return (ext || type).toUpperCase();
  };

  return (
    <div className="min-h-screen bg-[#0D0620] pt-28 px-6">
      <Dock items={dockItems} panelHeight={68} baseItemSize={50} magnification={70} />
      <div className="max-w-6xl mx-auto">
        <div className="doc-header mb-6">
          <div>
            <h1 className="doc-title">Documents</h1>
            <p className="doc-sub">Upload and manage your document collection</p>
          </div>
          <div className="doc-stats">
            <div className="stat">
              <span className="stat-label">Total</span>
              <span className="stat-value">{documents.length}</span>
            </div>
            <div className="stat">
              <span className="stat-label">Indexed</span>
              <span className="stat-value">{documents.filter(d => d.embeddingsGenerated).length}</span>
            </div>
            <div className="stat">
              <span className="stat-label">Summarized</span>
              <span className="stat-value">{documents.filter(d => !!d.summary).length}</span>
            </div>
          </div>
        </div>

        {/* Upload Card */}
        <Card className="doc-card mb-6">
          <div className="flex items-center gap-3 mb-4">
            <Upload size={20} className="text-[#CF9EFF]" />
            <h2 className="doc-card-title">Upload Document</h2>
          </div>
          
          <div className="space-y-4">
            <div className="flex items-center gap-4">
              <label className="flex-1" id="file-input">
                <input
                  type="file"
                  onChange={handleFileSelect}
                  accept=".pdf,.docx,.doc,.txt"
                  className="doc-file-input"
                />
              </label>
              <Button
                onClick={handleUpload}
                disabled={!file || uploading}
                loading={uploading}
              >
                Upload
              </Button>
              {file && !uploading && (
                <Button
                  variant="ghost"
                  onClick={() => {
                    setFile(null);
                    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
                    if (fileInput) fileInput.value = '';
                  }}
                >
                  <X size={16} />
                </Button>
              )}
            </div>
            
            {uploading && (
              <div className="space-y-2">
                <div className="doc-progress">
                  <div
                    className="doc-progress-bar"
                    style={{ width: `${uploadProgress}%` }}
                  />
                </div>
                <p className="text-sm text-[#CF9EFF]">Uploading: {uploadProgress}%</p>
              </div>
            )}
            
            {file && !uploading && (
              <p className="text-sm text-[#CF9EFF]">
                Selected: {file.name} ({formatFileSize(file.size)})
              </p>
            )}
          </div>
          
          {error && (
            <div className="doc-error">
              <AlertCircle size={16} className="flex-shrink-0 mt-0.5 text-[#CF9EFF]" />
              {error}
            </div>
          )}
          
          <p className="mt-4 text-xs text-[#CF9EFF]">
            Supported formats: PDF, DOCX, DOC, TXT · Max size: 50MB
          </p>
        </Card>

        {/* Documents List */}
        <Card className="doc-card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="doc-card-title">Your Documents</h2>
            <Badge className="bg-[#120A24] text-[#EDE3FF] border border-[#2A1B45]">{documents.length} {documents.length === 1 ? 'document' : 'documents'}</Badge>
          </div>

          {loading ? (
            <div className="py-12"><LoadingSpinner size="lg" /></div>
          ) : documents.length === 0 ? (
            <div className="text-center py-12">
              <FileText size={48} className="text-[#A98BFF] mx-auto mb-3" />
              <p className="text-[#CF9EFF] mb-2">No documents yet</p>
              <p className="text-sm text-[#CF9EFF]">Upload your first document to get started</p>
            </div>
          ) : (
            <div className="doc-grid">
              {documents.map((doc) => (
                <div key={doc.id} className="doc-tile" onClick={() => navigate(`/documents/${doc.id}`)}>
                  <div className="doc-tile-header">
                    <FileText size={18} className="text-[#CF9EFF]" />
                    <div className="doc-tile-title truncate">{doc.filename}</div>
                    <Badge size="sm" className="doc-type-badge">{formatFileType(doc.fileType)}</Badge>
                  </div>
                  <div className="doc-tile-meta">
                    <span>{formatFileSize(doc.size)}</span>
                    <span>·</span>
                    <span>{new Date(doc.uploadTime).toLocaleDateString()}</span>
                    {doc.version && doc.version > 1 && (
                      <>
                        <span>·</span>
                        <Badge size="sm" className="doc-type-badge">v{doc.version}</Badge>
                      </>
                    )}
                  </div>
                  <div className="doc-tile-badges">
                    {doc.embeddingsGenerated ? (
                      <Badge size="sm" className="badge-summarized">
                        <CheckCircle size={12} className="mr-1" /> Indexed
                      </Badge>
                    ) : (
                      <Badge size="sm" className="badge-processing">
                        <AlertCircle size={12} className="mr-1" /> Processing
                      </Badge>
                    )}
                    {doc.summary && (
                      <Badge size="sm" className="badge-summarized">
                        <CheckCircle size={12} className="mr-1" /> Summarized
                      </Badge>
                    )}
                  </div>
                  <div className="doc-tile-actions" onClick={(e) => e.stopPropagation()}>
                    <Button size="sm" variant="outline" className="btn-regen" onClick={() => handleRegenerateSummary(doc.id)}>
                      <RefreshCw size={14} className="mr-1" /> Regenerate Summary
                    </Button>
                    <Button size="sm" variant="outline" className="btn-delete" onClick={() => confirmDelete(doc)}>
                      <Trash2 size={14} className="mr-1" /> Delete
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={deleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        title="Delete Document"
        footer={
          <>
            <Button variant="outline" onClick={() => setDeleteModalOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="danger"
              onClick={handleDelete}
              loading={!!deletingId}
            >
              Delete
            </Button>
          </>
        }
      >
        <p className="text-[#CF9EFF]">
          Are you sure you want to delete <strong>{selectedDoc?.filename}</strong>? This action cannot be undone.
        </p>
      </Modal>
    </div>
  );
};

export default DocumentsPage;
