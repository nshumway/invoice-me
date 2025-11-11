import React from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
  required?: boolean;
}

export const Input: React.FC<InputProps> = ({
  label,
  error,
  helperText,
  required = false,
  className = '',
  id,
  ...props
}) => {
  const inputId = id || `input-${label?.replace(/\s+/g, '-').toLowerCase()}`;

  const inputClasses = `
    w-full bg-gray-700 text-gray-100 border rounded px-3 py-2
    focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent
    disabled:opacity-50 disabled:cursor-not-allowed
    ${error ? 'border-error-500 focus:ring-error-500' : 'border-gray-600'}
    ${className}
  `.trim();

  return (
    <div className="w-full">
      {label && (
        <label htmlFor={inputId} className="block text-sm font-medium mb-2 text-gray-200">
          {label}
          {required && <span className="text-error-500 ml-1">*</span>}
        </label>
      )}
      <input id={inputId} className={inputClasses} {...props} />
      {error && <p className="text-error-400 text-sm mt-1">{error}</p>}
      {!error && helperText && <p className="text-gray-400 text-sm mt-1">{helperText}</p>}
    </div>
  );
};
