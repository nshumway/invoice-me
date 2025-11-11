import { apiClient } from './client';
import type { InternalAxiosRequestConfig } from 'axios';

interface RequestMetadata {
  startTime: number;
  timeoutIds: number[];
}

declare module 'axios' {
  export interface InternalAxiosRequestConfig {
    metadata?: RequestMetadata;
  }
}

interface LoadingCallbacks {
  onRequestStart: () => void;
  onRequestEnd: () => void;
  showToast: (message: string) => void;
  hideToast: () => void;
  updateToast: (message: string) => void;
}

export const setupLoadingInterceptors = (callbacks: LoadingCallbacks) => {
  const { onRequestStart, onRequestEnd, showToast, hideToast, updateToast } = callbacks;

  // Request interceptor
  const requestInterceptor = apiClient.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      onRequestStart();

      // Track request start time and set up progressive timeouts
      config.metadata = {
        startTime: Date.now(),
        timeoutIds: [],
      };

      // 5 seconds: Show initial toast
      const timeout1 = window.setTimeout(() => {
        showToast('Server is starting up, please wait...');
      }, 5000);

      // 15 seconds: Update toast
      const timeout2 = window.setTimeout(() => {
        updateToast('Still loading... The server is waking up on free hosting.');
      }, 15000);

      // 30 seconds: Final update
      const timeout3 = window.setTimeout(() => {
        updateToast('Almost there... Free tier servers can take up to 60 seconds to start.');
      }, 30000);

      config.metadata.timeoutIds = [timeout1, timeout2, timeout3];

      return config;
    },
    error => {
      onRequestEnd();
      return Promise.reject(error);
    }
  );

  // Response interceptor
  const responseInterceptor = apiClient.interceptors.response.use(
    response => {
      // Clear all timeouts
      if (response.config.metadata?.timeoutIds) {
        response.config.metadata.timeoutIds.forEach(id => window.clearTimeout(id));
      }

      onRequestEnd();
      hideToast();
      return response;
    },
    error => {
      // Clear all timeouts
      if (error.config?.metadata?.timeoutIds) {
        error.config.metadata.timeoutIds.forEach((id: number) => window.clearTimeout(id));
      }

      onRequestEnd();
      hideToast();
      return Promise.reject(error);
    }
  );

  // Return cleanup function
  return () => {
    apiClient.interceptors.request.eject(requestInterceptor);
    apiClient.interceptors.response.eject(responseInterceptor);
  };
};
