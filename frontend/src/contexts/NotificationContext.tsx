import React, { createContext, useContext, useState, useEffect, ReactNode, useCallback } from 'react';
import { NotificationMessage } from '../types';
import { websocketService } from '../services/websocketService';
import { MAX_NOTIFICATIONS } from '../config/constants';

interface NotificationContextType {
  notifications: NotificationMessage[];
  unreadCount: number;
  addNotification: (notification: NotificationMessage) => void;
  markAsRead: (id: string) => void;
  markAllAsRead: () => void;
  clearNotification: (id: string) => void;
  clearAll: () => void;
}

const NotificationContext = createContext<NotificationContextType | undefined>(undefined);

export const NotificationProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [notifications, setNotifications] = useState<NotificationMessage[]>([]);

  useEffect(() => {
    // Subscribe to WebSocket notifications
    const unsubscribe = websocketService.onNotification((notification) => {
      addNotification(notification);
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
        addNotification,
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
