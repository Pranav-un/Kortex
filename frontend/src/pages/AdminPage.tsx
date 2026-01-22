import React, { useState, useEffect } from 'react';
import { adminService } from '../services/adminService';
import type { UserManagement, SystemHealth, SystemStats, EmbeddingStatus, FailedEmbedding } from '../types';
import { Card, Button, Badge, LoadingSpinner, Modal } from '../components/ui';
import { Users, Activity, Database, AlertTriangle, CheckCircle, XCircle, RefreshCw, Trash2 } from 'lucide-react';

type TabType = 'users' | 'system' | 'embeddings';

const AdminPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<TabType>('users');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Users
  const [users, setUsers] = useState<UserManagement[]>([]);
  const [selectedUser, setSelectedUser] = useState<UserManagement | null>(null);
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  // System
  const [health, setHealth] = useState<SystemHealth | null>(null);
  const [stats, setStats] = useState<SystemStats | null>(null);

  // Embeddings
  const [embeddingStatus, setEmbeddingStatus] = useState<EmbeddingStatus | null>(null);
  const [failedEmbeddings, setFailedEmbeddings] = useState<FailedEmbedding[]>([]);

  useEffect(() => {
    loadData();
  }, [activeTab]);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      if (activeTab === 'users') {
        const usersData = await adminService.getAllUsers();
        setUsers(usersData);
      } else if (activeTab === 'system') {
        const [healthData, statsData] = await Promise.all([
          adminService.getSystemHealth(),
          adminService.getSystemStats(),
        ]);
        setHealth(healthData);
        setStats(statsData);
      } else if (activeTab === 'embeddings') {
        const [statusData, failedData] = await Promise.all([
          adminService.getEmbeddingStatus(),
          adminService.getFailedEmbeddings(),
        ]);
        setEmbeddingStatus(statusData);
        setFailedEmbeddings(failedData);
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleUserStatus = async (user: UserManagement) => {
    setActionLoading(true);
    setError('');
    try {
      if (user.active) {
        await adminService.deactivateUser(user.id);
      } else {
        await adminService.activateUser(user.id);
      }
      await loadData();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to update user status');
    } finally {
      setActionLoading(false);
    }
  };

  const confirmDeleteUser = (user: UserManagement) => {
    setSelectedUser(user);
    setDeleteModalOpen(true);
  };

  const handleDeleteUser = async () => {
    if (!selectedUser) return;
    setActionLoading(true);
    setError('');
    try {
      await adminService.deleteUser(selectedUser.id);
      setDeleteModalOpen(false);
      setSelectedUser(null);
      await loadData();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to delete user');
    } finally {
      setActionLoading(false);
    }
  };

  const handleRetryEmbedding = async (documentId: number) => {
    setActionLoading(true);
    setError('');
    try {
      await adminService.retryEmbedding(documentId);
      await loadData();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to retry embedding');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 p-6">
      <div className="max-w-7xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-slate-900 mb-2">Admin Panel</h1>
          <p className="text-slate-600">Manage users, monitor system health, and track embeddings</p>
        </div>

        {/* Tabs */}
        <div className="flex gap-2 mb-6">
          <button
            onClick={() => setActiveTab('users')}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-colors ${
              activeTab === 'users'
                ? 'bg-white text-slate-900 shadow'
                : 'text-slate-600 hover:bg-white/50'
            }`}
          >
            <Users size={20} />
            Users
          </button>
          <button
            onClick={() => setActiveTab('system')}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-colors ${
              activeTab === 'system'
                ? 'bg-white text-slate-900 shadow'
                : 'text-slate-600 hover:bg-white/50'
            }`}
          >
            <Activity size={20} />
            System Health
          </button>
          <button
            onClick={() => setActiveTab('embeddings')}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-colors ${
              activeTab === 'embeddings'
                ? 'bg-white text-slate-900 shadow'
                : 'text-slate-600 hover:bg-white/50'
            }`}
          >
            <Database size={20} />
            Embeddings
          </button>
        </div>

        {error && (
          <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
            {error}
          </div>
        )}

        {/* Users Tab */}
        {activeTab === 'users' && (
          <Card>
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold text-slate-900">User Management</h2>
              <Badge>{users.length} users</Badge>
            </div>

            {loading ? (
              <div className="py-12">
                <LoadingSpinner size="lg" />
              </div>
            ) : (
              <div className="space-y-3">
                {users.map((user) => (
                  <div
                    key={user.id}
                    className="flex items-center justify-between p-4 rounded-lg border border-slate-200 hover:bg-slate-50"
                  >
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-1">
                        <h3 className="font-medium text-slate-900">{user.name}</h3>
                        <Badge variant={user.role === 'ADMIN' ? 'info' : 'default'} size="sm">
                          {user.role}
                        </Badge>
                        <Badge variant={user.active ? 'success' : 'error'} size="sm">
                          {user.active ? 'Active' : 'Inactive'}
                        </Badge>
                      </div>
                      <p className="text-sm text-slate-600 mb-2">{user.email}</p>
                      <div className="flex items-center gap-4 text-xs text-slate-500">
                        <span>{user.documentCount} documents</span>
                        <span>·</span>
                        <span>{user.totalStorageFormatted} storage</span>
                        <span>·</span>
                        <span>Joined {new Date(user.createdAt).toLocaleDateString()}</span>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <Button
                        size="sm"
                        variant={user.active ? 'outline' : 'secondary'}
                        onClick={() => handleToggleUserStatus(user)}
                        disabled={actionLoading}
                      >
                        {user.active ? 'Deactivate' : 'Activate'}
                      </Button>
                      <Button
                        size="sm"
                        variant="danger"
                        onClick={() => confirmDeleteUser(user)}
                        disabled={actionLoading}
                      >
                        <Trash2 size={14} />
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Card>
        )}

        {/* System Health Tab */}
        {activeTab === 'system' && (
          <div className="space-y-6">
            {loading ? (
              <Card>
                <div className="py-12">
                  <LoadingSpinner size="lg" />
                </div>
              </Card>
            ) : (
              <>
                {/* Health Status */}
                {health && (
                  <Card>
                    <div className="flex items-center justify-between mb-6">
                      <h2 className="text-lg font-semibold text-slate-900">System Health</h2>
                      <Badge 
                        variant={
                          health.status === 'healthy' ? 'success' : 
                          health.status === 'degraded' ? 'warning' : 'error'
                        }
                      >
                        {health.status.toUpperCase()}
                      </Badge>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                      <div className="p-4 rounded-lg border border-slate-200">
                        <div className="flex items-center justify-between mb-2">
                          <span className="text-sm font-medium text-slate-700">Database</span>
                          {health.database.status === 'up' ? (
                            <CheckCircle size={18} className="text-green-600" />
                          ) : (
                            <XCircle size={18} className="text-red-600" />
                          )}
                        </div>
                        <p className="text-xs text-slate-600">{health.database.message}</p>
                        <p className="text-xs text-slate-500 mt-1">{health.database.responseTimeMs}ms</p>
                      </div>

                      <div className="p-4 rounded-lg border border-slate-200">
                        <div className="flex items-center justify-between mb-2">
                          <span className="text-sm font-medium text-slate-700">Vector DB</span>
                          {health.vectorDatabase.status === 'up' ? (
                            <CheckCircle size={18} className="text-green-600" />
                          ) : (
                            <XCircle size={18} className="text-red-600" />
                          )}
                        </div>
                        <p className="text-xs text-slate-600">{health.vectorDatabase.message}</p>
                        <p className="text-xs text-slate-500 mt-1">{health.vectorDatabase.responseTimeMs}ms</p>
                      </div>

                      <div className="p-4 rounded-lg border border-slate-200">
                        <div className="flex items-center justify-between mb-2">
                          <span className="text-sm font-medium text-slate-700">Storage</span>
                          {health.storage.status === 'up' ? (
                            <CheckCircle size={18} className="text-green-600" />
                          ) : (
                            <XCircle size={18} className="text-red-600" />
                          )}
                        </div>
                        <p className="text-xs text-slate-600">{health.storage.message}</p>
                        <p className="text-xs text-slate-500 mt-1">{health.storage.responseTimeMs}ms</p>
                      </div>
                    </div>

                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                      <div>
                        <p className="text-slate-600 mb-1">Memory Usage</p>
                        <p className="font-semibold text-slate-900">
                          {health.resources.memoryUsagePercentage.toFixed(1)}%
                        </p>
                        <p className="text-xs text-slate-500">
                          {health.resources.usedMemoryMB} / {health.resources.maxMemoryMB} MB
                        </p>
                      </div>
                      <div>
                        <p className="text-slate-600 mb-1">Active Threads</p>
                        <p className="font-semibold text-slate-900">{health.resources.activeThreads}</p>
                      </div>
                      <div>
                        <p className="text-slate-600 mb-1">Disk Usage</p>
                        <p className="font-semibold text-slate-900">{health.resources.diskUsedFormatted}</p>
                        <p className="text-xs text-slate-500">of {health.resources.diskTotalFormatted}</p>
                      </div>
                      <div>
                        <p className="text-slate-600 mb-1">Uptime</p>
                        <p className="font-semibold text-slate-900">
                          {Math.floor(health.uptimeSeconds / 3600)}h
                        </p>
                      </div>
                    </div>
                  </Card>
                )}

                {/* System Stats */}
                {stats && (
                  <Card>
                    <h2 className="text-lg font-semibold text-slate-900 mb-6">System Statistics</h2>
                    
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
                      <div>
                        <p className="text-sm text-slate-600 mb-1">Total Users</p>
                        <p className="text-2xl font-bold text-slate-900">{stats.totalUsers}</p>
                        <p className="text-xs text-slate-500 mt-1">
                          {stats.activeUsers} active · {stats.adminUsers} admins
                        </p>
                      </div>
                      <div>
                        <p className="text-sm text-slate-600 mb-1">Total Documents</p>
                        <p className="text-2xl font-bold text-slate-900">{stats.totalDocuments}</p>
                        <p className="text-xs text-slate-500 mt-1">{stats.totalStorageFormatted}</p>
                      </div>
                      <div>
                        <p className="text-sm text-slate-600 mb-1">With Embeddings</p>
                        <p className="text-2xl font-bold text-slate-900">{stats.documentsWithEmbeddings}</p>
                        <p className="text-xs text-slate-500 mt-1">{stats.embeddingCoverage.toFixed(1)}% coverage</p>
                      </div>
                      <div>
                        <p className="text-sm text-slate-600 mb-1">Processing Success</p>
                        <p className="text-2xl font-bold text-slate-900">{stats.processingSuccessRate.toFixed(1)}%</p>
                      </div>
                    </div>

                    <div className="mt-6 pt-6 border-t border-slate-200 grid grid-cols-3 gap-4 text-sm">
                      <div>
                        <p className="text-slate-600 mb-1">Last 24 Hours</p>
                        <p className="font-semibold text-slate-900">{stats.uploadsLast24Hours} uploads</p>
                      </div>
                      <div>
                        <p className="text-slate-600 mb-1">Last 7 Days</p>
                        <p className="font-semibold text-slate-900">{stats.uploadsLast7Days} uploads</p>
                      </div>
                      <div>
                        <p className="text-slate-600 mb-1">Last 30 Days</p>
                        <p className="font-semibold text-slate-900">{stats.uploadsLast30Days} uploads</p>
                      </div>
                    </div>
                  </Card>
                )}
              </>
            )}
          </div>
        )}

        {/* Embeddings Tab */}
        {activeTab === 'embeddings' && (
          <div className="space-y-6">
            {loading ? (
              <Card>
                <div className="py-12">
                  <LoadingSpinner size="lg" />
                </div>
              </Card>
            ) : (
              <>
                {embeddingStatus && (
                  <Card>
                    <h2 className="text-lg font-semibold text-slate-900 mb-6">Embedding Status</h2>
                    
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-6 mb-6">
                      <div>
                        <p className="text-sm text-slate-600 mb-1">Total Documents</p>
                        <p className="text-2xl font-bold text-slate-900">{embeddingStatus.totalDocuments}</p>
                      </div>
                      <div>
                        <p className="text-sm text-slate-600 mb-1">With Embeddings</p>
                        <p className="text-2xl font-bold text-green-600">{embeddingStatus.documentsWithEmbeddings}</p>
                      </div>
                      <div>
                        <p className="text-sm text-slate-600 mb-1">Pending</p>
                        <p className="text-2xl font-bold text-yellow-600">{embeddingStatus.documentsPending}</p>
                      </div>
                      <div>
                        <p className="text-sm text-slate-600 mb-1">Failed</p>
                        <p className="text-2xl font-bold text-red-600">{embeddingStatus.documentsFailed}</p>
                      </div>
                    </div>

                    <div className="mb-4">
                      <div className="flex items-center justify-between text-sm mb-2">
                        <span className="text-slate-600">Completion</span>
                        <span className="font-semibold text-slate-900">{embeddingStatus.completionPercentage.toFixed(1)}%</span>
                      </div>
                      <div className="w-full bg-slate-200 rounded-full h-2">
                        <div
                          className="h-full bg-green-600 rounded-full transition-all"
                          style={{ width: `${embeddingStatus.completionPercentage}%` }}
                        />
                      </div>
                    </div>

                    <div className="text-sm text-slate-600">
                      <p>{embeddingStatus.chunksWithEmbeddings} / {embeddingStatus.totalChunks} chunks processed</p>
                    </div>
                  </Card>
                )}

                {failedEmbeddings.length > 0 && (
                  <Card>
                    <div className="flex items-center gap-2 mb-4">
                      <AlertTriangle size={20} className="text-red-600" />
                      <h2 className="text-lg font-semibold text-slate-900">Failed Embeddings</h2>
                    </div>

                    <div className="space-y-3">
                      {failedEmbeddings.map((item) => (
                        <div
                          key={item.documentId}
                          className="flex items-start justify-between p-4 rounded-lg border border-red-200 bg-red-50"
                        >
                          <div className="flex-1">
                            <h3 className="font-medium text-slate-900 mb-1">{item.documentName}</h3>
                            <p className="text-sm text-slate-600 mb-2">{item.ownerEmail}</p>
                            <p className="text-xs text-red-700 mb-2">{item.errorMessage}</p>
                            <div className="flex items-center gap-3 text-xs text-slate-500">
                              <span>{item.failedChunks} / {item.totalChunks} chunks failed</span>
                              <span>·</span>
                              <span>Uploaded {new Date(item.uploadTime).toLocaleDateString()}</span>
                            </div>
                          </div>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleRetryEmbedding(item.documentId)}
                            disabled={actionLoading}
                          >
                            <RefreshCw size={14} className="mr-1" />
                            Retry
                          </Button>
                        </div>
                      ))}
                    </div>
                  </Card>
                )}
              </>
            )}
          </div>
        )}
      </div>

      {/* Delete User Modal */}
      <Modal
        isOpen={deleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        title="Delete User"
        footer={
          <>
            <Button variant="outline" onClick={() => setDeleteModalOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="danger"
              onClick={handleDeleteUser}
              loading={actionLoading}
            >
              Delete User
            </Button>
          </>
        }
      >
        <p className="text-slate-700">
          Are you sure you want to delete user <strong>{selectedUser?.name}</strong> ({selectedUser?.email})? 
          This will permanently delete the user and all their documents. This action cannot be undone.
        </p>
      </Modal>
    </div>
  );
};

export default AdminPage;
