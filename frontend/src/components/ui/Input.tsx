import React from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
  fullWidth?: boolean;
}

export const Input: React.FC<InputProps> = ({
  label,
  error,
  helperText,
  fullWidth = true,
  className = '',
  id,
  ...props
}) => {
  const inputId = id || label?.toLowerCase().replace(/\s+/g, '-');
  const widthClass = fullWidth ? 'w-full' : '';

  return (
    <div className={widthClass}>
      {label && (
        <label 
          htmlFor={inputId} 
          className="block text-sm font-medium text-slate-700 mb-1"
        >
          {label}
        </label>
      )}
      <input
        id={inputId}
        className={`block ${widthClass} px-3 py-2 border ${
          error ? 'border-red-300 focus:ring-red-500 focus:border-red-500' : 'border-slate-300 focus:ring-slate-500 focus:border-slate-500'
        } rounded-lg shadow-sm focus:outline-none focus:ring-2 text-sm ${className}`}
        {...props}
      />
      {error && (
        <p className="mt-1 text-sm text-red-600">{error}</p>
      )}
      {helperText && !error && (
        <p className="mt-1 text-sm text-slate-500">{helperText}</p>
      )}
    </div>
  );
};
