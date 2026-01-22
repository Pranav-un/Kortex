import { apiClient } from './apiClient';
import type { AuthResponse, LoginRequest, RegisterRequest, User } from '../types';
import { TOKEN_KEY, USER_KEY } from '../config/constants';

export const authService = {
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>('/auth/login', credentials);
    this.setAuthData(response);
    return response;
  },

  async register(data: RegisterRequest): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>('/auth/register', data);
    this.setAuthData(response);
    return response;
  },

  async getProfile(): Promise<User> {
    return apiClient.get<User>('/users/me');
  },

  async updateProfile(name: string): Promise<User> {
    return apiClient.put<User>('/users/me', { name });
  },

  async updatePassword(currentPassword: string, newPassword: string): Promise<void> {
    await apiClient.put('/users/me/password', { currentPassword, newPassword });
  },

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    window.location.href = '/login';
  },

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  },

  isAuthenticated(): boolean {
    return !!this.getToken();
  },

  getCurrentUser(): User | null {
    const userStr = localStorage.getItem(USER_KEY);
    return userStr ? JSON.parse(userStr) : null;
  },

  isAdmin(): boolean {
    const user = this.getCurrentUser();
    return user?.role === 'ADMIN';
  },

  setAuthData(response: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, response.token);
    const user: User = {
      id: response.userId,
      email: response.email,
      name: response.name,
      role: response.role,
      active: true,
      createdAt: new Date().toISOString(),
    };
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
};
