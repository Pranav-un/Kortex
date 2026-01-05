import React from 'react';

const AdminPage: React.FC = () => {
  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="max-w-7xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">Admin Panel</h1>
        <div className="bg-white rounded-lg shadow p-6">
          <p className="text-gray-600">
            Admin management panel coming soon...
          </p>
          <p className="text-sm text-gray-500 mt-4">
            Features: User management, system health, embedding monitoring, statistics
          </p>
        </div>
      </div>
    </div>
  );
};

export default AdminPage;
