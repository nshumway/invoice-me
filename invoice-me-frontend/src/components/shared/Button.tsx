import React from 'react';

export type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'success' | 'ghost';
export type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  fullWidth?: boolean;
  children: React.ReactNode;
}

const variantClasses: Record<ButtonVariant, string> = {
  primary:
    'bg-primary-600 text-white hover:bg-primary-700 focus-visible:ring-primary-500 disabled:bg-primary-600/50',
  secondary:
    'bg-gray-600 text-white hover:bg-gray-700 focus-visible:ring-gray-500 disabled:bg-gray-600/50',
  danger:
    'bg-error-600 text-white hover:bg-error-700 focus-visible:ring-error-500 disabled:bg-error-600/50',
  success:
    'bg-success-600 text-white hover:bg-success-700 focus-visible:ring-success-500 disabled:bg-success-600/50',
  ghost:
    'bg-transparent text-gray-300 hover:bg-gray-700 focus-visible:ring-gray-500 disabled:opacity-50',
};

const sizeClasses: Record<ButtonSize, string> = {
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-4 py-2 text-base',
  lg: 'px-6 py-3 text-lg',
};

export const Button: React.FC<ButtonProps> = ({
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  disabled = false,
  className = '',
  children,
  type = 'button',
  ...props
}) => {
  const baseClasses =
    'rounded font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-offset-gray-900 disabled:cursor-not-allowed';
  const widthClass = fullWidth ? 'w-full' : '';

  return (
    <button
      type={type}
      disabled={disabled}
      className={`${baseClasses} ${variantClasses[variant]} ${sizeClasses[size]} ${widthClass} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
};
