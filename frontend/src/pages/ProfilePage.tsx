import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { authService } from '../services/authService';
import type { User } from '../types';
import { Card, Button, Input, LoadingSpinner, Badge } from '../components/ui';
import { User as UserIcon, Save, KeyRound, FileText, Search as SearchIcon, MessageSquare, BarChart3, LayoutDashboard, LogOut } from 'lucide-react';
import Dock from '../components/magicui/Dock';
import { ROUTES } from '../config/constants';
import { useNotifications } from '../contexts/NotificationContext';
import { useAuth } from '../contexts/AuthContext';

const ProfilePage: React.FC = () => {
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

  const [profile, setProfile] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [name, setName] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [savingProfile, setSavingProfile] = useState(false);
  const [changingPassword, setChangingPassword] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const load = async () => {
      setError('');
      try {
        const user = await authService.getProfile();
        setProfile(user);
        setName(user.name);
      } catch (e: any) {
        setError(e?.response?.data?.message || 'Failed to load profile');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const saveProfile = async () => {
    if (!name.trim()) return;
    setSavingProfile(true);
    setError('');
    try {
      const updated = await authService.updateProfile(name.trim());
      setProfile(updated);
      addToast({ type: 'success', message: 'Profile updated.' });
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Failed to update profile');
      addToast({ type: 'error', message: 'Profile update failed.' });
    } finally {
      setSavingProfile(false);
    }
  };

  const changePassword = async () => {
    if (!currentPassword || !newPassword) return;
    setChangingPassword(true);
    setError('');
    try {
      await authService.updatePassword(currentPassword, newPassword);
      setCurrentPassword('');
      setNewPassword('');
      addToast({ type: 'success', message: 'Password changed successfully.' });
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Failed to change password');
      addToast({ type: 'error', message: 'Password change failed.' });
    } finally {
      setChangingPassword(false);
    }
  };

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
      <div className="max-w-4xl mx-auto">
        <div className="mb-6 flex items-center justify-between">
          <div>
            <h1 className="page-title">Profile</h1>
            <p className="page-sub">Manage your account information and security</p>
          </div>
          {profile && (
            <Badge className="bg-[#120A24] text-[#EDE3FF] border border-[#2A1B45]">{profile.role}</Badge>
          )}
        </div>

        {/* User Info */}
        <Card className="page-card mb-6">
          <div className="flex items-center gap-3 mb-4">
            <UserIcon size={20} className="text-[#CF9EFF]" />
            <h2 className="page-section-title">Account Details</h2>
          </div>
          {profile ? (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm text-[#CF9EFF]">Email</span>
                <span className="text-[#EDE3FF] font-medium">{profile.email}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-[#CF9EFF]">User ID</span>
                <span className="text-[#EDE3FF]">{profile.id}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-[#CF9EFF]">Created</span>
                <span className="text-[#EDE3FF]">{new Date(profile.createdAt).toLocaleString()}</span>
              </div>
            </div>
          ) : (
            <p className="text-[#CF9EFF]">No profile data</p>
          )}
        </Card>

        {/* Update Name */}
        <Card className="page-card mb-6">
          <div className="flex items-center gap-3 mb-4">
            <Save size={20} className="text-[#CF9EFF]" />
            <h2 className="page-section-title">Update Display Name</h2>
          </div>
          <div className="space-y-4">
            <Input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Enter your display name"
              fullWidth
              variant="dark"
            />
            <div className="flex justify-end">
              <Button onClick={saveProfile} loading={savingProfile}>Save</Button>
            </div>
          </div>
        </Card>

        {/* Change Password */}
        <Card className="page-card">
          <div className="flex items-center gap-3 mb-4">
            <KeyRound size={20} className="text-[#CF9EFF]" />
            <h2 className="page-section-title">Change Password</h2>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-[#CF9EFF] mb-1">Current Password</label>
              <input
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                className="page-select"
                placeholder="Enter current password"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-[#CF9EFF] mb-1">New Password</label>
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className="page-select"
                placeholder="Enter new password"
              />
            </div>
          </div>
          <div className="flex justify-end mt-4">
            <Button onClick={changePassword} loading={changingPassword}>Update Password</Button>
          </div>
          {error && (
            <div className="page-error mt-4">{error}</div>
          )}
        </Card>
      </div>
    </div>
  );
};

export default ProfilePage;
