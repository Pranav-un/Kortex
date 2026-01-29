import React from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
  fullWidth?: boolean;
  variant?: 'light' | 'dark';
}

export const Input: React.FC<InputProps> = ({
  label,
  error,
  helperText,
  fullWidth = true,
  variant = 'light',
  className = '',
  id,
  ...props
}) => {
  const inputId = id || label?.toLowerCase().replace(/\s+/g, '-');
  const widthClass = fullWidth ? 'w-full' : '';

  const baseInput =
    variant === 'dark'
      ? `block ${widthClass} px-3 py-2 rounded-md focus:outline-none focus:ring-2 text-sm bg-[#0D0620] border ${
          error
            ? '!border-red-400 focus:!ring-red-500 focus:!border-red-500'
            : '!border-[#CF9EFF]/30 focus:!ring-[#CF9EFF] focus:!border-[#CF9EFF]'
        } text-slate-100 placeholder:text-slate-400`
      : `block ${widthClass} px-3 py-2 border ${
          error ? 'border-red-300 focus:ring-red-500 focus:border-red-500' : 'border-slate-300 focus:ring-slate-500 focus:border-slate-500'
        } rounded-lg shadow-sm focus:outline-none focus:ring-2 text-sm`;

  return (
    <div className={widthClass}>
      {label && (
        <label
          htmlFor={inputId}
          className={`block text-sm font-medium mb-1 ${variant === 'dark' ? 'text-slate-300' : 'text-slate-700'}`}
        >
          {label}
        </label>
      )}
      <input
        id={inputId}
        className={`${baseInput} ${className}`}
        {...props}
      />
      {error && (
        <p className="mt-1 text-sm text-red-600">{error}</p>
      )}
      {helperText && !error && (
        <p className={`mt-1 text-sm ${variant === 'dark' ? 'text-slate-400' : 'text-slate-500'}`}>{helperText}</p>
      )}
    </div>
  );
};
