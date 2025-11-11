import React from 'react';
import { Button } from './shared';

interface ConfirmDialogProps {
  isOpen: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
  isLoading?: boolean;
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  isOpen,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  onConfirm,
  onCancel,
  isLoading = false,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center z-50 p-4">
      <div className="bg-gray-800 rounded-lg shadow-xl max-w-md w-full border border-gray-700">
        <div className="p-6">
          <h2 className="text-xl font-bold mb-4 text-gray-100">{title}</h2>
          <p className="text-gray-300 mb-6">{message}</p>
          <div className="flex flex-col sm:flex-row gap-3 justify-end">
            <Button type="button" onClick={onCancel} disabled={isLoading} variant="secondary">
              {cancelLabel}
            </Button>
            <Button type="button" onClick={onConfirm} disabled={isLoading} variant="danger">
              {isLoading ? 'Processing...' : confirmLabel}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};
