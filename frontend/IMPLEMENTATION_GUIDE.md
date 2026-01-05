# Kortex Frontend Implementation Guide

## ✅ Completed: Foundation & Architecture

### 1. Configuration & Types
- ✅ `src/config/constants.ts` - API URLs, routes, constants
- ✅ `src/types/index.ts` - Complete TypeScript interfaces

### 2. Services Layer  
- ✅ `src/services/apiClient.ts` - Axios HTTP client with interceptors
- ✅ `src/services/authService.ts` - Authentication API calls
- ✅ `src/services/documentService.ts` - Document management API
- ✅ `src/services/searchService.ts` - Search & Q&A API
- ✅ `src/services/analyticsService.ts` - Analytics API
- ✅ `src/services/adminService.ts` - Admin management API
- ✅ `src/services/websocketService.ts` - STOMP WebSocket client

### 3. Contexts
- ✅ `src/contexts/AuthContext.tsx` - Authentication state management
- ✅ `src/contexts/NotificationContext.tsx` - Real-time notifications

### 4. Core Components
- ✅ `src/components/ProtectedRoute.tsx` - Route guard

## 📋 TODO: Feature Modules

### Required Component Structure

```
src/
├── components/
│   ├── common/
│   │   ├── Header.tsx              # Top navigation with user menu
│   │   ├── Sidebar.tsx             # Side navigation menu
│   │   ├── Notification Bell.tsx   # Notification dropdown
│   │   ├── LoadingSpinner.tsx      # Loading indicator
│   │   └── ErrorMessage.tsx        # Error display
│   │
│   ├── auth/
│   │   ├── LoginForm.tsx           # Login page
│   │   └── RegisterForm.tsx        # Registration page
│   │
│   ├── documents/
│   │   ├── DocumentList.tsx        # Document grid/list
│   │   ├── DocumentCard.tsx        # Single document card
│   │   ├── DocumentUpload.tsx      # Upload modal/form
│   │   └── DocumentDetails.tsx     # Document detail view
│   │
│   ├── search/
│   │   ├── SearchBar.tsx           # Search input
│   │   ├── SearchResults.tsx       # Results display
│   │   └── SearchFilters.tsx       # Filter options
│   │
│   ├── chat/
│   │   ├── ChatInterface.tsx       # Main chat UI
│   │   ├── ChatMessage.tsx         # Message bubble
│   │   ├── ChatInput.tsx           # Question input
│   │   └── CitationList.tsx        # Source citations
│   │
│   ├── analytics/
│   │   ├── AnalyticsDashboard.tsx  # Main dashboard
│   │   ├── OverviewCards.tsx       # Stats cards
│   │   ├── KeywordCloud.tsx        # Keyword visualization
│   │   ├── UploadChart.tsx         # Upload trends chart
│   │   └── ActivityFeed.tsx        # Recent activity
│   │
│   └── admin/
│       ├── AdminLayout.tsx         # Admin panel layout
│       ├── UserManagement.tsx      # User list & controls
│       ├── SystemHealth.tsx        # Health dashboard
│       ├── EmbeddingMonitor.tsx    # Embedding status
│       └── SystemStats.tsx         # Statistics display
│
├── pages/
│   ├── LoginPage.tsx
│   ├── RegisterPage.tsx
│   ├── DocumentsPage.tsx
│   ├── SearchPage.tsx
│   ├── ChatPage.tsx
│   ├── AnalyticsPage.tsx
│   └── AdminPage.tsx
│
└── App.tsx                         # Main app with routing
```

## 🚀 Next Steps

### Phase 1: Authentication UI (Priority: High)
1. Create `LoginForm.tsx` with email/password inputs
2. Create `RegisterForm.tsx` with name/email/password
3. Create `LoginPage.tsx` and `RegisterPage.tsx`
4. Add form validation and error handling

### Phase 2: Layout & Navigation (Priority: High)
1. Create `Header.tsx` with logo, navigation, user menu
2. Create `Sidebar.tsx` with route links
3. Create `NotificationBell.tsx` with dropdown
4. Create main layout component

### Phase 3: Document Management (Priority: High)
1. Create `DocumentUpload.tsx` with drag-drop
2. Create `DocumentList.tsx` with grid view
3. Create `DocumentCard.tsx` with actions
4. Add delete confirmation modals

### Phase 4: Search & Chat (Priority: Medium)
1. Create `SearchBar.tsx` with autocomplete
2. Create `SearchResults.tsx` with highlighting
3. Create `ChatInterface.tsx` with message history
4. Create `ChatInput.tsx` with send button
5. Add citation links in results

### Phase 5: Analytics Dashboard (Priority: Medium)
1. Create `AnalyticsDashboard.tsx` with grid layout
2. Create `OverviewCards.tsx` with statistics
3. Create `UploadChart.tsx` using Recharts
4. Create `KeywordCloud.tsx` for tag visualization
5. Create `ActivityFeed.tsx` for recent uploads

