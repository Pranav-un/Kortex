import React from 'react';

const AnalyticsPage: React.FC = () => {
  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="max-w-7xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">Analytics Dashboard</h1>
        <div className="bg-white rounded-lg shadow p-6">
          <p className="text-gray-600">
            Analytics dashboard coming soon...
          </p>
          <p className="text-sm text-gray-500 mt-4">
            Features: Word count, reading time, keyword frequency, upload trends, activity logs
          </p>
        </div>
      </div>
    </div>
  );
};

export default AnalyticsPage;
