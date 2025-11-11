import React from 'react';

export type AlertVariant = 'info' | 'success' | 'warning' | 'error';

interface AlertProps {
  children: React.ReactNode;
  variant?: AlertVariant;
  className?: string;
  icon?: React.ReactNode;
}

const variantClasses: Record<AlertVariant, string> = {
  info: 'bg-primary-900/30 border-primary-700/50 text-primary-200',
  success: 'bg-success-900/30 border-success-700/50 text-success-200',
  warning: 'bg-warning-900/30 border-warning-700/50 text-warning-200',
  error: 'bg-error-900/30 border-error-700/50 text-error-200',
};

export const Alert: React.FC<AlertProps> = ({
  children,
  variant = 'info',
  className = '',
  icon,
}) => {
  const baseClasses = 'rounded-lg p-3 border flex items-start gap-2';

  return (
    <div className={`${baseClasses} ${variantClasses[variant]} ${className}`}>
      {icon && <div className="flex-shrink-0 mt-0.5">{icon}</div>}
      <div className="flex-1">{children}</div>
    </div>
  );
};
