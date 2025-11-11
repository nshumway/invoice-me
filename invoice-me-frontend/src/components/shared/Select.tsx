import React from 'react';

interface SelectOption {
  value: string;
  label: string;
}

interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
  helperText?: string;
  required?: boolean;
  options: SelectOption[];
  placeholder?: string;
}

export const Select: React.FC<SelectProps> = ({
  label,
  error,
  helperText,
  required = false,
  options,
  placeholder,
  className = '',
  id,
  ...props
}) => {
  const selectId = id || `select-${label?.replace(/\s+/g, '-').toLowerCase()}`;

  const selectClasses = `
    w-full bg-gray-700 text-gray-100 border rounded px-3 py-2
    focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-500 focus-visible:border-transparent
    disabled:opacity-50 disabled:cursor-not-allowed
    ${error ? 'border-error-500 focus-visible:ring-error-500' : 'border-gray-600'}
    ${className}
  `.trim();

  return (
    <div className="w-full">
      {label && (
        <label htmlFor={selectId} className="block text-sm font-medium mb-2 text-gray-200">
          {label}
          {required && <span className="text-error-500 ml-1">*</span>}
        </label>
      )}
      <select id={selectId} className={selectClasses} {...props}>
        {placeholder && (
          <option value="" disabled>
            {placeholder}
          </option>
        )}
        {options.map(option => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error && <p className="text-error-400 text-sm mt-1">{error}</p>}
      {!error && helperText && <p className="text-gray-400 text-sm mt-1">{helperText}</p>}
    </div>
  );
};
