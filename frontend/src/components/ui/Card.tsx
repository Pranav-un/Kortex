import React from 'react';

interface CardProps {
  children: React.ReactNode;
  className?: string;
  padding?: 'none' | 'sm' | 'md' | 'lg';
  hover?: boolean;
  onClick?: () => void;
}

export const Card: React.FC<CardProps> = ({ 
  children, 
  className = '', 
  padding = 'md',
  hover = false,
  onClick
}) => {
  const paddingStyles = {
    none: 'p-0',
    sm: 'p-4',
    md: 'p-6',
    lg: 'p-8',
  };

  const hoverStyle = hover ? 'hover:shadow-md transition-shadow' : '';

  return (
    <div 
      className={`bg-white rounded-lg shadow border border-slate-200 ${paddingStyles[padding]} ${hoverStyle} ${className}`}
      onClick={onClick}
    >
      {children}
    </div>
  );
};
