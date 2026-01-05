import React from 'react';

const DocumentsPage: React.FC = () => {
  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="max-w-7xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">Documents</h1>
        <div className="bg-white rounded-lg shadow p-6">
          <p className="text-gray-600">
            Document management interface coming soon...
          </p>
          <p className="text-sm text-gray-500 mt-4">
            Features: Upload, List, Search, Delete documents with AI processing
          </p>
        </div>
      </div>
    </div>
  );
};

export default DocumentsPage;
