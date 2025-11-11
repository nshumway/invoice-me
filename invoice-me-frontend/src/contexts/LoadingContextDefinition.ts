import { createContext } from 'react';

export interface LoadingContextType {
  incrementPending: () => void;
  decrementPending: () => void;
  showToast: (message: string) => void;
  hideToast: () => void;
  updateToast: (message: string) => void;
}

export const LoadingContext = createContext<LoadingContextType | undefined>(undefined);
