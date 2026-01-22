import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Document } from '../types';
import { documentService } from '../services/documentService';
import { Card, Button, Badge, LoadingSpinner, Modal } from '../components/ui';
import { FileText, Upload, Trash2, RefreshCw, CheckCircle, AlertCircle, X } from 'lucide-react';

const DocumentsPage: React.FC = () => {
  const navigate = useNavigate();
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string>('');
  const [file, setFile] = useState<File | null>(null);
  const [uploadProgress, setUploadProgress] = useState<number>(0);
  const [uploading, setUploading] = useState<boolean>(false);
  const [selectedDoc, setSelectedDoc] = useState<Document | null>(null);
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

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
      await documentService.uploadDocument(file, (p) => setUploadProgress(p));
      setFile(null);
      // Reset file input
      const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
      if (fileInput) fileInput.value = '';
      await loadDocuments();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Upload failed');
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

  return (
    <div className="min-h-screen bg-slate-50 p-6">
      <div className="max-w-7xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-slate-900 mb-2">Documents</h1>
          <p className="text-slate-600">Upload and manage your document collection</p>
        </div>

        {/* Upload Card */}
        <Card className="mb-6">
          <div className="flex items-center gap-3 mb-4">
            <Upload size={24} className="text-slate-700" />
            <h2 className="text-lg font-semibold text-slate-900">Upload Document</h2>
          </div>
          
          <div className="space-y-4">
            <div className="flex items-center gap-4">
              <label className="flex-1">
                <input
                  type="file"
                  onChange={handleFileSelect}
                  accept=".pdf,.docx,.doc,.txt"
                  className="block w-full text-sm text-slate-700 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-slate-100 file:text-slate-700 hover:file:bg-slate-200 cursor-pointer"
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
                <div className="w-full bg-slate-200 rounded-full h-2 overflow-hidden">
                  <div
                    className="h-full bg-slate-900 transition-all duration-300"
                    style={{ width: `${uploadProgress}%` }}
                  />
                </div>
                <p className="text-sm text-slate-600">Uploading: {uploadProgress}%</p>
              </div>
            )}
            
            {file && !uploading && (
              <p className="text-sm text-slate-600">
                Selected: {file.name} ({formatFileSize(file.size)})
              </p>
            )}
          </div>
          
          {error && (
            <div className="mt-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm flex items-start gap-2">
              <AlertCircle size={16} className="flex-shrink-0 mt-0.5" />
              {error}
            </div>
          )}
          
          <p className="mt-4 text-xs text-slate-500">
            Supported formats: PDF, DOCX, DOC, TXT · Max size: 50MB
          </p>
        </Card>

        {/* Documents List */}
        <Card>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-slate-900">Your Documents</h2>
            <Badge>{documents.length} {documents.length === 1 ? 'document' : 'documents'}</Badge>
          </div>

          {loading ? (
            <div className="py-12">
              <LoadingSpinner size="lg" />
            </div>
          ) : documents.length === 0 ? (
            <div className="text-center py-12">
              <FileText size={48} className="text-slate-300 mx-auto mb-3" />
              <p className="text-slate-600 mb-2">No documents yet</p>
              <p className="text-sm text-slate-500">Upload your first document to get started</p>
            </div>
          ) : (
            <div className="space-y-3">
              {documents.map((doc) => (
                <div
                  key={doc.id}
                  className="flex items-start gap-4 p-4 rounded-lg border border-slate-200 hover:bg-slate-50 transition-colors cursor-pointer"
                  onClick={() => navigate(`/documents/${doc.id}`)}
                >
                  <FileText size={20} className="text-slate-600 flex-shrink-0 mt-1" />
                  
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-3 mb-2">
                      <div className="flex-1 min-w-0">
                        <h3 className="font-medium text-slate-900 truncate mb-1 hover:text-slate-700">
                          {doc.filename}
                        </h3>
                        <div className="flex items-center gap-3 text-sm text-slate-600">
                          <span>{formatFileSize(doc.size)}</span>
                          <span>·</span>
                          <span>{new Date(doc.uploadTime).toLocaleDateString()}</span>
                          {doc.version && doc.version > 1 && (
                            <>
                              <span>·</span>
                              <Badge variant="info" size="sm">v{doc.version}</Badge>
                            </>
                          )}
                        </div>
                      </div>
                      <Badge variant="default" size="sm">{doc.fileType}</Badge>
                    </div>
                    
                    <div className="flex items-center gap-2 mb-3">
                      {doc.embeddingsGenerated ? (
                        <Badge variant="success" size="sm">
                          <CheckCircle size={12} className="mr-1" />
                          Indexed
                        </Badge>
                      ) : (
                        <Badge variant="warning" size="sm">
                          <AlertCircle size={12} className="mr-1" />
                          Processing
                        </Badge>
                      )}
                      {doc.summary && (
                        <Badge variant="info" size="sm">
                          <CheckCircle size={12} className="mr-1" />
                          Summarized
                        </Badge>
                      )}
                    </div>
                    
                    <div className="flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleRegenerateSummary(doc.id)}
                      >
                        <RefreshCw size={14} className="mr-1" />
                        Regenerate Summary
                      </Button>
                      <Button
                        size="sm"
                        variant="danger"
                        onClick={() => confirmDelete(doc)}
                      >
                        <Trash2 size={14} className="mr-1" />
                        Delete
                      </Button>
                    </div>
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
        <p className="text-slate-700">
          Are you sure you want to delete <strong>{selectedDoc?.filename}</strong>? This action cannot be undone.
        </p>
      </Modal>
    </div>
  );
};

export default DocumentsPage;
