import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { InvoiceListViewModel } from './InvoiceListViewModel';
import { invoiceApi } from '../../api/invoiceApi';
import type { InvoiceListItem } from '../../models/Invoice';

vi.mock('../../api/invoiceApi');

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  return ({ children }: { children: React.ReactNode }) => (
    <BrowserRouter>
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    </BrowserRouter>
  );
};

describe('InvoiceListViewModel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should load invoices successfully', async () => {
    const mockInvoices: InvoiceListItem[] = [
      {
        id: '1',
        invoiceNumber: 'INV-001',
        customerName: 'Customer 1',
        invoiceDate: '2025-11-09',
        status: 'DRAFT',
        total: 100,
        amountPaid: 0,
      },
    ];

    vi.mocked(invoiceApi.listAll).mockResolvedValue(mockInvoices);

    const { result } = renderHook(() => InvoiceListViewModel(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.invoices).toEqual(mockInvoices);
    expect(result.current.isError).toBe(false);
  });

  it('should handle error when loading invoices fails', async () => {
    vi.mocked(invoiceApi.listAll).mockRejectedValue(new Error('Network error'));

    const { result } = renderHook(() => InvoiceListViewModel(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.isError).toBe(true);
    expect(result.current.errorMessage).toBe('Network error');
  });
});
