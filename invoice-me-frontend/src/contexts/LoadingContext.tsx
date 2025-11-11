import React, { useState, useCallback, useRef, useEffect } from 'react';
import { Toast } from '../components/shared/Toast';
import { LoadingOverlay } from '../components/shared/LoadingOverlay';
import { setupLoadingInterceptors } from '../api/loadingInterceptors';
import { LoadingContext } from './LoadingContextDefinition';

export const LoadingProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [pendingRequests, setPendingRequests] = useState(0);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const pendingCountRef = useRef(0);

  const incrementPending = useCallback(() => {
    pendingCountRef.current += 1;
    setPendingRequests(pendingCountRef.current);
  }, []);

  const decrementPending = useCallback(() => {
    pendingCountRef.current = Math.max(0, pendingCountRef.current - 1);
    setPendingRequests(pendingCountRef.current);
  }, []);

  const showToast = useCallback((message: string) => {
    setToastMessage(message);
  }, []);

  const hideToast = useCallback(() => {
    setToastMessage(null);
  }, []);

  const updateToast = useCallback((message: string) => {
    setToastMessage(message);
  }, []);

  // Setup axios interceptors
  useEffect(() => {
    const cleanup = setupLoadingInterceptors({
      onRequestStart: incrementPending,
      onRequestEnd: decrementPending,
      showToast,
      hideToast,
      updateToast,
    });

    return cleanup;
  }, [incrementPending, decrementPending, showToast, hideToast, updateToast]);

  return (
    <LoadingContext.Provider
      value={{
        incrementPending,
        decrementPending,
        showToast,
        hideToast,
        updateToast,
      }}
    >
      {children}
      {pendingRequests > 0 && <LoadingOverlay />}
      {toastMessage && <Toast message={toastMessage} variant="info" onClose={hideToast} />}
    </LoadingContext.Provider>
  );
};
