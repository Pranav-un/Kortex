import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { User, LoginRequest, RegisterRequest } from '../types';
import { authService } from '../services/authService';
import { websocketService } from '../services/websocketService';

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
  isAdmin: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check if user is already authenticated
    const initAuth = async () => {
      try {
        if (authService.isAuthenticated()) {
          const userData = await authService.getProfile();
          setUser(userData);
          
          // Connect to WebSocket after successful authentication
          try {
            await websocketService.connect();
          } catch (error) {
            console.error('Failed to connect to WebSocket:', error);
          }
        }
      } catch (error) {
        console.error('Failed to load user profile:', error);
        authService.logout();
      } finally {
        setLoading(false);
      }
    };

    initAuth();
  }, []);

  const login = async (credentials: LoginRequest) => {
    const response = await authService.login(credentials);
    const userData: User = {
      id: response.userId,
      email: response.email,
      name: response.name,
      role: response.role,
      active: true,
      createdAt: new Date().toISOString(),
    };
    setUser(userData);

    // Connect to WebSocket after login
    try {
      await websocketService.connect();
    } catch (error) {
      console.error('Failed to connect to WebSocket:', error);
    }
  };

  const register = async (data: RegisterRequest) => {
    const response = await authService.register(data);
    const userData: User = {
      id: response.userId,
      email: response.email,
      name: response.name,
      role: response.role,
      active: true,
      createdAt: new Date().toISOString(),
    };
    setUser(userData);

    // Connect to WebSocket after registration
    try {
      await websocketService.connect();
    } catch (error) {
      console.error('Failed to connect to WebSocket:', error);
    }
  };

  const logout = () => {
    websocketService.disconnect();
    authService.logout();
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        login,
        register,
        logout,
        isAuthenticated: !!user,
        isAdmin: user?.role === 'ADMIN',
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
