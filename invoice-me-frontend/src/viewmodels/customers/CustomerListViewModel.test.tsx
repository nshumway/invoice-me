import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { CustomerListViewModel } from './CustomerListViewModel';
import { customerApi } from '../../api/customerApi';
import type { CustomerListItem } from '../../models/Customer';

// Mock the API
vi.mock('../../api/customerApi', () => ({
  customerApi: {
    listAll: vi.fn(),
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
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    </BrowserRouter>
  );
};

describe('CustomerListViewModel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockCustomers: CustomerListItem[] = [
    {
      id: '1',
      companyName: 'Acme Corp',
      email: 'contact@acme.com',
      totalOutstanding: '$1,234.56',
    },
    {
      id: '2',
      companyName: 'Tech Solutions',
      email: 'info@techsolutions.com',
      totalOutstanding: '$0.00',
    },
  ];

  it('should initialize with loading state', () => {
    vi.mocked(customerApi.listAll).mockImplementation(
      () => new Promise(() => {}) // Never resolves
    );

    const { result } = renderHook(() => CustomerListViewModel(), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(true);
    expect(result.current.isError).toBe(false);
    expect(result.current.customers).toBeUndefined();
  });

  it('should load customers successfully', async () => {
    vi.mocked(customerApi.listAll).mockResolvedValue(mockCustomers);

    const { result } = renderHook(() => CustomerListViewModel(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.customers).toEqual(mockCustomers);
    expect(result.current.isError).toBe(false);
    expect(result.current.errorMessage).toBeNull();
  });

  it('should handle empty customer list', async () => {
    vi.mocked(customerApi.listAll).mockResolvedValue([]);

    const { result } = renderHook(() => CustomerListViewModel(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.customers).toEqual([]);
    expect(result.current.customers?.length).toBe(0);
  });

  it('should handle error when loading customers fails', async () => {
    const errorMessage = 'Network error';
    vi.mocked(customerApi.listAll).mockRejectedValue(new Error(errorMessage));

    const { result } = renderHook(() => CustomerListViewModel(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.isLoading).toBe(false);
    expect(result.current.errorMessage).toBe(errorMessage);
  });

  it('should provide default error message for unknown error types', async () => {
    vi.mocked(customerApi.listAll).mockRejectedValue('Unknown error');

    const { result } = renderHook(() => CustomerListViewModel(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.errorMessage).toBe('Failed to load customers');
  });

  it('should navigate to create page when handleCreateNew is called', () => {
    vi.mocked(customerApi.listAll).mockResolvedValue(mockCustomers);

    const { result } = renderHook(() => CustomerListViewModel(), {
      wrapper: createWrapper(),
    });

    result.current.handleCreateNew();

    expect(mockNavigate).toHaveBeenCalledWith('/customers/new');
  });

  it('should navigate to customer detail page when handleRowClick is called', () => {
    vi.mocked(customerApi.listAll).mockResolvedValue(mockCustomers);

    const { result } = renderHook(() => CustomerListViewModel(), {
      wrapper: createWrapper(),
    });

    const customerId = '123';
    result.current.handleRowClick(customerId);

    expect(mockNavigate).toHaveBeenCalledWith(`/customers/${customerId}`);
  });

  it('should navigate to correct customer detail page for multiple clicks', () => {
    vi.mocked(customerApi.listAll).mockResolvedValue(mockCustomers);

    const { result } = renderHook(() => CustomerListViewModel(), {
      wrapper: createWrapper(),
    });

    result.current.handleRowClick('customer-1');
    expect(mockNavigate).toHaveBeenCalledWith('/customers/customer-1');

    result.current.handleRowClick('customer-2');
    expect(mockNavigate).toHaveBeenCalledWith('/customers/customer-2');

    expect(mockNavigate).toHaveBeenCalledTimes(2);
  });

  it('should use correct query key for React Query', async () => {
    vi.mocked(customerApi.listAll).mockResolvedValue(mockCustomers);

    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <BrowserRouter>
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
      </BrowserRouter>
    );

    renderHook(() => CustomerListViewModel(), { wrapper });

    await waitFor(() => {
      const queryData = queryClient.getQueryData(['customers', 'list']);
      expect(queryData).toEqual(mockCustomers);
    });
  });

  it('should refetch customers when query is invalidated', async () => {
    const initialCustomers = [mockCustomers[0]];
    const updatedCustomers = mockCustomers;

    vi.mocked(customerApi.listAll).mockResolvedValueOnce(initialCustomers);

    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <BrowserRouter>
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
      </BrowserRouter>
    );

    const { result } = renderHook(() => CustomerListViewModel(), { wrapper });

    await waitFor(() => {
      expect(result.current.customers).toEqual(initialCustomers);
    });

    // Simulate data update and invalidate
    vi.mocked(customerApi.listAll).mockResolvedValueOnce(updatedCustomers);
    await queryClient.invalidateQueries({ queryKey: ['customers', 'list'] });

    await waitFor(() => {
      expect(result.current.customers).toEqual(updatedCustomers);
    });
  });
});
