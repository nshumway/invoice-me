import React from 'react';

interface CardProps {
  children: React.ReactNode;
  className?: string;
  padding?: 'none' | 'sm' | 'md' | 'lg';
}

const paddingClasses = {
  none: '',
  sm: 'p-4',
  md: 'p-6',
  lg: 'p-8',
};

export const Card: React.FC<CardProps> = ({ children, className = '', padding = 'md' }) => {
  const baseClasses = 'bg-gray-800 rounded-lg shadow-xl border border-gray-700';

  return <div className={`${baseClasses} ${paddingClasses[padding]} ${className}`}>{children}</div>;
};
