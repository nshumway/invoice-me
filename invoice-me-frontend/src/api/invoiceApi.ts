import { apiClient } from './client';
import type { ApiResponse } from '../models/ApiResponse';
import type {
  Invoice,
  InvoiceListItem,
  CreateInvoiceRequest,
  UpdateInvoiceRequest,
} from '../models/Invoice';

export const invoiceApi = {
  // POST /api/invoices
  create: async (request: CreateInvoiceRequest): Promise<Invoice> => {
    const response = await apiClient.post<ApiResponse<Invoice>>('/invoices', request);
    return response.data.data;
  },

  // GET /api/invoices
  listAll: async (customerId?: string): Promise<InvoiceListItem[]> => {
    const url = customerId ? `/invoices?customerId=${customerId}` : '/invoices';
    const response = await apiClient.get<ApiResponse<InvoiceListItem[]>>(url);
    return response.data.data;
  },

  // GET /api/invoices/:id
  getById: async (id: string): Promise<Invoice> => {
    const response = await apiClient.get<ApiResponse<Invoice>>(`/invoices/${id}`);
    return response.data.data;
  },

  // PUT /api/invoices/:id
  update: async (id: string, request: UpdateInvoiceRequest): Promise<Invoice> => {
    const response = await apiClient.put<ApiResponse<Invoice>>(`/invoices/${id}`, request);
    return response.data.data;
  },

  // POST /api/invoices/:id/send
  markAsSent: async (id: string, version: number): Promise<Invoice> => {
    const response = await apiClient.post<ApiResponse<Invoice>>(`/invoices/${id}/send`, {
      version,
    });
    return response.data.data;
  },

  // DELETE /api/invoices/:id?version=X
  delete: async (id: string, version: number): Promise<void> => {
    await apiClient.delete(`/invoices/${id}?version=${version}`);
  },
};
