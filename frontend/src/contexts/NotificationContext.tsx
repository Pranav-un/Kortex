import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import type { ReactNode } from 'react';
import type { NotificationMessage } from '../types';
import { websocketService } from '../services/websocketService';
import { MAX_NOTIFICATIONS } from '../config/constants';

interface Toast {
  id: string;
  type: 'success' | 'error' | 'info' | 'warning';
  message: string;
}

interface NotificationContextType {
  notifications: NotificationMessage[];
  unreadCount: number;
  toasts: Toast[];
  addNotification: (notification: NotificationMessage) => void;
  addToast: (toast: Omit<Toast, 'id'>) => void;
  removeToast: (id: string) => void;
  markAsRead: (id: string) => void;
  markAllAsRead: () => void;
  clearNotification: (id: string) => void;
  clearAll: () => void;
}

const NotificationContext = createContext<NotificationContextType | undefined>(undefined);

export const NotificationProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [notifications, setNotifications] = useState<NotificationMessage[]>([]);
  const [toasts, setToasts] = useState<Toast[]>([]);

  useEffect(() => {
    // Subscribe to WebSocket notifications
    const unsubscribe = websocketService.onNotification((notification) => {
      addNotification(notification);
      // Also show as toast - determine type based on notification type
      const toastType = notification.type.includes('FAILED') ? 'error' : 
                        notification.type.includes('COMPLETE') || notification.type.includes('GENERATED') ? 'success' : 
                        'info';
      addToast({
        type: toastType,
        message: notification.message,
      });
    });

    return () => {
      unsubscribe();
    };
  }, []);

  const addNotification = useCallback((notification: NotificationMessage) => {
    setNotifications((prev) => {
      const updated = [notification, ...prev];
      // Keep only last MAX_NOTIFICATIONS
      return updated.slice(0, MAX_NOTIFICATIONS);
    });
  }, []);

  const addToast = useCallback((toast: Omit<Toast, 'id'>) => {
    const newToast: Toast = {
      ...toast,
      id: `toast-${Date.now()}-${Math.random()}`,
    };
    setToasts((prev) => [...prev, newToast]);
  }, []);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const markAsRead = useCallback((id: string) => {
    setNotifications((prev) =>
      prev.map((notif) =>
        notif.id === id ? { ...notif, read: true } : notif
      )
    );
  }, []);

  const markAllAsRead = useCallback(() => {
    setNotifications((prev) =>
      prev.map((notif) => ({ ...notif, read: true }))
    );
  }, []);

  const clearNotification = useCallback((id: string) => {
    setNotifications((prev) => prev.filter((notif) => notif.id !== id));
  }, []);

  const clearAll = useCallback(() => {
    setNotifications([]);
  }, []);

  const unreadCount = notifications.filter((n) => !n.read).length;

  return (
    <NotificationContext.Provider
      value={{
        notifications,
        unreadCount,
        toasts,
        addNotification,
        addToast,
        removeToast,
        markAsRead,
        markAllAsRead,
        clearNotification,
        clearAll,
      }}
    >
      {children}
    </NotificationContext.Provider>
  );
};

export const useNotifications = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications must be used within NotificationProvider');
  }
  return context;
};
