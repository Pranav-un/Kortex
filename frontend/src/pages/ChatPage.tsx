import React from 'react';

const ChatPage: React.FC = () => {
  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="max-w-7xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">AI Chat</h1>
        <div className="bg-white rounded-lg shadow p-6">
          <p className="text-gray-600">
            RAG-powered chat interface coming soon...
          </p>
          <p className="text-sm text-gray-500 mt-4">
            Features: Question answering, citations, document-specific queries
          </p>
        </div>
      </div>
    </div>
  );
};

export default ChatPage;
