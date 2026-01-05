// API Configuration
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
export const WS_BASE_URL = import.meta.env.VITE_WS_BASE_URL || 'http://localhost:8080/ws';

// Local Storage Keys
export const TOKEN_KEY = 'kortex_auth_token';
export const USER_KEY = 'kortex_user';

// Pagination
export const DEFAULT_PAGE_SIZE = 10;
export const MAX_PAGE_SIZE = 100;

// File Upload
export const MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
export const ALLOWED_FILE_TYPES = ['.pdf', '.docx', '.doc', '.txt'];
export const ALLOWED_MIME_TYPES = [
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/msword',
  'text/plain'
];

// Search
export const DEFAULT_SEARCH_LIMIT = 10;
export const MAX_SEARCH_LIMIT = 100;

// Analytics
export const DEFAULT_KEYWORD_LIMIT = 20;
export const DEFAULT_ACTIVITY_LIMIT = 20;

// Notification
export const NOTIFICATION_DISPLAY_DURATION = 5000; // 5 seconds
export const MAX_NOTIFICATIONS = 50;

// Routes
export const ROUTES = {
  HOME: '/',
  LOGIN: '/login',
  REGISTER: '/register',
  DOCUMENTS: '/documents',
  SEARCH: '/search',
  CHAT: '/chat',
  ANALYTICS: '/analytics',
  ADMIN: '/admin',
  ADMIN_USERS: '/admin/users',
  ADMIN_SYSTEM: '/admin/system',
  ADMIN_EMBEDDINGS: '/admin/embeddings',
} as const;
