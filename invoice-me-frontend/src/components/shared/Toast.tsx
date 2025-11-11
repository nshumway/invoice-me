import React, { useEffect } from 'react';

export interface ToastProps {
  message: string;
  variant?: 'info' | 'success' | 'warning' | 'error';
  onClose?: () => void;
  duration?: number | null; // null means no auto-dismiss
}

const variantClasses = {
  info: 'bg-primary-900/95 border-primary-700/50 text-primary-100',
  success: 'bg-success-900/95 border-success-700/50 text-success-100',
  warning: 'bg-warning-900/95 border-warning-700/50 text-warning-100',
  error: 'bg-error-900/95 border-error-700/50 text-error-100',
};

export const Toast: React.FC<ToastProps> = ({
  message,
  variant = 'info',
  onClose,
  duration = null,
}) => {
  useEffect(() => {
    if (duration && onClose) {
      const timer = setTimeout(onClose, duration);
      return () => clearTimeout(timer);
    }
  }, [duration, onClose]);

  return (
    <div
      className={`fixed top-4 right-4 z-50 max-w-sm w-full sm:w-auto shadow-lg rounded-lg border p-4 flex items-start gap-3 animate-slide-in ${variantClasses[variant]}`}
      role="alert"
      aria-live="polite"
      aria-atomic="true"
    >
      {/* Loading spinner icon */}
      <div className="flex-shrink-0">
        <svg
          className="animate-spin h-5 w-5"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
          />
          <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          />
        </svg>
      </div>

      {/* Message */}
      <div className="flex-1">
        <p className="text-sm font-medium">{message}</p>
      </div>

      {/* Close button */}
      {onClose && (
        <button
          onClick={onClose}
          className="flex-shrink-0 hover:opacity-70 transition-opacity"
          aria-label="Close"
        >
          <svg
            className="h-5 w-5"
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fillRule="evenodd"
              d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
              clipRule="evenodd"
            />
          </svg>
        </button>
      )}
    </div>
  );
};
