import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { LoginViewModel } from './LoginViewModel';
import { authApi } from '../../api/authApi';
import { AuthProvider } from '../../context/AuthContext';
import type { AuthResponse } from '../../models/User';

// Mock the API
vi.mock('../../api/authApi', () => ({
  authApi: {
    login: vi.fn(),
  },
}));

// Mock react-router-dom navigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return ({ children }: { children: React.ReactNode }) => (
    <BrowserRouter>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>{children}</AuthProvider>
      </QueryClientProvider>
    </BrowserRouter>
  );
};

describe('LoginViewModel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('should initialize with empty email and password', () => {
    const { result } = renderHook(() => LoginViewModel(), {
      wrapper: createWrapper(),
    });

    expect(result.current.email).toBe('');
    expect(result.current.password).toBe('');
    expect(result.current.errors).toEqual({});
    expect(result.current.isSubmitting).toBe(false);
  });

  it('should update email when setEmail is called', () => {
    const { result } = renderHook(() => LoginViewModel(), {
      wrapper: createWrapper(),
    });

    act(() => {
      result.current.setEmail('test@example.com');
    });

    expect(result.current.email).toBe('test@example.com');
  });

  it('should update password when setPassword is called', () => {
    const { result } = renderHook(() => LoginViewModel(), {
      wrapper: createWrapper(),
    });

    act(() => {
      result.current.setPassword('password123');
    });

    expect(result.current.password).toBe('password123');
  });

  it('should validate email is required', () => {
    const { result } = renderHook(() => LoginViewModel(), {
      wrapper: createWrapper(),
    });

    act(() => {
      result.current.setPassword('password123');
      result.current.handleSubmit({
        preventDefault: vi.fn(),
      } as unknown as React.FormEvent);
    });

    expect(result.current.errors.email).toBe('Email is required');
  });

  it('should validate password is required', () => {
    const { result } = renderHook(() => LoginViewModel(), {
      wrapper: createWrapper(),
    });

    act(() => {
      result.current.setEmail('test@example.com');
      result.current.handleSubmit({ preventDefault: vi.fn() } as unknown as React.FormEvent);
    });

    expect(result.current.errors.password).toBe('Password is required');
  });

  it('should not submit when validation fails', () => {
    const { result } = renderHook(() => LoginViewModel(), {
      wrapper: createWrapper(),
    });

    act(() => {
      result.current.handleSubmit({ preventDefault: vi.fn() } as unknown as React.FormEvent);
    });

    expect(authApi.login).not.toHaveBeenCalled();
    expect(result.current.errors).toEqual({
      email: 'Email is required',
      password: 'Password is required',
    });
  });

  it('should submit login request when form is valid', async () => {
    const mockResponse = {
      token: 'mock-token',
      user: {
        id: '123',
        email: 'test@example.com',
        firstName: 'Test',
        lastName: 'User',
      },
    };

    vi.mocked(authApi.login).mockResolvedValue(mockResponse);

    const { result } = renderHook(() => LoginViewModel(), {
      wrapper: createWrapper(),
    });

    act(() => {
      result.current.setEmail('test@example.com');
      result.current.setPassword('password123');
    });

    await act(async () => {
      result.current.handleSubmit({ preventDefault: vi.fn() } as unknown as React.FormEvent);
    });

    await waitFor(() => {
      expect(authApi.login).toHaveBeenCalled();
      const callArgs = vi.mocked(authApi.login).mock.calls[0];
      expect(callArgs[0]).toEqual({
        email: 'test@example.com',
        password: 'password123',
      });
    });
  });

  it('should navigate to /customers on successful login', async () => {
    const mockResponse = {
      token: 'mock-token',
      user: {
        id: '123',
        email: 'test@example.com',
        firstName: 'Test',
        lastName: 'User',
      },
    };

    vi.mocked(authApi.login).mockResolvedValue(mockResponse);

    const { result } = renderHook(() => LoginViewModel(), {
      wrapper: createWrapper(),
    });

    act(() => {
      result.current.setEmail('test@example.com');
      result.current.setPassword('password123');
    });

    await act(async () => {
      result.current.handleSubmit({ preventDefault: vi.fn() } as unknown as React.FormEvent);
    });

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/customers');
    });
  });

  it('should set error message on login failure with API error message', async () => {
    const errorMessage = 'Invalid credentials';
    vi.mocked(authApi.login).mockRejectedValue({
      response: {
        data: {
          message: errorMessage,
        },
      },
    });

    const { result } = renderHook(() => LoginViewModel(), {
      wrapper: createWrapper(),
    });

    act(() => {
      result.current.setEmail('test@example.com');
      result.current.setPassword('wrongpassword');
    });

    await act(async () => {
      result.current.handleSubmit({ preventDefault: vi.fn() } as unknown as React.FormEvent);
    });

    await waitFor(() => {
      expect(result.current.errors.submit).toBe(errorMessage);
    });
  });

  it('should set generic error message on login failure without API message', async () => {
    vi.mocked(authApi.login).mockRejectedValue(new Error('Network error'));

    const { result } = renderHook(() => LoginViewModel(), {
      wrapper: createWrapper(),
    });

    act(() => {
      result.current.setEmail('test@example.com');
      result.current.setPassword('password123');
    });

    await act(async () => {
      result.current.handleSubmit({ preventDefault: vi.fn() } as unknown as React.FormEvent);
    });

    await waitFor(() => {
      expect(result.current.errors.submit).toBe('Login failed');
    });
  });

  it('should set isSubmitting to true while login is in progress', async () => {
    let resolveLogin: (value: AuthResponse) => void;
    const loginPromise = new Promise<AuthResponse>(resolve => {
      resolveLogin = resolve;
    });

    vi.mocked(authApi.login).mockReturnValue(loginPromise);

    const { result } = renderHook(() => LoginViewModel(), {
      wrapper: createWrapper(),
    });

    act(() => {
      result.current.setEmail('test@example.com');
      result.current.setPassword('password123');
    });

    act(() => {
      result.current.handleSubmit({ preventDefault: vi.fn() } as unknown as React.FormEvent);
    });

    await waitFor(() => {
      expect(result.current.isSubmitting).toBe(true);
    });

    act(() => {
      resolveLogin!({
        token: 'mock-token',
        user: {
          id: '123',
          email: 'test@example.com',
          firstName: 'Test',
          lastName: 'User',
        },
      });
    });

    await waitFor(() => {
      expect(result.current.isSubmitting).toBe(false);
    });
  });

  it('should clear errors when starting new submission with valid data', async () => {
    const { result } = renderHook(() => LoginViewModel(), {
      wrapper: createWrapper(),
    });

    // First submission with validation error
    act(() => {
      result.current.handleSubmit({ preventDefault: vi.fn() } as unknown as React.FormEvent);
    });

    expect(result.current.errors).toEqual({
      email: 'Email is required',
      password: 'Password is required',
    });

    // Second submission with valid data
    vi.mocked(authApi.login).mockResolvedValue({
      token: 'mock-token',
      user: {
        id: '123',
        email: 'test@example.com',
        firstName: 'Test',
        lastName: 'User',
      },
    });

    act(() => {
      result.current.setEmail('test@example.com');
      result.current.setPassword('password123');
    });

    await act(async () => {
      result.current.handleSubmit({ preventDefault: vi.fn() } as unknown as React.FormEvent);
    });

    await waitFor(() => {
      expect(result.current.errors).toEqual({});
    });
  });
});