### Phase 6: Admin Panel (Priority: Low)
1. Create `UserManagement.tsx` with table
2. Create `SystemHealth.tsx` with status indicators
3. Create `EmbeddingMonitor.tsx` with progress bars
4. Add user activation/deactivation controls

### Phase 7: Notifications (Priority: Medium)
1. Create notification toast component
2. Create notification panel
3. Add sound/desktop notifications (optional)
4. Implement notification filtering

## 📦 Required npm Packages (Already Installed)
```bash
npm install react-router-dom axios @stomp/stompjs sockjs-client recharts lucide-react
npm install --save-dev @types/sockjs-client
```

## 🎨 Recommended Additional Packages
```bash
# UI Components & Styling
npm install tailwindcss postcss autoprefixer
npm install @headlessui/react  # For modals, dropdowns
npm install react-hot-toast     # For notifications

# Form Handling
npm install react-hook-form
npm install zod @hookform/resolvers  # For validation

# Date Formatting
npm install date-fns
```

## 🔧 Configuration Files Needed

### tailwind.config.js
```javascript
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: '#3b82f6',
        secondary: '#8b5cf6',
      },
    },
  },
  plugins: [],
};
```

### postcss.config.js
```javascript
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
};
```

### .env.example
```
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_BASE_URL=http://localhost:8080/ws
```

## 🎯 Implementation Priority

**Week 1: Core Functionality**
- Authentication (Login/Register)
- Document Upload & List
- Basic navigation

**Week 2: AI Features**
- Semantic Search UI
- Chat/Q&A Interface
- Real-time Notifications

**Week 3: Analytics & Admin**
- Analytics Dashboard
- Admin Panel
- Polish & Testing

## 📝 Code Examples

### Example: Document Upload Component
```typescript
import { useState } from 'react';
import { documentService } from '../services/documentService';
import { useNotifications } from '../contexts/NotificationContext';

export const DocumentUpload: React.FC = () => {
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const { addNotification } = useNotifications();

  const handleUpload = async (file: File) => {
    setUploading(true);
    try {
      await documentService.uploadDocument(file, setProgress);
      addNotification({
        id: Date.now().toString(),
        type: 'DOCUMENT_UPLOADED',
        title: 'Upload Complete',
        message: `${file.name} uploaded successfully`,
        timestamp: new Date().toISOString(),
        read: false,
        documentId: null,
        documentName: file.name,
        data: null,
      });
    } catch (error) {
      console.error('Upload failed:', error);
    } finally {
      setUploading(false);
      setProgress(0);
    }
  };

  return (
    <div className="upload-component">
      {/* Upload UI */}
    </div>
  );
};
```

### Example: Using Auth Context
```typescript
import { useAuth } from '../contexts/AuthContext';

export const Header: React.FC = () => {
  const { user, logout, isAdmin } = useAuth();

  return (
    <header>
      <span>Welcome, {user?.name}</span>
      {isAdmin && <Link to="/admin">Admin Panel</Link>}
      <button onClick={logout}>Logout</button>
    </header>
  );
};
```

## 🚨 Important Notes

1. **WebSocket Connection**: Automatically connects after login via AuthContext
2. **Token Management**: Auto-refresh handled by apiClient interceptor
3. **Error Handling**: 401 responses auto-redirect to login
4. **Type Safety**: All API responses are typed
5. **Notification System**: Real-time via WebSocket + local state

## 📖 API Integration Examples

All services are ready to use:
```typescript
// Authentication
await authService.login({ email, password });
await authService.register({ name, email, password });

// Documents
const docs = await documentService.getDocuments();
await documentService.uploadDocument(file, onProgress);

// Search & Q&A
const results = await searchService.search(query, 10);
const answer = await searchService.askQuestion(question);

// Analytics
const overview = await analyticsService.getOverview();
const keywords = await analyticsService.getKeywordFrequency(20);

// Admin (requires ADMIN role)
const users = await adminService.getAllUsers();
const health = await adminService.getSystemHealth();
```

## ✅ Architecture Checklist

- [x] API client with interceptors
- [x] Authentication service & context
- [x] WebSocket service & notifications context
- [x] Type-safe API calls
- [x] Protected routes
- [x] Service layer for all backend APIs
- [ ] UI components (in progress)
- [ ] Routing setup (in progress)
- [ ] Form validation
- [ ] Error boundaries
- [ ] Loading states
- [ ] Responsive design

## 🎨 Design Recommendations

1. **Color Scheme**: Blue primary (trust), purple secondary (AI/tech)
2. **Icons**: Lucide React (already installed)
3. **Charts**: Recharts (already installed)
4. **Animations**: CSS transitions, avoid heavy libraries
5. **Mobile**: Responsive design from day 1

## 🔐 Security Considerations

1. **Token Storage**: LocalStorage (consider HttpOnly cookies for production)
2. **XSS Protection**: React escapes by default
3. **CSRF**: Stateless JWT (no cookies = no CSRF)
4. **Input Validation**: Validate on both frontend and backend
5. **Admin Routes**: Protected by `requireAdmin` prop

---

**Status**: Foundation complete, ready for UI implementation
**Next Action**: Create authentication pages and main layout
