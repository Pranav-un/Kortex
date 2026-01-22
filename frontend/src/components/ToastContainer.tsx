import React from 'react';
import { useNotifications } from '../contexts/NotificationContext';
import { Toast } from './ui';

interface ToastItem {
  id: string;
  type: 'success' | 'error' | 'info' | 'warning';
  message: string;
}

const ToastContainer: React.FC = () => {
  const { toasts, removeToast } = useNotifications();

  return (
    <div className="fixed top-4 right-4 z-50 flex flex-col gap-2 max-w-md">
      {toasts.map((toast: ToastItem) => (
        <Toast
          key={toast.id}
          type={toast.type}
          message={toast.message}
          onClose={() => removeToast(toast.id)}
        />
      ))}
    </div>
  );
};

export default ToastContainer;
