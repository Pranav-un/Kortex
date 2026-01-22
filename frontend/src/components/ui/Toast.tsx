import React, { useEffect } from 'react';
import { X, CheckCircle, AlertCircle, Info, AlertTriangle } from 'lucide-react';

export interface ToastProps {
  type: 'success' | 'error' | 'info' | 'warning';
  message: string;
  onClose: () => void;
}

export const Toast: React.FC<ToastProps> = ({
  type,
  message,
  onClose,
}) => {
  useEffect(() => {
    const timer = setTimeout(() => {
      onClose();
    }, 5000);
    return () => clearTimeout(timer);
  }, [onClose]);

  const icons = {
    success: <CheckCircle size={20} className="text-green-600" />,
    error: <AlertCircle size={20} className="text-red-600" />,
    info: <Info size={20} className="text-blue-600" />,
    warning: <AlertTriangle size={20} className="text-yellow-600" />,
  };

  const styles = {
    success: 'bg-green-50 border-green-200',
    error: 'bg-red-50 border-red-200',
    info: 'bg-blue-50 border-blue-200',
    warning: 'bg-yellow-50 border-yellow-200',
  };

  return (
    <div className={`flex items-start gap-3 p-4 rounded-lg border shadow-lg ${styles[type]} animate-slide-in-right`}>
      <div className="flex-shrink-0 mt-0.5">
        {icons[type]}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm text-slate-700">
          {message}
        </p>
      </div>
      <button
        onClick={() => onClose()}
        className="flex-shrink-0 text-slate-400 hover:text-slate-600 transition-colors"
      >
        <X size={18} />
      </button>
    </div>
  );
};
