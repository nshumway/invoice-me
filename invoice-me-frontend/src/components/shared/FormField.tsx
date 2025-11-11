import React from 'react';

interface FormFieldProps {
  children: React.ReactNode;
  className?: string;
}

export const FormField: React.FC<FormFieldProps> = ({ children, className = '' }) => {
  return <div className={`space-y-1 ${className}`}>{children}</div>;
};
