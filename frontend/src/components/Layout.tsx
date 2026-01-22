import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { 
  FileText, 
  Search, 
  MessageSquare, 
  BarChart3, 
  Shield, 
  LogOut,
  Menu,
  X,
  LayoutDashboard
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { ROUTES } from '../config/constants';

interface LayoutProps {
  children: React.ReactNode;
}

export const Layout: React.FC<LayoutProps> = ({ children }) => {
  const { user, logout, isAdmin } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate(ROUTES.LOGIN);
  };

  const navItems = [
    { path: ROUTES.DASHBOARD, label: 'Dashboard', icon: LayoutDashboard },
    { path: ROUTES.DOCUMENTS, label: 'Documents', icon: FileText },
    { path: ROUTES.SEARCH, label: 'Search', icon: Search },
    { path: ROUTES.CHAT, label: 'AI Chat', icon: MessageSquare },
    { path: ROUTES.ANALYTICS, label: 'Analytics', icon: BarChart3 },
    ...(isAdmin ? [{ path: ROUTES.ADMIN, label: 'Admin', icon: Shield }] : []),
  ];

  return (
    <div className="min-h-screen bg-slate-50">
      {/* Mobile menu button */}
      <button
        onClick={() => setSidebarOpen(!sidebarOpen)}
        className="lg:hidden fixed top-4 left-4 z-50 p-2 rounded-lg bg-white shadow-md text-slate-700 hover:bg-slate-100"
      >
        {sidebarOpen ? <X size={24} /> : <Menu size={24} />}
      </button>

      {/* Sidebar */}
      <aside
        className={`fixed inset-y-0 left-0 z-40 w-64 bg-white border-r border-slate-200 transform transition-transform duration-200 ease-in-out lg:translate-x-0 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="flex flex-col h-full">
          {/* Logo */}
          <div className="flex items-center px-6 py-5 border-b border-slate-200">
            <h1 className="text-xl font-bold text-slate-900">Kortex</h1>
          </div>

          {/* Navigation */}
          <nav className="flex-1 px-4 py-6 space-y-1 overflow-y-auto">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = location.pathname === item.path;
              
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  onClick={() => setSidebarOpen(false)}
                  className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-slate-900 text-white'
                      : 'text-slate-700 hover:bg-slate-100'
                  }`}
                >
                  <Icon size={20} />
                  {item.label}
                </Link>
              );
            })}
          </nav>

          {/* User section */}
          <div className="border-t border-slate-200 p-4">
            <div className="flex items-center gap-3 px-3 py-2 rounded-lg bg-slate-50">
              <div className="flex items-center justify-center w-8 h-8 rounded-full bg-slate-300 text-slate-700 text-sm font-semibold">
                {user?.name?.charAt(0).toUpperCase() || 'U'}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-slate-900 truncate">
                  {user?.name}
                </p>
                <p className="text-xs text-slate-500 truncate">
                  {user?.email}
                </p>
              </div>
            </div>
            <button
              onClick={handleLogout}
              className="flex items-center gap-3 w-full px-3 py-2 mt-2 text-sm font-medium text-slate-700 hover:bg-slate-100 rounded-lg transition-colors"
            >
              <LogOut size={20} />
              Sign out
            </button>
          </div>
        </div>
      </aside>

      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-30 bg-slate-900 bg-opacity-50 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Main content */}
      <main className="lg:pl-64">
        <div className="min-h-screen">
          {children}
        </div>
      </main>
    </div>
  );
};
